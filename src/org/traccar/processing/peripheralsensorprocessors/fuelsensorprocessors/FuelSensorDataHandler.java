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

    private static int minValuesForMovingAvg;
    private static int minValuesForOutlierDetection;
    private static int maxInMemoryPreviousPositionsListSize;
    private static int hoursOfDataToLoad;
    private static int maxValuesForAlerts;
    private static int currentEventLookBackSeconds;
    private static final Map<String, Double> sensorTypeThresholdMap = new ConcurrentHashMap<>();

    private final Map<Long, Boolean> possibleDataLossByDevice = new ConcurrentHashMap<>();
    private final Map<Long, Position> nonOutlierInLastWindowByDevice = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, TreeMultiset<Position>>> previousPositions = new ConcurrentHashMap<>();
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

        sensorTypeThresholdMap
                .put("digital",
                     Context.getConfig()
                            .getDouble("processing.peripheralSensorData.fuelLevelChangeThresholdLitersDigital"));

        sensorTypeThresholdMap
                .put("analog",
                     Context.getConfig()
                            .getDouble("processing.peripheralSensorData.fuelLevelChangeThresholdLitersAnalog"));
    }

    public FuelSensorDataHandler() {
        loadOldPositions();
    }

    public FuelSensorDataHandler(boolean loader) {
        // Do nothing constructor for tests.
    }

    @Override
    protected Position handlePosition(Position position) {
        try {
            long deviceId = position.getDeviceId();

            Optional<PeripheralSensor> peripheralSensorOnDevice =
                    Context.getPeripheralSensorManager().getSensorByDeviceId(deviceId);

            if (!peripheralSensorOnDevice.isPresent()) {
                updateWithLastAvailable(position, Position.KEY_FUEL_LEVEL);
                return position;
            }

            updateLatestKnownPosition(position);

            if (!position.getAttributes().containsKey(Position.KEY_ODOMETER)
                    && deviceLastKnownOdometerPositionLookup.containsKey(deviceId)) {

                Position lastPosition = deviceLastKnownOdometerPositionLookup.get(deviceId);
                position.set(Position.KEY_ODOMETER, (int) lastPosition.getAttributes().get(Position.KEY_ODOMETER));
            }

            if ((position.getAttributes().containsKey(Position.KEY_POWER)
                    && ((Number) position.getAttributes().get(Position.KEY_POWER)).doubleValue()  <= 0.0)) {

                Log.debug("Device power too low, updating with last known fuel level for deviceId"
                                  + position.getDeviceId());
                updateWithLastAvailable(position, Position.KEY_FUEL_LEVEL);
                return position;
            }

            long sensorIdOnPosition = peripheralSensorOnDevice.get().getPeripheralSensorId();
            String fuelSensorType = peripheralSensorOnDevice.get().getTypeName().split("_")[1].toLowerCase();

            handleSensorData(position, sensorIdOnPosition, sensorTypeThresholdMap.get(fuelSensorType));

        } catch (Exception e) {
            Log.debug(String.format("Exception in processing fuel info: %s", e.getMessage()));
            e.printStackTrace();
        } finally {
            return position;
        }
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

    private void updateWithLastAvailable(final Position position, final String attributeToUpdate) {
        long deviceId = position.getDeviceId();
        if (!previousPositions.containsKey(deviceId)) {
            Log.debug("deviceId not found in previousPositions" + deviceId);
            return;
        }

        Map<Long, TreeMultiset<Position>> sensorReadingsFromDevice = previousPositions.get(deviceId);

        if (sensorReadingsFromDevice.size() < 1) {
            Log.debug("No readings for sensors found on deviceId: " + deviceId);
            return;
        }

        Optional<Long> sensorId = sensorReadingsFromDevice.keySet().stream().findFirst();

        if (!sensorId.isPresent() || sensorReadingsFromDevice.get(sensorId.get()).size() < 1) {
            Log.debug("No relevant sensorId found on deviceId: " + deviceId + ": "
                 + "sensorId present: " + sensorId.isPresent()
                 + "keySet: " + sensorReadingsFromDevice.keySet()
                 + " readings: " + (sensorId.isPresent() ? sensorReadingsFromDevice.get(sensorId.get()).size() : 0));
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
        if (!lastKnownPosition.getAttributes().containsKey(attributeToUpdate)) {
            Log.debug(String.format("Last known position for deviceId %d doesn't have property %s set.",
                                    position.getDeviceId(), attributeToUpdate));
            return;
        }

        switch(attributeToUpdate) {
            case Position.KEY_FUEL_LEVEL:
                position.set(attributeToUpdate, (double) lastKnownPosition.getAttributes().get(attributeToUpdate));
                break;
        }
    }

    private Optional<Position> getLastKnownPositionForDevice(Position position) {
        long deviceId = position.getDeviceId();
        if (!previousPositions.containsKey(deviceId)) {
            Log.debug("deviceId not found in previousPositions" + deviceId);
            return Optional.empty();
        }

        Map<Long, TreeMultiset<Position>> sensorReadingsFromDevice = previousPositions.get(deviceId);

        if (sensorReadingsFromDevice.isEmpty()) {
            Log.debug("No readings for sensors found on deviceId: " + deviceId);
            return Optional.empty();
        }

        Optional<Long> sensorId = sensorReadingsFromDevice.keySet().stream().findFirst();

        if (!sensorId.isPresent() || sensorReadingsFromDevice.get(sensorId.get()).size() < 1) {
            Log.debug("No relevant sensorId found on deviceId: " + deviceId + ": "
                    + "sensorId present: " + sensorId.isPresent()
                    + "keySet: " + sensorReadingsFromDevice.keySet()
                    + " readings: " + (sensorId.isPresent() ? sensorReadingsFromDevice.get(sensorId.get()).size() : 0));
            return Optional.empty();
        }

        return Optional.of(sensorReadingsFromDevice.get(sensorId.get()).lastEntry().getElement());
    }

    private Optional<Position> findFirstNonOutlierInLastWindow(Position position, int currentWindowOffset) {
        long deviceId = position.getDeviceId();
        if (!previousPositions.containsKey(deviceId)) {
            Log.debug("deviceId not found in previousPositions" + deviceId);
            return Optional.empty();
        }

        Map<Long, TreeMultiset<Position>> sensorReadingsFromDevice = previousPositions.get(deviceId);

        if (sensorReadingsFromDevice.isEmpty()) {
            Log.debug("No readings for sensors found on deviceId: " + deviceId);
            return Optional.empty();
        }

        Optional<Long> sensorId = sensorReadingsFromDevice.keySet().stream().findFirst();

        if (!sensorId.isPresent()) {
            Log.debug(String.format("No sensor detected on device %d",
                                    deviceId));
            return Optional.empty();
        }

        // Will return Optional.empty if there aren't enough elements in the list.
        return sensorReadingsFromDevice
                .get(sensorId.get())
                .descendingMultiset()
                .stream()
                .skip(currentWindowOffset)
                .filter(p -> p.getAttributes().containsKey(Position.KEY_FUEL_IS_OUTLIER) &&
                             !(boolean) p.getAttributes().get(Position.KEY_FUEL_IS_OUTLIER))
                .findFirst();
    }

    private void loadOldPositions() {
        this.loadingOldDataFromDB = true;
        Collection<Device> devices = Context.getDeviceManager().getAllDevices();

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
                                  Long sensorId,
                                  double fuelLevelChangeThreshold) {

        long deviceId = position.getDeviceId();

        if (!previousPositions.containsKey(deviceId) || !previousPositions.get(deviceId).containsKey(sensorId)) {
            TreeMultiset<Position> positions =
                    TreeMultiset.create(Comparator.comparing(p -> p.getDeviceTime().getTime()));
            Map<Long, TreeMultiset<Position>> sensorPositions = new ConcurrentHashMap<>();
            sensorPositions.put(sensorId, positions);
            previousPositions.put(deviceId, sensorPositions);
        }

        TreeMultiset<Position> positionsForDeviceSensor = previousPositions.get(deviceId).get(sensorId);

        if (loadingOldDataFromDB || position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
            // This is a position from the DB, add to the list and move on.
            // If we don't skip further processing, it might trigger FCM notification unnecessarily.
            positionsForDeviceSensor.add(position);
            removeFirstPositionIfNecessary(positionsForDeviceSensor, deviceId);
            return;
        }

        //If this is a back dated packet, do nothing
        Optional<Position> lastPacketProcessed = getLastKnownPositionForDevice(position);

        if (lastPacketProcessed.isPresent()
            && position.getDeviceTime().compareTo((lastPacketProcessed.get().getDeviceTime())) <= 0) {

            Log.debug(String.format("Backdated packets detected for device: %d. Skipping fuel processing for them",
                                    deviceId));
            return;
        }

        List<Position> relevantPositionsListForAverages =
                FuelSensorDataHandlerHelper.getRelevantPositionsSubList(
                        positionsForDeviceSensor,
                        position,
                        minValuesForMovingAvg - 1,
                        currentEventLookBackSeconds);

        Log.debug("Size of list before getting averages, deviceId: " + relevantPositionsListForAverages.size() + ", " + deviceId);

        relevantPositionsListForAverages.add(position);
        double currentFuelLevelAverage =
                FuelSensorDataHandlerHelper.getAverageFuelValue(relevantPositionsListForAverages);

        // KEY_FUEL_LEVEL will hold the smoothed data, which is average of raw values in the relevant list.
        // Until the number of positions in the list comes up to the expected number of positions for calculating
        // averages and / or outliers, this will calculate the average of the existing list and set that on the
        // current position, so it's available for later calculations. This will also make sure that we are able to send
        // this info to any client that's listening for these updates.

        position.set(Position.KEY_FUEL_LEVEL, currentFuelLevelAverage);
        positionsForDeviceSensor.add(position);

        // Detect and remove outliers
        List<Position> relevantPositionsListForOutliers =
                FuelSensorDataHandlerHelper.getRelevantPositionsSubList(
                        positionsForDeviceSensor, position, minValuesForOutlierDetection, currentEventLookBackSeconds);

        if (relevantPositionsListForAverages.size() == 1) {
            possibleDataLossByDevice.put(deviceId, true);
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
                fuelTankMaxVolume);

        Position outlierCheckPosition = relevantPositionsListForOutliers.get(indexOfPositionEvaluation);
        outlierCheckPosition.set(Position.KEY_FUEL_IS_OUTLIER, outlierPresent);

        // Note: Need to do this in a better way since this is a direct write to the db and can slow things down.
        // We could use an external queue and update these positions from there, without affecting processing here.
        // Also, we do not want to lose any data coming in, so we'll only mark the position as an outlier rather
        // than deleting it.
        updatePosition(outlierCheckPosition);

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

        currentFuelLevelAverage = FuelSensorDataHandlerHelper.getAverageFuelValue(relevantPositionsListForAverages);
        positionUnderEvaluation.set(Position.KEY_FUEL_LEVEL, currentFuelLevelAverage);

        // Update the position in the db so the recalculated average is reflected there.
        updatePosition(positionUnderEvaluation);

        // At this point we know indexOfPositionEvaluation in the new window is not an outlier. So if we haven't found
        // the first outlier in the last window yet, go find it.
        boolean possibleDataLoss = possibleDataLossByDevice.getOrDefault(deviceId, false);
        if (possibleDataLoss && !nonOutlierInLastWindowByDevice.containsKey(deviceId)) {
            Optional<Position> nonOutlierInLastWindow =
                    findFirstNonOutlierInLastWindow(outlierCheckPosition, relevantPositionsListForOutliers.size());
            if (nonOutlierInLastWindow.isPresent()) {
                nonOutlierInLastWindowByDevice.put(deviceId, nonOutlierInLastWindow.get());
            }
        }

        if (possibleDataLoss && nonOutlierInLastWindowByDevice.containsKey(deviceId)) {
            Optional<FuelActivity> fuelActivity =
                    FuelDataActivityChecker.checkForActivityIfDataLoss(outlierCheckPosition,
                                                                       nonOutlierInLastWindowByDevice.get(deviceId),
                                                                       fuelTankMaxVolume);

            if(fuelActivity.isPresent()) {
                sendNotificationIfNecessary(deviceId, fuelActivity.get());
            }
            possibleDataLossByDevice.remove(deviceId);
            nonOutlierInLastWindowByDevice.remove(deviceId);
        }


        //-- End Outliers

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
                                                             sensorId,
                                                             fuelLevelChangeThreshold);

            sendNotificationIfNecessary(deviceId, fuelActivity);
        }

        removeFirstPositionIfNecessary(positionsForDeviceSensor, deviceId);
    }

    private void sendNotificationIfNecessary(final long deviceId, final FuelActivity fuelActivity) {
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
            String eventType =
                    fuelActivity.getActivityType() == FuelActivityType.FUEL_FILL
                            ? Event.TYPE_FUEL_FILL
                            : Event.TYPE_FUEL_DRAIN;

            Event event = new Event(eventType, deviceId,
                                    fuelActivity.getActivityStartPosition().getId());
            event.set("startTime", fuelActivity.getActivityStartTime().getTime());
            event.set("endTime", fuelActivity.getActivityEndTime().getTime());
            event.set("volume", fuelActivity.getChangeVolume());
            event.set("endPositionId", fuelActivity.getActivityEndPosition().getId());

            try {
                getDataManager().addObject(event);
            } catch (SQLException error) {
                Log.warning("Error while saving fuel event to DB", error);
            }

            Context.getFcmPushNotificationManager().updateFuelActivity(fuelActivity);
        }
    }

    private static void updatePosition(final Position outlierPosition) {
        try {
            Context.getDataManager().updateObject(outlierPosition);
        } catch (SQLException e) {
            Log.debug("Exception while updating outlier position with id: " + outlierPosition.getId());
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
