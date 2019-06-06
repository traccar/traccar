package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import com.google.common.collect.BoundType;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.PeripheralSensor;
import org.traccar.model.Position;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by saurako on 8/11/18.
 */
public class FuelSensorDataHandlerHelper {

    private static int logForDeviceId;

    static {
        logForDeviceId = Context.getConfig().getInteger("processing.peripheralSensorData.logForDeviceId");
    }

    private final static double MULTIPLIER;

    static {
        MULTIPLIER = Context.getConfig()
                            .getDouble("processing.peripheralSensorData.deviationMultiplier");
    }

    public static List<Position> getRelevantPositionsSubList(TreeMultiset<Position> positionsForSensor,
                                                             Position position,
                                                             int minListSize,
                                                             String sensorOutlierFieldName,
                                                             int currentEventLookBackSeconds) {
        return getRelevantPositionsSubList(positionsForSensor,
                                           position,
                                           minListSize,
                                           sensorOutlierFieldName,
                                           false,
                                           currentEventLookBackSeconds);
    }

    public static List<Position> getRelevantPositionsSubList(TreeMultiset<Position> positionsForSensor,
                                                             Position position,
                                                             int minListSize,
                                                             String sensorOutlierFieldName,
                                                             boolean excludeOutliers,
                                                             int currentEventLookBackSeconds) {
        return getRelevantPositionsSubList(positionsForSensor,
                                           position,
                                           minListSize,
                                           sensorOutlierFieldName,
                                           excludeOutliers,
                                           currentEventLookBackSeconds,
                                           0);

    }

    public static List<Position> getRelevantPositionsSubList(TreeMultiset<Position> positionsForSensor,
                                                             Position position,
                                                             int minListSize,
                                                             String sensorOutlierFieldName,
                                                             boolean excludeOutliers,
                                                             int currentEventLookBackSeconds,
                                                             long previousWindowStart) {

        if (positionsForSensor.size() < minListSize) {
            return positionsForSensor.stream()
                                     .filter(p -> !excludeOutliers || positionIsMarkedOutlier(p, sensorOutlierFieldName))
                                     .collect(Collectors.toList());
        }

        Position fromPosition = new Position();
        fromPosition.setDeviceTime(getAdjustedDate(position.getDeviceTime(),
                                                   Calendar.SECOND,
                                                   -currentEventLookBackSeconds));

        SortedMultiset<Position> positionsSubset =
                positionsForSensor.subMultiset(fromPosition, BoundType.OPEN, position, BoundType.CLOSED);

        long deviceId = position.getDeviceId();

        if (positionsSubset.size() < minListSize) {
            logDebugIfDeviceId("[RELEVANT_SUBLIST] sublist is lesser than "
                              + minListSize + " returning " + positionsSubset.size(), deviceId);
            return positionsSubset.stream()
                                  .filter(p -> p.getDeviceTime().getTime() > previousWindowStart)
                                  .filter(p -> !excludeOutliers || positionIsMarkedOutlier(p, sensorOutlierFieldName))
                                  .collect(Collectors.toList());
        }

        List<Position> filteredSublistToReturn =
                positionsSubset.stream()
                               .filter(p -> p.getDeviceTime().getTime() > previousWindowStart)
                               .filter(p -> !excludeOutliers || positionIsMarkedOutlier(p, sensorOutlierFieldName))
                               .collect(Collectors.toList());

        int listMaxIndex = filteredSublistToReturn.size();

        if (filteredSublistToReturn.size() < minListSize) {
            return filteredSublistToReturn;
        }

        List<Position> sublistToReturn = filteredSublistToReturn.subList(listMaxIndex - minListSize, listMaxIndex);

        logDebugIfDeviceId("[RELEVANT_SUBLIST] sublist size: " + sublistToReturn.size(), deviceId);

        return sublistToReturn;
    }

    private static boolean positionIsMarkedOutlier(final Position position, final String sensorOutlierFieldName) {
        return position.getAttributes().containsKey(sensorOutlierFieldName)
                && !(boolean) position.getAttributes().get(sensorOutlierFieldName);
    }

    public static Date getAdjustedDate(Date fromDate, int type, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.add(type, amount);
        return cal.getTime();
    }

    public static boolean isOutlierPresentInSublist(List<Position> rawFuelOutlierSublist,
                                                    int indexOfPositionEvaluated,
                                                    Optional<Long> fuelTankMaxCapacity,
                                                    PeripheralSensor fuelSensor) {

        // Make a copy so we don't affect the original incoming list esp in the sort below,
        // since the order of the incoming list needs to be preserved to remove / mark the right
        // Position as an outlier.
        String calibFueldField = fuelSensor.getCalibFuelFieldName();
        List<Position> copyOfRawValues = new ArrayList<>();
        for (Position p : rawFuelOutlierSublist) {
            Position tempPosition = new Position();
            tempPosition.set(calibFueldField, (double) p.getAttributes().get(calibFueldField));
            tempPosition.setDeviceTime(p.getDeviceTime());
            copyOfRawValues.add(tempPosition);
        }

        int listSize = copyOfRawValues.size();

        double sumOfValues =
                copyOfRawValues.stream()
                               .mapToDouble(p -> (double) p.getAttributes()
                                                           .get(calibFueldField))
                               .sum();

        double mean = sumOfValues / (double) listSize;


        double sumOfSquaredDifferenceOfMean =
                copyOfRawValues.stream()
                               .mapToDouble(p -> {
                                   double differenceOfMean =
                                           (double) p.getAttributes()
                                                     .get(calibFueldField) - mean;
                                   return differenceOfMean * differenceOfMean;
                               }).sum();


        double rawFuelOfPositionEvaluated =
                (double) copyOfRawValues.get(indexOfPositionEvaluated)
                                        .getAttributes()
                                        .get(calibFueldField);

        copyOfRawValues.sort(Comparator.comparing(p -> (double) p.getAttributes()
                                                                 .get(calibFueldField)));

        int midPointOfList = (listSize - 1) / 2;

        double medianRawFuelValue = (double) copyOfRawValues.get(midPointOfList)
                                                            .getAttributes()
                                                            .get(calibFueldField);

        double standardDeviation = Math.sqrt(sumOfSquaredDifferenceOfMean / (double) listSize);

        if (fuelTankMaxCapacity.isPresent()) {
            double allowedDeviation = fuelTankMaxCapacity.get() * 0.01;

            if (allowedDeviation > 5.0) {
                allowedDeviation = 5.0;
            }

            if ((allowedDeviation / MULTIPLIER) > standardDeviation) {
                standardDeviation = allowedDeviation / MULTIPLIER;
            }
        }

        // 2 standard deviations away
        double lowerBoundOnRawFuelValue = medianRawFuelValue - (MULTIPLIER * standardDeviation);
        double upperBoundOnRawFuelValue = medianRawFuelValue + (MULTIPLIER * standardDeviation);

        boolean isOutlier = rawFuelOfPositionEvaluated < lowerBoundOnRawFuelValue
                || rawFuelOfPositionEvaluated > upperBoundOnRawFuelValue;

        long deviceId = rawFuelOutlierSublist.get(0).getDeviceId();
        logDebugIfDeviceId("[OUTLIER_STAT] sumOfValues: " + sumOfValues
                  + " mean: " + mean
                  + " sumOfSquaredDifferenceOfMean: " + sumOfSquaredDifferenceOfMean
                  + " deviceTime of position evaluated: " + (copyOfRawValues.get(indexOfPositionEvaluated).getDeviceTime())
                  + " rawFuelOfPositionEvaluated: " + rawFuelOfPositionEvaluated
                  + " standardDeviation: " + standardDeviation
                  + " lowerBoundOnRawFuelValue: " + lowerBoundOnRawFuelValue
                  + " upperBoundOnRawFuelValue: " + upperBoundOnRawFuelValue
                  + " isOutlier: " + isOutlier, deviceId);

        return isOutlier;
    }

    public static Double getAverageFuelValue(List<Position> fuelLevelReadings, PeripheralSensor fuelSensor) {

        // Omit values that are 0s, to avoid skewing the average. This is mostly useful in handling 0s from the
        // analog sensor, which are noise.
        double total = 0.0;
        double size = 0.0;

        for (Position position : fuelLevelReadings) {
            double level = (Double) position.getAttributes().get(fuelSensor.getCalibFuelFieldName());
            if (level > 0.0 && !Double.isNaN(level)) {
                total += level;
                size += 1.0;
            }
        }

        if (size == 0.0) {
            return 0.0;
        }

        double avg = total / size;
        long deviceId = fuelLevelReadings.get(0).getDeviceId();

        logDebugIfDeviceId("[FUEL_ACTIVITY_AVERAGES] deviceId: " + deviceId
                          + " average: " + avg
                          + " averages list size: " + fuelLevelReadings.size(), deviceId);

        return avg;
    }

    public static void updatePosition(final Position outlierPosition) {
        try {
            Context.getDataManager().updateObject(outlierPosition);
        } catch (SQLException e) {
            logDebugIfDeviceId("Exception while updating outlier position with id: " + outlierPosition.getId(), outlierPosition.getDeviceId());
        }
    }

    public static void logDebugIfDeviceId(String logMessage, long deviceId) {
        if (deviceId == logForDeviceId) {
            Log.debug(logMessage);
        }
    }
}
