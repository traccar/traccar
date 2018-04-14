package org.traccar.processing;

import org.traccar.BaseDataHandler;
import org.traccar.Context;
import org.traccar.model.PeripheralSensor;
import org.traccar.model.Position;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class PeripheralSensorDataHandler extends BaseDataHandler {

    public static final int MIN_VALUES_FOR_MOVING_AVERAGE = 2;
    public static final int MAX_VALUES_FOR_ALERTS = 180; // 30 minutes of data, with a payload coming in every 10 seconds.
    public static final int FUEL_THEFT_THRESHOLD_LITRES = 5;
    public static final int FUEL_THEFT_DISTANCE_THRESHOLD = 10;

    // TODO: Load previous readings from DB on startup
    private final Map<Integer, List<Position>> previousPositions =
            new ConcurrentHashMap<>();

    private final Map<Long, List<Long>> previousOdometerReadings =
            new ConcurrentHashMap<>();


    @Override
    protected Position handlePosition(Position position) {
        Optional<List<PeripheralSensor>> peripheralSensorsOnDevice =
                Context.getPeripheralSensorManager()
                       .getLinkedPeripheralSensors(position.getDeviceId());

        if (position.getAttributes().containsKey("odometer")) {
            handleFuelConsumption(position);
        }

        if (position.getAttributes().containsKey("sensorId") &&
                peripheralSensorsOnDevice.isPresent()) {

            // Digital fuel sensor data
            handleDigitalFuelSensorData(position, peripheralSensorsOnDevice.get());
        }

        return position;
    }

    private void handleFuelConsumption(final Position position) {
        Long deviceId = position.getDeviceId();
        Long odometerReading = (Long) position.getAttributes().get("odometer");

        if (!previousOdometerReadings.containsKey(deviceId)) {
            List<Long> deviceOdometerReadings = new ArrayList<>();
            deviceOdometerReadings.add(odometerReading);
            previousOdometerReadings.put(deviceId, deviceOdometerReadings);
            return;
        }

        List<Long> deviceOdometerReadings = previousOdometerReadings.get(deviceId);

        if (deviceOdometerReadings.size() == MAX_VALUES_FOR_ALERTS) {
            deviceOdometerReadings.remove(0);
        }

        deviceOdometerReadings.add(odometerReading);
        int totalReadingsIndex = deviceOdometerReadings.size() - 1;

        Long distanceSinceLast = deviceOdometerReadings.get(totalReadingsIndex) - deviceOdometerReadings.get(0);

        if (distanceSinceLast < 0) {
            // Negative distance could mean we got an older position than we already had
            return;
        }

        List<Position> devicePreviousPositions = previousPositions.get(deviceId);
        int devicePositionsTotalIndex = devicePreviousPositions.size() - 1;
        Double fuelLevelDifference = (Double) devicePreviousPositions.get(devicePositionsTotalIndex)
                                                           .getAttributes().get("fuel") -
                                     (Double) devicePreviousPositions.get(0).getAttributes().get("fuel");

        if (distanceSinceLast >= 0 &&
            distanceSinceLast < FUEL_THEFT_DISTANCE_THRESHOLD  &&
            fuelLevelDifference >= FUEL_THEFT_THRESHOLD_LITRES) {

            // TODO send alert!
        }
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

        if (!isPositionTimestampWithinWindow(position)) {
            return;
        }

        int previousReadings = previousPositions.containsKey(sensorId)? previousPositions.get(sensorId).size() : 0;
        double calibratedFuelLevel = getCalibratedFuelLevel(fuelLevel.get());

        if (previousReadings == 0) {
            List<Position> positionsForSensor = new ArrayList<>();
            positionsForSensor.add(position);

            previousPositions.put(sensorId, positionsForSensor);
            return;
        }

        if (!isPositionTimestampWithinWindow(position,
                                            previousPositions.get(sensorId)
                                                             .get(previousPositions.get(sensorId).size() - 1)
                                                             .getDeviceTime())) {

            return;
        }

        // Calculate current level based on moving average. And factor in calibration data.

        int maxReadingsIndex = previousPositions.get(sensorId).size() - 1;
        double currentFuelLevelAverage = getAverageValue(
                previousPositions.get(sensorId)
                                 .subList(maxReadingsIndex - MIN_VALUES_FOR_MOVING_AVERAGE, maxReadingsIndex));

        List<Position> devicePreviousPositions = previousPositions.get(sensorId);
        if (devicePreviousPositions.size() == MAX_VALUES_FOR_ALERTS) {
            devicePreviousPositions.remove(0);
        }

        position.set(Position.KEY_FUEL_LEVEL, currentFuelLevelAverage);
        devicePreviousPositions.add(position);
    }

    private boolean isPositionTimestampWithinWindow(Position position) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -5);
        Date fiveMinutesBack = calendar.getTime();

        return isPositionTimestampWithinWindow(position, fiveMinutesBack);

    }

    private boolean isPositionTimestampWithinWindow(Position position, Date sometimeBack) {
        return position.getDeviceTime().compareTo(sometimeBack) >= 0;
    }

    private Double getAverageValue(List<Position> fuelLevelReadings) {

        Double total = 0.0;
        for (Position position : fuelLevelReadings) {
            total += (Double) position.getAttributes().get(Position.KEY_FUEL_LEVEL);
        }
        return total / fuelLevelReadings.size();
    }

    private Double getCalibratedFuelLevel(Long sensorFuelLevel) {
        // TODO: Have to use the calibration data, once we know what it means. For now return a pre-determined number :D
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
