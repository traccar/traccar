package org.traccar;

import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Position;

import java.util.Optional;

public class IgnitionStateHandler extends BaseDataHandler {

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

        // TODO: consider KEY_EXTERNAL_BATTERY_DISCONNECT
        // TODO: will need to calculate using averages.

        Optional<Integer> maybeCurrentVoltage = getMilliVoltsByProtocol(position);

        if (!maybeCurrentVoltage.isPresent()) {
            Log.debug("Current external voltage not found on position.");
            return position;
        }

        Position lastPosition = Context.getDeviceManager().getLastPosition(deviceId);
        if (lastPosition.getDeviceTime().getTime() > position.getDeviceTime().getTime()) {
            Log.debug(String.format("Back dated payload for calculating ignition for deviceId: %d. Ignoring.", deviceId));
            return position;
        }

        int upperThreshold = device.getInteger(BATTERY_UPPER_MILLI_VOLTS_THRESHOLD_FIELD_NAME);
        int lowerThreshold = device.getInteger(BATTERY_LOWER_MILLI_VOLTS_THRESHOLD_FIELD_NAME);

        

        if (maybeCurrentVoltage.get() > upperThreshold) {
            position.set(Position.KEY_CALCULATED_IGNITION, true);
        }

        if (maybeCurrentVoltage.get() <= upperThreshold
            && maybeCurrentVoltage.get() >= lowerThreshold) {
            // Carry forward the previous value, if present.
            Position lastPosition = Context.getDeviceManager().getLastPosition(deviceId);
            if (position.getDeviceTime().getTime() > lastPosition.getDeviceTime().getTime()) {
                if (lastPosition.getAttributes().containsKey(Position.KEY_CALCULATED_IGNITION)) {
                    position.set(Position.KEY_CALCULATED_IGNITION, lastPosition.getBoolean(Position.KEY_CALCULATED_IGNITION));
                }
            }
        }

        if (maybeCurrentVoltage.get() < lowerThreshold) {
            position.set(Position.KEY_CALCULATED_IGNITION, false);
        }

        return position;
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
