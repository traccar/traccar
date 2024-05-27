package org.traccar.reports;

import org.jxls.util.JxlsHelper;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.model.DeviceUtil;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.reports.common.ReportUtils;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DeviceReportProvider {

    private final Config config;
    private final ReportUtils reportUtils;
    private final Storage storage;

    @Inject
    public DeviceReportProvider(Config config, ReportUtils reportUtils, Storage storage) {
        this.config = config;
        this.reportUtils = reportUtils;
        this.storage = storage;
    }

    public void getExcel(OutputStream outputStream,
            long userId, Collection<Long> deviceIds) throws StorageException, IOException {
        Collection<Device> result = new ArrayList<>();
        for (Device device : DeviceUtil.getAccessibleDevices(storage, userId, deviceIds, new ArrayList<>())) {
            if (device.getGroupId() > 0) {
                Group group = storage.getObject(Group.class, new Request(
                        new Columns.All(),
                        new Condition.Equals("id", device.getGroupId())));
                device.setGroupName(group.getName());
            }

            List<Position> latestPosition = storage.getObjects(Position.class, new Request(
                    new Columns.All(),
                    new Condition.Equals("deviceId", device.getId()),
                    new Order("fixTime", true, 1)));

            if (!latestPosition.isEmpty()) {
                device.setDeviceTime(latestPosition.get(0).getDeviceTime());
                device.setLatitude(latestPosition.get(0).getLatitude());
                device.setLongitude(latestPosition.get(0).getLongitude());
                device.setAddress(latestPosition.get(0).getAddress());

                if (device.hasAttribute("serial_number"))
                    device.setSerialNumber(device.getString("serial_number"));

                if (device.hasAttribute("serialNumber"))
                    device.setSerialNumber(device.getString("serialNumber"));

                if (device.hasAttribute("imei"))
                    device.setImei(device.getString("imei"));
            }
            result.add(device);
        }

        File file = Paths.get(config.getString(Keys.TEMPLATES_ROOT), "export", "device.xlsx").toFile();
        try (InputStream inputStream = new FileInputStream(file)) {
            var context = reportUtils.initializeContext(userId);
            context.putVar("devices", result);
            JxlsHelper.getInstance().setUseFastFormulaProcessor(false)
                    .processTemplate(inputStream, outputStream, context);
        }
    }
}
