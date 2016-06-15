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
    protected Collection<Event> analizePosition(Position position) {
         if (!isLastPosition() || !position.getValid()) {
            return null;
        }

         if (getDevice() == null) {
            return null;
        }

        List<Long> currentGeofences = geofenceManager.getCurrentDeviceGeofences(position);
        List<Long> oldGeofences = new ArrayList<Long>();
        if (getDevice().getGeofenceIds() != null) {
            oldGeofences.addAll(getDevice().getGeofenceIds());
        }
        List<Long> newGeofences = new ArrayList<Long>(currentGeofences);
        newGeofences.removeAll(oldGeofences);
        oldGeofences.removeAll(currentGeofences);

        getDevice().setGeofenceIds(currentGeofences);

        Collection<Event> events = new ArrayList<>();
        try {
            if (dataManager.getLastEvents(position.getDeviceId(),
                    Event.TYPE_GEOFENCE_ENTER, suppressRepeated).isEmpty()) {
                for (Long geofenceId : newGeofences) {
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
                for (Long geofenceId : oldGeofences) {
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
