package org.traccar.events;

import org.traccar.BaseEventHandler;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class CommandResultEventHandler extends BaseEventHandler {

    @Override
    protected Event analizePosition(Position position) {
        Object commandResult = position.getAttributes().get(Position.KEY_RESULT);
        if (commandResult != null) {
            return new Event(Event.TYPE_COMMAND_RESULT, position.getDeviceId(), position.getId());
        }
        return null;
    }

}
