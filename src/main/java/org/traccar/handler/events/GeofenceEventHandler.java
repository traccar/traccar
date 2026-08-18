/*
 * Copyright 2016 - 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Calendar;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.HashSet;
import java.util.Set;

public class GeofenceEventHandler extends BaseEventHandler {

    private final CacheManager cacheManager;
    private final boolean segmentCrossingEnabled;

    @Inject
    public GeofenceEventHandler(Config config, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        segmentCrossingEnabled = config.getBoolean(Keys.EVENT_GEOFENCE_SEGMENT_CROSSING);
    }

    private void handleEvent(String type, Position position, Geofence geofence, Callback callback) {
        if (geofence != null) {
            long calendarId = geofence.getCalendarId();
            Calendar calendar = calendarId != 0 ? cacheManager.getObject(Calendar.class, calendarId) : null;
            if (calendar == null || calendar.checkMoment(position.getFixTime())) {
                Event event = new Event(type, position);
                event.setGeofenceId(geofence.getId());
                callback.eventDetected(event);
            }
        }
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        if (!PositionUtil.isLatest(cacheManager, position)) {
            return;
        }

        Set<Long> oldGeofences = new HashSet<>();
        Position lastPosition = cacheManager.getPosition(position.getDeviceId());
        if (lastPosition != null && lastPosition.getGeofenceIds() != null) {
            oldGeofences.addAll(lastPosition.getGeofenceIds());
        }

        Set<Long> newGeofences = new HashSet<>();
        if (position.getGeofenceIds() != null) {
            newGeofences.addAll(position.getGeofenceIds());
        }

        if (segmentCrossingEnabled && lastPosition != null) {
            for (Geofence geofence : cacheManager.getDeviceObjects(position.getDeviceId(), Geofence.class)) {
                if (!oldGeofences.contains(geofence.getId()) && !newGeofences.contains(geofence.getId())
                        && geofence.containsSegment(lastPosition, position)) {
                    handleEvent(Event.TYPE_GEOFENCE_CROSSED, position, geofence, callback);
                }
            }
        }

        if (position.getGeofenceIds() != null) {
            newGeofences.removeAll(oldGeofences);
            position.getGeofenceIds().forEach(oldGeofences::remove);
        }

        for (long geofenceId : oldGeofences) {
            Geofence geofence = cacheManager.getObject(Geofence.class, geofenceId);
            handleEvent(Event.TYPE_GEOFENCE_EXIT, position, geofence, callback);
        }
        for (long geofenceId : newGeofences) {
            Geofence geofence = cacheManager.getObject(Geofence.class, geofenceId);
            handleEvent(Event.TYPE_GEOFENCE_ENTER, position, geofence, callback);
        }
    }
}
