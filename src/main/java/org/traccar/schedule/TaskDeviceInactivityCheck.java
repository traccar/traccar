/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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

import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskDeviceInactivityCheck implements Runnable {

    public static final String ATTRIBUTE_DEVICE_INACTIVITY_START = "deviceInactivityStart";
    public static final String ATTRIBUTE_DEVICE_INACTIVITY_PERIOD = "deviceInactivityPeriod";
    public static final String ATTRIBUTE_LAST_UPDATE = "lastUpdate";

    private static final long CHECK_PERIOD_MINUTES = 15;

    public void schedule(ScheduledExecutorService executor) {
        executor.scheduleAtFixedRate(this, CHECK_PERIOD_MINUTES, CHECK_PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();
        long checkPeriod = TimeUnit.MINUTES.toMillis(CHECK_PERIOD_MINUTES);

        Map<Event, Position> events = new HashMap<>();
        for (Device device : Context.getDeviceManager().getAllDevices()) {
            if (device.getLastUpdate() != null && checkDevice(device, currentTime, checkPeriod)) {
                Event event = new Event(Event.TYPE_DEVICE_INACTIVE, device.getId());
                event.set(ATTRIBUTE_LAST_UPDATE, device.getLastUpdate().getTime());
                events.put(event, null);
            }
        }

        Context.getNotificationManager().updateEvents(events);
    }

    private boolean checkDevice(Device device, long currentTime, long checkPeriod) {
        long deviceInactivityStart = device.getLong(ATTRIBUTE_DEVICE_INACTIVITY_START);
        if (deviceInactivityStart > 0) {
            long timeThreshold = device.getLastUpdate().getTime() + deviceInactivityStart;
            if (currentTime >= timeThreshold) {

                if (currentTime - checkPeriod < timeThreshold) {
                    return true;
                }

                long deviceInactivityPeriod = device.getLong(ATTRIBUTE_DEVICE_INACTIVITY_PERIOD);
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
