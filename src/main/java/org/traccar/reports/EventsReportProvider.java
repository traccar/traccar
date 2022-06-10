/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 - 2018 Andrey Kunitsyn (andrey@traccar.org)
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
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Group;
import org.traccar.model.Maintenance;
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
import java.util.HashMap;
import java.util.Iterator;

public class EventsReportProvider {

    private final ReportUtils reportUtils;

    @Inject
    public EventsReportProvider(ReportUtils reportUtils) {
        this.reportUtils = reportUtils;
    }

    public Collection<Event> getObjects(
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Collection<String> types, Date from, Date to) throws StorageException {
        reportUtils.checkPeriodLimit(from, to);
        ArrayList<Event> result = new ArrayList<>();
        for (long deviceId: reportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<Event> events = Context.getDataManager().getEvents(deviceId, from, to);
            boolean all = types.isEmpty() || types.contains(Event.ALL_EVENTS);
            for (Event event : events) {
                if (all || types.contains(event.getType())) {
                    long geofenceId = event.getGeofenceId();
                    long maintenanceId = event.getMaintenanceId();
                    if ((geofenceId == 0 || reportUtils.getObject(userId, Geofence.class, geofenceId) != null)
                            && (maintenanceId == 0
                            || reportUtils.getObject(userId, Maintenance.class, maintenanceId) != null)) {
                       result.add(event);
                    }
                }
            }
        }
        return result;
    }

    public void getExcel(
            OutputStream outputStream, long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Collection<String> types, Date from, Date to) throws StorageException, IOException {
        reportUtils.checkPeriodLimit(from, to);
        ArrayList<DeviceReportSection> devicesEvents = new ArrayList<>();
        ArrayList<String> sheetNames = new ArrayList<>();
        HashMap<Long, String> geofenceNames = new HashMap<>();
        HashMap<Long, String> maintenanceNames = new HashMap<>();
        for (long deviceId: reportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<Event> events = Context.getDataManager().getEvents(deviceId, from, to);
            boolean all = types.isEmpty() || types.contains(Event.ALL_EVENTS);
            for (Iterator<Event> iterator = events.iterator(); iterator.hasNext();) {
                Event event = iterator.next();
                if (all || types.contains(event.getType())) {
                    long geofenceId = event.getGeofenceId();
                    long maintenanceId = event.getMaintenanceId();
                    if (geofenceId != 0) {
                        Geofence geofence = reportUtils.getObject(userId, Geofence.class, geofenceId);
                        if (geofence != null) {
                            geofenceNames.put(geofenceId, geofence.getName());
                        } else {
                            iterator.remove();
                        }
                    } else if (maintenanceId != 0) {
                        Maintenance maintenance = reportUtils.getObject(userId, Maintenance.class, maintenanceId);
                        if (maintenance != null) {
                            maintenanceNames.put(maintenanceId, maintenance.getName());
                        } else {
                            iterator.remove();
                        }
                    }
                } else {
                    iterator.remove();
                }
            }
            DeviceReportSection deviceEvents = new DeviceReportSection();
            Device device = Context.getIdentityManager().getById(deviceId);
            deviceEvents.setDeviceName(device.getName());
            sheetNames.add(WorkbookUtil.createSafeSheetName(deviceEvents.getDeviceName()));
            if (device.getGroupId() != 0) {
                Group group = Context.getGroupsManager().getById(device.getGroupId());
                if (group != null) {
                    deviceEvents.setGroupName(group.getName());
                }
            }
            deviceEvents.setObjects(events);
            devicesEvents.add(deviceEvents);
        }
        String templatePath = Context.getConfig().getString("report.templatesPath",
                "templates/export/");
        try (InputStream inputStream = new FileInputStream(templatePath + "/events.xlsx")) {
            var context = reportUtils.initializeContext(userId);
            context.putVar("devices", devicesEvents);
            context.putVar("sheetNames", sheetNames);
            context.putVar("geofenceNames", geofenceNames);
            context.putVar("maintenanceNames", maintenanceNames);
            context.putVar("from", from);
            context.putVar("to", to);
            reportUtils.processTemplateWithSheets(inputStream, outputStream, context);
        }
    }
}
