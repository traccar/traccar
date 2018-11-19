package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import com.google.common.collect.BoundType;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Position;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by saurako on 8/11/18.
 */
public class FuelSensorDataHandlerHelper {

    private final static double MULTIPLIER;

    static {
        MULTIPLIER = Context.getConfig()
                            .getDouble("processing.peripheralSensorData.deviationMultiplier");
    }

    public static List<Position> getRelevantPositionsSubList(TreeMultiset<Position> positionsForSensor,
                                                             Position position,
                                                             int minListSize,
                                                             int currentEventLookBackSeconds) {
        return getRelevantPositionsSubList(positionsForSensor,
                                           position,
                                           minListSize,
                                           false,
                                           currentEventLookBackSeconds);
    }

    public static List<Position> getRelevantPositionsSubList(TreeMultiset<Position> positionsForSensor,
                                                              Position position,
                                                              int minListSize,
                                                              boolean excludeOutliers,
                                                              int currentEventLookBackSeconds) {

        if (positionsForSensor.size() <= minListSize) {
            return positionsForSensor.stream()
                                     .filter(p -> !excludeOutliers || positionIsMarkedOutlier(p))
                                     .collect(Collectors.toList());
        }

        Position fromPosition = new Position();
        fromPosition.setDeviceTime(getAdjustedDate(position.getDeviceTime(),
                                                   Calendar.SECOND,
                                                   -currentEventLookBackSeconds));

        SortedMultiset<Position> positionsSubset =
                positionsForSensor.subMultiset(fromPosition, BoundType.OPEN, position, BoundType.CLOSED);

        if (positionsSubset.size() <= minListSize) {
            Log.debug("[RELEVANT_SUBLIST] sublist is lesser than "
                              + minListSize + " returning " + positionsSubset.size());
            return positionsSubset.stream()
                                  .filter(p -> !excludeOutliers || positionIsMarkedOutlier(p))
                                  .collect(Collectors.toList());
        }

        List<Position> filteredSublistToReturn =
                positionsSubset.stream()
                               .filter(p -> !excludeOutliers || positionIsMarkedOutlier(p))
                               .collect(Collectors.toList());

        int listMaxIndex = filteredSublistToReturn.size();

        if (filteredSublistToReturn.size() <= minListSize) {
            return filteredSublistToReturn;
        }

        List<Position> sublistToReturn = filteredSublistToReturn.subList(listMaxIndex - minListSize, listMaxIndex);

        Log.debug("[RELEVANT_SUBLIST] sublist size: " + sublistToReturn.size());

        return sublistToReturn;
    }

    private static boolean positionIsMarkedOutlier(final Position position) {
        return position.getAttributes().containsKey(Position.KEY_FUEL_IS_OUTLIER)
                && !(boolean) position.getAttributes().get(Position.KEY_FUEL_IS_OUTLIER);
    }

    public static Date getAdjustedDate(Date fromDate, int type, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.add(type, amount);
        return cal.getTime();
    }

    public static boolean isOutlierPresentInSublist(List<Position> rawFuelOutlierSublist,
                                                    int indexOfPositionEvaluated,
                                                    Optional<Long> fuelTankMaxCapacity) {

        // Make a copy so we don't affect the original incoming list esp in the sort below,
        // since the order of the incoming list needs to be preserved to remove / mark the right
        // Position as an outlier.
        List<Position> copyOfRawValues = new ArrayList<>();
        for (Position p : rawFuelOutlierSublist) {
            Position tempPosition = new Position();
            tempPosition.set(Position.KEY_CALIBRATED_FUEL_LEVEL, (double) p.getAttributes().get(Position.KEY_CALIBRATED_FUEL_LEVEL));
            copyOfRawValues.add(tempPosition);
        }

        int listSize = copyOfRawValues.size();

        double sumOfValues =
                copyOfRawValues.stream()
                                     .mapToDouble(p -> (double) p.getAttributes()
                                                                 .get(Position.KEY_CALIBRATED_FUEL_LEVEL))
                                     .sum();

        double mean = sumOfValues / (double) listSize;


        double sumOfSquaredDifferenceOfMean =
                copyOfRawValues.stream()
                                     .mapToDouble(p -> {
                                         double differenceOfMean =
                                                 (double) p.getAttributes()
                                                           .get(Position.KEY_CALIBRATED_FUEL_LEVEL) - mean;
                                         return differenceOfMean * differenceOfMean;
                                     }).sum();



        double rawFuelOfPositionEvaluated =
                (double) copyOfRawValues.get(indexOfPositionEvaluated)
                                              .getAttributes()
                                              .get(Position.KEY_CALIBRATED_FUEL_LEVEL);

        copyOfRawValues.sort(Comparator.comparing(p -> (double) p.getAttributes()
                                                                       .get(Position.KEY_CALIBRATED_FUEL_LEVEL)));

        int midPointOfList = (listSize - 1) / 2;

        double medianRawFuelValue = (double) copyOfRawValues.get(midPointOfList)
                                                                  .getAttributes()
                                                                  .get(Position.KEY_CALIBRATED_FUEL_LEVEL);

        double standardDeviation = Math.sqrt(sumOfSquaredDifferenceOfMean / (double) listSize);

        if (fuelTankMaxCapacity.isPresent()) {
            double allowedDeviation = fuelTankMaxCapacity.get() * 0.01;

            if ((allowedDeviation / MULTIPLIER) > standardDeviation) {
                standardDeviation = allowedDeviation / MULTIPLIER;
            }
        }

        // 2 standard deviations away
        double lowerBoundOnRawFuelValue = medianRawFuelValue - (MULTIPLIER * standardDeviation);
        double upperBoundOnRawFuelValue = medianRawFuelValue + (MULTIPLIER * standardDeviation);

        boolean isOutlier = rawFuelOfPositionEvaluated < lowerBoundOnRawFuelValue
                            || rawFuelOfPositionEvaluated > upperBoundOnRawFuelValue;

        Log.debug("[OUTLIER_STAT] sumOfValues: " + sumOfValues
                  + " mean: " + mean
                  + " sumOfSquaredDifferenceOfMean: " + sumOfSquaredDifferenceOfMean
                  + " rawFuelOfPositionEvaluated: " + rawFuelOfPositionEvaluated
                  + " standardDeviation: " + standardDeviation
                  + " lowerBoundOnRawFuelValue: " + lowerBoundOnRawFuelValue
                  + " upperBoundOnRawFuelValue: " + upperBoundOnRawFuelValue
                  + " isOutlier: " + isOutlier);

        return isOutlier;
    }

    public static Double getAverageFuelValue(List<Position> fuelLevelReadings) {

        // Omit values that are 0s, to avoid skewing the average. This is mostly useful in handling 0s from the
        // analog sensor, which are noise.
        double total = 0.0;
        double size = 0.0;

        for (Position position : fuelLevelReadings) {
            double level = (Double) position.getAttributes().get(Position.KEY_CALIBRATED_FUEL_LEVEL);
            if (level > 0.0 && !Double.isNaN(level)) {
                total += level;
                size += 1.0;
            }
        }

        if (size == 0.0) {
            return 0.0;
        }

        double avg = total / size;

        Log.debug("[FUEL_ACTIVITY_AVERAGES] deviceId: " + fuelLevelReadings.get(0).getDeviceId()
                          + " average: " + avg
                          + " averages list size: " + fuelLevelReadings.size());

        return avg;
    }
}
