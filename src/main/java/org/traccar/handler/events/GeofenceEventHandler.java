/*
 * Copyright 2016 - 2021 Anton Tananaev (anton@traccar.org)
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.channel.ChannelHandler;
import org.traccar.database.CalendarManager;
import org.traccar.database.ConnectionManager;
import org.traccar.database.GeofenceManager;
import org.traccar.database.IdentityManager;
import org.traccar.model.Calendar;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

@ChannelHandler.Sharable
public class GeofenceEventHandler extends BaseEventHandler {

    private final IdentityManager identityManager;
    private final GeofenceManager geofenceManager;
    private final CalendarManager calendarManager;
    private final ConnectionManager connectionManager;

    public GeofenceEventHandler(
            IdentityManager identityManager, GeofenceManager geofenceManager, CalendarManager calendarManager,
            ConnectionManager connectionManager) {
        this.identityManager = identityManager;
        this.geofenceManager = geofenceManager;
        this.calendarManager = calendarManager;
        this.connectionManager = connectionManager;
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        Device device = identityManager.getById(position.getDeviceId());
        if (device == null) {
            return null;
        }
        if (!identityManager.isLatestPosition(position) || !position.getValid()) {
            return null;
        }

        List<Long> currentGeofences = geofenceManager.getCurrentDeviceGeofences(position);
        List<Long> oldGeofences = new ArrayList<>();
        if (device.getGeofenceIds() != null) {
            oldGeofences.addAll(device.getGeofenceIds());
        }
        List<Long> newGeofences = new ArrayList<>(currentGeofences);
        newGeofences.removeAll(oldGeofences);
        oldGeofences.removeAll(currentGeofences);

        device.setGeofenceIds(currentGeofences);
        if (!oldGeofences.isEmpty() || !newGeofences.isEmpty()) {
            connectionManager.updateDevice(device);
        }

        Map<Event, Position> events = new HashMap<>();
        for (long geofenceId : oldGeofences) {
            long calendarId = geofenceManager.getById(geofenceId).getCalendarId();
            Calendar calendar = calendarId != 0 ? calendarManager.getById(calendarId) : null;
            if (calendar == null || calendar.checkMoment(position.getFixTime())) {
                Event event = new Event(Event.TYPE_GEOFENCE_EXIT, position);
                event.setGeofenceId(geofenceId);
                events.put(event, position);
            }
        }
        for (long geofenceId : newGeofences) {
            long calendarId = geofenceManager.getById(geofenceId).getCalendarId();
            Calendar calendar = calendarId != 0 ? calendarManager.getById(calendarId) : null;
            if (calendar == null || calendar.checkMoment(position.getFixTime())) {
                Event event = new Event(Event.TYPE_GEOFENCE_ENTER, position);
                event.setGeofenceId(geofenceId);
                events.put(event, position);
            }
        }
        return events;
    }

}
