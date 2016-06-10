package org.traccar.events;

import java.sql.SQLException;
import java.util.Set;

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
    protected Event analizePosition(Position position) {
        Event event = null;
        if (!isLastPosition() || !position.getValid()) {
            return event;
        }

        Device device = dataManager.getDeviceById(position.getDeviceId());
        if (device == null) {
            return event;
        }

        Set<Long> geofences = geofenceManager.getAllDeviceGeofences(position.getDeviceId());
        if (geofences == null) {
            return event;
        }
        long geofenceId = 0;
        for (Long geofence : geofences) {
            if (geofenceManager.getGeofence(geofence).getGeometry()
                    .containsPoint(position.getLatitude(), position.getLongitude())) {
                geofenceId = geofence;
                break;
            }
        }

        if (device.getGeofenceId() != geofenceId) {
            try {
                if (geofenceId == 0) {
                    event = new Event(Event.TYPE_GEOFENCE_EXIT, position.getDeviceId(), position.getId());
                    event.setGeofenceId(device.getGeofenceId());
                } else {
                    event = new Event(Event.TYPE_GEOFENCE_ENTER, position.getDeviceId(), position.getId());
                    event.setGeofenceId(geofenceId);
                }
                if (event != null && !dataManager.getLastEvents(
                    position.getDeviceId(), event.getType(), suppressRepeated).isEmpty()) {
                    event = null;
                }
                device.setGeofenceId(geofenceId);
                dataManager.updateDeviceStatus(device);
            } catch (SQLException error) {
                Log.warning(error);
            }

        }
        return event;
    }
}
