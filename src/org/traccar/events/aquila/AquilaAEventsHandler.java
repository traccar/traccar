package org.traccar.events.aquila;

import org.traccar.BaseEventHandler;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by saurako on 5/24/18.
 */
public class AquilaAEventsHandler extends BaseEventHandler {

    public static final int MILLIS_IN_15_MINS = 900_000;
    private Map<String, String> positionInfoToEventTypeMap = new ConcurrentHashMap<>();

    private Map<Long, List<Event>> deviceIdToExtBatteryDisconnectMap = new ConcurrentHashMap<>();

    public AquilaAEventsHandler() {
        positionInfoToEventTypeMap.put(Position.KEY_CASE_OPEN, Event.TYPE_CASE_OPEN);
        positionInfoToEventTypeMap.put(Position.KEY_HARD_ACCELERATION, Event.TYPE_HARD_ACCELERATION);
        positionInfoToEventTypeMap.put(Position.KEY_HARD_BRAKING, Event.TYPE_HARD_BRAKING);
        positionInfoToEventTypeMap.put(Position.KEY_INTERNAL_BATTERY_LOW, Event.TYPE_INTERNAL_BATTERY_LOW);
        positionInfoToEventTypeMap.put(Position.KEY_OVERSPEED_START, Event.TYPE_OVERSPEED_START);
        positionInfoToEventTypeMap.put(Position.KEY_OVERSPEED_END, Event.TYPE_OVERSPEED_END);
    }

    @Override
    protected Map<Event, Position> analyzePosition(final Position position) {
        Map<Event, Position> result = new ConcurrentHashMap<>();
        Map<String, Object> attributes = position.getAttributes();

        // Special logic to handle Position.KEY_EXTERNAL_BATTERY_DISCONNECT
        if (attributes.containsKey(Position.KEY_EXTERNAL_BATTERY_DISCONNECT)
            && (boolean) attributes.get(Position.KEY_EXTERNAL_BATTERY_DISCONNECT)) {

            long deviceId = position.getDeviceId();
            String eventType = Event.TYPE_EXTERNAL_BATTERY_DISCONNECT;
            Event extDisconnectEvent = createEvent(position, eventType);

            if (!deviceIdToExtBatteryDisconnectMap.containsKey(deviceId)) {
                List<Event> extDisconnectList = new ArrayList<>();
                extDisconnectList.add(extDisconnectEvent);
                deviceIdToExtBatteryDisconnectMap.put(deviceId, extDisconnectList);
            } else if (deviceIdToExtBatteryDisconnectMap.get(deviceId).size() < 3) {
                deviceIdToExtBatteryDisconnectMap.get(deviceId).add(extDisconnectEvent);
            } else if (deviceIdToExtBatteryDisconnectMap.get(deviceId).size() >= 3) {
                Event firstEvent = deviceIdToExtBatteryDisconnectMap.get(deviceId).get(0);
                long firstEventTime = (long) firstEvent.getAttributes().get("startTime");
                long currentEventTime = position.getDeviceTime().getTime();

                // If we see 3 or more ext battery disconnects within 15 mins for this device,
                // generate an alert
                long diffInTimes = currentEventTime - firstEventTime;
                if (diffInTimes <= MILLIS_IN_15_MINS) {
                    Event currentEvent = createEvent(position, eventType);
                    result.put(currentEvent, position);
                }

                // Clear the list of these events
                deviceIdToExtBatteryDisconnectMap.get(deviceId).clear();
            }
        }

        for (String eventString : positionInfoToEventTypeMap.keySet()) {
             if (attributes.containsKey(eventString) && (boolean) attributes.get(eventString)) {
                String eventType = positionInfoToEventTypeMap.get(eventString);
                Event event = createEvent(position, eventType);
                result.put(event, position);
            }
        }

        return result;
    }

    private static Event createEvent(final Position position, final String eventType) {
        final Event event = new Event(eventType, position.getDeviceId(), position.getId());
        event.set("startTime", position.getDeviceTime().getTime());

        // Setting the start and end lat long to keep it consistent with the fuel events.
        event.set("startLat", position.getLatitude());
        event.set("startLong", position.getLongitude());
        event.set("endLat", position.getLatitude());
        event.set("endLong", position.getLongitude());

        return event;
    }
}
