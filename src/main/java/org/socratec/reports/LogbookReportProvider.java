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

import com.itextpdf.html2pdf.HtmlConverter;
import jakarta.inject.Inject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
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

        // Reuse the common data preparation logic
        ArrayList<LogbookReportSection> devicesLogbook = createLogbookReportSections(
                userId, deviceIds, groupIds, from, to);

        ArrayList<String> sheetNames = new ArrayList<>();
        for (LogbookReportSection deviceLogbook : devicesLogbook) {
            sheetNames.add(WorkbookUtil.createSafeSheetName(deviceLogbook.getDeviceName()));
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

    public void getPdf(OutputStream outputStream,
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws StorageException, IOException {
        reportUtils.checkPeriodLimit(from, to);

        // Reuse the same data preparation logic
        ArrayList<LogbookReportSection> devicesLogbook = createLogbookReportSections(
                userId, deviceIds, groupIds, from, to);

        // Initialize Velocity Engine
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loaders", "file");
        velocityEngine.setProperty("resource.loader.file.class",
                "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        velocityEngine.setProperty("resource.loader.file.path",
                config.getString(Keys.TEMPLATES_ROOT) + "/export");
        velocityEngine.init();

        // Create Velocity Context using reportUtils for consistency
        var jxlsContext = reportUtils.initializeContext(userId);
        VelocityContext velocityContext = new VelocityContext();

        // Transfer relevant variables from JXLS context to Velocity context
        velocityContext.put("devices", devicesLogbook);
        velocityContext.put("from", from);
        velocityContext.put("to", to);
        velocityContext.put("distanceUnit", jxlsContext.getVar("distanceUnit"));
        velocityContext.put("speedUnit", jxlsContext.getVar("speedUnit"));
        velocityContext.put("volumeUnit", jxlsContext.getVar("volumeUnit"));
        velocityContext.put("timezone", jxlsContext.getVar("timezone"));
        velocityContext.put("locale", jxlsContext.getVar("locale"));
        velocityContext.put("dateTool", jxlsContext.getVar("dateTool"));
        velocityContext.put("numberTool", jxlsContext.getVar("numberTool"));

        // Process template
        StringWriter writer = new StringWriter();
        velocityEngine.getTemplate("logbook.vm").merge(velocityContext, writer);
        String htmlContent = writer.toString();

        // Convert HTML to PDF
        HtmlConverter.convertToPdf(htmlContent, outputStream);
    }

    private ArrayList<LogbookReportSection> createLogbookReportSections(
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws StorageException {
        ArrayList<LogbookReportSection> devicesLogbook = new ArrayList<>();
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
        return devicesLogbook;
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
