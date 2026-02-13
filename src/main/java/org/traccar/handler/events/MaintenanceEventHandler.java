/*
 * Copyright 2016 - 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler.events;

import jakarta.inject.Inject;
import org.traccar.model.Event;
import org.traccar.model.Maintenance;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

public class MaintenanceEventHandler extends BaseEventHandler {

    private final CacheManager cacheManager;

    @Inject
    public MaintenanceEventHandler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        Position lastPosition = cacheManager.getPosition(position.getDeviceId());
        if (lastPosition == null || position.getFixTime().compareTo(lastPosition.getFixTime()) < 0) {
            return;
        }

        for (Maintenance maintenance : cacheManager.getDeviceObjects(position.getDeviceId(), Maintenance.class)) {
            if (maintenance.getPeriod() != 0) {
                double oldValue = getValue(lastPosition, maintenance.getType());
                double newValue = getValue(position, maintenance.getType());
                if (oldValue != 0.0 && newValue != 0.0 && newValue >= maintenance.getStart()) {
                    if (oldValue < maintenance.getStart()
                        || (long) ((oldValue - maintenance.getStart()) / maintenance.getPeriod())
                        < (long) ((newValue - maintenance.getStart()) / maintenance.getPeriod())) {
                        Event event = new Event(Event.TYPE_MAINTENANCE, position);
                        event.setMaintenanceId(maintenance.getId());
                        event.set(maintenance.getType(), newValue);
                        callback.eventDetected(event);
                    }
                }
            }
        }
    }

    private double getValue(Position position, String type) {
        return switch (type) {
            case "serverTime" -> position.getServerTime().getTime();
            case "deviceTime" -> position.getDeviceTime().getTime();
            case "fixTime" -> position.getFixTime().getTime();
            default -> position.getDouble(type);
        };
    }

}
