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
package org.traccar.database;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class NotificationManager {

    private final DataManager dataManager;

    public NotificationManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void updateEvent(Event event, Position position) {
        try {
            dataManager.addEvent(event);
        } catch (SQLException error) {
            Log.warning(error);
        }

        Set<Long> users = Context.getPermissionsManager().getDeviceUsers(event.getDeviceId());
        for (Long userId : users) {
            if (event.getGeofenceId() == 0 || Context.getGeofenceManager() != null
                    && Context.getGeofenceManager().checkGeofence(userId, event.getGeofenceId())) {
                Context.getConnectionManager().updateEvent(userId, event, position);
            }
        }
    }

    public void updateEvents(Collection<Event> events, Position position) {

        for (Event event : events) {
            updateEvent(event, position);
        }
    }
}
