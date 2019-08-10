package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.StringUtil;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.PeripheralSensor;
import org.traccar.model.Position;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.FuelActivity.FuelActivityType;

import java.util.*;
import java.util.stream.Collectors;

public class FuelDataActivityChecker {

    private static final String STATIONARY_TYPE = "stationary";
    private static final String TYPE_ATTR_NAME = "type";

    private static Set<String> adjustVolumeFor = Sets.newConcurrentHashSet();

    static {
        adjustVolumeFor.add(FuelActivityType.FUEL_FILL.name());
        adjustVolumeFor.add(FuelActivityType.EXPECTED_FUEL_FILL.name());
        adjustVolumeFor.add(FuelActivityType.PROBABLE_FUEL_FILL.name());
    }

    public static FuelActivity checkForActivity(List<Position> readingsForDevice,
                                                Map<String, FuelEventMetadata> deviceFuelEventMetadata,
                                                PeripheralSensor fuelSensor) {

        FuelActivity fuelActivity = new FuelActivity();
        String calibFuelDataField = fuelSensor.getCalibFuelFieldName();

        final int readingsSize = readingsForDevice.size();
        final int midPoint = (readingsSize - 1) / 2;
        double leftSum = 0, rightSum = 0;

        boolean ignitionOnInAnyRHWindow = false;
        for (int i = 0; i <= midPoint; i++) {
            leftSum += (double) readingsForDevice.get(i).getAttributes().get(calibFuelDataField);
            rightSum += (double) readingsForDevice.get(i + midPoint).getAttributes().get(calibFuelDataField);
            ignitionOnInAnyRHWindow |= readingsForDevice.get(i + midPoint).getBoolean(Position.KEY_IGNITION);
        }

        double leftMean = leftSum / (midPoint + 1);
        double rightMean = rightSum / (midPoint + 1);
        double diffInMeans = leftMean - rightMean;

        Position lowestInLHWindow = readingsForDevice.get(0);
        Position highestInLHWindow = readingsForDevice.get(midPoint);
        Position lowestInRHWindow = readingsForDevice.get(midPoint + 1);
        Position highestInRHWindow = readingsForDevice.get(readingsSize - 1);

        long deviceId = readingsForDevice.get(0).getDeviceId();

        DeviceConsumptionInfo consumptionInfo = Context.getDeviceManager().getDeviceConsumptionInfo(deviceId);
        String deviceType = Context.getDeviceManager().getById(deviceId).getString(TYPE_ATTR_NAME);
        boolean isStationary = StringUtils.isNoneBlank(deviceType) && deviceType.equals(STATIONARY_TYPE);

        double fuelLevelChangeThreshold = consumptionInfo.getFuelActivityThreshold();

        String deviceSensorLookupKey = deviceId + "_" + fuelSensor.getPeripheralSensorId();
        double fillThreshold = fuelSensor.getFillThreshold().isPresent()? fuelSensor.getFillThreshold().get() : fuelLevelChangeThreshold;
        double drainThreshold = fuelSensor.getDrainThreshold().isPresent()? fuelSensor.getDrainThreshold().get() : fuelLevelChangeThreshold;
        double ignOnDrainThreshold = fuelSensor.getIgnOnDrainThreshold().isPresent()? fuelSensor.getIgnOnDrainThreshold().get() : drainThreshold;
        double ignOffDrainThreshold = fuelSensor.getIgnOffDrainThreshold().isPresent()? fuelSensor.getIgnOffDrainThreshold().get() : drainThreshold;
        double drainThresholdToUseForStart = ignitionOnInAnyRHWindow? ignOnDrainThreshold : ignOffDrainThreshold;

        FuelSensorDataHandlerHelper.logDebugIfDeviceId(
                "[FUEL_ACTIVITY] lookupKey: " + deviceSensorLookupKey
                        + " diffInMeans: " + diffInMeans
                        + " fillThreshold: " + fillThreshold
                        + " ignitionOnInAnyRHWindow: " + ignitionOnInAnyRHWindow
                        + " drainThreshold: " + drainThreshold
                        + " ignOnDrainThreshold: " + ignOnDrainThreshold
                        + " ignOffDrainThreshold: " + ignOffDrainThreshold
                        + " drainThresholdToUseForStart: " + drainThresholdToUseForStart
                        + " diffInMeans > fillThreshold: " + (Math.abs(diffInMeans) > fillThreshold)
                        + " diffInMeans > drainThreshold: " + (Math.abs(diffInMeans) > drainThreshold), deviceId);

        boolean isEnginelessHourly = consumptionInfo.getDeviceConsumptionType().toLowerCase().equals(DeviceConsumptionInfo.ENGINELESS_HOURLY_CONSUMPTION_TYPE);

        Optional<String> possibleActivityStartTypeStartType = determinePossibleActivityStartType(diffInMeans,
                                                                                                 fillThreshold,
                                                                                                 drainThresholdToUseForStart);

        if (possibleActivityStartTypeStartType.isPresent()) {
            String startType = possibleActivityStartTypeStartType.get();

            // For enginelesshourly devices, we want to register drains when fuel flows out of the tank - which is it's
            // normal consumption mechanism.
            // We connect the ignition to the pump that drains fuel from the tank, and check if the ignition
            // was on when we detected the start of the event, and off when the event end is detected.
            boolean isDrain = startType.equals(FuelActivityType.FUEL_DRAIN.name());
            boolean isFill = startType.equals(FuelActivityType.FUEL_FILL.name());

            if (isEnginelessHourly && isDrain && !ignitionOnInAnyRHWindow) {
                FuelSensorDataHandlerHelper.logDebugIfDeviceId("Detected fuel drain start but ignition not on in any RH Window", deviceId);
                return new FuelActivity();
            }

            String logMessage = String.format("Fuel event started: %d %b %b %b", deviceId, isDrain, isFill, ignitionOnInAnyRHWindow);
            FuelSensorDataHandlerHelper.logDebugIfDeviceId(logMessage, deviceId);

            String oppositeEventType = startType.equals(FuelActivityType.FUEL_FILL.name())? FuelActivityType.FUEL_DRAIN.name() :
                                       FuelActivityType.FUEL_FILL.name();

            String activityLookupKey = getActivityLookupKey(deviceSensorLookupKey, startType);
            String oppositeActivityLookupKey = getActivityLookupKey(deviceSensorLookupKey, oppositeEventType);

            if (!deviceFuelEventMetadata.containsKey(oppositeActivityLookupKey)) {

            }

            if (!deviceFuelEventMetadata.containsKey(activityLookupKey)) {
                Position midPointPosition = readingsForDevice.get(midPoint);
                deviceFuelEventMetadata.put(activityLookupKey, new FuelEventMetadata());

                FuelEventMetadata fuelEventMetadata = deviceFuelEventMetadata.get(activityLookupKey);
                double leftMedian = getMedianValue(readingsForDevice, 0, midPoint + 1, fuelSensor);

                Position eventStartPosition = midPointPosition;
                double startLevel = leftMedian;

                if (isStationary && ignitionOnInAnyRHWindow) {
                    if (isFill) {
                        eventStartPosition = lowestInRHWindow;
                        startLevel = lowestInRHWindow.getDouble(fuelSensor.getCalibFuelFieldName());
                        Log.info("Setting start to lowestInRHWindow");
                    } else if (isDrain) {
                        eventStartPosition = highestInRHWindow;
                        startLevel = highestInRHWindow.getDouble(fuelSensor.getCalibFuelFieldName());
                        Log.info("Setting start to highestInRHWindow");
                    }
                }

                fuelEventMetadata.setStartLevel(startLevel);

                fuelEventMetadata.setErrorCheckStart((double) readingsForDevice.get(0)
                                                                               .getAttributes()
                                                                               .get(calibFuelDataField));

                fuelEventMetadata.setStartTime(eventStartPosition.getDeviceTime());
                fuelEventMetadata.setActivityStartPosition(eventStartPosition);

                FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_START] Activity start detected: lookupKey" + activityLookupKey + " at: "
                                  + eventStartPosition.getDeviceTime(), deviceId);

                StringBuilder rawFuelValuesInReadings = new StringBuilder();
                StringBuilder timestamps = new StringBuilder();
                readingsForDevice.forEach(p -> {
                    rawFuelValuesInReadings.append((double) p.getAttributes()
                                                             .get(calibFuelDataField));
                    timestamps.append(p.getDeviceTime());
                });

                // Set window on metadata so we can use it for outlier check in event later
                // This is the first time we're creating the window list, so add all positions that were passed in.
                // the next time we add to this list, we'll have an overlapping section, that'll need to be removed
                // before adding to the window list.
                List<Position> window = new ArrayList<>();
                window.addAll(readingsForDevice);
                fuelEventMetadata.setActivityWindow(window);


                FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_START] rawFuelValues that crossed threshold for lookupKey: "
                                           + activityLookupKey + " - " + rawFuelValuesInReadings, deviceId);
                FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_START] Midpoint: "
                                  + midPointPosition.getAttributes()
                                                    .get(calibFuelDataField), deviceId);

                FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_START] Left median: " + leftMedian, deviceId);
                FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_START] metadata: " + fuelEventMetadata, deviceId);

            } else {
                // We've already identified the start here, and since we've crossed the threshold, we're between the start
                // and end of the event. So just add to the window list
                appendActivityWindow(readingsForDevice, deviceFuelEventMetadata.get(activityLookupKey));
            }
        }

        boolean ignitionOnRHWindowLast = readingsForDevice.get(readingsSize - 1).getBoolean(Position.KEY_IGNITION);
        double drainThresholdToUseForEnd = ignitionOnRHWindowLast? ignOnDrainThreshold : ignOffDrainThreshold;

        Optional<String> possibleActivityEndType = determinePossibleActivityEndType(diffInMeans,
                                                                                    fillThreshold,
                                                                                    drainThresholdToUseForEnd,
                                                                                    deviceFuelEventMetadata,
                                                                                    deviceSensorLookupKey);

        if (possibleActivityEndType.isPresent()) {

            String endType = possibleActivityEndType.get();
            boolean isEndDrain = endType.equals(FuelActivityType.FUEL_DRAIN.name());
            boolean isEndFill = endType.equals(FuelActivityType.FUEL_FILL.name());

            if (isEnginelessHourly && isEndDrain && ignitionOnRHWindowLast) {

                // Ignition is still on, we're probably not done with the "drain" for the "enginelesshourly" types.
                FuelSensorDataHandlerHelper.logDebugIfDeviceId("Detected fuel drain end but ignition is still on last rh window", deviceId);
                return new FuelActivity();
            }

            String logMessage = String.format("Fuel event ended: %d %b %b %f", deviceId, isEndDrain, ignitionOnRHWindowLast, drainThresholdToUseForEnd);
            FuelSensorDataHandlerHelper.logDebugIfDeviceId(logMessage, deviceId);

            String activityLookupKey = getActivityLookupKey(deviceSensorLookupKey, endType);

            Position midPointPosition = readingsForDevice.get(midPoint);
            FuelEventMetadata fuelEventMetadata = deviceFuelEventMetadata.get(activityLookupKey);
            double rightMedian = getMedianValue(readingsForDevice, midPoint, readingsSize, fuelSensor);

            Position endPosition = midPointPosition;
            double endLevel = rightMedian;

            if (isStationary && ignitionOnRHWindowLast) {
                if (isEndFill) {
                    endPosition = highestInLHWindow;
                    endLevel = endPosition.getDouble(fuelSensor.getCalibFuelFieldName());
                    Log.info("Setting end to highestInLHWindow");
                } else if (isEndDrain) {
                    endPosition = lowestInLHWindow;
                    endLevel = endPosition.getDouble(fuelSensor.getCalibFuelFieldName());
                    Log.info("Setting end to lowestInLHWindow");
                }
            }

            fuelEventMetadata.setEndLevel(endLevel);

            fuelEventMetadata.setErrorCheckEnd((double) readingsForDevice.get(readingsForDevice.size() - 1)
                                                                         .getAttributes()
                                                                         .get(calibFuelDataField));

            fuelEventMetadata.setEndTime(endPosition.getDeviceTime());
            fuelEventMetadata.setActivityEndPosition(endPosition);

            double fuelChangeVolume = fuelEventMetadata.getEndLevel() - fuelEventMetadata.getStartLevel();
            double errorCheckFuelChange = fuelEventMetadata.getErrorCheckEnd() - fuelEventMetadata.getErrorCheckStart();

            // We've identified the end. Add to the activity window one last time, so we can use the values for
            // checking for an outlier in the determined activity window below.
            appendActivityWindow(readingsForDevice, fuelEventMetadata);

            FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_END] Activity end detected: lookupKey" + activityLookupKey + " at: "
                              + endPosition.getDeviceTime(), deviceId);

            StringBuilder rawFuelValuesInReadings = new StringBuilder();
            StringBuilder timestamps = new StringBuilder();
            readingsForDevice.forEach(p -> {
                rawFuelValuesInReadings.append((double) p.getAttributes()
                                                         .get(calibFuelDataField) + ", ");
                timestamps.append(p.getDeviceTime());
            });
            FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_END] rawFuelValues that crossed threshold for lookupKey: " + activityLookupKey
                              + " - " + rawFuelValuesInReadings, deviceId);
            FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_END] corresponding timestamps: " + timestamps, deviceId);
            FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_END] Midpoint: " + endPosition.getAttributes()
                                                                         .get(calibFuelDataField), deviceId);
            FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_END] Right median: " + endLevel, deviceId);
            FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_END] metadata: " + fuelEventMetadata, deviceId);
            FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_END] fuelChangeVolume: " + fuelChangeVolume, deviceId);
            FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY_END] errorCheckFuelChange: " + errorCheckFuelChange, deviceId);

            Optional<Long> maxCapacity = Context.getPeripheralSensorManager().getFuelTankMaxCapacity(deviceId, fuelSensor.getPeripheralSensorId());

            // If fuel consumption is not as expected, means we have some activity going on.
            if (fuelChangeVolume < 0.0) {
                fuelActivity.setActivityType(FuelActivity.FuelActivityType.FUEL_DRAIN);
                setActivityParameters(fuelActivity, fuelEventMetadata, fuelChangeVolume, consumptionInfo, deviceType);
                checkForMissedOutlier(fuelEventMetadata, fuelActivity, fuelLevelChangeThreshold, fuelSensor);
                deviceFuelEventMetadata.remove(activityLookupKey);
            } else if (fuelChangeVolume > 0.0) {
                fuelActivity.setActivityType(FuelActivity.FuelActivityType.FUEL_FILL);
                setActivityParameters(fuelActivity, fuelEventMetadata, fuelChangeVolume, consumptionInfo, deviceType);
                checkForMissedOutlier(fuelEventMetadata, fuelActivity, fuelLevelChangeThreshold, fuelSensor);
                deviceFuelEventMetadata.remove(activityLookupKey);
            } else {
                // The start may have been detected as a false positive. In any case, remove after we determine the kind
                // of activity.
                FuelSensorDataHandlerHelper.logDebugIfDeviceId("[FUEL_ACTIVITY] Removing event metadata from list to avoid false positives: "
                                  + activityLookupKey, deviceId);
                deviceFuelEventMetadata.remove(activityLookupKey);
            }
        }

        return fuelActivity;
    }

    private static String getActivityLookupKey(String deviceSensorLookupKey, String activityTypeName) {
        return String.format("%s_%s", deviceSensorLookupKey, activityTypeName);
    }

    private static Optional<String> determinePossibleActivityStartType(double diffInMeans,
                                                                       double fillThreshold,
                                                                       double drainThreshold) {
        if (diffInMeans < 0 && Math.abs(diffInMeans) > fillThreshold) {
            return Optional.of(FuelActivityType.FUEL_FILL.name());
        }

        if (diffInMeans > 0 && Math.abs(diffInMeans) > drainThreshold) {
            return Optional.of(FuelActivityType.FUEL_DRAIN.name());
        }

        return Optional.empty();
    }

    private static Optional<String> determinePossibleActivityEndType(double diffInMeans,
                                                                     double fillThreshold,
                                                                     double drainThreshold,
                                                                     Map<String, FuelEventMetadata> deviceFuelEventMetadata,
                                                                     String deviceSensorLookupKey) {

        String drainLookupKey = getActivityLookupKey(deviceSensorLookupKey, FuelActivityType.FUEL_DRAIN.name());
        String fillLookupKey = getActivityLookupKey(deviceSensorLookupKey, FuelActivityType.FUEL_FILL.name());

        if (Math.abs(diffInMeans) < drainThreshold
                && deviceFuelEventMetadata.containsKey(drainLookupKey)) {
            return Optional.of(FuelActivityType.FUEL_DRAIN.name());
        }

        if (Math.abs(diffInMeans) < fillThreshold
                && deviceFuelEventMetadata.containsKey(fillLookupKey)) {
            return Optional.of(FuelActivityType.FUEL_FILL.name());
        }

        return Optional.empty();
    }

    public static Optional<FuelActivity> checkForActivityIfDataLoss(final Position position,
                                                                    final Position lastPosition,
                                                                    final Optional<Long> maxTankMaxVolume,
                                                                    PeripheralSensor fuelSensor) {


        long deviceId = position.getDeviceId();
        DeviceConsumptionInfo consumptionInfo = Context.getDeviceManager().getDeviceConsumptionInfo(position.getDeviceId());
        String deviceType = Context.getDeviceManager().getById(deviceId).getString(TYPE_ATTR_NAME);

        final boolean requiredFieldsPresent =
                FuelConsumptionChecker.checkRequiredFieldsPresent(lastPosition, position, consumptionInfo, fuelSensor);

        if (!requiredFieldsPresent) {
            // Not enough info to process data loss.
            return Optional.empty();
        }

        String calibFuelDataField = fuelSensor.getCalibFuelFieldName();

        String maxCapacityString = maxTankMaxVolume.map(Object::toString).orElse("n/a");
        FuelSensorDataHandlerHelper.logDebugIfDeviceId(String.format("[ActivityChecker] MaxCapacity of sensorId %d on deviceId %d is %s ", fuelSensor.getPeripheralSensorId(), position.getDeviceId(), maxCapacityString), deviceId);

        ExpectedFuelConsumption expectedFuelConsumption =
                FuelConsumptionChecker.getExpectedFuelConsumptionValues(lastPosition, position, maxTankMaxVolume, consumptionInfo);

        double calculatedFuelChangeVolume = position.getDouble(calibFuelDataField)
                - lastPosition.getDouble(calibFuelDataField);

        if (expectedFuelConsumption != null && Math.abs(calculatedFuelChangeVolume) > expectedFuelConsumption.allowedDeviation) {
            if (calculatedFuelChangeVolume < 0.0) {
                boolean isConsumptionExpected =
                        FuelConsumptionChecker.isFuelConsumptionAsExpected(calculatedFuelChangeVolume,
                                                                           expectedFuelConsumption);

                if (isConsumptionExpected) {
                    FuelSensorDataHandlerHelper.logDebugIfDeviceId(String.format(
                            "Determined data loss, but cannot identify fuel event since fuel consumption " +
                                    " is within expected range: %s", expectedFuelConsumption), deviceId);

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
                    return checkNoise(activity, expectedFuelConsumption);
                } else {
                    double possibleFuelFill =
                            expectedFuelConsumption.expectedCurrentFuelConsumed -
                                    Math.abs(calculatedFuelChangeVolume);

                    FuelEventMetadata fuelEventMetadata = new FuelEventMetadata();
                    fuelEventMetadata.setActivityStartPosition(lastPosition);
                    fuelEventMetadata.setActivityEndPosition(position);
                    fuelEventMetadata.setStartTime(lastPosition.getDeviceTime());
                    fuelEventMetadata.setEndTime(position.getDeviceTime());

                    FuelActivity activity = new FuelActivity();
                    activity.setActivityType(FuelActivity.FuelActivityType.PROBABLE_FUEL_FILL);
                    setActivityParameters(activity, fuelEventMetadata, possibleFuelFill, consumptionInfo, deviceType);
                    return Optional.of(activity);
                }
            } else {
                double expectedFuelFill =
                        calculatedFuelChangeVolume + expectedFuelConsumption.expectedCurrentFuelConsumed;

                FuelEventMetadata fuelEventMetadata = new FuelEventMetadata();
                fuelEventMetadata.setActivityStartPosition(lastPosition);
                fuelEventMetadata.setActivityEndPosition(position);
                fuelEventMetadata.setStartTime(lastPosition.getDeviceTime());
                fuelEventMetadata.setEndTime(position.getDeviceTime());

                FuelActivity activity = new FuelActivity();
                activity.setActivityType(FuelActivity.FuelActivityType.EXPECTED_FUEL_FILL);
                setActivityParameters(activity, fuelEventMetadata, expectedFuelFill, consumptionInfo, deviceType);
                return Optional.of(activity);
            }
        }

        return Optional.empty();
    }

    private static Optional<FuelActivity> checkNoise(FuelActivity activity,
                                                     ExpectedFuelConsumption expectedFuelConsumption) {

        return Math.abs(activity.getChangeVolume()) > expectedFuelConsumption.allowedDeviation ? Optional.of(activity) : Optional.empty();
    }

    private static void setActivityParameters(final FuelActivity fuelActivity,
                                              final FuelEventMetadata fuelEventMetadata,
                                              final double fuelChangeVolume,
                                              final DeviceConsumptionInfo consumptionInfo,
                                              final String deviceType) {

        fuelActivity.setActivityStartTime(fuelEventMetadata.getStartTime());
        fuelActivity.setActivityEndTime(fuelEventMetadata.getEndTime());
        fuelActivity.setActivityStartPosition(fuelEventMetadata.getActivityStartPosition());
        fuelActivity.setActivityEndPosition(fuelEventMetadata.getActivityEndPosition());

        double finalVolume = fuelChangeVolume;

        if (StringUtil.isNotBlank(deviceType)
                && deviceType.equals(STATIONARY_TYPE)
                && adjustVolumeFor.contains(fuelActivity.getActivityType().name())) {
            double adjustedVolume = getAdjustedVolume(consumptionInfo, fuelEventMetadata);
            finalVolume += adjustedVolume;
        }

        fuelActivity.setChangeVolume(finalVolume);

    }

    private static double getAdjustedVolume(DeviceConsumptionInfo consumptionInfo,
                                            FuelEventMetadata fuelEventMetadata) {


        Position start = fuelEventMetadata.getActivityStartPosition();
        Position end = fuelEventMetadata.getActivityEndPosition();

        ExpectedFuelConsumption consumption =
                FuelConsumptionChecker.getExpectedHourlyFuelConsumptionValues(start, end, 0, consumptionInfo);

        return consumption.expectedCurrentFuelConsumed;
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
                                         final int end,
                                         PeripheralSensor fuelSensor) {

        String calibFuelField = fuelSensor.getCalibFuelFieldName();
        final List<Double> readings = readingsForDevice.subList(start, end)
                                                       .stream()
                                                       .map(p -> (double) p.getAttributes()
                                                                           .get(calibFuelField))
                                                       .collect(Collectors.toList());

        // Sort them in the ascending order
        readings.sort(Comparator.naturalOrder());

        // pick the middle position
        return readings.get((readings.size() - 1) / 2);
    }

    private static void checkForMissedOutlier(final FuelEventMetadata fuelEventMetadata,
                                              final FuelActivity fuelActivity,
                                              double fuelLevelChangeThreshold,
                                              PeripheralSensor fuelSensor) {

        double minFuelValue = 0.0, maxFuelValue = 0.0;

        FuelActivityType activityType = fuelActivity.getActivityType();
        Position startPositon = fuelActivity.getActivityStartPosition();
        Position endPosition = fuelActivity.getActivityEndPosition();
        String calibFuelField = fuelSensor.getCalibFuelFieldName();

        // Note: these can be the only 2 cases, since this method is not called during data loss checks.
        if ( activityType == FuelActivityType.FUEL_FILL) {
            minFuelValue = startPositon.getDouble(calibFuelField) - fuelLevelChangeThreshold;
            maxFuelValue = endPosition.getDouble(calibFuelField) + fuelLevelChangeThreshold;
        } else if (activityType == FuelActivityType.FUEL_DRAIN) {
            minFuelValue = endPosition.getDouble(calibFuelField) - fuelLevelChangeThreshold;
            maxFuelValue = startPositon.getDouble(calibFuelField) + fuelLevelChangeThreshold;
        }

        for (Position position : fuelEventMetadata.getActivityWindow()) {
            boolean isOutlier = position.getDouble(calibFuelField) < minFuelValue
                                || position.getDouble(calibFuelField) > maxFuelValue;

            position.set(fuelSensor.getFuelOutlierFieldName(), isOutlier);

            // These are amazingly expensive DB write calls. 2nd occurence of this type of writes.
            // Need to make a better async way to do this.
            if (isOutlier) {
                FuelSensorDataHandlerHelper.updatePosition(position);
            }
        }
    }
}

