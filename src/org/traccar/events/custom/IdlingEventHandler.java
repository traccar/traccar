package org.traccar.events.custom;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdlingEventHandler extends BaseEventHandler {

    private Map<Long, Position> deviceIdlingStartPositionMap = new ConcurrentHashMap<>();
    private Map<Long, Event> deviceCurrentEventMap = new ConcurrentHashMap<>();

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
        if (!Context.getFcmPushNotificationManager().deviceHasNotificationTypeEnabled(deviceId, Event.TYPE_DEVICE_IDLING)) {
            Log.debug(String.format("[idling] Device %d not configured for this event. Skipping.", deviceId));
            return null;
        }

        long maxIdlingTimeMillis =
                Context.getDeviceManager()
                       .lookupAttributeLong(deviceId,
                                            "idlingMaxTimeSeconds",
                                            300L,
                                            true) * 1000L;

        if (!deviceIdlingStartPositionMap.containsKey(deviceId)) {
            if (position.getLong(Position.KEY_IGN_ON_MILLIS) > 0 && position.getDouble(Position.KEY_DISTANCE) == 0.0) {
                Log.debug(String.format("[idling] Starting meter on %d", deviceId));
                deviceIdlingStartPositionMap.put(deviceId, position);
            }
            return null;
        }

        Position idlingStartPosition = deviceIdlingStartPositionMap.get(deviceId);

        if (idlingStartPosition == null) {
            Log.debug(String.format("[idling] No previous compare position found for deviceId %d", deviceId));
            return null;
        }

        if (idlingStartPosition.getDeviceTime().getTime() > position.getDeviceTime().getTime()) {
            Log.debug(String.format("[idling] Back dated payload for checking standstill of deviceId: %d. Ignoring.", deviceId));
            return null;
        }

        Position actualLastPosition = Context.getDeviceManager().getLastPosition(deviceId);
        if (position.getDeviceTime().getTime() - actualLastPosition.getDeviceTime().getTime() >= DATA_LOSS_FOR_IGNITION_MILLIS) {
            // Reset if data loss
            Log.debug(String.format("[idling] Data loss detected on %d. Resetting meter.", deviceId));
            if (deviceIdlingStartPositionMap.containsKey(deviceId)) {
                deviceIdlingStartPositionMap.remove(deviceId);
            }

            if (deviceCurrentEventMap.containsKey(deviceId)) {
                deviceCurrentEventMap.remove(deviceId);
            }

            return null;
        }

        long deviceRunTime = position.getLong(Position.KEY_TOTAL_IGN_ON_MILLIS) - idlingStartPosition.getLong(Position.KEY_TOTAL_IGN_ON_MILLIS);
        double distance = position.getDouble(Position.KEY_TOTAL_DISTANCE) - idlingStartPosition.getDouble(Position.KEY_TOTAL_DISTANCE);

        Log.debug(String.format("[idling] Checking idling on %d. runTime: %d distance: %f", deviceId, deviceRunTime, distance));

        if (deviceRunTime > maxIdlingTimeMillis
                && distance < MIN_DISTANCE
                && !deviceCurrentEventMap.containsKey(deviceId)) { // Engine run but not moved

            Log.debug(String.format("[idling] Registering vehicle idling event on deviceId: %d", deviceId));
            Event event = new Event(Event.TYPE_DEVICE_IDLING, position.getDeviceId(), idlingStartPosition.getId(), idlingStartPosition.getDeviceTime());
            event.set("endPositionId", position.getId());
            event.set("startTime", idlingStartPosition.getDeviceTime().getTime());
            event.set("idlingTime", deviceRunTime);
            deviceCurrentEventMap.put(deviceId, event);
            return Collections.singletonMap(event, position);
        }

        if (deviceRunTime > maxIdlingTimeMillis
                && distance < MIN_DISTANCE
                && deviceCurrentEventMap.containsKey(deviceId)) {
            // Still idling, not moved. Update current event
            Event updateEvent = deviceCurrentEventMap.get(deviceId);
            accumulateIdlingTime(updateEvent, position);

            return null; // Don't make any more new events in db.
        }

        if (position.getLong(Position.KEY_IGN_ON_MILLIS) == 0 || distance > MIN_DISTANCE) {
            Log.debug(String.format("[idling] Allowed movement detected or ignition switched off on %d. runTime: %d distance: %f", deviceId, deviceRunTime, distance));
            if (deviceCurrentEventMap.containsKey(deviceId)) {
                Event finalizedEvent = deviceCurrentEventMap.get(deviceId);
                accumulateIdlingTime(finalizedEvent, position);

                try {
                    Log.debug(String.format("[idling] Updating final idling time for %d", deviceId));
                    Context.getDataManager().updateObject(finalizedEvent);
                    deviceCurrentEventMap.remove(deviceId);
                    deviceIdlingStartPositionMap.remove(deviceId);
                } catch (SQLException e) {
                    e.printStackTrace();
                    Log.debug(String.format("[idling] Error updating event for deviceId: %d, startTime: %d", deviceId, finalizedEvent.getDeviceTime().getTime()));
                }
                return null;   // Make a new event in the db.
            }

            if (deviceIdlingStartPositionMap.containsKey(deviceId)) {
                // Reset since we detected either movement or ignition
                deviceIdlingStartPositionMap.put(deviceId, position);
            }

        }

        Log.debug(String.format("[idling] Idling on %d not sufficient by time or distance. runTime: %d distance: %f", deviceId, deviceRunTime, distance));

        // If the deviceRunTime is lesser than the max idling time, we don't care if the device has moved or not.
        return null;
    }

    private void accumulateIdlingTime(Event event, Position currentPosition) {
        long idlingTimeAccumulator = event.getLong("idlingTime") + currentPosition.getLong(Position.KEY_IGN_ON_MILLIS);
        event.set("idlingTime", idlingTimeAccumulator);
        event.set("endTime", currentPosition.getDeviceTime().getTime());
        event.set("endPositionId", currentPosition.getId());

        Log.debug(String.format("[idling] Device %d still standing, accumulated run-time %d", currentPosition.getDeviceId(), idlingTimeAccumulator));
    }
}
