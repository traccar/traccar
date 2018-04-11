package org.traccar.processing;

import org.traccar.BaseDataHandler;
import org.traccar.Context;
import org.traccar.database.PeripheralSensorManager;
import org.traccar.model.PeripheralSensor;
import org.traccar.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PeripheralSensorDataHandler extends BaseDataHandler {

    public static final int MIN_VALUES_FOR_MOVING_AVERAGE = 2;
    private final Map<Integer, List<Double>> fuelSensorPreviousReadings =
            new ConcurrentHashMap<>();

    @Override
    protected Position handlePosition(Position position) {
        Optional<List<PeripheralSensor>> peripheralSensorsOnDevice =
                Context.getPeripheralSensorManager().getLinkedPeripheralSensors(position.getDeviceId());

        if (position.getAttributes().containsKey("sensorId") &&
                peripheralSensorsOnDevice.isPresent()) {

            // Digital fuel sensor data
            handleDigitalFuelSensorData(position, peripheralSensorsOnDevice.get());
        }

        return position;
    }

    private void handleDigitalFuelSensorData(Position position,
                                             List<PeripheralSensor> digitalFuelSensorsOnDevice) {

        Integer sensorId = (Integer) position.getAttributes().get("sensorId");
        String sensorDataString = (String) position.getAttributes().get("sensorData");

        Optional<Long> fuelLevel = getFuelLevelFromSensorData(sensorDataString);
        if (!fuelLevel.isPresent()) {
            return;
        }

        for (PeripheralSensor peripheralSensor : digitalFuelSensorsOnDevice) {
            if (peripheralSensor.getPeripheralSensorId() == sensorId) {
                handleSensorData(position, sensorId, fuelLevel);
            }
        }

    }

    private void handleSensorData(Position position,
                                  Integer sensorId,
                                  Optional<Long> fuelLevel) {

        if (!fuelLevel.isPresent()) {
            return;
        }

        int previousReadings = fuelSensorPreviousReadings.containsKey(sensorId)? fuelSensorPreviousReadings.get(sensorId).size() : 0;
        double calibratedFuelLevel = getCalibratedFuelLevel(fuelLevel.get());

        if (previousReadings == 0) {
            List<Double> fuelReadingsForSensor = new ArrayList<>();
            fuelReadingsForSensor.add(calibratedFuelLevel);
            fuelSensorPreviousReadings.put(sensorId, fuelReadingsForSensor);
            return;
        } else if (previousReadings < MIN_VALUES_FOR_MOVING_AVERAGE) {
            fuelSensorPreviousReadings.get(sensorId).add(calibratedFuelLevel);
            return;
        }

        // Calculate current level based on moving average. And factor in calibration data.

        double currentFuelLevelAverage = getAverageValue(fuelSensorPreviousReadings.get(sensorId));
        fuelSensorPreviousReadings.get(sensorId).remove(0); // remove the first value in the list
        fuelSensorPreviousReadings.get(sensorId).add(currentFuelLevelAverage);
        position.set(Position.KEY_FUEL_LEVEL, currentFuelLevelAverage);
    }

    private Double getAverageValue(List<Double> fuelLevelReadings) {

        Double total = 0.0;
        for (Double value : fuelLevelReadings) {
            total += value;
        }
        return total / fuelLevelReadings.size();
    }

    private Double getCalibratedFuelLevel(Long sensorFuelLevel) {
        // Have to use the calibration data, once we know what it means. For now return a pre-determined number :D
        return sensorFuelLevel / 10.47;
    }

    private Optional<Long> getFuelLevelFromSensorData(String sensorDataString) {
        if (sensorDataString == null) {
            return Optional.empty();
        }

        String [] sensorDataParts = sensorDataString.split(" "); // Split on space to get the 3 parts
        String frequencyString = sensorDataParts[0];
        String temperatureString = sensorDataParts[1];
        String fuelLevelString = sensorDataParts[MIN_VALUES_FOR_MOVING_AVERAGE];

        if (frequencyString == null || frequencyString.isEmpty() ||
                temperatureString == null || temperatureString.isEmpty() ||
                fuelLevelString == null || fuelLevelString.isEmpty()) {
            return Optional.empty();
        }

        String [] frequencyParts = frequencyString.split("F=");
        if (frequencyParts.length < MIN_VALUES_FOR_MOVING_AVERAGE) {
            return Optional.empty();
        }

        // If frequency is > xFFF (4096), it is invalid per the spec; so ignore it.
        Long frequency = Long.parseLong(frequencyParts[1], 16);
        if (frequency > 0xFFF) {
            return Optional.empty();
        }

        String [] fuelParts = fuelLevelString.split("N=");
        if (fuelParts.length < MIN_VALUES_FOR_MOVING_AVERAGE) {
            return Optional.empty();
        }

        Long fuelLevel = Long.parseLong(fuelParts[1].split("\\.")[0], 16); // Have to account for fractional part
        return Optional.of(fuelLevel);
    }
}
