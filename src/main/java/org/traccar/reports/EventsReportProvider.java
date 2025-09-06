/*
 * Copyright 2016 - 2025 Anton Tananaev (anton@traccar.org)
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
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.model.DeviceUtil;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Group;
import org.traccar.model.Maintenance;
import org.traccar.model.Position;
import org.traccar.reports.common.ReportUtils;
import org.traccar.reports.model.DeviceReportSection;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class EventsReportProvider {

    private final Config config;
    private final ReportUtils reportUtils;
    private final Storage storage;

    @Inject
    public EventsReportProvider(Config config, ReportUtils reportUtils, Storage storage) {
        this.config = config;
        this.reportUtils = reportUtils;
        this.storage = storage;
    }

    private Stream<Event> getEvents(long deviceId, Date from, Date to) throws StorageException {
        return storage.getObjectsStream(Event.class, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("deviceId", deviceId),
                        new Condition.Between("eventTime", from, to)),
                new Order("eventTime")));
    }

    private boolean filterType(Collection<String> types, Collection<String> alarms, Event event) {
        if (!types.contains(event.getType())) {
            return false;
        }
        return !event.getType().equals(Event.TYPE_ALARM) || alarms.isEmpty()
                || alarms.contains(event.getString(Position.KEY_ALARM));
    }

    public Stream<Event> getObjects(
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Collection<String> types, Collection<String> alarms, Date from, Date to) throws StorageException {
        reportUtils.checkPeriodLimit(from, to);
        boolean all = types.isEmpty() || types.contains(Event.ALL_EVENTS);

        return DeviceUtil.getAccessibleDevices(storage, userId, deviceIds, groupIds).stream()
                .flatMap(device -> {
                    try {
                        return getEvents(device.getId(), from, to);
                    } catch (StorageException e) {
                        return Stream.of();
                    }
                })
                .filter(event -> all || filterType(types, alarms, event))
                .filter(event -> {
                    long geofenceId = event.getGeofenceId();
                    if (geofenceId > 0 && reportUtils.getObject(userId, Geofence.class, geofenceId) == null) {
                        return false;
                    }
                    long maintenanceId = event.getMaintenanceId();
                    if (maintenanceId > 0 && reportUtils.getObject(userId, Maintenance.class, maintenanceId) == null) {
                        return false;
                    }
                    return true;
                });
    }

    public void getExcel(
            OutputStream outputStream, long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Collection<String> types, Collection<String> alarms,
            Date from, Date to) throws StorageException, IOException {
        reportUtils.checkPeriodLimit(from, to);

        List<DeviceReportSection> devicesEvents = new ArrayList<>();
        List<String> sheetNames = new ArrayList<>();
        HashMap<Long, String> geofenceNames = new HashMap<>();
        HashMap<Long, String> maintenanceNames = new HashMap<>();
        HashMap<Long, Position> positions = new HashMap<>();
        for (Device device: DeviceUtil.getAccessibleDevices(storage, userId, deviceIds, groupIds)) {
            List<Event> events = new ArrayList<>();
            boolean all = types.isEmpty() || types.contains(Event.ALL_EVENTS);
            try (var unfilteredEvents = getEvents(device.getId(), from, to)) {
                var iterator = unfilteredEvents.iterator();
                while (iterator.hasNext()) {
                    Event event = iterator.next();
                    if (all || filterType(types, alarms, event)) {
                        long geofenceId = event.getGeofenceId();
                        long maintenanceId = event.getMaintenanceId();
                        if (geofenceId != 0) {
                            Geofence geofence = reportUtils.getObject(userId, Geofence.class, geofenceId);
                            if (geofence != null) {
                                geofenceNames.put(geofenceId, geofence.getName());
                                events.add(event);
                            }
                        } else if (maintenanceId != 0) {
                            Maintenance maintenance = reportUtils.getObject(userId, Maintenance.class, maintenanceId);
                            if (maintenance != null) {
                                maintenanceNames.put(maintenanceId, maintenance.getName());
                                events.add(event);
                            }
                        } else {
                            events.add(event);
                        }
                    }
                }
            }
            for (Event event : events) {
                long positionId = event.getPositionId();
                if (positionId > 0) {
                    Position position = storage.getObject(Position.class, new Request(
                            new Columns.All(), new Condition.Equals("id", positionId)));
                    positions.put(positionId, position);
                }
            }
            DeviceReportSection deviceEvents = new DeviceReportSection();
            deviceEvents.setDeviceName(device.getName());
            sheetNames.add(WorkbookUtil.createSafeSheetName(deviceEvents.getDeviceName()));
            if (device.getGroupId() > 0) {
                Group group = storage.getObject(Group.class, new Request(
                        new Columns.All(), new Condition.Equals("id", device.getGroupId())));
                if (group != null) {
                    deviceEvents.setGroupName(group.getName());
                }
            }
            deviceEvents.setObjects(events);
            devicesEvents.add(deviceEvents);
        }

        File file = Paths.get(config.getString(Keys.TEMPLATES_ROOT), "export", "events.xlsx").toFile();
        try (InputStream inputStream = new FileInputStream(file)) {
            var context = reportUtils.initializeContext(userId);
            context.putVar("devices", devicesEvents);
            context.putVar("sheetNames", sheetNames);
            context.putVar("geofenceNames", geofenceNames);
            context.putVar("maintenanceNames", maintenanceNames);
            context.putVar("positions", positions);
            context.putVar("from", from);
            context.putVar("to", to);
            reportUtils.processTemplateWithSheets(inputStream, outputStream, context);
        }
    }
}
