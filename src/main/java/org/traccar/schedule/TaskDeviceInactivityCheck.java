/*
 * Copyright 2020 - 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.database.NotificationManager;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TaskDeviceInactivityCheck extends SingleScheduleTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskDeviceInactivityCheck.class);

    public static final String ATTRIBUTE_DEVICE_INACTIVITY_START = "deviceInactivityStart";
    public static final String ATTRIBUTE_DEVICE_INACTIVITY_PERIOD = "deviceInactivityPeriod";
    public static final String ATTRIBUTE_LAST_UPDATE = "lastUpdate";

    private static final long CHECK_PERIOD_MINUTES = 15;

    private final Storage storage;
    private final NotificationManager notificationManager;

    @Inject
    public TaskDeviceInactivityCheck(Storage storage, NotificationManager notificationManager) {
        this.storage = storage;
        this.notificationManager = notificationManager;
    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        executor.scheduleAtFixedRate(this, CHECK_PERIOD_MINUTES, CHECK_PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();
        long checkPeriod = TimeUnit.MINUTES.toMillis(CHECK_PERIOD_MINUTES);

        Map<Event, Position> events = new HashMap<>();

        try {
            Map<Long, Group> groups = storage.getObjects(Group.class, new Request(new Columns.All()))
                    .stream().collect(Collectors.toMap(Group::getId, group -> group));
            for (Device device : storage.getObjects(Device.class, new Request(new Columns.All()))) {
                if (device.getLastUpdate() != null && checkDevice(device, groups, currentTime, checkPeriod)) {
                    Event event = new Event(Event.TYPE_DEVICE_INACTIVE, device.getId());
                    event.set(ATTRIBUTE_LAST_UPDATE, device.getLastUpdate().getTime());
                    events.put(event, null);
                }
            }
        } catch (StorageException e) {
            LOGGER.warn("Database error", e);
        }

        notificationManager.updateEvents(events);
    }

    private long getAttribute(Device device, Map<Long, Group> groups, String key) {
        long deviceValue = device.getLong(key);
        if (deviceValue > 0) {
            return deviceValue;
        } else {
            long groupId = device.getGroupId();
            while (groupId > 0) {
                Group group = groups.get(groupId);
                if (group == null) {
                    return 0;
                }
                long groupValue = group.getLong(key);
                if (groupValue > 0) {
                    return groupValue;
                }
                groupId = group.getGroupId();
            }
            return 0;
        }
    }

    private boolean checkDevice(Device device, Map<Long, Group> groups, long currentTime, long checkPeriod) {
        long deviceInactivityStart = getAttribute(device, groups, ATTRIBUTE_DEVICE_INACTIVITY_START);
        if (deviceInactivityStart > 0) {
            long timeThreshold = device.getLastUpdate().getTime() + deviceInactivityStart;
            if (currentTime >= timeThreshold) {

                if (currentTime - checkPeriod < timeThreshold) {
                    return true;
                }

                long deviceInactivityPeriod = getAttribute(device, groups, ATTRIBUTE_DEVICE_INACTIVITY_PERIOD);
                if (deviceInactivityPeriod > 0) {
                    long count = (currentTime - timeThreshold - 1) / deviceInactivityPeriod;
                    timeThreshold += count * deviceInactivityPeriod;
                    return currentTime - checkPeriod < timeThreshold;
                }

            }
        }
        return false;
    }

}
