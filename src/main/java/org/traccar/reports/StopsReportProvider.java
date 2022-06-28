/*
 * Copyright 2017 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.reports;

import org.apache.poi.ss.util.WorkbookUtil;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.reports.common.ReportUtils;
import org.traccar.reports.model.DeviceReportSection;
import org.traccar.reports.model.StopReportItem;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class StopsReportProvider {

    private final Config config;
    private final ReportUtils reportUtils;
    private final Storage storage;

    @Inject
    public StopsReportProvider(Config config, ReportUtils reportUtils, Storage storage) {
        this.config = config;
        this.reportUtils = reportUtils;
        this.storage = storage;
    }

    private Collection<StopReportItem> detectStops(Device device, Date from, Date to) throws StorageException {
        boolean ignoreOdometer = config.getBoolean(Keys.REPORT_IGNORE_ODOMETER);
        var positions = PositionUtil.getPositions(storage, device.getId(), from, to);
        return reportUtils.detectTripsAndStops(device, positions, ignoreOdometer, StopReportItem.class);
    }

    public Collection<StopReportItem> getObjects(
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws StorageException {
        reportUtils.checkPeriodLimit(from, to);

        ArrayList<StopReportItem> result = new ArrayList<>();
        for (Device device: reportUtils.getAccessibleDevices(userId, deviceIds, groupIds)) {
            result.addAll(detectStops(device, from, to));
        }
        return result;
    }

    public void getExcel(
            OutputStream outputStream, long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws StorageException, IOException {
        reportUtils.checkPeriodLimit(from, to);

        ArrayList<DeviceReportSection> devicesStops = new ArrayList<>();
        ArrayList<String> sheetNames = new ArrayList<>();
        for (Device device: reportUtils.getAccessibleDevices(userId, deviceIds, groupIds)) {
            Collection<StopReportItem> stops = detectStops(device, from, to);
            DeviceReportSection deviceStops = new DeviceReportSection();
            deviceStops.setDeviceName(device.getName());
            sheetNames.add(WorkbookUtil.createSafeSheetName(deviceStops.getDeviceName()));
            if (device.getGroupId() > 0) {
                Group group = storage.getObject(Group.class, new Request(
                        new Columns.All(), new Condition.Equals("id", "id", device.getGroupId())));
                if (group != null) {
                    deviceStops.setGroupName(group.getName());
                }
            }
            deviceStops.setObjects(stops);
            devicesStops.add(deviceStops);
        }

        File file = Paths.get(config.getString(Keys.TEMPLATES_ROOT), "export", "stops.xlsx").toFile();
        try (InputStream inputStream = new FileInputStream(file)) {
            var context = reportUtils.initializeContext(userId);
            context.putVar("devices", devicesStops);
            context.putVar("sheetNames", sheetNames);
            context.putVar("from", from);
            context.putVar("to", to);
            reportUtils.processTemplateWithSheets(inputStream, outputStream, context);
        }
    }

}
