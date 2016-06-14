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
            if (event.getGeofenceId() == 0
                    || Context.getGeofenceManager().checkGeofence(userId, event.getGeofenceId())) {
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
