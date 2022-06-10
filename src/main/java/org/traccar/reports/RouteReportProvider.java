/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 Andrey Kunitsyn (andrey@traccar.org)
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
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.reports.common.ReportUtils;
import org.traccar.reports.model.DeviceReportSection;
import org.traccar.storage.StorageException;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class RouteReportProvider {

    private final ReportUtils reportUtils;

    @Inject
    public RouteReportProvider(ReportUtils reportUtils) {
        this.reportUtils = reportUtils;
    }

    public Collection<Position> getObjects(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws StorageException {
        reportUtils.checkPeriodLimit(from, to);
        ArrayList<Position> result = new ArrayList<>();
        for (long deviceId: reportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            result.addAll(Context.getDataManager().getPositions(deviceId, from, to));
        }
        return result;
    }

    public void getExcel(OutputStream outputStream,
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws StorageException, IOException {
        reportUtils.checkPeriodLimit(from, to);
        ArrayList<DeviceReportSection> devicesRoutes = new ArrayList<>();
        ArrayList<String> sheetNames = new ArrayList<>();
        for (long deviceId: reportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<Position> positions = Context.getDataManager()
                    .getPositions(deviceId, from, to);
            DeviceReportSection deviceRoutes = new DeviceReportSection();
            Device device = Context.getIdentityManager().getById(deviceId);
            deviceRoutes.setDeviceName(device.getName());
            sheetNames.add(WorkbookUtil.createSafeSheetName(deviceRoutes.getDeviceName()));
            if (device.getGroupId() != 0) {
                Group group = Context.getGroupsManager().getById(device.getGroupId());
                if (group != null) {
                    deviceRoutes.setGroupName(group.getName());
                }
            }
            deviceRoutes.setObjects(positions);
            devicesRoutes.add(deviceRoutes);
        }
        String templatePath = Context.getConfig().getString("report.templatesPath",
                "templates/export/");
        try (InputStream inputStream = new FileInputStream(templatePath + "/route.xlsx")) {
            var context = reportUtils.initializeContext(userId);
            context.putVar("devices", devicesRoutes);
            context.putVar("sheetNames", sheetNames);
            context.putVar("from", from);
            context.putVar("to", to);
            reportUtils.processTemplateWithSheets(inputStream, outputStream, context);
        }
    }
}
