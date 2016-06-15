package org.traccar.events;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class MotionEventHandler extends BaseEventHandler {

    private static final double SPEED_THRESHOLD  = 0.01;
    private int suppressRepeated;

    public MotionEventHandler() {
        suppressRepeated = Context.getConfig().getInteger("event.suppressRepeated", 60);
    }

    @Override
    protected Collection<Event> analizePosition(Position position) {
        Collection<Event> result = null;
        if (!isLastPosition()) {
            return null;
        }

        double speed = position.getSpeed();
        boolean valid = position.getValid();
        if (getDevice() == null) {
            return null;
        }
        String motion = getDevice().getMotion();
        if (motion == null) {
            motion = Device.STATUS_STOPPED;
        }
        if (valid && speed > SPEED_THRESHOLD && !motion.equals(Device.STATUS_MOVING)) {
            Context.getConnectionManager().updateDevice(position.getDeviceId(), Device.STATUS_MOVING, null);
            result = new ArrayList<>();
            result.add(new Event(Event.TYPE_DEVICE_MOVING, position.getDeviceId(), position.getId()));
        } else if (valid && speed < SPEED_THRESHOLD && motion.equals(Device.STATUS_MOVING)) {
            Context.getConnectionManager().updateDevice(position.getDeviceId(), Device.STATUS_STOPPED, null);
            result = new ArrayList<>();
            result.add(new Event(Event.TYPE_DEVICE_STOPPED, position.getDeviceId(), position.getId()));
        }
        try {
            if (result != null && !result.isEmpty()) {
                for (Event event : result) {
                    if (!Context.getDataManager().getLastEvents(position.getDeviceId(),
                            event.getType(), suppressRepeated).isEmpty()) {
                        event = null;
                    }
                }
            }
        } catch (SQLException error) {
            Log.warning(error);
        }
        return result;
    }

}
