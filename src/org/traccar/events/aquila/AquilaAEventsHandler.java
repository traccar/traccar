package org.traccar.events.aquila;

import org.traccar.BaseEventHandler;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by saurako on 5/24/18.
 */
public class AquilaAEventsHandler extends BaseEventHandler {

    private Map<String, String> positionInfoToEventTypeMap = new ConcurrentHashMap<>();

    public AquilaAEventsHandler() {
        positionInfoToEventTypeMap.put(Position.KEY_CASE_OPEN, Event.TYPE_CASE_OPEN);
        positionInfoToEventTypeMap.put(Position.KEY_HARD_ACCELERATION, Event.TYPE_HARD_ACCELERATION);
        positionInfoToEventTypeMap.put(Position.KEY_HARD_BRAKING, Event.TYPE_HARD_BRAKING);
        positionInfoToEventTypeMap.put(Position.KEY_INTERNAL_BATTERY_LOW, Event.TYPE_INTERNAL_BATTERY_LOW);
        positionInfoToEventTypeMap.put(Position.KEY_OVERSPEED_START, Event.TYPE_OVERSPEED_START);
        positionInfoToEventTypeMap.put(Position.KEY_OVERSPEED_END, Event.TYPE_OVERSPEED_END);

        positionInfoToEventTypeMap.put(Position.KEY_EXTERNAL_BATTERY_DISCONNECT,
                                       Event.TYPE_EXTERNAL_BATTERY_DISCONNECT);
    }

    @Override
    protected Map<Event, Position> analyzePosition(final Position position) {
        Map<Event, Position> result = new ConcurrentHashMap<>();
        Map<String, Object> attributes = position.getAttributes();

        for (String eventString : positionInfoToEventTypeMap.keySet()) {

            if (attributes.containsKey(eventString) && (boolean) attributes.get(eventString)) {
                String eventType = positionInfoToEventTypeMap.get(eventString);
                Event event = new Event(eventType, position.getDeviceId(), position.getId());
                event.set("startTime", position.getDeviceTime().getTime());
                result.put(event, position);
            }
        }

        return result;
    }
}
