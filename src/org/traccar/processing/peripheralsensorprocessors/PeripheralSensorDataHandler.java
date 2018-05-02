package org.traccar.processing.peripheralsensorprocessors;

import com.google.common.collect.BoundType;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import org.apache.commons.lang.StringUtils;
import org.traccar.BaseDataHandler;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.PeripheralSensor;
import org.traccar.model.Position;
import org.traccar.transforms.model.FuelSensorCalibration;
import org.traccar.transforms.model.SensorPointsMap;
import org.traccar.helper.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeripheralSensorDataHandler extends BaseDataHandler {

    private static final int MIN_VALUES_FOR_MOVING_AVERAGE = 5;
    private static final int MESSAGE_FREQUENCY = 30; // 1 every 30 seconds
    private static final int MAX_MESSAGES_TO_LOAD = (3600 * 24) / MESSAGE_FREQUENCY;

    // At least 5 minutes of data, with a payload coming in every 30 seconds.
    // We'll find the left and right hand means for a given value after we have at least these many data points.
    private static final int MAX_VALUES_FOR_ALERTS = 9;

    private static final double FUEL_LEVEL_CHANGE_THRESHOLD_LITRES_DIGITAL = 5.31;
    private static final double FUEL_LEVEL_CHANGE_THRESHOLD_LITRES_ANALOG = 12;
    private static final double FUEL_ERROR_THRESHOLD = 0.75;

    private static final String FUEL_ANALOG = "FUEL_ANALOG";
    private static final String SENSOR_ID = "sensorId";
    private static final String SENSOR_DATA = "sensorData";
    private static final String ADC_1 = "adc1";
    private static final String FREQUENCY_PREFIX = "F=";
    private static final String FUEL_PART_PREFIX = "N=";

    // TODO: Load previous readings from DB on startup
    private final Map<String, TreeMultiset<Position>> previousPositions =
            new ConcurrentHashMap<>();

    private final Map<String, FuelEventMetadata> deviceFuelEventMetadata =
            new ConcurrentHashMap<>();

    public PeripheralSensorDataHandler() {
            loadOldPositions();
            System.out.println("LOADED OLD DATA" + previousPositions.get("1_1").size());
    }

    @Override
    protected Position handlePosition(Position position) {
        Optional<List<PeripheralSensor>> peripheralSensorsOnDevice =
                getLinkedDevices(position.getDeviceId());

        if (!peripheralSensorsOnDevice.isPresent()) {
            return position;
        }

        Optional<Integer> sensorIdOnPosition =
                getSensorId(position, peripheralSensorsOnDevice.get());

        if (!sensorIdOnPosition.isPresent()) {
            return position;
        }

        if (position.getAttributes().containsKey(SENSOR_DATA)) {
            // Digital fuel sensor data
            handleDigitalFuelSensorData(position, sensorIdOnPosition.get(), FUEL_LEVEL_CHANGE_THRESHOLD_LITRES_DIGITAL);
        }

        if (position.getAttributes().containsKey(ADC_1)) {
            handleAnalogFuelSensorData(position, sensorIdOnPosition.get(), FUEL_LEVEL_CHANGE_THRESHOLD_LITRES_ANALOG);
        }

        return position;
    }

    private Date getAdjustedDate(Date fromDate, int type, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.add(type,amount);
        return cal.getTime();
    }

    private void loadOldPositions() {
        Collection<Device> devices = Context.getDeviceManager().getAllDevices();

        Date oneDayAgo = getAdjustedDate(new Date(), Calendar.DAY_OF_MONTH, -3);

        for (Device device : devices) {
            Optional<List<PeripheralSensor>> linkedDevices =
                    getLinkedDevices(device.getId());

            if (!linkedDevices.isPresent()) {
                continue;
            }

            try {
                Collection<Position> devicePositionsInLastDay =
                        Context.getDataManager().getPositions(device.getId(), oneDayAgo, new Date());

                for (Position position : devicePositionsInLastDay) {
                    handlePosition(position);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Optional<List<PeripheralSensor>> getLinkedDevices(long deviceId) {
        return Context.getPeripheralSensorManager()
                      .getLinkedPeripheralSensors(deviceId);
    }

    private Optional<Integer> getSensorId(Position position, List<PeripheralSensor> peripheralSensorsOnDevice) {

        if (position.getAttributes().containsKey(SENSOR_ID)) {
            final int positionSensorId = (int) position.getAttributes().get(SENSOR_ID);
            Optional<PeripheralSensor> digitalSensor =
                    peripheralSensorsOnDevice.stream().filter(p -> p.getPeripheralSensorId() == positionSensorId).findFirst();

            return digitalSensor.map(sensor -> (int) sensor.getPeripheralSensorId());
        }

        if (position.getAttributes().containsKey(ADC_1)) {
            Optional<PeripheralSensor> analogSensor =
                    peripheralSensorsOnDevice.stream().filter(p -> p.getTypeName().equals(FUEL_ANALOG)).findFirst();

            return analogSensor.map(sensor -> (int) sensor.getPeripheralSensorId());
        }

        return Optional.empty();
    }

    private void handleDigitalFuelSensorData(Position position,
                                             int sensorId,
                                             double fuelLevelChangeThreshold) {

        String sensorDataString = (String) position.getAttributes().get(SENSOR_DATA);
        if (StringUtils.isBlank(sensorDataString)) {
            return;
        }

        Optional<Long> fuelLevelPoints = getFuelLevelPointsFromDigitalSensorData(sensorDataString);

        if (!fuelLevelPoints.isPresent()) {
            return;
        }

        handleSensorData(position,
                sensorId,
                fuelLevelPoints.get(),
                fuelLevelChangeThreshold);
    }

    private void handleAnalogFuelSensorData(Position position,
                                            int sensorId, double fuelLevelChangeThreshold) {

        Long fuelLevel = (Long) position.getAttributes().get(ADC_1);
        handleSensorData(position,
                sensorId,
                fuelLevel,
                fuelLevelChangeThreshold);
    }

    private void handleSensorData(Position position,
                                  Integer sensorId,
                                  Long fuelLevelPoints, double fuelLevelChangeThreshold) {

        long deviceId = position.getDeviceId();
        String lookupKey = deviceId + "_" + sensorId;

        if (!previousPositions.containsKey(lookupKey)) {
            TreeMultiset<Position> positions = TreeMultiset.create(Comparator.comparing(Position::getDeviceTime));
            previousPositions.put(lookupKey, positions);
        }

        TreeMultiset<Position> positionsForDeviceSensor = previousPositions.get(lookupKey);

        if (position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
            // This is an old position, add to the list and move on
            positionsForDeviceSensor.add(position);
            //TODO: removing isn't working properly
            removeFirstPositionIfNecessary(positionsForDeviceSensor);
            return;
        }

        Optional<Double> maybeCalibratedFuelLevel = getCalibratedFuelLevel(deviceId, sensorId, fuelLevelPoints);

        if (!maybeCalibratedFuelLevel.isPresent()) {
            // We don't have calibration data for sensor.
            return;
        }

        double calibratedFuelLevel = maybeCalibratedFuelLevel.get();
        position.set(Position.KEY_FUEL_LEVEL, calibratedFuelLevel);
        positionsForDeviceSensor.add(position);

        List<Position> relevantPositionsList =
                getRelevantPositionsSubListForAverages(positionsForDeviceSensor, position, MAX_VALUES_FOR_ALERTS);

        relevantPositionsList.add(position);

        double currentFuelLevelAverage = getAverageValue(relevantPositionsList);
        position.set(Position.KEY_FUEL_LEVEL, currentFuelLevelAverage);

        if (relevantPositionsList.size() >= MAX_VALUES_FOR_ALERTS) {
            FuelActivity fuelActivity = checkForActivity(relevantPositionsList, deviceFuelEventMetadata, sensorId, fuelLevelChangeThreshold);
            if (fuelActivity.getActivityType() != FuelActivity.FuelActivityType.NONE) {
//                Context.getFcmPushNotificationManager().updateFuelActivity(fuelActivity, position);
                Log.info("FUEL ACTIVITY DETECTED: " + fuelActivity.getActivityType() +
                        " starting at: " + fuelActivity.getActivityStartTime() +
                        " ending at: " + fuelActivity.getActivityEndTime() +
                        " volume: " + fuelActivity.getChangeVolume());
            }
        }

        removeFirstPositionIfNecessary(positionsForDeviceSensor);
    }

    private void removeFirstPositionIfNecessary(TreeMultiset<Position> positionsForDeviceSensor) {
        if (positionsForDeviceSensor.size() > MAX_MESSAGES_TO_LOAD) {
            castToPositionsList(Arrays.asList(positionsForDeviceSensor.toArray())).remove(0);
        }
    }


    private List<Position> getRelevantPositionsSubListForAverages(TreeMultiset<Position> positionsForSensor,
                                                                  Position position,
                                                                  int minListSize) {

        if (positionsForSensor.size() < minListSize) {
            return castToPositionsList(Arrays.asList(positionsForSensor.toArray()));
        }

        if ((int) position.getAttributes().get("event") >= 100) {
            Position fromPosition = new Position();
            Date fromDeviceTime = getAdjustedDate(position.getDeviceTime(), Calendar.MINUTE, -2);
            fromPosition.setDeviceTime(fromDeviceTime);

            Position toPosition = new Position();
            Date toDeviceTime = getAdjustedDate(position.getDeviceTime(), Calendar.MINUTE, 2);
            toPosition.setDeviceTime(toDeviceTime);

            SortedMultiset<Position> positionsSubset =
                    positionsForSensor.subMultiset(fromPosition, BoundType.OPEN, toPosition, BoundType.CLOSED);

            return castToPositionsList(Arrays.asList(positionsSubset.toArray()));
        }

        List<Position> positionsList = castToPositionsList(Arrays.asList(positionsForSensor.toArray()));
        int maxReadingsIndex = positionsForSensor.size();
        // TODO: Crawl to get the last minListSize so that there's no big gap in data.
        return positionsList.subList(maxReadingsIndex - minListSize, maxReadingsIndex - 1);
    }

    private List<Position> castToPositionsList(List<Object> positions) {
        List<Position> positionsList = new ArrayList<>();
        positions.forEach(p -> positionsList.add((Position) p));
        return positionsList;
    }

    public FuelActivity checkForActivity(List<Position> readingsForDevice,
                                 Map<String, FuelEventMetadata> deviceFuelEventMetadata,
                                 long sensorId,
                                 double fuelLevelChangeThreshold) {

        FuelActivity fuelActivity = new FuelActivity();

        int midPoint = (readingsForDevice.size() - 1) / 2;
        double leftSum = 0, rightSum = 0;

        for (int i = 0; i <= midPoint; i++) {
            leftSum += (double) readingsForDevice.get(i).getAttributes().get(Position.KEY_FUEL_LEVEL);
            rightSum += (double) readingsForDevice.get(i + midPoint).getAttributes().get(Position.KEY_FUEL_LEVEL);
        }

        double leftMean = leftSum / (midPoint + 1);
        double rightMean = rightSum / (midPoint + 1);
        double diffInMeans = Math.abs(leftMean - rightMean);

        long deviceId = readingsForDevice.get(0).getDeviceId();
        String lookupKey = deviceId + "_" + sensorId;

        if (diffInMeans > fuelLevelChangeThreshold) {
            if (!deviceFuelEventMetadata.containsKey(lookupKey)) {
                deviceFuelEventMetadata.put(lookupKey, new FuelEventMetadata());
                FuelEventMetadata fuelEventMetadata = deviceFuelEventMetadata.get(lookupKey);
                fuelEventMetadata.startLevel = (double) readingsForDevice.get(midPoint).getAttributes().get(Position.KEY_FUEL_LEVEL);
                fuelEventMetadata.errorCheckStart = (double) readingsForDevice.get(0).getAttributes().get(Position.KEY_FUEL_LEVEL);
                fuelEventMetadata.startTime = readingsForDevice.get(midPoint).getDeviceTime();
            }
        }

        if (diffInMeans < fuelLevelChangeThreshold && deviceFuelEventMetadata.containsKey(lookupKey)) {
            FuelEventMetadata fuelEventMetadata = deviceFuelEventMetadata.get(lookupKey);
            fuelEventMetadata.endLevel = (double) readingsForDevice.get(midPoint).getAttributes().get(Position.KEY_FUEL_LEVEL);
            fuelEventMetadata.errorCheckEnd = (double) readingsForDevice.get(readingsForDevice.size() - 1).getAttributes().get(Position.KEY_FUEL_LEVEL);
            fuelEventMetadata.endTime = readingsForDevice.get(midPoint).getDeviceTime();

            double fuelChangeVolume = fuelEventMetadata.endLevel - fuelEventMetadata.startLevel;
            double errorCheckFuelChange = fuelEventMetadata.errorCheckEnd - fuelEventMetadata.errorCheckStart;
            double errorCheck = fuelChangeVolume * FUEL_ERROR_THRESHOLD;

            if (fuelChangeVolume < 0 && errorCheckFuelChange < errorCheck) {
                fuelActivity.setActivityType(FuelActivity.FuelActivityType.FUEL_DRAIN);
                fuelActivity.setChangeVolume(fuelChangeVolume);
                fuelActivity.setActivityStartTime(fuelEventMetadata.startTime);
                fuelActivity.setActivityEndTime(fuelEventMetadata.endTime);
                deviceFuelEventMetadata.remove(lookupKey);
            }

            if (fuelChangeVolume > 0 && errorCheckFuelChange > errorCheck) {
                fuelActivity.setActivityType(FuelActivity.FuelActivityType.FUEL_FILL);
                fuelActivity.setChangeVolume(fuelChangeVolume);
                fuelActivity.setActivityStartTime(fuelEventMetadata.startTime);
                fuelActivity.setActivityEndTime(fuelEventMetadata.endTime);
                deviceFuelEventMetadata.remove(lookupKey);
            }
        }

        return fuelActivity;
    }

    private Double getAverageValue(List<Position> fuelLevelReadings) {

        Double total = 0.0;
        for (Position position : fuelLevelReadings) {
            total += (Double) position.getAttributes().get(Position.KEY_FUEL_LEVEL);
        }
        return total / (fuelLevelReadings.size() + 1);
    }

    private Optional<Double> getCalibratedFuelLevel(Long deviceId, Integer sensorId, Long sensorFuelLevelPoints) {

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
        Date startTime;
        Date endTime;
    }
}
