package org.traccar.events.custom;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommonGPSEventsHandler extends BaseEventHandler {

    private static int batteryEventsAlertThreshold;
    private Map<Long, List<Event>> deviceIdToExtBatteryDisconnectMap = new ConcurrentHashMap<>();
    private Map<Long, List<Event>> deviceIdToIntBatteryLowMap = new ConcurrentHashMap<>();

    static {
        batteryEventsAlertThreshold = Context.getConfig().getInteger("processing.peripheralSensorData.batteryEventsThreshold");
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        return processPosition(position);
    }

    protected Map<Event, Position> processPosition(Position position) {
        Map<Event, Position> result = new ConcurrentHashMap<>();
        Map<String, Object> attributes = position.getAttributes();
        handleBatteryEvent(position,
                           Position.KEY_EXTERNAL_BATTERY_DISCONNECT,
                           deviceIdToExtBatteryDisconnectMap,
                           result,
                           attributes);

        handleBatteryEvent(position,
                           Position.KEY_INTERNAL_BATTERY_LOW,
                           deviceIdToIntBatteryLowMap,
                           result,
                           attributes);
        return result;
    }

    private void handleBatteryEvent(final Position position,
                                    final String eventType,
                                    final Map<Long, List<Event>> eventsMap,
                                    final Map<Event, Position> result,
                                    final Map<String, Object> attributes) {

        long deviceId = position.getDeviceId();

        boolean isExpectedBatteryEvent = attributes.containsKey(eventType)
                && (boolean) attributes.get(eventType);

        boolean charge = attributes.containsKey(Position.KEY_CHARGE)
                && (boolean) attributes.get(Position.KEY_CHARGE);

        if (isExpectedBatteryEvent) {
            registerEventIfNecessary(position, eventType, eventsMap, result, deviceId);
            return;
        }

        if (eventsMap.containsKey(deviceId)
                && !eventsMap.get(deviceId).isEmpty()) {

            if (charge) {
                registerEventIfNecessary(position, eventType, eventsMap, result, deviceId);
                return;
            }

            eventsMap.get(deviceId).clear();
        }

    }

    private void registerEventIfNecessary(final Position position,
                                          final String eventType,
                                          final Map<Long, List<Event>> eventsMap,
                                          final Map<Event, Position> result,
                                          final long deviceId) {

        Event batteryEvent = createEvent(position, eventType);

        if (!eventsMap.containsKey(deviceId)) {
            List<Event> eventsList = new ArrayList<>();
            eventsList.add(batteryEvent);
            eventsMap.put(deviceId, eventsList);
            return;
        }

        List<Event> eventsListForDevice = eventsMap.get(deviceId);

        if (eventsListForDevice.isEmpty()) {
            eventsMap.get(deviceId).add(batteryEvent);
            return;
        }

        if (eventsListForDevice.size() > 0 && eventsListForDevice.size() < batteryEventsAlertThreshold) {

            Event lastEventInList = eventsListForDevice.get(eventsListForDevice.size() - 1);

            long lastEventTime = (long) lastEventInList.getAttributes().get("startTime");
            long currentEventTime = position.getDeviceTime().getTime();

            if (currentEventTime > lastEventTime) {
                eventsListForDevice.add(batteryEvent);
            }
        }

        if (eventsListForDevice.size() >= batteryEventsAlertThreshold) {
            // Generate an alert if we see the configured number or more ext battery disconnects consecutively
            Event currentEvent = createEvent(position, eventType);
            result.put(currentEvent, position);

            // Clear the list of these events
            eventsMap.get(deviceId).clear();
        }
    }

    protected Event createEvent(final Position position, final String eventType) {
        final Event event = new Event(eventType, position.getDeviceId(), position.getId(), position.getDeviceTime());
        event.set("startTime", position.getDeviceTime().getTime());

        // Setting the start and end lat long to keep it consistent with the fuel events.
        event.set("startLat", position.getLatitude());
        event.set("startLong", position.getLongitude());
        event.set("endLat", position.getLatitude());
        event.set("endLong", position.getLongitude());

        return event;
    }
}
