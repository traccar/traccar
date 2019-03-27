package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Position;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.FuelActivity.FuelActivityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FuelDataActivityChecker {

    private static final Double fuelLevelChangeThreshold;

    static {
        fuelLevelChangeThreshold =
                Context.getConfig()
                       .getDouble("processing.peripheralSensorData.fuelLevelChangeThresholdLiters");
    }

    public static FuelActivity checkForActivity(List<Position> readingsForDevice,
                                                Map<String, FuelEventMetadata> deviceFuelEventMetadata,
                                                Long sensorId) {

        FuelActivity fuelActivity = new FuelActivity();

        final int readingsSize = readingsForDevice.size();
        final int midPoint = (readingsSize - 1) / 2;
        double leftSum = 0, rightSum = 0;

        for (int i = 0; i <= midPoint; i++) {
            leftSum += (double) readingsForDevice.get(i).getAttributes().get(Position.KEY_CALIBRATED_FUEL_LEVEL);
            rightSum += (double) readingsForDevice.get(i + midPoint).getAttributes().get(Position.KEY_CALIBRATED_FUEL_LEVEL);
        }

        double leftMean = leftSum / (midPoint + 1);
        double rightMean = rightSum / (midPoint + 1);
        double diffInMeans = Math.abs(leftMean - rightMean);

        long deviceId = readingsForDevice.get(0).getDeviceId();
        String lookupKey = deviceId + "_" + sensorId;
        Log.debug("[FUEL_ACTIVITY] deviceId: " + deviceId + "diffInMeans: " + diffInMeans
                          + " fuelLevelChangeThreshold: " + fuelLevelChangeThreshold
                          + " diffInMeans > fuelLevelChangeThreshold: " + (diffInMeans > fuelLevelChangeThreshold));

        if (diffInMeans > fuelLevelChangeThreshold) {

            if (!deviceFuelEventMetadata.containsKey(lookupKey)) {
                Position midPointPosition = readingsForDevice.get(midPoint);
                deviceFuelEventMetadata.put(lookupKey, new FuelEventMetadata());

                FuelEventMetadata fuelEventMetadata = deviceFuelEventMetadata.get(lookupKey);
                double leftMedian = getMedianValue(readingsForDevice, 0, midPoint);
                fuelEventMetadata.setStartLevel(leftMedian);

                fuelEventMetadata.setErrorCheckStart((double) readingsForDevice.get(0)
                                                                               .getAttributes()
                                                                               .get(Position.KEY_CALIBRATED_FUEL_LEVEL));

                fuelEventMetadata.setStartTime(midPointPosition.getDeviceTime());
                fuelEventMetadata.setActivityStartPosition(midPointPosition);

                Log.debug("[FUEL_ACTIVITY_START] Activity start detected: deviceId" + deviceId + " at: "
                                  + midPointPosition.getDeviceTime());

                StringBuilder rawFuelValuesInReadings = new StringBuilder();
                StringBuilder timestamps = new StringBuilder();
                readingsForDevice.forEach(p -> {
                    rawFuelValuesInReadings.append((double) p.getAttributes()
                                                             .get(Position.KEY_CALIBRATED_FUEL_LEVEL) + ", ");
                    timestamps.append(p.getDeviceTime());
                });

                // Set window on metadata so we can use it for outlier check in event later
                // This is the first time we're creating the window list, so add all positions that were passed in.
                // the next time we add to this list, we'll have an overlapping section, that'll need to be removed
                // before adding to the window list.
                List<Position> window = new ArrayList<>();
                window.addAll(readingsForDevice);
                fuelEventMetadata.setActivityWindow(window);


                Log.debug("[FUEL_ACTIVITY_START] rawFuelValues that crossed threshold for deviceId: " + deviceId
                                  + " - " + rawFuelValuesInReadings);
                Log.debug("[FUEL_ACTIVITY_START] corresponding timestamps: " + timestamps);
                Log.debug("[FUEL_ACTIVITY_START] Midpoint: "
                                  + midPointPosition.getAttributes()
                                                    .get(Position.KEY_CALIBRATED_FUEL_LEVEL));

                Log.debug("[FUEL_ACTIVITY_START] Left median: " + leftMedian);
                Log.debug("[FUEL_ACTIVITY_START] metadata: " + fuelEventMetadata);

            } else {
                // We've already identified the start here, and since we've crossed the threshold, we're between the start
                // and end of the event. So just add to the window list
                appendActivityWindow(readingsForDevice, deviceFuelEventMetadata.get(lookupKey));
            }
        }

        if (diffInMeans < fuelLevelChangeThreshold && deviceFuelEventMetadata.containsKey(lookupKey)) {

            Position midPointPosition = readingsForDevice.get(midPoint);
            FuelEventMetadata fuelEventMetadata = deviceFuelEventMetadata.get(lookupKey);

            double rightMedian = getMedianValue(readingsForDevice, midPoint, readingsSize);
            fuelEventMetadata.setEndLevel(rightMedian);

            fuelEventMetadata.setErrorCheckEnd((double) readingsForDevice.get(readingsForDevice.size() - 1)
                                                                         .getAttributes()
                                                                         .get(Position.KEY_CALIBRATED_FUEL_LEVEL));

            fuelEventMetadata.setEndTime(midPointPosition.getDeviceTime());
            fuelEventMetadata.setActivityEndPosition(midPointPosition);

            double fuelChangeVolume = fuelEventMetadata.getEndLevel() - fuelEventMetadata.getStartLevel();
            double errorCheckFuelChange = fuelEventMetadata.getErrorCheckEnd() - fuelEventMetadata.getErrorCheckStart();

            // We've identified the end. Add to the activity window one last time, so we can use the values for
            // checking for an outlier in the determined activity window below.
            appendActivityWindow(readingsForDevice, fuelEventMetadata);

            Log.debug("[FUEL_ACTIVITY_END] Activity end detected: deviceId" + deviceId + " at: "
                              + midPointPosition.getDeviceTime());

            StringBuilder rawFuelValuesInReadings = new StringBuilder();
            StringBuilder timestamps = new StringBuilder();
            readingsForDevice.forEach(p -> {
                rawFuelValuesInReadings.append((double) p.getAttributes()
                                                         .get(Position.KEY_CALIBRATED_FUEL_LEVEL) + ", ");
                timestamps.append(p.getDeviceTime());
            });
            Log.debug("[FUEL_ACTIVITY_END] rawFuelValues that crossed threshold for deviceId: " + deviceId
                              + " - " + rawFuelValuesInReadings);
            Log.debug("[FUEL_ACTIVITY_END] corresponding timestamps: " + timestamps);
            Log.debug("[FUEL_ACTIVITY_END] Midpoint: " + midPointPosition.getAttributes()
                                                                         .get(Position.KEY_CALIBRATED_FUEL_LEVEL));
            Log.debug("[FUEL_ACTIVITY_START] Right median: " + rightMedian);
            Log.debug("[FUEL_ACTIVITY_END] metadata: " + fuelEventMetadata);
            Log.debug("[FUEL_ACTIVITY_END] fuelChangeVolume: " + fuelChangeVolume);
            Log.debug("[FUEL_ACTIVITY_END] errorCheckFuelChange: " + errorCheckFuelChange);

            Optional<Long> maxCapacity = Context.getPeripheralSensorManager().getFuelTankMaxCapacity(deviceId, sensorId);
            boolean isDataLoss = FuelDataLossChecker.isFuelEventDueToDataLoss(fuelEventMetadata, maxCapacity);

            if (!isDataLoss && fuelChangeVolume < 0.0) {
                fuelActivity.setActivityType(FuelActivity.FuelActivityType.FUEL_DRAIN);
                setActivityParameters(fuelActivity, fuelEventMetadata, fuelChangeVolume);
                checkForMissedOutlier(fuelEventMetadata, fuelActivity);
                // TODO: So now that we've checked and marked positions outliers in the event, what next?
                deviceFuelEventMetadata.remove(lookupKey);
            } else if (fuelChangeVolume > 0.0) {
                fuelActivity.setActivityType(FuelActivity.FuelActivityType.FUEL_FILL);
                setActivityParameters(fuelActivity, fuelEventMetadata, fuelChangeVolume);
                checkForMissedOutlier(fuelEventMetadata, fuelActivity);
                // TODO: So now that we've checked and marked positions outliers in the event, what next?
                deviceFuelEventMetadata.remove(lookupKey);
            } else {
                // The start may have been detected as a false positive. In any case, remove after we determine the kind
                // of activity.
                Log.debug("[FUEL_ACTIVITY] Removing event metadata from list to avoid false positives: "
                                  + lookupKey);
                deviceFuelEventMetadata.remove(lookupKey);
            }
        }

        return fuelActivity;
    }

    public static Optional<FuelActivity> checkForActivityIfDataLoss(final Position position,
                                                                    final Position lastPosition,
                                                                    final Optional<Long> maxTankMaxVolume) {

        final boolean requiredFieldsPresent = FuelDataLossChecker.checkRequiredFieldsPresent(lastPosition, position);
        if (!requiredFieldsPresent) {
            // Not enough info to process data loss.
            return Optional.empty();
        }

        ExpectedFuelConsumption expectedFuelConsumption =
                FuelDataLossChecker.getExpectedFuelConsumptionValues(lastPosition, position, maxTankMaxVolume);

        double calculatedFuelChangeVolume = position.getDouble(Position.KEY_CALIBRATED_FUEL_LEVEL)
                - lastPosition.getDouble(Position.KEY_CALIBRATED_FUEL_LEVEL);

        if (Math.abs(calculatedFuelChangeVolume) > expectedFuelConsumption.allowedDeviation) {
            if (calculatedFuelChangeVolume < 0.0) {
                boolean isConsumptionExpected =
                        FuelDataLossChecker.isFuelConsumptionAsExpected(calculatedFuelChangeVolume,
                                                                        expectedFuelConsumption);

                if (isConsumptionExpected) {
                    Log.info(String.format(
                            "Determined data loss, but cannot identify fuel event since fuel consumption " +
                                    " is within expected range: %s", expectedFuelConsumption));

                    return Optional.empty();
                }

                if (Math.abs(calculatedFuelChangeVolume) > expectedFuelConsumption.expectedMaxFuelConsumed) {
                    // TODO: if Math.abs(calculatedFuelChangeVolume) > expectedFuelConsumption.expectedMaxFuelConsumed
                    // then the following calculation will return a +ve number. Same in the FILL part. Fix this or
                    // real drain - based on what we
                    // want in the db actually.
                    double possibleFuelDrain =
                            Math.abs(calculatedFuelChangeVolume) -
                                    expectedFuelConsumption.expectedCurrentFuelConsumed;
                    FuelActivity activity =
                            new FuelActivity(FuelActivity.FuelActivityType.PROBABLE_FUEL_DRAIN,
                                             possibleFuelDrain, lastPosition, position);
                    return Optional.of(activity);
                } else {
                    double possibleFuelFill =
                            expectedFuelConsumption.expectedCurrentFuelConsumed -
                                    Math.abs(calculatedFuelChangeVolume);
                    FuelActivity activity =
                            new FuelActivity(FuelActivity.FuelActivityType.PROBABLE_FUEL_FILL,
                                             possibleFuelFill, lastPosition, position);
                    return Optional.of(activity);
                }
            } else {
                double expectedFuelFill =
                        calculatedFuelChangeVolume + expectedFuelConsumption.expectedCurrentFuelConsumed;
                FuelActivity activity =
                        new FuelActivity(FuelActivity.FuelActivityType.EXPECTED_FUEL_FILL,
                                         expectedFuelFill, lastPosition, position);
                return Optional.of(activity);
            }
        }

        return Optional.empty();
    }

    private static void setActivityParameters(final FuelActivity fuelActivity,
                                              final FuelEventMetadata fuelEventMetadata,
                                              final double fuelChangeVolume) {
        fuelActivity.setChangeVolume(fuelChangeVolume);
        fuelActivity.setActivityStartTime(fuelEventMetadata.getStartTime());
        fuelActivity.setActivityEndTime(fuelEventMetadata.getEndTime());
        fuelActivity.setActivityStartPosition(fuelEventMetadata.getActivityStartPosition());
        fuelActivity.setActivityEndPosition(fuelEventMetadata.getActivityEndPosition());
    }

    private static void appendActivityWindow(final List<Position> readingsForDevice,
                                             final FuelEventMetadata fuelEventMetadata) {

        List<Position> activityWindow = fuelEventMetadata.getActivityWindow();

        // Clone readingsForDevice, so we don't alter that list unintentionally coz it's passed in as an arg.
        List<Position> copyOfReadingsForDevice = new ArrayList<>(readingsForDevice);

        copyOfReadingsForDevice.removeAll(activityWindow);
        activityWindow.addAll(copyOfReadingsForDevice);
    }

    private static double getMedianValue(final List<Position> readingsForDevice,
                                         final int start,
                                         final int end) {

        final List<Double> readings = readingsForDevice.subList(start, end)
                                                       .stream()
                                                       .map(p -> (double) p.getAttributes()
                                                                           .get(Position.KEY_CALIBRATED_FUEL_LEVEL))
                                                       .collect(Collectors.toList());

        // Sort them in the ascending order
        readings.sort(Comparator.naturalOrder());

        // pick the middle position
        return readings.get((readings.size() - 1) / 2);
    }

    private static void checkForMissedOutlier(final FuelEventMetadata fuelEventMetadata,
                                              final FuelActivity fuelActivity) {

        double minFuelValue = 0.0, maxFuelValue = 0.0;

        FuelActivityType activityType = fuelActivity.getActivityType();
        Position startPositon = fuelActivity.getActivityStartPosition();
        Position endPosition = fuelActivity.getActivityEndPosition();

        // Note: these can be the only 2 cases, since this method is not called during data loss checks.
        if ( activityType == FuelActivityType.FUEL_FILL) {
            minFuelValue = startPositon.getDouble(Position.KEY_CALIBRATED_FUEL_LEVEL) - fuelLevelChangeThreshold;
            maxFuelValue = endPosition.getDouble(Position.KEY_CALIBRATED_FUEL_LEVEL) + fuelLevelChangeThreshold;
        } else if (activityType == FuelActivityType.FUEL_DRAIN) {
            minFuelValue = endPosition.getDouble(Position.KEY_CALIBRATED_FUEL_LEVEL) - fuelLevelChangeThreshold;
            maxFuelValue = startPositon.getDouble(Position.KEY_CALIBRATED_FUEL_LEVEL) + fuelLevelChangeThreshold;
        }

        for (Position position : fuelEventMetadata.getActivityWindow()) {
            boolean isOutlier = position.getDouble(Position.KEY_CALIBRATED_FUEL_LEVEL) < minFuelValue
                                || position.getDouble(Position.KEY_CALIBRATED_FUEL_LEVEL) > maxFuelValue;

            position.set(Position.KEY_FUEL_IS_OUTLIER, isOutlier);

            // These are amazingly expensive DB write calls. 2nd occurence of this type of writes.
            // Need to make a better async way to do this.
            if (isOutlier) {
                FuelSensorDataHandlerHelper.updatePosition(position);
            }
        }
    }
}

