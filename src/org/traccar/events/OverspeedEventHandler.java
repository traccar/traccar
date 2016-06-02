package org.traccar.events;

import java.sql.SQLException;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.helper.Log;
import org.traccar.helper.UnitsConverter;

public class OverspeedEventHandler extends BaseEventHandler {

    private double globalSpeedLimit;
    private int suppressRepeated;

    public OverspeedEventHandler() {
        globalSpeedLimit = UnitsConverter.knotsFromKph(Context.getConfig().getInteger("event.globalSpeedLimit", 0));
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

        if (valid && globalSpeedLimit != 0 && speed > globalSpeedLimit) {
            try {
                if (Context.getDataManager().getLastEvents(
                        position.getDeviceId(), Event.TYPE_DEVICE_OVERSPEED, suppressRepeated).isEmpty()) {
                    event = new Event(Event.TYPE_DEVICE_OVERSPEED, position.getDeviceId(), position.getId());
                }
            } catch (SQLException error) {
                Log.warning(error);
            }

        }
        return event;
    }

}
