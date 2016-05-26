package org.traccar.events;

import org.traccar.BaseEventHandler;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class CommandResultEventHandler extends BaseEventHandler {

    @Override
    protected Event analizePosition(Position position) {
        Object cmdResult = position.getAttributes().get(Position.KEY_RESULT);
        if (cmdResult != null) {
            return new Event(Event.COMMAND_RESULT, position.getDeviceId(), position.getId());
        }
        return null;
    }

}
