/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.ChannelHandler;
import org.traccar.config.Config;
import org.traccar.helper.model.GeofenceUtil;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Calendar;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Position;
import org.traccar.session.ConnectionManager;
import org.traccar.session.cache.CacheManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ChannelHandler.Sharable
public class GeofenceEventHandler extends BaseEventHandler {

    private final Config config;
    private final CacheManager cacheManager;
    private final ConnectionManager connectionManager;

    @Inject
    public GeofenceEventHandler(Config config, CacheManager cacheManager, ConnectionManager connectionManager) {
        this.config = config;
        this.cacheManager = cacheManager;
        this.connectionManager = connectionManager;
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        Device device = cacheManager.getObject(Device.class, position.getDeviceId());
        if (device == null) {
            return null;
        }
        if (!PositionUtil.isLatest(cacheManager, position) || !position.getValid()) {
            return null;
        }

        List<Long> currentGeofences = GeofenceUtil.getCurrentGeofences(config, cacheManager, position);
        List<Long> oldGeofences = new ArrayList<>();
        if (device.getGeofenceIds() != null) {
            oldGeofences.addAll(device.getGeofenceIds());
        }
        List<Long> newGeofences = new ArrayList<>(currentGeofences);
        newGeofences.removeAll(oldGeofences);
        oldGeofences.removeAll(currentGeofences);

        device.setGeofenceIds(currentGeofences);
        if (!oldGeofences.isEmpty() || !newGeofences.isEmpty()) {
            connectionManager.updateDevice(true, device);
        }

        Map<Event, Position> events = new HashMap<>();
        for (long geofenceId : oldGeofences) {
            long calendarId = cacheManager.getObject(Geofence.class, geofenceId).getCalendarId();
            Calendar calendar = calendarId != 0 ? cacheManager.getObject(Calendar.class, calendarId) : null;
            if (calendar == null || calendar.checkMoment(position.getFixTime())) {
                Event event = new Event(Event.TYPE_GEOFENCE_EXIT, position);
                event.setGeofenceId(geofenceId);
                events.put(event, position);
            }
        }
        for (long geofenceId : newGeofences) {
            long calendarId = cacheManager.getObject(Geofence.class, geofenceId).getCalendarId();
            Calendar calendar = calendarId != 0 ? cacheManager.getObject(Calendar.class, calendarId) : null;
            if (calendar == null || calendar.checkMoment(position.getFixTime())) {
                Event event = new Event(Event.TYPE_GEOFENCE_ENTER, position);
                event.setGeofenceId(geofenceId);
                events.put(event, position);
            }
        }
        return events;
    }

}
