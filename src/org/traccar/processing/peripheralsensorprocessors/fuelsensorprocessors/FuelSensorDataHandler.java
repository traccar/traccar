package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import com.google.common.collect.TreeMultiset;
import org.traccar.BaseDataHandler;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
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

    private static int minValuesForMovingAvg;
    private static int minValuesForOutlierDetection;
    private static int maxInMemoryPreviousPositionsListSize;
    private static int hoursOfDataToLoad;
    private static int maxValuesForAlerts;
    private static int currentEventLookBackSeconds;
    private static int dataLossThresholdSeconds;

    private final Map<Long, Boolean> possibleDataLossByDevice = new ConcurrentHashMap<>();
    private final Map<Long, Position> nonOutlierInLastWindowByDevice = new ConcurrentHashMap<>();
    private final Map<Long, TreeMultiset<Position>> previousPositions = new ConcurrentHashMap<>();
    private final Map<String, FuelEventMetadata> deviceFuelEventMetadata = new ConcurrentHashMap<>();
    private final Map<Long, Position> deviceLastKnownOdometerPositionLookup = new ConcurrentHashMap<>();
    private boolean loadingOldDataFromDB = false;

    static {
        int messageFrequencyInSeconds = Context.getConfig()
                                               .getInteger("processing.peripheralSensorData.messageFrequency");

        hoursOfDataToLoad = Context.getConfig().getInteger("processing.peripheralSensorData.hoursOfDataToLoad");

        minValuesForMovingAvg = Context.getConfig()
                                       .getInteger("processing.peripheralSensorData.minValuesForMovingAverage");

        minValuesForOutlierDetection =
                Context.getConfig().getInteger("processing.peripheralSensorData.minValuesForOutlierDetection");

        int minHoursOfDataInMemory = Context.getConfig()
                                            .getInteger("processing.peripheralSensorData.minHoursOfDataInMemory");

        // If hoursOfDataToLoad = 0, then keep at least minHoursOfDataInMemory hours of data in memory
        maxInMemoryPreviousPositionsListSize =
                (SECONDS_IN_ONE_HOUR * (hoursOfDataToLoad > 0
                        ? hoursOfDataToLoad : minHoursOfDataInMemory)) / messageFrequencyInSeconds;

        maxValuesForAlerts = Context.getConfig()
                                    .getInteger("processing.peripheralSensorData.maxValuesForAlerts");

        currentEventLookBackSeconds =
                Context.getConfig()
                       .getInteger("processing.peripheralSensorData.currentEventLookBackSeconds");

        dataLossThresholdSeconds =
                Context.getConfig()
                       .getInteger("processing.peripheralSensorData.dataLossThresholdSeconds") * 1000;


    }

    public FuelSensorDataHandler() {
        loadOldPositions();
    }

    @Override
    protected Position handlePosition(Position position) {
        try {

            updateLatestKnownPosition(position);

            long deviceId = position.getDeviceId();

            Optional<List<PeripheralSensor>> peripheralSensorOnDevice =
                    Context.getPeripheralSensorManager().getSensorByDeviceId(deviceId);

            if (!peripheralSensorOnDevice.isPresent()) {
                Log.debug(String.format("No sensors found on deviceId: %d. Refreshing sensors map.", deviceId));
                Context.getPeripheralSensorManager().refreshPeripheralSensorsMap();
                return position;
            }

            List<PeripheralSensor> sensorsOnDeviceList = peripheralSensorOnDevice.get();
            if (sensorsOnDeviceList.isEmpty()) {
                Log.debug(String.format("Sensors list empty for deviceId: %d. Refreshing sensors map.", deviceId));
                Context.getPeripheralSensorManager().refreshPeripheralSensorsMap();
                return position;
            }

            if (!previousPositions.containsKey(deviceId)) {
                TreeMultiset<Position> positions =
                        TreeMultiset.create(Comparator.comparing(p -> p.getDeviceTime().getTime()));
                previousPositions.put(deviceId, positions);
            }

            TreeMultiset<Position> positionsForDeviceSensor = previousPositions.get(deviceId);

            //If this is a back dated packet, do nothing
            Optional<Position> lastPacketProcessed = getLastKnownPositionForDevice(position);

            if (lastPacketProcessed.isPresent()) {

                if (position.getDeviceTime().compareTo((lastPacketProcessed.get().getDeviceTime())) <= 0) {
                    Log.debug(String.format("Backdated packets detected for device: %d. Skipping fuel processing for them",
                                            deviceId));
                    return position;
                }

                if (position.getDeviceTime().getTime() - lastPacketProcessed.get().getDeviceTime().getTime() > dataLossThresholdSeconds) {
                    possibleDataLossByDevice.put(deviceId, true);
                }
            }

            for (PeripheralSensor fuelSensor : sensorsOnDeviceList) {
                String fuelDataField = fuelSensor.getFuelDataFieldName();
                if (loadingOldDataFromDB || position.getAttributes().containsKey(fuelDataField)) {
                    // This is a position from the DB, add to the list and move on.
                    // If we don't skip further processing, it might trigger FCM notification unnecessarily.
                    positionsForDeviceSensor.add(position);
                    removeFirstPositionIfNecessary(positionsForDeviceSensor, deviceId);
                    return position;
                }

                processSensorData(position, fuelSensor);
            }

        } catch (Exception e) {
            Log.debug(String.format("Exception in processing fuel info: %s", e.getMessage()));
            e.printStackTrace();
        } finally {
            return position;
        }
    }

    private void processSensorData(Position position, PeripheralSensor fuelSensor) {
        long deviceId = position.getDeviceId();
        String calibFuelDataField = fuelSensor.getCalibFuelFieldName();

        if (!position.getAttributes().containsKey(calibFuelDataField)) {
            updateWithLastAvailable(position, ALL_FUEL_FIELDS, fuelSensor);
            return;
        }

        if (!position.getAttributes().containsKey(Position.KEY_ODOMETER)
                && deviceLastKnownOdometerPositionLookup.containsKey(deviceId)) {

            Position lastPosition = deviceLastKnownOdometerPositionLookup.get(deviceId);
            position.set(Position.KEY_ODOMETER, (int) lastPosition.getAttributes().get(Position.KEY_ODOMETER));
        }

        if ((position.getAttributes().containsKey(calibFuelDataField) &&
                ((Number) position.getAttributes().get(calibFuelDataField)).longValue()  <= 0L) ||
                (position.getAttributes().containsKey(Position.KEY_POWER) &&
                        ((Number) position.getAttributes().get(Position.KEY_POWER)).doubleValue()  <= 0.0)) {
            Log.debug("Device power too low, updating with last known fuel level for deviceId"
                              + position.getDeviceId());
            updateWithLastAvailable(position, ALL_FUEL_FIELDS, fuelSensor);
        }

        if (position.getBoolean(Position.KEY_CHARGE)) {
            Log.debug("Device on internal charge. Ignoring reported fuel value and updating with last known fuel level for deviceId"
                              + position.getDeviceId());
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
            || ((int) lastKnownAttributes.get(Position.KEY_ODOMETER) >
                    (int) currentAttributes.get(Position.KEY_ODOMETER))
            || ((double) lastKnownAttributes.get(Position.KEY_TOTAL_DISTANCE) >
                    (double) currentAttributes.get(Position.KEY_TOTAL_DISTANCE))) {

            return;
        }

        deviceLastKnownOdometerPositionLookup.put(position.getDeviceId(), position);
    }

    private void updateWithLastAvailable(final Position position,
                                         final String attributeToUpdate,
                                         PeripheralSensor fuelSensor) {

        long deviceId = position.getDeviceId();
        if (!previousPositions.containsKey(deviceId)) {
            Log.debug("deviceId not found in previousPositions" + deviceId);
            return;
        }

        TreeMultiset<Position> sensorReadingsFromDevice = previousPositions.get(deviceId);

        if (sensorReadingsFromDevice.size() < 1) {
            Log.debug("No previous readings found for deviceId: " + deviceId);
            return;
        }

        // This should ideally average the readings from all sensors, but for now we'll just pick the first sensor and
        // use the last available level.

        Optional<Position> possibleLastKnownPosition = getLastKnownPositionForDevice(position);
        if (!possibleLastKnownPosition.isPresent()) {
            Log.debug("Last known position not found for deviceId: " + position.getDeviceId());
            return;
        }

        Position lastKnownPosition = possibleLastKnownPosition.get();
        if (!attributeToUpdate.equals(ALL_FUEL_FIELDS)
                && !lastKnownPosition.getAttributes().containsKey(attributeToUpdate)) {
            Log.debug(String.format("Last known position for deviceId %d doesn't have property %s set.",
                                    position.getDeviceId(), attributeToUpdate));
            return;
        }

        switch(attributeToUpdate) {
            case ALL_FUEL_FIELDS:
                String calibFuelDataField = fuelSensor.getCalibFuelFieldName();
                String fuelDataField = fuelSensor.getFuelDataFieldName();
                position.set(calibFuelDataField, (double) lastKnownPosition.getAttributes().get(calibFuelDataField));
                position.set(fuelDataField, (double) lastKnownPosition.getAttributes().get(fuelDataField));
                break;
        }
    }

    private Optional<Position> getLastKnownPositionForDevice(Position position) {
        long deviceId = position.getDeviceId();
        if (!previousPositions.containsKey(deviceId)) {
            Log.debug("deviceId not found in previousPositions" + deviceId);
            return Optional.empty();
        }

        TreeMultiset<Position> readingsFromDevice = previousPositions.get(deviceId);

        if (readingsFromDevice.isEmpty()) {
            Log.debug("No previous readings found for deviceId: " + deviceId);
            return Optional.empty();
        }


        return Optional.of(readingsFromDevice.lastEntry().getElement());
    }

    private Optional<Position> findFirstNonOutlierInLastWindow(long deviceId,
                                                               PeripheralSensor fuelSensor) {

        if (!previousPositions.containsKey(deviceId)) {
            Log.debug("deviceId not found in previousPositions" + deviceId);
            return Optional.empty();
        }

        TreeMultiset<Position> sensorReadingsFromDevice = previousPositions.get(deviceId);

        if (sensorReadingsFromDevice.isEmpty()) {
            Log.debug("No readings for sensors found on deviceId: " + deviceId);
            return Optional.empty();
        }

        long sensorId = fuelSensor.getPeripheralSensorId();
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

    private void loadOldPositions() {
        this.loadingOldDataFromDB = true;
        Collection<Device> devices = Context.getDeviceManager().getAllDevices();

        if (this.hoursOfDataToLoad == 0) {
            loadingOldDataFromDB = false;
            return;
        }

        // Load latest 24 hour of data for device
        try {

            Collection<Position> latestPositionsOfDevices = Context.getDataManager().getLatestPositions();
            if (latestPositionsOfDevices.isEmpty()) {
                this.loadingOldDataFromDB = false;
                return;
            }

            Map<Long, Date> deviceIdToLatestDateMap = new ConcurrentHashMap<>();
            latestPositionsOfDevices.stream().forEach(p -> {
                deviceIdToLatestDateMap.put(p.getDeviceId(), p.getDeviceTime());
            });

            for (Device device : devices) {
                Optional<List<PeripheralSensor>> linkedDevices = Context.getPeripheralSensorManager()
                                                                        .getLinkedPeripheralSensors(device.getId());

                long deviceId = device.getId();

                if (!linkedDevices.isPresent() || !deviceIdToLatestDateMap.containsKey(deviceId)) {
                    continue;
                }

                Date deviceLastPositionDate = deviceIdToLatestDateMap.get(device.getId());
                Date hoursAgo = FuelSensorDataHandlerHelper.getAdjustedDate(
                        deviceLastPositionDate, Calendar.HOUR_OF_DAY, -this.hoursOfDataToLoad);

                Log.info(String.format("Loading data from %s to %s for deviceId %d",
                                       hoursAgo, deviceLastPositionDate, device.getId()));

                Collection<Position> devicePositionsInLastDay =
                        getDataManager().getPositions(device.getId(), hoursAgo, new Date());

                for (Position position : devicePositionsInLastDay) {
                    handlePosition(position);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.loadingOldDataFromDB = false;
    }

    private void handleSensorData(Position position,
                                  PeripheralSensor fuelSensor) {

        long deviceId = position.getDeviceId();
        long sensorId = fuelSensor.getPeripheralSensorId();

        TreeMultiset<Position> positionsForDeviceSensor = previousPositions.get(deviceId);
        String fuelDataField = fuelSensor.getFuelDataFieldName();

        List<Position> relevantPositionsListForAverages =
                FuelSensorDataHandlerHelper.getRelevantPositionsSubList(
                        positionsForDeviceSensor,
                        position,
                        minValuesForMovingAvg - 1,
                        currentEventLookBackSeconds);

        Log.debug(String.format("Size of list before getting averages: %d, deviceId: %d, sensorId: %d",
                                relevantPositionsListForAverages.size(), deviceId, fuelSensor.getPeripheralSensorId()));

        relevantPositionsListForAverages.add(position);
        double currentFuelLevelAverage =
                FuelSensorDataHandlerHelper.getAverageFuelValue(relevantPositionsListForAverages, fuelSensor);

        // KEY_FUEL_LEVEL will hold the smoothed data, which is average of raw values in the relevant list.
        // Until the number of positions in the list comes up to the expected number of positions for calculating
        // averages and / or outliers, this will calculate the average of the existing list and set that on the
        // current position, so it's available for later calculations. This will also make sure that we are able to send
        // this info to any client that's listening for these updates.

        position.set(fuelDataField, currentFuelLevelAverage);
        positionsForDeviceSensor.add(position);

        // Detect and remove outliers
        List<Position> relevantPositionsListForOutliers =
                FuelSensorDataHandlerHelper.getRelevantPositionsSubList(
                        positionsForDeviceSensor, position, minValuesForOutlierDetection, currentEventLookBackSeconds);



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

        if (relevantPositionsListForOutliers.size() < minValuesForOutlierDetection) {
            // positions in this case will have isFuelOutlier left blank (neither true nor false) i.e.
            // not evaluated.
            Log.debug("List too small for outlier detection");
            return;
        }

        Optional<Long> fuelTankMaxVolume =
                Context.getPeripheralSensorManager().getFuelTankMaxCapacity(deviceId, sensorId);

        int indexOfPositionEvaluation = (minValuesForOutlierDetection - 1) / 2;

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
                                                                        minValuesForMovingAvg,
                                                                        true,
                                                                        currentEventLookBackSeconds);

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

            List<Position> relevantPositionsListForAlerts =
                    FuelSensorDataHandlerHelper.getRelevantPositionsSubList(
                            positionsForDeviceSensor,
                            positionUnderEvaluation,
                            maxValuesForAlerts,
                            true,
                            currentEventLookBackSeconds);

            if (!this.loadingOldDataFromDB && relevantPositionsListForAlerts.size() >= maxValuesForAlerts) {
                // We'll use the smoothed values to check for activity.
                FuelActivity fuelActivity =
                        FuelDataActivityChecker.checkForActivity(relevantPositionsListForAlerts,
                                                                 deviceFuelEventMetadata,
                                                                 fuelSensor);

                sendNotificationIfNecessary(deviceId, fuelActivity, fuelSensor, fuelTankMaxVolume);
            }
        }

        removeFirstPositionIfNecessary(positionsForDeviceSensor, deviceId);
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
                Log.debug("Detected drain not within expected consumption");
                sendNotificationIfNecessary(deviceId, fuelActivity, fuelSensor.getPeripheralSensorId());
            } else {
                Log.debug("Detected drain that was within expected consumption, not sending notification.");
            }

        } else {
            sendNotificationIfNecessary(deviceId, fuelActivity, fuelSensor.getPeripheralSensorId());
        }
    }

    private void sendNotificationIfNecessary(final long deviceId, final FuelActivity fuelActivity, long peripheralSensorId) {
        if (fuelActivity.getActivityType() != FuelActivityType.NONE) {
            Log.debug("[FUEL_ACTIVITY]  DETECTED: " + fuelActivity.getActivityType()
                      + " starting at: " + fuelActivity.getActivityStartTime()
                      + " ending at: " + fuelActivity.getActivityEndTime()
                      + " volume: " + fuelActivity.getChangeVolume()
                      + " start lat, long " + fuelActivity.getActivityStartPosition().getLatitude()
                      + ", " + fuelActivity.getActivityStartPosition().getLongitude()
                      + " end lat, long " + fuelActivity.getActivityEndPosition().getLatitude()
                      + ", " + fuelActivity.getActivityEndPosition().getLongitude());

            // Add event to events table
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
                Log.warning("Error while saving fuel event to DB", error);
            }

            // Adding the sensor ID to the FCM notification does not make sense, since the end user does not care
            // about these IDs. In the future, if we think it is necessary, we'll add names to sensors so it is
            // clear which "tank" this notification came from.
            Context.getFcmPushNotificationManager().updateFuelActivity(fuelActivity);
        }
    }

    private void removeFirstPositionIfNecessary(TreeMultiset<Position> positionsForDeviceSensor, long deviceId) {
        if (positionsForDeviceSensor.size() > maxInMemoryPreviousPositionsListSize) {
            Position toRemove = positionsForDeviceSensor.firstEntry().getElement();
            positionsForDeviceSensor.remove(toRemove);
            Log.debug("Size of positionsForDeviceSensor with deviceId = " + deviceId
                      + " after removing position: " + positionsForDeviceSensor.size());
        }
    }
}
