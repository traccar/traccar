package org.traccar.processing;

import org.apache.commons.lang.StringUtils;
import org.traccar.BaseDataHandler;
import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.PeripheralSensor;
import org.traccar.model.Position;
import org.traccar.transforms.model.FuelSensorCalibration;
import org.traccar.transforms.model.SensorPointsMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeripheralSensorDataHandler extends BaseDataHandler {

    private static final int MIN_VALUES_FOR_MOVING_AVERAGE = 5;

    // At least 5 minutes of data, with a payload coming in every 30 seconds.
    // We'll find the left and right hand means for a given value after we have at least these many data points.
    private static final int MAX_VALUES_FOR_ALERTS = 9;

    private static final double FUEL_THEFT_THRESHOLD_LITRES_DIGITAL = 5.31;
    private static final double FUEL_THEFT_THRESHOLD_LITRES_ANALOG = 12;
    private static final double FUEL_THEFT_DISTANCE_THRESHOLD = 10;
    private static final double FUEL_ERROR_THRESHOLD = 0.75;

    private static final String FUEL_ANALOG = "FUEL_ANALOG";
    private static final String ODOMETER = "odometer";
    private static final String SENSOR_ID = "sensorId";
    private static final String SENSOR_DATA = "sensorData";
    private static final String ADC_1 = "adc1";
    private static final String FUEL = "fuel";
    private static final String EVENT = "event";
    private static final String FREQUENCY_PREFIX = "F=";
    private static final String FUEL_PART_PREFIX = "N=";

    // TODO: Load previous readings from DB on startup
    private final Map<Long, List<Position>> previousPositions =
            new ConcurrentHashMap<>();

    private final Map<Long, List<Integer>> previousOdometerReadings =
            new ConcurrentHashMap<>();

    private final Map<Long, FuelEventMetadata> deviceFuelEventMetadata =
            new ConcurrentHashMap<>();

    @Override
    protected Position handlePosition(Position position) {

        Optional<List<PeripheralSensor>> peripheralSensorsOnDevice =
                Context.getPeripheralSensorManager()
                       .getLinkedPeripheralSensors(position.getDeviceId());

//        if (position.getAttributes().containsKey(ODOMETER)) {
//            handleFuelConsumption(position);
//        }

        if (position.getAttributes().containsKey(SENSOR_ID) &&
                position.getAttributes().containsKey(SENSOR_DATA) &&
                peripheralSensorsOnDevice.isPresent()) {

            // Digital fuel sensor data
            handleDigitalFuelSensorData(position, peripheralSensorsOnDevice.get());
        }

        if (position.getAttributes().containsKey(ADC_1) &&
                peripheralSensorsOnDevice.isPresent()) {

            handleAnalogFuelSensorData(
                    position,
                    peripheralSensorsOnDevice.get());

            // Add in weight sensing also here.

        }

        return position;
    }

    private void handleFuelConsumption(final Position position) {
        Long deviceId = position.getDeviceId();
        Integer odometerReading = (Integer) position.getAttributes().get(ODOMETER);

        if (!previousOdometerReadings.containsKey(deviceId)) {
            List<Integer> deviceOdometerReadings = new ArrayList<>();
            deviceOdometerReadings.add(odometerReading);
            previousOdometerReadings.put(deviceId, deviceOdometerReadings);
            return;
        }

        List<Integer> deviceOdometerReadings = previousOdometerReadings.get(deviceId);

        if (deviceOdometerReadings.size() == MAX_VALUES_FOR_ALERTS) {
            deviceOdometerReadings.remove(0);
        }

        deviceOdometerReadings.add(odometerReading);
        int totalReadingsIndex = deviceOdometerReadings.size() - 1;

        Integer distanceSinceLast = deviceOdometerReadings.get(totalReadingsIndex) - deviceOdometerReadings.get(0);

        if (distanceSinceLast < 0) {
            // Negative distance could mean we got an older position than we already had
            return;
        }

        List<Position> devicePreviousPositions = previousPositions.get(deviceId);
        int devicePositionsTotalIndex = devicePreviousPositions.size() - 1;
        Double fuelLevelDifference = (Double) devicePreviousPositions.get(devicePositionsTotalIndex)
                                                           .getAttributes().get(FUEL) -
                                     (Double) devicePreviousPositions.get(0).getAttributes().get(FUEL);

        if (distanceSinceLast >= 0 &&
            distanceSinceLast < FUEL_THEFT_DISTANCE_THRESHOLD  &&
            fuelLevelDifference >= FUEL_THEFT_THRESHOLD_LITRES_DIGITAL) {

            // TODO send alert!
        }
    }

    private void handleDigitalFuelSensorData(Position position,
                                             List<PeripheralSensor> digitalFuelSensorsOnDevice) {

        Integer sensorId = (Integer) position.getAttributes().get(SENSOR_ID);
        String sensorDataString = (String) position.getAttributes().get(SENSOR_DATA);

        Optional<Long> fuelLevelPoints = getFuelLevelPointsFromDigitalSensorData(sensorDataString);
        if (!fuelLevelPoints.isPresent()) {
            return;
        }

        for (PeripheralSensor peripheralSensor : digitalFuelSensorsOnDevice) {
            if (peripheralSensor.getPeripheralSensorId() == sensorId) {
                handleSensorData(position,
                        peripheralSensor.getPeripheralSensorId(),
                        fuelLevelPoints.get(),
                        FUEL_THEFT_THRESHOLD_LITRES_DIGITAL);
            }
        }
    }

    private void handleAnalogFuelSensorData(Position position,
                                            List<PeripheralSensor> analogFuelSensorsOnDevice) {

        for (PeripheralSensor fuelSensor : analogFuelSensorsOnDevice) {
            if (fuelSensor.getTypeName().equals(FUEL_ANALOG)) {
                Long fuelLevel = (Long) position.getAttributes().get(ADC_1);
                handleSensorData(position,
                        analogFuelSensorsOnDevice.get(0).getPeripheralSensorId(),
                        fuelLevel,
                        FUEL_THEFT_THRESHOLD_LITRES_ANALOG);
            }
        }
    }

    private void handleSensorData(Position position,
                                  Long sensorId,
                                  Long fuelLevelPoints,
                                  double fuelTheftThreshold) {

        if (!isPositionTimestampWithinWindow(position)) {
            if ((int) position.getAttributes().get(EVENT) == 101) {
                // Handle old packet --> do we care, if we're calculating slopes of lines?
            }
            else {
                return;
            }
        }

        long deviceId = position.getDeviceId();
        Optional<Double> maybeCalibratedFuelLevel = getCalibratedFuelLevel(deviceId, sensorId, fuelLevelPoints);

        if (!maybeCalibratedFuelLevel.isPresent()) {
            // We don't have calibration data for sensor.
            return;
        }

        double calibratedFuelLevel = maybeCalibratedFuelLevel.get();
        position.set(Position.KEY_FUEL_LEVEL, calibratedFuelLevel);

        if (!previousPositions.containsKey(deviceId)) {
            List<Position> positionsForSensor = new ArrayList<>();
            positionsForSensor.add(position);
            previousPositions.put(position.getDeviceId(), positionsForSensor);
            return;
        }

        List<Position> readingsForDevice = previousPositions.get(deviceId);

        // Add the current position into list only if it is within a certain
        // timeframe of the last position recorded in the list
        if (!isPositionTimestampWithinWindow(position,
                readingsForDevice
                        .get(readingsForDevice.size() - 1)
                        .getDeviceTime())) {

            return;
        }

        // Take average of existing values if we have less than MIN_VALUES_FOR_MOVING_AVERAGE
        if (readingsForDevice.size() < MIN_VALUES_FOR_MOVING_AVERAGE) {
            double currentFuelLevelAverage = getAverageValue(
                    calibratedFuelLevel, readingsForDevice);

            position.set(Position.KEY_FUEL_LEVEL, currentFuelLevelAverage);
            readingsForDevice.add(position);
            return;
        }

        int maxReadingsIndex = readingsForDevice.size();

        // Get average of the last MIN_VALUES_FOR_MOVING_AVERAGE values.
        double currentFuelLevelAverage = getAverageValue(
                calibratedFuelLevel,
                previousPositions.get(deviceId)
                                 .subList(maxReadingsIndex - MIN_VALUES_FOR_MOVING_AVERAGE, maxReadingsIndex - 1));

        position.set(Position.KEY_FUEL_LEVEL, currentFuelLevelAverage);
        readingsForDevice.add(position);

        if (readingsForDevice.size() >= MAX_VALUES_FOR_ALERTS) {
            checkForActivity(readingsForDevice, deviceFuelEventMetadata, fuelTheftThreshold, currentFuelLevelAverage);
            readingsForDevice.remove(0);
        }
    }

    public void checkForActivity(List<Position> readingsForDevice,
                                 Map<Long, FuelEventMetadata> deviceFuelEventMetadata,
                                 double fuelTheftThreshold,
                                 final double currentFuelLevelAverage) {

        int midPoint = (readingsForDevice.size() - 1) / 2;

//        System.out.println("SIZE: " + readingsForDevice.size() + " : MIDPOINT: " + midPoint);

        double leftSum = 0, rightSum = 0;

        for (int i = 0; i <= midPoint; i++) {
            leftSum += (double) readingsForDevice.get(i).getAttributes().get(Position.KEY_FUEL_LEVEL);
            rightSum += (double) readingsForDevice.get(i + midPoint).getAttributes().get(Position.KEY_FUEL_LEVEL);
        }

        double leftMean = leftSum / (midPoint + 1);
        double rightMean = rightSum / (midPoint + 1);

        double diffInMeans = Math.abs(leftMean - rightMean);

        long deviceId = readingsForDevice.get(0).getDeviceId();

        System.out.println("diff in means: " + diffInMeans + ", theft threshold: " + fuelTheftThreshold);
        if (diffInMeans > fuelTheftThreshold) {
            if (!deviceFuelEventMetadata.containsKey(deviceId)) {
                deviceFuelEventMetadata.put(deviceId, new FuelEventMetadata());
                FuelEventMetadata fuelEventMetadata = deviceFuelEventMetadata.get(deviceId);
                fuelEventMetadata.startLevel = (double) readingsForDevice.get(midPoint).getAttributes().get(Position.KEY_FUEL_LEVEL);
                fuelEventMetadata.errorCheckStart = (double) readingsForDevice.get(0).getAttributes().get(Position.KEY_FUEL_LEVEL);
                System.out.println("START: " + fuelEventMetadata.startLevel + " : " + fuelEventMetadata.errorCheckStart);
            }
        }

        if (diffInMeans < fuelTheftThreshold && deviceFuelEventMetadata.containsKey(deviceId)) {
            FuelEventMetadata fuelEventMetadata = deviceFuelEventMetadata.get(deviceId);
            fuelEventMetadata.endLevel = (double) readingsForDevice.get(midPoint).getAttributes().get(Position.KEY_FUEL_LEVEL);
            fuelEventMetadata.errorCheckEnd = (double) readingsForDevice.get(readingsForDevice.size() - 1).getAttributes().get(Position.KEY_FUEL_LEVEL);

            double fuelChangeVolume = fuelEventMetadata.endLevel - fuelEventMetadata.startLevel;
            double errorCheckFuelChange = fuelEventMetadata.errorCheckEnd - fuelEventMetadata.errorCheckStart;
            double errorCheck = fuelChangeVolume * FUEL_ERROR_THRESHOLD;

            System.out.println("Fuel change volume: " + fuelChangeVolume + ", errorCheckFuelChange: " + errorCheckFuelChange + ", errorCheck" + errorCheck);

            // TODO: send meaningful info in notification.
            if (fuelChangeVolume < 0 && errorCheckFuelChange < errorCheck) {
//                Event fuelDrainEvent = new Event(Event.TYPE_FUEL_DRAIN, deviceId);
//                Context.getFcmPushNotificationManager().updateEvent(fuelDrainEvent, readingsForDevice.get(midPoint));
                System.out.println("FUEL DRAIN DETECTED: " + fuelChangeVolume + " : " + readingsForDevice.get(midPoint).getServerTime());
                deviceFuelEventMetadata.remove(deviceId);

            }

            if (fuelChangeVolume > 0 && errorCheckFuelChange > errorCheck) {
//                Event fuelFill = new Event(Event.TYPE_FUEL_FILL, deviceId);
//                Context.getFcmPushNotificationManager().updateEvent(fuelFill, readingsForDevice.get(midPoint));
                System.out.println("FUEL FILL DETECTED: " + fuelChangeVolume + " : " + readingsForDevice.get(midPoint).getServerTime());
                System.out.println("START: " + fuelEventMetadata.startLevel + " : " + fuelEventMetadata.errorCheckStart);
                System.out.println("END: " + fuelEventMetadata.endLevel + " : " + fuelEventMetadata.errorCheckEnd);
                deviceFuelEventMetadata.remove(deviceId);
            }
        }
    }

    private boolean isPositionTimestampWithinWindow(Position position) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -10);
        Date fiveMinutesBack = calendar.getTime();

        return isPositionTimestampWithinWindow(position, fiveMinutesBack);
    }

    private boolean isPositionTimestampWithinWindow(Position position, Date sometimeBack) {
        return position.getDeviceTime().compareTo(sometimeBack) >= 0;
    }

    private Double getAverageValue(double calibratedFuelLevel, List<Position> fuelLevelReadings) {

        Double total = calibratedFuelLevel;
        for (Position position : fuelLevelReadings) {
            total += (Double) position.getAttributes().get(Position.KEY_FUEL_LEVEL);
        }
        return total / (fuelLevelReadings.size() + 1);
    }

    private Optional<Double> getCalibratedFuelLevel(Long deviceId, Long sensorId, Long sensorFuelLevelPoints) {

        Optional<FuelSensorCalibration> fuelSensorCalibration =
                Context.getPeripheralSensorManager().
                        getDeviceSensorCalibrationData(deviceId, sensorId);

        if (!fuelSensorCalibration.isPresent()) {
            return Optional.empty();
        }

        // Make a B-tree map of the points to fuel level map
        TreeMap<Long, SensorPointsMap> sensorPointsToVolumeMap = new TreeMap<>(fuelSensorCalibration.get().getSensorPointsMap());
        SensorPointsMap previousFuelLevelInfo = sensorPointsToVolumeMap.floorEntry(sensorFuelLevelPoints).getValue();

        double currentAveragePointsPerLitre = previousFuelLevelInfo.getPointsPerLitre(); // A
        long previousPoint = sensorPointsToVolumeMap.floorKey(sensorFuelLevelPoints); //B
        long previousFuelLevel = previousFuelLevelInfo.getFuelLevel(); // D

        return Optional.of(((sensorFuelLevelPoints - previousPoint) / currentAveragePointsPerLitre) + previousFuelLevel);
    }

    private Optional<Long> getFuelLevelPointsFromDigitalSensorData(String sensorDataString) {
        if (StringUtils.isBlank(sensorDataString)) {
            return Optional.empty();
        }

        String [] sensorDataParts = sensorDataString.split(" "); // Split on space to get the 3 parts
        String frequencyString = sensorDataParts[0];
        String temperatureString = sensorDataParts[1];
        String fuelLevelPointsString = sensorDataParts[2];

        if (StringUtils.isBlank(frequencyString) ||
                StringUtils.isBlank(temperatureString) ||
                StringUtils.isBlank(fuelLevelPointsString)) {

            return Optional.empty();
        }

        String [] frequencyParts = frequencyString.split(FREQUENCY_PREFIX);
        if (frequencyParts.length < 2) {
            return Optional.empty();
        }

        // If frequency is > xFFF (4096), it is invalid per the spec; so ignore it.
        Long frequency = Long.parseLong(frequencyParts[1], 16);
        if (frequency > 0xFFF) {
            return Optional.empty();
        }

        String [] fuelParts = fuelLevelPointsString.split(FUEL_PART_PREFIX);
        if (fuelParts.length < 2) {
            return Optional.empty();
        }

        Long fuelSensorPoints = Long.parseLong(fuelParts[1].split("\\.")[0], 16); // Have to account for fractional part
        return Optional.of(fuelSensorPoints);
    }

    public class FuelEventMetadata {
        double startLevel;
        double endLevel;
        double errorCheckStart;
        double errorCheckEnd;
    }
}
