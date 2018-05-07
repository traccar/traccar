package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import com.google.common.collect.BoundType;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import org.apache.commons.lang.StringUtils;
import org.traccar.BaseDataHandler;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.PeripheralSensor;
import org.traccar.model.Position;
import org.traccar.transforms.model.FuelSensorCalibration;
import org.traccar.transforms.model.SensorPointsMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FuelSensorDataHandler extends BaseDataHandler {

    private static final int MIN_VALUES_FOR_MOVING_AVERAGE =
            Context.getConfig().getInteger("processing.peripheralSensorData.minValuesForMovingAverage");

    private static final int MESSAGE_FREQUENCY =
            Context.getConfig().getInteger("processing.peripheralSensorData.messageFrequency");

    private static final int HOURS_OF_DATA_TO_LOAD =
            Context.getConfig().getInteger("processing.peripheralSensorData.hoursOfDataToLoad");

    private static final int MAX_MESSAGES_TO_LOAD = (3600 * HOURS_OF_DATA_TO_LOAD) / MESSAGE_FREQUENCY;

    private static final int MAX_VALUES_FOR_ALERTS =
            Context.getConfig().getInteger("processing.peripheralSensorData.maxValuesForAlerts");

    public static final int STORED_EVENT_LOOK_AROUND_SECONDS =
            Context.getConfig().getInteger("processing.peripheralSensorData.storedEventLookAroundSeconds");

    public static final int CURRENT_EVENT_LOOK_BACK_SECONDS =
            Context.getConfig().getInteger("processing.peripheralSensorData.currentEventLookBackSeconds");

    private static final double FUEL_LEVEL_CHANGE_THRESHOLD_LITRES_DIGITAL =
            Context.getConfig().getDouble("processing.peripheralSensorData.fuelLevelChangeThresholdLitersDigital");

    private static final double FUEL_LEVEL_CHANGE_THRESHOLD_LITRES_ANALOG =
            Context.getConfig().getDouble("processing.peripheralSensorData.fuelLevelChangeThresholdLitersAnalog");

    private static final double FUEL_ERROR_THRESHOLD =
            Context.getConfig().getDouble("processing.peripheralSensorData.fuelErrorThreshold");

    private static final String FUEL_ANALOG = "FUEL_ANALOG";
    private static final String SENSOR_ID = "sensorId";
    private static final String SENSOR_DATA = "sensorData";
    private static final String ADC_1 = "adc1";

    private static final String FREQUENCY_PREFIX = "F=";
    private static final String FUEL_PART_PREFIX = "N=";

    private final Map<String, TreeMultiset<Position>> previousPositions =
            new ConcurrentHashMap<>();

    private final Map<String, FuelEventMetadata> deviceFuelEventMetadata =
            new ConcurrentHashMap<>();

    private boolean loadingOldDataFromDB;

    public FuelSensorDataHandler() {
        loadOldPositions();
    }

    public FuelSensorDataHandler(boolean loader) {

    }

    @Override
    protected Position handlePosition(Position position) {
        try {
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
        } catch (Exception e) {
//            Log.info(String.format("Exception in processing fuel info: %s", e.getMessage()));
            e.printStackTrace();
        } finally {
            return position;
        }
    }

    private void loadOldPositions() {
        this.loadingOldDataFromDB = true;
        Collection<Device> devices = Context.getDeviceManager().getAllDevices();

        Date oneDayAgo = getAdjustedDate(new Date(), Calendar.DAY_OF_MONTH, -1);

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
                this.loadingOldDataFromDB = false;

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
                    peripheralSensorsOnDevice.stream()
                                             .filter(p -> p.getPeripheralSensorId() == positionSensorId).findFirst();

            return digitalSensor.map(sensor -> (int) sensor.getPeripheralSensorId());
        }

        if (position.getAttributes().containsKey(ADC_1)) {
            Optional<PeripheralSensor> analogSensor =
                    peripheralSensorsOnDevice.stream()
                                             .filter(p -> p.getTypeName().equals(FUEL_ANALOG)).findFirst();

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

        Optional<Long> fuelLevelPoints =
                getFuelLevelPointsFromDigitalSensorData(sensorDataString);

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

        // Handle sudden drops in voltage.
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
            // This is a position from the DB, add to the list and move on
            positionsForDeviceSensor.add(position);
            removeFirstPositionIfNecessary(positionsForDeviceSensor);
            return;
        }

        Optional<Double> maybeCalibratedFuelLevel =
                getCalibratedFuelLevel(deviceId, sensorId, fuelLevelPoints);

        if (!maybeCalibratedFuelLevel.isPresent()) {
            // We don't have calibration data for sensor.
            return;
        }

        double calibratedFuelLevel = maybeCalibratedFuelLevel.get();
        position.set(Position.KEY_FUEL_LEVEL, calibratedFuelLevel);

        List<Position> relevantPositionsListForAverages =
                getRelevantPositionsSubList(positionsForDeviceSensor,
                                            position,
                                            MIN_VALUES_FOR_MOVING_AVERAGE);

        double currentFuelLevelAverage = getAverageValue(position, relevantPositionsListForAverages);
        position.set(Position.KEY_FUEL_LEVEL, currentFuelLevelAverage);
        positionsForDeviceSensor.add(position);

        // reassign the list so we get the correct required sublist size for alerts
        List<Position> relevantPositionsListForAlerts =
                getRelevantPositionsSubList(positionsForDeviceSensor,
                                            position,
                                            MAX_VALUES_FOR_ALERTS);


        if (!this.loadingOldDataFromDB && relevantPositionsListForAlerts.size() >= MAX_VALUES_FOR_ALERTS) {
            FuelActivity fuelActivity =
                    checkForActivity(relevantPositionsListForAlerts,
                                                      deviceFuelEventMetadata,
                                                      sensorId,
                                                      fuelLevelChangeThreshold);

            if (fuelActivity.getActivityType() != FuelActivity.FuelActivityType.NONE) {
                //Log.info
                System.out.println("FUEL ACTIVITY DETECTED: " + fuelActivity.getActivityType() +
                         " starting at: " + fuelActivity.getActivityStartTime() +
                         " ending at: " + fuelActivity.getActivityEndTime() +
                         " volume: " + fuelActivity.getChangeVolume() +
                         " start lat, long " + fuelActivity.getActivitystartPosition().getLatitude() +
                         ", " + fuelActivity.getActivitystartPosition().getLongitude() +
                         " end lat, long " + fuelActivity.getActivityEndPosition().getLatitude()
                         + ", " + fuelActivity.getActivityEndPosition().getLongitude());

//                Context.getFcmPushNotificationManager().updateFuelActivity(fuelActivity);
            }
        }

        removeFirstPositionIfNecessary(positionsForDeviceSensor);
    }

    private void removeFirstPositionIfNecessary(TreeMultiset<Position> positionsForDeviceSensor) {
        if (positionsForDeviceSensor.size() > MAX_MESSAGES_TO_LOAD) {
            Position toRemove = positionsForDeviceSensor.firstEntry().getElement();
            positionsForDeviceSensor.remove(toRemove);
        }
    }

    private Date getAdjustedDate(Date fromDate, int type, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.add(type,amount);
        return cal.getTime();
    }

    private Double getAverageValue(Position currentPosition,
                                   List<Position> fuelLevelReadings) {

        Double total = (Double) currentPosition.getAttributes().get(Position.KEY_FUEL_LEVEL);
        for (Position position : fuelLevelReadings) {
            total += (Double) position.getAttributes().get(Position.KEY_FUEL_LEVEL);
        }

        return total / (fuelLevelReadings.size() + 1.0);
    }


    private List<Position> getRelevantPositionsSubList(TreeMultiset<Position> positionsForSensor,
                                                                       Position position,
                                                                       int minListSize) {

        if ((int) position.getAttributes().get("event") >= 100) {

            Position fromPosition = new Position();
            fromPosition.setDeviceTime(getAdjustedDate(position.getDeviceTime(), Calendar.SECOND, -STORED_EVENT_LOOK_AROUND_SECONDS));

            Position toPosition = new Position();
            toPosition.setDeviceTime(getAdjustedDate(position.getDeviceTime(), Calendar.SECOND, STORED_EVENT_LOOK_AROUND_SECONDS));

            List<Position> listToReturn = positionsForSensor.subMultiset(fromPosition, BoundType.OPEN, toPosition, BoundType.CLOSED)
                                                            .stream()
                                                            .collect(Collectors.toList());

            Log.info("STORED DATA relevant list size: " + listToReturn.size());
            return listToReturn;
        }

        if (positionsForSensor.size() <= minListSize) {
            return positionsForSensor.stream()
                                     .collect(Collectors.toList());
        }

        Position fromPosition = new Position();
        fromPosition.setDeviceTime(getAdjustedDate(position.getDeviceTime(), Calendar.SECOND, -CURRENT_EVENT_LOOK_BACK_SECONDS));

        SortedMultiset<Position> positionsSubset =
                positionsForSensor.subMultiset(fromPosition, BoundType.OPEN, position, BoundType.CLOSED);

        if (positionsSubset.size() <= minListSize) {
            return positionsForSensor.stream()
                                     .collect(Collectors.toList());
        }

        int listMaxIndex = positionsSubset.size() - 1;

        return positionsSubset.stream()
                              .collect(Collectors.toList())
                              .subList(listMaxIndex - minListSize, listMaxIndex);
    }

    private Optional<Double> getCalibratedFuelLevel(Long deviceId, Integer sensorId, Long sensorFuelLevelPoints) {

        Optional<FuelSensorCalibration> fuelSensorCalibration =
                Context.getPeripheralSensorManager().
                        getDeviceSensorCalibrationData(deviceId, sensorId);

        if (!fuelSensorCalibration.isPresent()) {
            return Optional.empty();
        }

        // Make a B-tree map of the points to fuel level map
        TreeMap<Long, SensorPointsMap> sensorPointsToVolumeMap =
                new TreeMap<>(fuelSensorCalibration.get().getSensorPointsMap());

        SensorPointsMap previousFuelLevelInfo = sensorPointsToVolumeMap.floorEntry(sensorFuelLevelPoints).getValue();

        double currentAveragePointsPerLitre = previousFuelLevelInfo.getPointsPerLitre();
        long previousPoint = sensorPointsToVolumeMap.floorKey(sensorFuelLevelPoints);
        long previousFuelLevel = previousFuelLevelInfo.getFuelLevel();

        return Optional.of(((sensorFuelLevelPoints - previousPoint) / currentAveragePointsPerLitre) + previousFuelLevel);
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
                fuelEventMetadata.activityStartPosition = readingsForDevice.get(midPoint);
            }
        }

        if (diffInMeans < fuelLevelChangeThreshold && deviceFuelEventMetadata.containsKey(lookupKey)) {
            FuelEventMetadata fuelEventMetadata = deviceFuelEventMetadata.get(lookupKey);
            fuelEventMetadata.endLevel = (double) readingsForDevice.get(midPoint).getAttributes().get(Position.KEY_FUEL_LEVEL);
            fuelEventMetadata.errorCheckEnd = (double) readingsForDevice.get(readingsForDevice.size() - 1).getAttributes().get(Position.KEY_FUEL_LEVEL);
            fuelEventMetadata.endTime = readingsForDevice.get(midPoint).getDeviceTime();
            fuelEventMetadata.activityEndPosition = readingsForDevice.get(midPoint);

            double fuelChangeVolume = fuelEventMetadata.endLevel - fuelEventMetadata.startLevel;
            double errorCheckFuelChange = fuelEventMetadata.errorCheckEnd - fuelEventMetadata.errorCheckStart;
            double errorCheck = fuelChangeVolume * FUEL_ERROR_THRESHOLD;

            if (fuelChangeVolume < 0.0 && errorCheckFuelChange < errorCheck) {
                fuelActivity.setActivityType(FuelActivity.FuelActivityType.FUEL_DRAIN);
                fuelActivity.setChangeVolume(fuelChangeVolume);
                fuelActivity.setActivityStartTime(fuelEventMetadata.startTime);
                fuelActivity.setActivityEndTime(fuelEventMetadata.endTime);
                fuelActivity.setActivitystartPosition(fuelEventMetadata.activityStartPosition);
                fuelActivity.setActivityEndPosition(fuelEventMetadata.activityEndPosition);
                deviceFuelEventMetadata.remove(lookupKey);
            }

            if (fuelChangeVolume > 0.0 && errorCheckFuelChange > errorCheck) {
                fuelActivity.setActivityType(FuelActivity.FuelActivityType.FUEL_FILL);
                fuelActivity.setChangeVolume(fuelChangeVolume);
                fuelActivity.setActivityStartTime(fuelEventMetadata.startTime);
                fuelActivity.setActivityEndTime(fuelEventMetadata.endTime);
                fuelActivity.setActivitystartPosition(fuelEventMetadata.activityStartPosition);
                fuelActivity.setActivityEndPosition(fuelEventMetadata.activityEndPosition);
                deviceFuelEventMetadata.remove(lookupKey);
            }
        }

        return fuelActivity;
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
}
