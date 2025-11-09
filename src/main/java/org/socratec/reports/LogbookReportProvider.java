package org.socratec.reports;

import org.apache.poi.ss.util.WorkbookUtil;
import org.socratec.reports.model.LogbookReportSection;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.model.DeviceUtil;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.reports.common.ReportUtils;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;
import org.socratec.model.LogbookEntry;
import org.socratec.model.LogbookEntryType;

import jakarta.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class LogbookReportProvider {

    private final Config config;
    private final ReportUtils reportUtils;
    private final Storage storage;

    private record SumReport(
            double totalDistance,
            double totalDuration,
            double privateDistance,
            double businessDistance,
            double privateDuration,
            double businessDuration
    ) { }

    @Inject
    public LogbookReportProvider(Config config, ReportUtils reportUtils, Storage storage) {
        this.config = config;
        this.reportUtils = reportUtils;
        this.storage = storage;
    }

    public Collection<LogbookEntry> getObjects(
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws StorageException {
        reportUtils.checkPeriodLimit(from, to);

        ArrayList<LogbookEntry> result = new ArrayList<>();
        for (Device device : DeviceUtil.getAccessibleDevices(storage, userId, deviceIds, groupIds)) {
            Collection<LogbookEntry> logbookEntries = storage.getObjects(LogbookEntry.class, new Request(
                    new Columns.All(),
                    new Condition.And(
                            new Condition.Equals("deviceId", device.getId()),
                            new Condition.Between("startTime", "from", from, "to", to)
                    ),
                    new Order("startTime")
            ));
            result.addAll(logbookEntries);
        }
        return result;
    }

    public void getExcel(OutputStream outputStream,
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws StorageException, IOException {
        reportUtils.checkPeriodLimit(from, to);

        ArrayList<LogbookReportSection> devicesLogbook = new ArrayList<>();
        ArrayList<String> sheetNames = new ArrayList<>();
        for (Device device: DeviceUtil.getAccessibleDevices(storage, userId, deviceIds, groupIds)) {
            Collection<LogbookEntry> logbookEntries = storage.getObjects(LogbookEntry.class, new Request(
                    new Columns.All(),
                    new Condition.And(
                            new Condition.Equals("deviceId", device.getId()),
                            new Condition.Between("startTime", "from", from, "to", to)
                    ),
                    new Order("startTime")
            ));
            LogbookReportSection deviceLogbook = new LogbookReportSection();
            deviceLogbook.setDeviceName(device.getName());
            sheetNames.add(WorkbookUtil.createSafeSheetName(deviceLogbook.getDeviceName()));
            if (device.getGroupId() > 0) {
                Group group = storage.getObject(Group.class, new Request(
                        new Columns.All(), new Condition.Equals("id", device.getGroupId())));
                if (group != null) {
                    deviceLogbook.setGroupName(group.getName());
                }
            }
            deviceLogbook.setObjects(logbookEntries);
            var sumReport = calculateAllSums(logbookEntries);
            deviceLogbook.setTotalDistance(sumReport.totalDistance);
            deviceLogbook.setPrivateDistance(sumReport.privateDistance);
            deviceLogbook.setBusinessDistance(sumReport.businessDistance);
            deviceLogbook.setTotalDuration(sumReport.totalDuration);
            deviceLogbook.setPrivateDuration(sumReport.privateDuration);
            deviceLogbook.setBusinessDuration(sumReport.businessDuration);
            devicesLogbook.add(deviceLogbook);
        }

        File file = Paths.get(config.getString(Keys.TEMPLATES_ROOT), "export", "logbook.xlsx").toFile();
        try (InputStream inputStream = new FileInputStream(file)) {
            var context = reportUtils.initializeContext(userId);
            context.putVar("devices", devicesLogbook);
            context.putVar("sheetNames", sheetNames);
            context.putVar("from", from);
            context.putVar("to", to);
            reportUtils.processTemplateWithSheets(inputStream, outputStream, context);
        }
    }

    private SumReport calculateAllSums(Collection<LogbookEntry> logbookEntries) {
        double totalDistance = 0;
        double totalDuration = 0;
        double privateDistance = 0;
        double businessDistance = 0;
        double privateDuration = 0;
        double businessDuration = 0;

        for (LogbookEntry entry : logbookEntries) {
            double distance = entry.getDistance();
            double duration = entry.getDuration();

            // Always add to totals
            totalDistance += distance;
            totalDuration += duration;

            // Add to type-specific totals (ignore NONE)
            if (entry.getType() == LogbookEntryType.BUSINESS) {
                businessDistance += distance;
                businessDuration += duration;
            } else if (entry.getType() == LogbookEntryType.PRIVATE) {
                privateDistance += distance;
                privateDuration += duration;
            }
            // NONE entries are ignored for type-specific calculations
        }

        return new SumReport(
            totalDistance, totalDuration,
            privateDistance, businessDistance,
            privateDuration, businessDuration
        );
    }
}
