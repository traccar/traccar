package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import com.google.common.collect.Lists;
import com.google.common.collect.TreeMultiset;
import org.traccar.BaseDataHandler;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Event;
import org.traccar.model.PeripheralSensor;
import org.traccar.model.Position;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.FuelActivity.FuelActivityType;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.traccar.Context.getDataManager;

public class FuelSensorDataHandler extends BaseDataHandler {

    private static final int SECONDS_IN_ONE_HOUR = 3600;
    private static final String ALL_FUEL_FIELDS = "ALL_FUEL_FIELDS";

    private static int maxInMemoryPreviousPositionsListSize;
    private static int hoursOfDataToLoad;
    private static int dataLossThresholdSeconds;

    private final Map<Long, Boolean> possibleDataLossByDevice = new ConcurrentHashMap<>();
    private final Map<Long, Position> nonOutlierInLastWindowByDevice = new ConcurrentHashMap<>();
    private final Map<String, TreeMultiset<Position>> previousPositions = new ConcurrentHashMap<>();
    private final Map<String, FuelEventMetadata> deviceFuelEventMetadata = new ConcurrentHashMap<>();
    private final Map<Long, Position> deviceLastKnownOdometerPositionLookup = new ConcurrentHashMap<>();
    private final Map<String, Position> previousAlertWindowStart = new ConcurrentHashMap<>();
    private boolean loadingOldDataFromDB = false;

    static {
        int messageFrequencyInSeconds = Context.getConfig()
                                               .getInteger("processing.peripheralSensorData.messageFrequency");

        hoursOfDataToLoad = Context.getConfig().getInteger("processing.peripheralSensorData.hoursOfDataToLoad");

        int minHoursOfDataInMemory = Context.getConfig()
                                            .getInteger("processing.peripheralSensorData.minHoursOfDataInMemory");

        // If hoursOfDataToLoad = 0, then keep at least minHoursOfDataInMemory hours of data in memory
        maxInMemoryPreviousPositionsListSize =
                (SECONDS_IN_ONE_HOUR * (hoursOfDataToLoad > 0
                        ? hoursOfDataToLoad : minHoursOfDataInMemory)) / messageFrequencyInSeconds;

        dataLossThresholdSeconds =
                Context.getConfig()
                       .getInteger("processing.peripheralSensorData.dataLossThresholdSeconds") * 1000;


    }

    public FuelSensorDataHandler(String protocol) {
        loadOldPositions(protocol);
    }

    @Override
    protected Position handlePosition(Position position) {
        long deviceId = position.getDeviceId();

        long startProcessing = new Date().getTime();
        logDebugIfNotLoading(String.format("[FuelSensorDataHandler] Received position for %d", deviceId), deviceId);
        try {

            updateLatestKnownPosition(position);

            Optional<List<PeripheralSensor>> peripheralSensorOnDevice =
                    Context.getPeripheralSensorManager().getSensorByDeviceId(deviceId);

            if (!peripheralSensorOnDevice.isPresent() || peripheralSensorOnDevice.get().isEmpty()) {
                logDebugIfNotLoading(String.format("No sensors found on deviceId: %d. Refreshing sensors map.", deviceId), deviceId);
                return position;
            }

            List<PeripheralSensor> sensorsOnDeviceList = peripheralSensorOnDevice.get();
            List<PeripheralSensor> sensorsListForProcessing = new ArrayList<>();
            sensorsListForProcessing.addAll(sensorsOnDeviceList);

            ProcessingInfo processingInfo = Context.getDeviceManager().getDeviceProcessingInfo(deviceId);
            String fuelProcessingType = processingInfo.getProcessingType();

            switch (fuelProcessingType) {
                case ProcessingInfo.AVG_FUEL_PROCESS_TYPE:
                case ProcessingInfo.SUM_FUEL_PROCESS_TYPE:
                    PeripheralSensor dummySensor = sensorsOnDeviceList.get(0).cloneMe(processingInfo.getFinalCalibFieldName());
                    sensorsListForProcessing = Lists.newArrayList();
                    sensorsListForProcessing.add(dummySensor);
                    break;
                default:
                    break;
            }

            if (sensorsListForProcessing.isEmpty()) {
                logDebugIfNotLoading(String.format("Sensors list empty for deviceId: %d. Refreshing sensors map.", deviceId), deviceId);
                return position;
            }

            for (PeripheralSensor sensorOnDevice : sensorsListForProcessing) {
                long sensorID = sensorOnDevice.getPeripheralSensorId();
                logDebugIfNotLoading(String.format("[FuelSensorDataHandler] Running sensor loop on %d %d", position.getDeviceId(), sensorID), deviceId);
                String lookUpKey = getLookupKey(deviceId, sensorID);
                if (!previousPositions.containsKey(lookUpKey)) {
                    TreeMultiset<Position> positions =
                            TreeMultiset.create(Comparator.comparing(p -> p.getDeviceTime().getTime()));
                    previousPositions.put(lookUpKey, positions);
                }

                TreeMultiset<Position> positionsForDeviceSensor = previousPositions.get(lookUpKey);

                //If this is a back dated packet, do nothing
                Optional<Position> lastPacketProcessed = getLastKnownPositionForSensor(lookUpKey, deviceId);

                if (lastPacketProcessed.isPresent()) {
                    if (position.getDeviceTime().compareTo((lastPacketProcessed.get().getDeviceTime())) <= 0) {
                        logDebugIfNotLoading(String.format("Backdated packets detected for device: %d. Skipping fuel processing for them",
                                                           deviceId), deviceId);
                        continue;
                    }

                    long lastTime = lastPacketProcessed.get().getDeviceTime().getTime();
                    long currentTime = position.getDeviceTime().getTime();
                    long diff = (currentTime - lastTime);
                    boolean greater = diff > dataLossThresholdSeconds;
                    String message = String.format("Data loss check for %s. last timestamp: %d, current timestamp: %d, diff: %d, >datalossseconds %b",
                                                   lookUpKey, lastTime, currentTime, diff, greater);
                    logDebugIfNotLoading(message, deviceId);
                    if (greater) {
                        possibleDataLossByDevice.put(deviceId, true);
                    }
                }

                String fuelDataField = sensorOnDevice.getFuelDataFieldName();
                if (loadingOldDataFromDB || position.getAttributes().containsKey(fuelDataField)) {
                    // This is a position from the DB, add to the list and move on.
                    // If we don't skip further processing, it might trigger FCM notification unnecessarily.
                    if (!positionsForDeviceSensor.contains(position)) {
                        positionsForDeviceSensor.add(position);
                    }
                    removeFirstPositionIfNecessary(positionsForDeviceSensor, lookUpKey);
                    continue;
                }

                processSensorData(position, sensorOnDevice);
            }

        } catch (Exception e) {
            Log.info(String.format("Exception in processing fuel info: %s", e.getMessage(), deviceId));
            e.printStackTrace();
        } finally {
            long endProcessing = new Date().getTime();
            long processingTime = endProcessing - startProcessing;
            logInfoIfNotLoading(
                    String.format("[FuelSensorDataHandler] Total processing time (ms) for deviceId: %d = %d %s",
                    position.getDeviceId(), processingTime, System.lineSeparator()));
        }
        return position;
    }

    private void logDebugIfNotLoading(String logMessage) {
        if (!loadingOldDataFromDB) {
            Log.debug(logMessage);
        }
    }

    private void logDebugIfNotLoading(String logMessage, long deviceId) {
        if (!loadingOldDataFromDB) {
            FuelSensorDataHandlerHelper.logDebugIfDeviceId(logMessage, deviceId);
        }
    }

    private void logInfoIfNotLoading(String logMessage) {
        if (!loadingOldDataFromDB) {
            Log.info(logMessage);
        }
    }

    private void processSensorData(Position position, PeripheralSensor fuelSensor) {
        long deviceId = position.getDeviceId();
        logDebugIfNotLoading(String.format("[FuelSensorDataHandler] processing sensor data %d %d", position.getDeviceId(), fuelSensor.getPeripheralSensorId()), deviceId);
        String calibFuelDataField = fuelSensor.getCalibFuelFieldName();

        if (!position.getAttributes().containsKey(calibFuelDataField)) {
            updateWithLastAvailable(position, ALL_FUEL_FIELDS, fuelSensor);
            return;
        }

        if (!position.getAttributes().containsKey(Position.KEY_ODOMETER)
                && deviceLastKnownOdometerPositionLookup.containsKey(deviceId)) {

            Position lastPosition = deviceLastKnownOdometerPositionLookup.get(deviceId);
            position.set(Position.KEY_ODOMETER, ((Number) lastPosition.getAttributes().get(Position.KEY_ODOMETER)).longValue());
        }

        if ((position.getAttributes().containsKey(calibFuelDataField) &&
                ((Number) position.getAttributes().get(calibFuelDataField)).longValue()  <= 0L) ||
                (position.getAttributes().containsKey(Position.KEY_POWER) &&
                        ((Number) position.getAttributes().get(Position.KEY_POWER)).doubleValue()  <= 0.0)) {
            logDebugIfNotLoading(String.format("Device power too low, updating with last known fuel level for deviceId %s", deviceId), deviceId);
            updateWithLastAvailable(position, ALL_FUEL_FIELDS, fuelSensor);
        }

        if (position.getBoolean(Position.KEY_CHARGE)) {
            logDebugIfNotLoading(String.format("Device on internal charge. Ignoring reported fuel value and updating with last known fuel level for deviceId %d", deviceId), deviceId);
            updateWithLastAvailable(position, ALL_FUEL_FIELDS, fuelSensor);
            return;
        }

        handleSensorData(position, fuelSensor);
    }

    private void updateLatestKnownPosition(Position position) {

        if (!position.getAttributes().containsKey(Position.KEY_ODOMETER)
                || !position.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)) {

            return;
        }

        Optional<Position> lastKnown = deviceLastKnownOdometerPositionLookup.containsKey(position.getDeviceId())?
                Optional.of(deviceLastKnownOdometerPositionLookup.get(position.getDeviceId())) : Optional.empty();

        if (!lastKnown.isPresent()) {
            deviceLastKnownOdometerPositionLookup.put(position.getDeviceId(), position);
            return;
        }

        Position lastKnownPosition = lastKnown.get();
        Map<String, Object> lastKnownAttributes = lastKnownPosition.getAttributes();
        Map<String, Object> currentAttributes = position.getAttributes();

        if (lastKnownPosition.getDeviceTime().getTime() > position.getDeviceTime().getTime()
            || (((Number) lastKnownAttributes.get(Position.KEY_ODOMETER)).longValue() >
                ((Number) currentAttributes.get(Position.KEY_ODOMETER)).longValue())
            || (((Number) lastKnownAttributes.get(Position.KEY_TOTAL_DISTANCE)).longValue() >
                ((Number) currentAttributes.get(Position.KEY_TOTAL_DISTANCE)).longValue())) {

            return;
        }

        deviceLastKnownOdometerPositionLookup.put(position.getDeviceId(), position);
    }

    private void updateWithLastAvailable(final Position position,
                                         final String attributeToUpdate,
                                         PeripheralSensor fuelSensor) {

        long deviceId = position.getDeviceId();
        String lookupKey = getLookupKey(deviceId, fuelSensor.getPeripheralSensorId());

        if (!previousPositions.containsKey(lookupKey)) {
            logDebugIfNotLoading(String.format("lookupKey not found in previousPositions: %s", lookupKey), deviceId);
            return;
        }

        TreeMultiset<Position> sensorReadingsFromDevice = previousPositions.get(lookupKey);

        if (sensorReadingsFromDevice.size() < 1) {
            logDebugIfNotLoading(String.format("No previous readings found for lookupKey: %s", lookupKey), deviceId);
            return;
        }

        // This should ideally average the readings from all sensors, but for now we'll just pick the first sensor and
        // use the last available level.

        Optional<Position> possibleLastKnownPosition = getLastKnownPositionForSensor(lookupKey, deviceId);
        if (!possibleLastKnownPosition.isPresent()) {
            logDebugIfNotLoading(String.format("Last known position not found for lookupKey: %s", lookupKey), deviceId);
            return;
        }

        Position lastKnownPosition = possibleLastKnownPosition.get();
        if (!attributeToUpdate.equals(ALL_FUEL_FIELDS)
                && !lastKnownPosition.getAttributes().containsKey(attributeToUpdate)) {
            logDebugIfNotLoading(String.format("Last known position for deviceId %d doesn't have property %s set.",
                                               position.getDeviceId(), attributeToUpdate), deviceId);
            return;
        }

        ProcessingInfo processingInfo = Context.getDeviceManager().getDeviceProcessingInfo(deviceId);
        String finalCalibField = processingInfo.getFinalCalibFieldName();

        switch(attributeToUpdate) {
            case ALL_FUEL_FIELDS:
                String calibFuelDataField = fuelSensor.getCalibFuelFieldName();
                String fuelDataField = fuelSensor.getFuelDataFieldName();
                if (lastKnownPosition.getAttributes().containsKey(calibFuelDataField)) {
                    position.set(calibFuelDataField, (double) lastKnownPosition.getAttributes().get(calibFuelDataField));
                }

                if (lastKnownPosition.getAttributes().containsKey(fuelDataField)) {
                    position.set(fuelDataField, (double) lastKnownPosition.getAttributes().get(fuelDataField));
                }

                if (lastKnownPosition.getAttributes().containsKey(finalCalibField)) {
                    position.set(finalCalibField, (double) lastKnownPosition.getAttributes().get(finalCalibField));
                }

                break;
            default:
                break;
        }
    }

    private Optional<Position> getLastKnownPositionForSensor(String sensorPositionsLookupKey, long deviceId) {

        if (!previousPositions.containsKey(sensorPositionsLookupKey)) {
            logDebugIfNotLoading(String.format("sensorPositionsLookupKey not found in previousPositions %s", sensorPositionsLookupKey), deviceId);
            return Optional.empty();
        }

        TreeMultiset<Position> readingsFromDevice = previousPositions.get(sensorPositionsLookupKey);

        if (readingsFromDevice.isEmpty()) {
            logDebugIfNotLoading(String.format("No previous readings found for sensorPositionsLookupKey: %s", sensorPositionsLookupKey), deviceId);
            return Optional.empty();
        }

        return Optional.of(readingsFromDevice.lastEntry().getElement());
    }

    private Optional<Position> findFirstNonOutlierInLastWindow(long deviceId,
                                                               PeripheralSensor fuelSensor) {

        String lookupKey = getLookupKey(deviceId, fuelSensor.getPeripheralSensorId());
        if (!previousPositions.containsKey(lookupKey)) {
            logDebugIfNotLoading(String.format("deviceId not found in lookupKey %s", lookupKey), deviceId);
            return Optional.empty();
        }

        TreeMultiset<Position> sensorReadingsFromDevice = previousPositions.get(lookupKey);

        if (sensorReadingsFromDevice.isEmpty()) {
            logDebugIfNotLoading(String.format("No readings for sensors found for lookupKey: %s", lookupKey), deviceId);
            return Optional.empty();
        }

        String outlierField = fuelSensor.getFuelOutlierFieldName();

        // Will return Optional.empty if there aren't enough elements in the list.
        return sensorReadingsFromDevice
                .descendingMultiset()
                .stream()
                .skip(1) // Skip the position that's the new one after data loss gap
                .filter(p -> p.getAttributes().containsKey(outlierField) &&
                             !(boolean) p.getAttributes().get(outlierField))
                .findFirst();
    }

    private String getLookupKey(long deviceId, long sensorId) {
        return String.format("%d_%d", deviceId, sensorId);
    }

    private void loadOldPositions(String protocol) {
        this.loadingOldDataFromDB = true;

        if (this.hoursOfDataToLoad == 0) {
            loadingOldDataFromDB = false;
            return;
        }

        // Load latest 24 hour of data for device
        try {
            Log.info(String.format("Loading data for %s protocol", protocol));

            Collection<Position> latestPositionsOfDevicesByProtocol =
                    Context.getDataManager().getLatestPositionsForProtocol(protocol);

            if (latestPositionsOfDevicesByProtocol.isEmpty()) {
                this.loadingOldDataFromDB = false;
                Log.info(String.format("Found 0 devices for %s protocol", protocol));
                return;
            }

            Map<Long, Date> deviceIdToLatestDateMap = new ConcurrentHashMap<>();

            latestPositionsOfDevicesByProtocol.stream().forEach(p -> {
                deviceIdToLatestDateMap.put(p.getDeviceId(), p.getDeviceTime());
            });

            Log.info(String.format("Number of active devices on %s protocol: %d", protocol, deviceIdToLatestDateMap.size()));

            for (Long deviceId : deviceIdToLatestDateMap.keySet()) {

                Optional<List<PeripheralSensor>> linkedDevices = Context.getPeripheralSensorManager()
                                                                        .getLinkedPeripheralSensors(deviceId);

                if (!linkedDevices.isPresent() || !deviceIdToLatestDateMap.containsKey(deviceId)) {
                    continue;
                }

                Date deviceLastPositionDate = deviceIdToLatestDateMap.get(deviceId);
                Date hoursAgo = FuelSensorDataHandlerHelper.getAdjustedDate(
                        deviceLastPositionDate, Calendar.HOUR_OF_DAY, -this.hoursOfDataToLoad);

                Log.info(String.format("Loading data from %s to %s for deviceId %d",
                                       hoursAgo, deviceLastPositionDate, deviceId));

                Collection<Position> devicePositionsInLastDay =
                        getDataManager().getPositions(deviceId, hoursAgo, new Date());

                for (Position position : devicePositionsInLastDay) {
                    handlePosition(position);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.loadingOldDataFromDB = false;
        previousPositions.forEach((s, t) -> Log.info(String.format("Loaded %d positions for lookupKey %s", t.size(), s)));
    }

    private void handleSensorData(Position position,
                                  PeripheralSensor fuelSensor) {
        long deviceId = position.getDeviceId();
        logDebugIfNotLoading(String.format("[FuelSensorDataHandler] handleSensorData sensor data %d %d", deviceId, fuelSensor.getPeripheralSensorId()), deviceId);

        long sensorId = fuelSensor.getPeripheralSensorId();
        String lookupKey = getLookupKey(deviceId, sensorId);

        logDebugIfNotLoading(String.format("Processing data for lookup key: %s", lookupKey), deviceId);

        TreeMultiset<Position> positionsForDeviceSensor = previousPositions.get(lookupKey);
        String fuelDataField = fuelSensor.getFuelDataFieldName();

        DeviceConsumptionInfo consumptionInfo = Context.getDeviceManager().getDeviceConsumptionInfo(deviceId);
        int averagesLookbackSeconds = (fuelSensor.getMovingAvgWindowSize() + 10) * consumptionInfo.getTransmissionFrequency();

        List<Position> relevantPositionsListForAverages =
                FuelSensorDataHandlerHelper.getRelevantPositionsSubList(
                        positionsForDeviceSensor,
                        position,
                        fuelSensor.getMovingAvgWindowSize() - 1,
                        fuelSensor.getFuelOutlierFieldName(),
                        averagesLookbackSeconds);

        logDebugIfNotLoading(String.format("[Averages] Size of list before getting averages: %d, deviceId: %d, sensorId: %d",
                                           relevantPositionsListForAverages.size(), deviceId, fuelSensor.getPeripheralSensorId()), deviceId);

        relevantPositionsListForAverages.add(position);
        double currentFuelLevelAverage =
                FuelSensorDataHandlerHelper.getAverageFuelValue(relevantPositionsListForAverages, fuelSensor);

        // KEY_FUEL_LEVEL will hold the smoothed data, which is average of raw values in the relevant list.
        // Until the number of positions in the list comes up to the expected number of positions for calculating
        // averages and / or outliers, this will calculate the average of the existing list and set that on the
        // current position, so it's available for later calculations. This will also make sure that we are able to send
        // this info to any client that's listening for these updates.

        position.set(fuelDataField, currentFuelLevelAverage);
        if (!positionsForDeviceSensor.contains(position)) {
            positionsForDeviceSensor.add(position);
        }

        // Detect and remove outliers
        int outlierLookbackSeconds = (fuelSensor.getOutlierWindowSize() + 10) * consumptionInfo.getTransmissionFrequency();
        List<Position> relevantPositionsListForOutliers =
                FuelSensorDataHandlerHelper.getRelevantPositionsSubList(positionsForDeviceSensor,
                                                                        position,
                                                                        fuelSensor.getOutlierWindowSize(),
                                                                        fuelSensor.getFuelOutlierFieldName(),
                                                                        outlierLookbackSeconds);



        // If we've detected data loss, find the first non outlier in the window before the loss gap, to use in further
        // in calculating if there was any fuel activity
        // TODO: REMOVE TEMP SKIPPING FOR DEVICE 6
        boolean possibleDataLoss = deviceId == 6? false : possibleDataLossByDevice.getOrDefault(deviceId, false);
        if (possibleDataLoss && !nonOutlierInLastWindowByDevice.containsKey(deviceId)) {
            Optional<Position> nonOutlierInLastWindow =
                    findFirstNonOutlierInLastWindow(position.getDeviceId(), fuelSensor);

            if (nonOutlierInLastWindow.isPresent()) {
                nonOutlierInLastWindowByDevice.put(deviceId, nonOutlierInLastWindow.get());
            }
        }

        if (relevantPositionsListForOutliers.size() < fuelSensor.getOutlierWindowSize()) {
            // positions in this case will have isFuelOutlier left blank (neither true nor false) i.e.
            // not evaluated.
            logDebugIfNotLoading(String.format("[Outliers] List too small for outlier detection. Outlier window size: %d", fuelSensor.getOutlierWindowSize()), deviceId);
            return;
        }

        Optional<Long> fuelTankMaxVolume =
                Context.getPeripheralSensorManager().getFuelTankMaxCapacity(deviceId, sensorId);

        int indexOfPositionEvaluation = (fuelSensor.getOutlierWindowSize() - 1) / 2;

        boolean outlierPresent = FuelSensorDataHandlerHelper.isOutlierPresentInSublist(
                relevantPositionsListForOutliers,
                indexOfPositionEvaluation,
                fuelTankMaxVolume, fuelSensor);

        Position outlierCheckPosition = relevantPositionsListForOutliers.get(indexOfPositionEvaluation);
        outlierCheckPosition.set(fuelSensor.getFuelOutlierFieldName(), outlierPresent);

        // Note: Need to do this in a better way since this is a direct write to the db and can slow things down.
        // We could use an external queue and update these positions from there, without affecting processing here.
        // Also, we do not want to lose any data coming in, so we'll only mark the position as an outlier rather
        // than deleting it.
        FuelSensorDataHandlerHelper.updatePosition(outlierCheckPosition);

        if (outlierPresent) {
            // Remove the outlier from our in memory list and move on.
            positionsForDeviceSensor.remove(outlierCheckPosition);
            return;
        }

        Position positionUnderEvaluation = relevantPositionsListForOutliers.get(indexOfPositionEvaluation);

        // Re-calculate and reset averages if there were no outliers.
        relevantPositionsListForAverages =
                FuelSensorDataHandlerHelper.getRelevantPositionsSubList(positionsForDeviceSensor,
                                                                        positionUnderEvaluation,
                                                                        fuelSensor.getMovingAvgWindowSize(),
                                                                        fuelSensor.getFuelOutlierFieldName(),
                                                                        true,
                                                                        averagesLookbackSeconds);

        currentFuelLevelAverage = FuelSensorDataHandlerHelper.getAverageFuelValue(relevantPositionsListForAverages, fuelSensor);
        positionUnderEvaluation.set(fuelDataField, currentFuelLevelAverage);

        // Update the position in the db so the recalculated average is reflected there.
        FuelSensorDataHandlerHelper.updatePosition(positionUnderEvaluation);

        //-- End Outliers

        if (possibleDataLoss && nonOutlierInLastWindowByDevice.containsKey(deviceId)) {
            Optional<FuelActivity> fuelActivity =
                    FuelDataActivityChecker.checkForActivityIfDataLoss(outlierCheckPosition,
                                                                       nonOutlierInLastWindowByDevice.get(deviceId),
                                                                       fuelTankMaxVolume, fuelSensor);

            fuelActivity.ifPresent(activity -> sendNotificationIfNecessary(deviceId, activity, fuelSensor, fuelTankMaxVolume));

            possibleDataLossByDevice.remove(deviceId);
            nonOutlierInLastWindowByDevice.remove(deviceId);
        } else {
            // There was no data loss, so check for regular events.
            possibleDataLossByDevice.remove(deviceId);
            nonOutlierInLastWindowByDevice.remove(deviceId);

            int alertsLookback = (fuelSensor.getEventsWindowSize() + 10) * consumptionInfo.getTransmissionFrequency();

            long previousAlertWindowStartTime = previousAlertWindowStart.containsKey(lookupKey)?
                    previousAlertWindowStart.get(lookupKey).getDeviceTime().getTime() : 0L;

            List<Position> relevantPositionsListForAlerts =
                    FuelSensorDataHandlerHelper.getRelevantPositionsSubList(
                            positionsForDeviceSensor,
                            positionUnderEvaluation,
                            fuelSensor.getEventsWindowSize(),
                            fuelSensor.getFuelOutlierFieldName(),
                            true,
                            alertsLookback,
                            previousAlertWindowStartTime);

            if (!this.loadingOldDataFromDB && relevantPositionsListForAlerts.size() >= fuelSensor.getEventsWindowSize()) {
                previousAlertWindowStart.put(lookupKey, relevantPositionsListForAlerts.get(0));
                Optional<FuelActivity> fuelActivity =
                        FuelDataActivityChecker.checkForActivity(relevantPositionsListForAlerts,
                                                                 deviceFuelEventMetadata,
                                                                 fuelSensor);

                if (fuelActivity.isPresent()) {
                    sendNotificationIfNecessary(deviceId, fuelActivity.get(), fuelSensor, fuelTankMaxVolume);
                }
            }
        }

        removeFirstPositionIfNecessary(positionsForDeviceSensor, lookupKey);
    }

    private void sendNotificationIfNecessary(final long deviceId,
                                             final FuelActivity fuelActivity,
                                             PeripheralSensor fuelSensor,
                                             Optional<Long> fuelTankMaxVolume) {

        boolean isDrain = fuelActivity.getActivityType() == FuelActivityType.FUEL_DRAIN
                || fuelActivity.getActivityType() == FuelActivityType.PROBABLE_FUEL_DRAIN;

        if (isDrain) {
            boolean isConsumptionExpected =
                    FuelConsumptionChecker.isFuelConsumptionAsExpected(fuelActivity.getActivityStartPosition(),
                                                                       fuelActivity.getActivityEndPosition(),
                                                                       fuelActivity.getChangeVolume(),
                                                                       fuelTankMaxVolume,
                                                                       fuelSensor);
            if (!isConsumptionExpected) {
                logDebugIfNotLoading(String.format("[Events] Detected drain not within expected consumption %d", deviceId),deviceId);
                sendNotificationIfNecessary(deviceId, fuelActivity, fuelSensor.getPeripheralSensorId());
            } else {
                logDebugIfNotLoading(String.format("[Events] Detected drain that was within expected consumption, not sending notification %d", deviceId), deviceId);
                fuelActivity.setActivityType(FuelActivityType.DRAIN_WITHIN_CONSUMPTION);
                saveEventToDB(deviceId, fuelActivity, fuelSensor.getPeripheralSensorId());
            }

        } else {
            sendNotificationIfNecessary(deviceId, fuelActivity, fuelSensor.getPeripheralSensorId());
        }
    }

    private void sendNotificationIfNecessary(final long deviceId, final FuelActivity fuelActivity, long peripheralSensorId) {
        if (fuelActivity.getActivityType() != FuelActivityType.NONE) {
            logDebugIfNotLoading("[FUEL_ACTIVITY]  DETECTED: " + fuelActivity.getActivityType()
                      + " starting at: " + fuelActivity.getActivityStartTime()
                      + " ending at: " + fuelActivity.getActivityEndTime()
                      + " volume: " + fuelActivity.getChangeVolume()
                      + " start lat, long " + fuelActivity.getActivityStartPosition().getLatitude()
                      + ", " + fuelActivity.getActivityStartPosition().getLongitude()
                      + " end lat, long " + fuelActivity.getActivityEndPosition().getLatitude()
                      + ", " + fuelActivity.getActivityEndPosition().getLongitude(), deviceId);

            // Add event to events table

            saveEventToDB(deviceId, fuelActivity, peripheralSensorId);

            // Adding the sensor ID to the FCM notification does not make sense, since the end user does not care
            // about these IDs. In the future, if we think it is necessary, we'll add names to sensors so it is
            // clear which "tank" this notification came from.
            Context.getFcmPushNotificationManager().updateFuelActivity(fuelActivity, peripheralSensorId);
        }
    }

    private void saveEventToDB(long deviceId, FuelActivity fuelActivity, long peripheralSensorId) {
        String eventType = fuelActivity.getActivityType().toString();
        Event event = new Event(eventType, deviceId,
                                fuelActivity.getActivityStartPosition().getId(), fuelActivity.getActivityStartTime());
        event.set("sensorId", peripheralSensorId);
        event.set("startTime", fuelActivity.getActivityStartTime().getTime());
        event.set("endTime", fuelActivity.getActivityEndTime().getTime());
        event.set("volume", fuelActivity.getChangeVolume());
        event.set("endPositionId", fuelActivity.getActivityEndPosition().getId());
        event.set("startLat", fuelActivity.getActivityStartPosition().getLatitude());
        event.set("startLong", fuelActivity.getActivityStartPosition().getLongitude());
        event.set("endLat", fuelActivity.getActivityEndPosition().getLatitude());
        event.set("endLong", fuelActivity.getActivityEndPosition().getLongitude());

        try {
            getDataManager().addObject(event);
        } catch (SQLException error) {
            Log.warning("[Events] Error while saving fuel event to DB", error);
        }
    }

    private void removeFirstPositionIfNecessary(TreeMultiset<Position> positionsForDeviceSensor, String lookUpKey) {
        if (positionsForDeviceSensor.size() > maxInMemoryPreviousPositionsListSize) {
            Position toRemove = positionsForDeviceSensor.firstEntry().getElement();
            positionsForDeviceSensor.remove(toRemove);
            logDebugIfNotLoading("Size of positionsForDeviceSensor with lookUpKey = " + lookUpKey
                      + " after removing position: " + positionsForDeviceSensor.size());
        }
    }
}
