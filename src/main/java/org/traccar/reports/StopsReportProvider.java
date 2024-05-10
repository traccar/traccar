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
import org.traccar.helper.model.DeviceUtil;
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

import jakarta.inject.Inject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

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

    public Collection<StopReportItem> getObjects(
            DeviceGroupQuery deviceGroupQuery) throws StorageException {
        reportUtils.checkPeriodLimit(deviceGroupQuery.getFrom(), deviceGroupQuery.getTo());

        ArrayList<StopReportItem> result = new ArrayList<>();
        for (Device device : DeviceUtil.getAccessibleDevices(storage, deviceGroupQuery.getUserId(),
                deviceGroupQuery.getDeviceIds(), deviceGroupQuery.getGroupIds())) {
            result.addAll(reportUtils.detectTripsAndStops(device, deviceGroupQuery.getFrom(), deviceGroupQuery.getTo(),
                    StopReportItem.class));
        }
        return result;
    }

    public void getExcel(
            OutputStream outputStream,
            DeviceGroupQuery deviceGroupQuery) throws StorageException, IOException {
        reportUtils.checkPeriodLimit(deviceGroupQuery.getFrom(), deviceGroupQuery.getTo());

        ArrayList<DeviceReportSection> devicesStops = new ArrayList<>();
        ArrayList<String> sheetNames = new ArrayList<>();
        for (Device device : DeviceUtil.getAccessibleDevices(storage, deviceGroupQuery.getUserId(),
                deviceGroupQuery.getDeviceIds(), deviceGroupQuery.getGroupIds())) {
            Collection<StopReportItem> stops = reportUtils.detectTripsAndStops(device, deviceGroupQuery.getFrom(),
                    deviceGroupQuery.getTo(), StopReportItem.class);
            DeviceReportSection deviceStops = new DeviceReportSection();
            deviceStops.setDeviceName(device.getName());
            sheetNames.add(WorkbookUtil.createSafeSheetName(deviceStops.getDeviceName()));
            if (device.getGroupId() > 0) {
                Group group = storage.getObject(Group.class, new Request(
                        new Columns.All(), new Condition.Equals("id", device.getGroupId())));
                if (group != null) {
                    deviceStops.setGroupName(group.getName());
                }
            }
            deviceStops.setObjects(stops);
            devicesStops.add(deviceStops);
        }

        File file = Paths.get(config.getString(Keys.TEMPLATES_ROOT), "export", "stops.xlsx").toFile();
        try (InputStream inputStream = new FileInputStream(file)) {
            var context = reportUtils.initializeContext(deviceGroupQuery.getUserId());
            context.putVar("devices", devicesStops);
            context.putVar("sheetNames", sheetNames);
            context.putVar("from", deviceGroupQuery.getFrom());
            context.putVar("to", deviceGroupQuery.getTo());
            reportUtils.processTemplateWithSheets(inputStream, outputStream, context);
        }
    }

}
