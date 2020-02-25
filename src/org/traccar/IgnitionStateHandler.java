package org.traccar;

import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Position;

import java.util.Optional;

public class IgnitionStateHandler extends BaseDataHandler {

    private static long DATA_LOSS_FOR_IGNITION_MILLIS;
    private static int MIN_DISTANCE;

    static {
        DATA_LOSS_FOR_IGNITION_MILLIS =
                Context.getConfig().getLong("processing.peripheralSensorData.ignitionDataLossThresholdSeconds") * 1000L;
        MIN_DISTANCE =
                Context.getConfig().getInteger("coordinates.minError");
    }

    private String BATTERY_UPPER_MILLI_VOLTS_THRESHOLD_FIELD_NAME = "ext_volt_upper";
    private String BATTERY_LOWER_MILLI_VOLTS_THRESHOLD_FIELD_NAME = "ext_volt_lower";

    @Override
    protected Position handlePosition(Position position) {
        long deviceId = position.getDeviceId();

        if (Context.getDeviceManager() == null) {
            return position;
        }

        Device device = Context.getDeviceManager().getById(deviceId);

        if (device == null) {
            Log.debug(String.format("Device not found: %d", deviceId));
            return position;
        }

        if (!device.getAttributes().containsKey(BATTERY_UPPER_MILLI_VOLTS_THRESHOLD_FIELD_NAME) ||
                !device.getAttributes().containsKey(BATTERY_LOWER_MILLI_VOLTS_THRESHOLD_FIELD_NAME)) {
            // Not going to log this coz this will produce way too many logs.
            return position;
        }

        // TODO: consider KEY_EXTERNAL_BATTERY_DISCONNEt

        Optional<Integer> maybeCurrentVoltage = getMilliVoltsByProtocol(position);

        if (!maybeCurrentVoltage.isPresent()) {
            Log.debug("Current external voltage not found on position.");
            return position;
        }

        Position lastPosition = Context.getDeviceManager().getLastPosition(deviceId);
        if (lastPosition == null) {
            position.set(Position.KEY_CALCULATED_IGNITION, false);
            initializeMeter(position);
            return position;
        }

        if (lastPosition.getDeviceTime().getTime() > position.getDeviceTime().getTime()) {
            Log.debug(String.format("Back dated payload for calculating ignition for deviceId: %d. Ignoring.", deviceId));
            return position;
        }

        if (position.getDeviceTime().getTime() - lastPosition.getDeviceTime().getTime() >= DATA_LOSS_FOR_IGNITION_MILLIS) {
            // Reset calc run time values in case of data loss.
            position.set(Position.KEY_CALCULATED_IGNITION, false);
            initializeMeter(position, lastPosition);
            return position;
        }

        // No data loss & we have last position. Calculate ignition state, and then run time.

        int upperThreshold = device.getInteger(BATTERY_UPPER_MILLI_VOLTS_THRESHOLD_FIELD_NAME);
        int lowerThreshold = device.getInteger(BATTERY_LOWER_MILLI_VOLTS_THRESHOLD_FIELD_NAME);
        double distance = position.getDouble(Position.KEY_DISTANCE);

        if (maybeCurrentVoltage.get() > upperThreshold || distance > MIN_DISTANCE) {
            position.set(Position.KEY_CALCULATED_IGNITION, true);
        } else if (distance > MIN_DISTANCE ||
                (maybeCurrentVoltage.get() <= upperThreshold
                        && maybeCurrentVoltage.get() >= lowerThreshold)) {
            // Carry forward the previous value, if present.
            if (lastPosition.getAttributes().containsKey(Position.KEY_CALCULATED_IGNITION)) {
                position.set(Position.KEY_CALCULATED_IGNITION, lastPosition.getBoolean(Position.KEY_CALCULATED_IGNITION));
            }

        } else if (maybeCurrentVoltage.get() < lowerThreshold) {
            position.set(Position.KEY_CALCULATED_IGNITION, false);
        }

        determineRunTimeFromState(position, lastPosition);
        return position;
    }

    private void determineRunTimeFromState(Position position, Position lastPosition) {

        if (!lastPosition.getAttributes().containsKey(Position.KEY_CALCULATED_IGNITION)) {
            initializeMeter(position);
        }

        boolean previousState = lastPosition.getBoolean(Position.KEY_CALCULATED_IGNITION);
        boolean currenState = position.getBoolean(Position.KEY_CALCULATED_IGNITION);

        if (currenState && !previousState) {
            // Start hour meter
            initializeMeter(position, lastPosition);
        } else if (!currenState && previousState) {
            // Continue hour meter, last increment
            continueRunningMeter(position, lastPosition);
        } else if (currenState && previousState) { // Has remained on.
            // Keep the hour meter running
            continueRunningMeter(position, lastPosition);
        } else if (!currenState && !previousState) {

            // Carry 0s over from last position.
            initializeMeter(position, lastPosition);
        }
    }

    private void initializeMeter(Position position) {
        position.set(Position.KEY_CALC_IGN_ON_MILLIS, 0L);
        position.set(Position.KEY_TOTAL_CALC_IGN_ON_MILLIS, 0L);
    }

    private void initializeMeter(final Position position, final Position lastPosition) {
        long totalHours = lastPosition.getLong(Position.KEY_TOTAL_CALC_IGN_ON_MILLIS);
        position.set(Position.KEY_CALC_IGN_ON_MILLIS, 0L);
        position.set(Position.KEY_TOTAL_CALC_IGN_ON_MILLIS, totalHours);
    }

    private void continueRunningMeter(final Position position, final Position lastPosition) {

        long millisIgnOn = position.getDeviceTime().getTime() - lastPosition.getDeviceTime().getTime();
        long totalMillisIgnOn = lastPosition.getLong(Position.KEY_TOTAL_CALC_IGN_ON_MILLIS) + millisIgnOn;

        position.set(Position.KEY_CALC_IGN_ON_MILLIS, millisIgnOn);
        position.set(Position.KEY_TOTAL_CALC_IGN_ON_MILLIS, totalMillisIgnOn);
    }

    private Optional<Integer> getMilliVoltsByProtocol(Position position) {
        Number currentVoltage = (Number) position.getAttributes().get(Position.KEY_POWER); // External battery voltage.

        switch (position.getProtocol().toLowerCase()) {
            case "aquila":
                return Optional.of(currentVoltage.intValue()); // Is already in milli volts
            case "teltonika":
                return Optional.of( (int) (currentVoltage.floatValue() * 1000)); // Convert from volts to milli volts
            default:
                Log.debug("Unknown protocol, not attempting to read external battery voltage values.");
        }

        return Optional.empty();
    }
}
