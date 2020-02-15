package org.traccar.events.custom;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StandstillEventHandler extends BaseEventHandler {

    private Map<Long, Position> deviceStandstillStartPositionMap = new ConcurrentHashMap<>();
    private Map<Long, Event> deviceCurrentEventMap = new ConcurrentHashMap<>();

    private static long DATA_LOSS_THRESHOLD_MILLIS;
    private static int MIN_DISTANCE;

    static {
        DATA_LOSS_THRESHOLD_MILLIS =
                Context.getConfig().getInteger("processing.peripheralSensorData.dataLossThresholdSeconds") * 1000;

        MIN_DISTANCE =
                Context.getConfig().getInteger("coordinates.minError");
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        long deviceId = position.getDeviceId();

        if (!Context.getFcmPushNotificationManager().deviceHasNotificationTypeEnabled(deviceId, Event.TYPE_DEVICE_STANDSTILL)) {
            Log.debug(String.format("[standstill] Device %d not configured for this event. Skipping.", deviceId));
            return null;
        }

        if (!deviceStandstillStartPositionMap.containsKey(deviceId)) {
            Log.debug(String.format("[standstill] Starting meter on %d", deviceId));
            deviceStandstillStartPositionMap.put(deviceId, position);
            return null;
        }

        Position standStillStartPosition = deviceStandstillStartPositionMap.get(deviceId);

        if (standStillStartPosition == null) {
            Log.debug(String.format("[standstill] No previous compare position found for deviceId %d", deviceId));
            return null;
        }

        if (standStillStartPosition.getDeviceTime().getTime() > position.getDeviceTime().getTime()) {
            Log.debug(String.format("[standstill] Back dated payload for checking standstill of deviceId: %d. Ignoring.", deviceId));
            return null;
        }

        Position actualLastPosition = Context.getDeviceManager().getLastPosition(deviceId);
        if (position.getDeviceTime().getTime() - actualLastPosition.getDeviceTime().getTime() >= DATA_LOSS_THRESHOLD_MILLIS) {
            // Reset if data loss
            Log.debug(String.format("[standstill]  Data loss detected on %d. Resetting meter.", deviceId));
            deviceStandstillStartPositionMap.put(deviceId, position);
            if (deviceCurrentEventMap.containsKey(deviceId)) {
                deviceCurrentEventMap.remove(deviceId);
            }
            return null;
        }

        long maxStandstillTimeMillis =
                Context.getDeviceManager()
                       .lookupAttributeLong(deviceId,
                                            "standStillMaxTimeSeconds",
                                            900L,
                                            true) * 1000L;

        long deviceStandstillTime = position.getDeviceTime().getTime() - standStillStartPosition.getDeviceTime().getTime();
        long deviceRunTime = position.getLong(Position.KEY_TOTAL_IGN_ON_MILLIS) - standStillStartPosition.getLong(Position.KEY_TOTAL_IGN_ON_MILLIS);
        double distance = position.getDouble(Position.KEY_TOTAL_DISTANCE) - standStillStartPosition.getDouble(Position.KEY_TOTAL_DISTANCE);

        Log.debug(String.format("[standstill] Checking standstill on %d. deviceStandstillTime: %d, deviceRunTime: %d distance: %f", deviceId, deviceStandstillTime, deviceRunTime, distance));

        if (deviceStandstillTime > maxStandstillTimeMillis
                && deviceRunTime == 0
                && distance < MIN_DISTANCE
                && !deviceCurrentEventMap.containsKey(deviceId)) {

            // Will not put data into db just yet.

            Event event = new Event(Event.TYPE_DEVICE_STANDSTILL, position.getDeviceId(), standStillStartPosition.getId(), standStillStartPosition.getDeviceTime());
            event.set("endPositionId", position.getId());
            event.set("startTime", standStillStartPosition.getDeviceTime().getTime());
            event.set("standStillTime", deviceStandstillTime);
            Log.debug(String.format("[standstill] Registering vehicle standstill event on deviceId: %d", deviceId));
            deviceCurrentEventMap.put(deviceId, event);
            return Collections.singletonMap(event, position);
        }

        if (deviceStandstillTime > maxStandstillTimeMillis
                && deviceRunTime == 0
                && distance < MIN_DISTANCE
                && deviceCurrentEventMap.containsKey(deviceId)) {

            // Still standing, update current event

            Event updateEvent = deviceCurrentEventMap.get(deviceId);
            accumulateStandingTime(updateEvent, position, deviceStandstillTime);
            return null; // Don't make more new events in db.
        }

        if (distance > MIN_DISTANCE || deviceRunTime > 0) {
            Log.debug(String.format("[standstill] Movement or ignition detected on %d. standStillTime: %d distance: %f", deviceId, deviceStandstillTime, distance));
            if (deviceCurrentEventMap.containsKey(deviceId)) {
                // Device moved. End the event and update it db.
                Event finalizedEvent = deviceCurrentEventMap.get(deviceId);
                accumulateStandingTime(finalizedEvent, position, deviceStandstillTime);

                try {
                    Log.debug(String.format("[standstill] Updating final standstill time for %d: ", deviceId));
                    Context.getDataManager().updateObject(finalizedEvent);
                    deviceCurrentEventMap.remove(deviceId);
                    deviceStandstillStartPositionMap.remove(deviceId);
                } catch (SQLException e) {
                    e.printStackTrace();
                    Log.debug(String.format("[standstill] Error updating event for deviceId: %d, startTime: %d", deviceId, finalizedEvent.getDeviceTime().getTime()));
                }

                return null;   // Don't make a new event in the db.
            }

            if (deviceStandstillStartPositionMap.containsKey(deviceId)) {
                // Reset since we detected either movement or ignition
                deviceStandstillStartPositionMap.put(deviceId, position);
            }
        }

        Log.debug(String.format("[standstill] Standstill on %d not sufficient by time or distance. runTime: %d distance: %f", deviceId, deviceStandstillTime, distance));

        // Device didn't stand for too long or moved within the allowed limit.
        return null;
    }

    private void accumulateStandingTime(Event event, Position currentPosition, long deviceStandstillTime) {
        Log.debug(String.format("[standstill] Device %d still standing, accumulated standstill time %d", currentPosition.getDeviceId(), deviceStandstillTime));
        event.set("standStillTime", deviceStandstillTime);
        event.set("endTime", currentPosition.getDeviceTime().getTime());
        event.set("endPositionId", currentPosition.getId());
    }
}
