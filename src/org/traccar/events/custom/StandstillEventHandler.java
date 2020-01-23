package org.traccar.events.custom;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StandstillEventHandler extends BaseEventHandler {

    private Map<Long, Position> deviceIdlingStartPositionMap = new ConcurrentHashMap<>();

    private static long DATA_LOSS_FOR_IGNITION_MILLIS;
    private static int MIN_DISTANCE;

    static {
        DATA_LOSS_FOR_IGNITION_MILLIS =
                Context.getConfig().getLong("processing.peripheralSensorData.ignitionDataLossThresholdSeconds") * 1000L;

        MIN_DISTANCE =
                Context.getConfig().getInteger("coordinates.minError");
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        long deviceId = position.getDeviceId();
        long maxIdlingTimeMillis =
                Context.getDeviceManager()
                       .lookupAttributeLong(deviceId,
                                            "idlingMaxTimeSeconds",
                                            300L,
                                            true) * 1000L;

        if (!deviceIdlingStartPositionMap.containsKey(deviceId)) {
            if (position.getLong(Position.KEY_IGN_ON_MILLIS) > 0) {
                deviceIdlingStartPositionMap.put(deviceId, position);
            }
            return null;
        }

        Position lastPosition = deviceIdlingStartPositionMap.get(deviceId);

        if (lastPosition == null) {
            Log.debug(String.format("No last position found for deviceId %d", deviceId));
            return null;
        }

        if (lastPosition.getDeviceTime().getTime() > position.getDeviceTime().getTime()) {
            Log.debug(String.format("Back dated payload for checking standstill of deviceId: %d. Ignoring.", deviceId));
            return null;
        }

        if (position.getDeviceTime().getTime() - lastPosition.getDeviceTime().getTime() >= DATA_LOSS_FOR_IGNITION_MILLIS) {
            // Reset if data loss
            if (deviceIdlingStartPositionMap.containsKey(deviceId)) {
                deviceIdlingStartPositionMap.remove(deviceId);
            }

            if (position.getLong(Position.KEY_IGN_ON_MILLIS) > 0) {
                deviceIdlingStartPositionMap.put(deviceId, position);
            }

            return null;
        }

        long deviceRunTime = position.getLong(Position.KEY_TOTAL_IGN_ON_MILLIS) - lastPosition.getLong(Position.KEY_TOTAL_IGN_ON_MILLIS);
        double distance = position.getDouble(Position.KEY_TOTAL_DISTANCE) - lastPosition.getDouble(Position.KEY_TOTAL_DISTANCE);

        if (deviceRunTime > maxIdlingTimeMillis && distance < MIN_DISTANCE) { // Engine run but not moved
            Event event = new Event(Event.TYPE_DEVICE_STANDSTILL, position.getDeviceId(), lastPosition.getId(), lastPosition.getDeviceTime());
            event.set("endPositionId", position.getId());
            event.set("startTime", lastPosition.getDeviceTime().getTime());
            event.set("endTime", position.getDeviceTime().getTime());
            event.set("idlingTime", deviceRunTime);
            deviceIdlingStartPositionMap.put(deviceId, position);
            Log.debug(String.format("Detected vehicle idling on deviceId: %d", deviceId));
            return Collections.singletonMap(event, position);
        }

        if (distance > MIN_DISTANCE) {
            deviceIdlingStartPositionMap.put(deviceId, position);
        }

        // If the deviceRunTime is lesser than the max idling time, we don't care if the device has moved or not.
        return null;
    }
}
