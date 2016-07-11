/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.events;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.database.DataManager;
import org.traccar.database.GeofenceManager;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class GeofenceEventHandler extends BaseEventHandler {

    private int suppressRepeated;
    private GeofenceManager geofenceManager;
    private DataManager dataManager;

    public GeofenceEventHandler() {
        suppressRepeated = Context.getConfig().getInteger("event.suppressRepeated", 60);
        geofenceManager = Context.getGeofenceManager();
        dataManager = Context.getDataManager();
    }

    @Override
    protected Collection<Event> analyzePosition(Position position) {
        Device device = dataManager.getDeviceById(position.getDeviceId());
        if (device == null) {
            return null;
        }
        if (position.getId() != device.getPositionId() || !position.getValid()) {
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

        Collection<Event> events = new ArrayList<>();
        try {
            if (dataManager.getLastEvents(position.getDeviceId(),
                    Event.TYPE_GEOFENCE_ENTER, suppressRepeated).isEmpty()) {
                for (long geofenceId : newGeofences) {
                    Event event = new Event(Event.TYPE_GEOFENCE_ENTER, position.getDeviceId(), position.getId());
                    event.setGeofenceId(geofenceId);
                    events.add(event);
                }
            }
        } catch (SQLException error) {
            Log.warning(error);
        }
        try {
            if (dataManager.getLastEvents(position.getDeviceId(),
                    Event.TYPE_GEOFENCE_EXIT, suppressRepeated).isEmpty()) {
                for (long geofenceId : oldGeofences) {
                    Event event = new Event(Event.TYPE_GEOFENCE_EXIT, position.getDeviceId(), position.getId());
                    event.setGeofenceId(geofenceId);
                    events.add(event);
                }
            }
        } catch (SQLException error) {
            Log.warning(error);
        }
        return events;
    }
}
