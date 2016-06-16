package org.traccar.events;

import java.util.ArrayList;
import java.util.Collection;

import org.traccar.BaseEventHandler;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class CommandResultEventHandler extends BaseEventHandler {

    @Override
    protected Collection<Event> analyzePosition(Position position) {
        Object commandResult = position.getAttributes().get(Position.KEY_RESULT);
        if (commandResult != null) {
            Collection<Event> events = new ArrayList<>();
            events.add(new Event(Event.TYPE_COMMAND_RESULT, position.getDeviceId(), position.getId()));
            return events;
        }
        return null;
    }

}
