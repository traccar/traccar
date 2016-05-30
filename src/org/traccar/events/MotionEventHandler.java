package org.traccar.events;

import java.sql.SQLException;

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
    protected Event analizePosition(Position position) {
        Event event = null;

        if (!isLastPosition()) {
            return event;
        }

        double speed = position.getSpeed();
        boolean valid = position.getValid();
        Device device = Context.getIdentityManager().getDeviceById(position.getDeviceId());
        if (device == null) {
            return event;
        }
        String motion = device.getMotion();
        if (valid && speed > SPEED_THRESHOLD && !motion.equals(Device.STATUS_MOVING)) {
            Context.getConnectionManager().updateDevice(position.getDeviceId(), Device.STATUS_MOVING, null);
            event = new Event(Event.TYPE_DEVICE_MOVING, position.getDeviceId(), position.getId());
        } else if (valid && speed < SPEED_THRESHOLD && motion.equals(Device.STATUS_MOVING)) {
            Context.getConnectionManager().updateDevice(position.getDeviceId(), Device.STATUS_STOPPED, null);
            event = new Event(Event.TYPE_DEVICE_STOPPED, position.getDeviceId(), position.getId());
        }
        try {
            if (event != null && !Context.getDataManager().getLastEvents(
                    position.getDeviceId(), event.getType(), suppressRepeated).isEmpty()) {
                event = null;
            }

        } catch (SQLException error) {
            Log.warning(error);
        }
        return event;
    }

}
