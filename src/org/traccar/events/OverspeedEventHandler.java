package org.traccar.events;

import java.sql.SQLException;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.helper.Log;
//import org.traccar.model.Device;
//import org.traccar.model.Group;
import org.traccar.helper.UnitsConverter;

public class OverspeedEventHandler extends BaseEventHandler {

    private double globalSpeedLimit;
    private long suppressRepeated;

    public OverspeedEventHandler() {
        globalSpeedLimit = Context.getConfig().getInteger("event.globalspeedlimit", 0);
        suppressRepeated = Context.getConfig().getLong("event.suppressrepeated", 60);
        try {
            Server server = Context.getDataManager().getServer();
            String speedUnit = server.getSpeedUnit();
            if (speedUnit != null) {
                switch (speedUnit) {
                    case "kmh" : globalSpeedLimit = UnitsConverter.knotsFromKph(globalSpeedLimit);
                                break;
                    case "mph" : globalSpeedLimit = UnitsConverter.knotsFromMph(globalSpeedLimit);
                    default : break;
                }
            }
        } catch (SQLException error) {
            Log.warning(error);
        }

    }

    @Override
    protected Event analizePosition(Position position) {
        Event event = null;
        if (!isLastPosition()) {
            return event;
        }
        double speed = position.getSpeed();
        boolean valid = position.getValid();
        double deviceSpeedLimit = 0;
        double groupSpeedLimit = 0;
//        Device device = Context.getIdentityManager().getDeviceById(position.getDeviceId());
//        if (device != null) {
//            deviceSpeedLimit = device.getSpeedLimit();
//            try {
//              Group group = Context.getDataManager().getGroupById(device.getGroupId());
//              if (group != null) {
//                  groupSpeedLimit = group.getSpeedLimit();
//              }
//            } catch (SQLException error) {
//                Log.warning(error);
//            }
//        }

        if (valid && globalSpeedLimit != 0 && speed > globalSpeedLimit
           || valid && groupSpeedLimit != 0 && speed > groupSpeedLimit
           || valid && deviceSpeedLimit != 0 && speed > deviceSpeedLimit) {
            try {
                if (Context.getDataManager().getLastEvents(
                        position.getDeviceId(), Event.DEVICE_OVERSPEED, suppressRepeated).isEmpty()) {
                    event = new Event(Event.DEVICE_OVERSPEED, position.getDeviceId(), position.getId());
                }
            } catch (SQLException error) {
                Log.warning(error);
            }

        }
        return event;
    }

}
