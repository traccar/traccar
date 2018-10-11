package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import org.traccar.helper.Log;
import org.traccar.model.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Created by saurako on 8/11/18.
 */
public class FuelSensorDataHandlerHelper {

    public static final double TWO_MULTIPLIER = 2.0;

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

            if ((allowedDeviation / 2) > standardDeviation) {
                standardDeviation = allowedDeviation / 2;
            }
        }

        // 2 standard deviations away
        double lowerBoundOnRawFuelValue = medianRawFuelValue - (TWO_MULTIPLIER * standardDeviation);
        double upperBoundOnRawFuelValue = medianRawFuelValue + (TWO_MULTIPLIER * standardDeviation);

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

    public static boolean isFuelEventDueToDataLoss(final FuelEventMetadata fuelEventMetadata,
                                                   final Optional<Long> maxCapacity) {


        double fuelChangeVolume = fuelEventMetadata.getEndLevel() - fuelEventMetadata.getStartLevel();
        return isDataLoss(fuelEventMetadata.getActivityStartPosition(),
                          fuelEventMetadata.getActivityEndPosition(),
                          fuelChangeVolume,
                          maxCapacity);
    }

    public static boolean isDataLoss(Position startPosition,
                                     Position endPosition,
                                     double calculatedFuelChangeVolume,
                                     Optional<Long> maxCapacity) {

        boolean requiredFieldsPresent = checkRequiredFieldsPresent(startPosition, endPosition);
        if (!requiredFieldsPresent) {
            // Not enough info to process data loss.
            return false;
        }

        ExpectedFuelConsumptionValues expectedFuelConsumptionValues =
                getExpectedFuelConsumptionValues(startPosition,endPosition, maxCapacity);

        boolean dataLoss =
                possibleDataLoss(calculatedFuelChangeVolume,
                                 expectedFuelConsumptionValues);

        if (dataLoss) {
            Log.debug(String.format("Data Loss: Distance covered %f, Exp fuel consumed: %f, actual fuel consumed: %f",
                                    expectedFuelConsumptionValues.maximumDistanceTravelled,
                                    expectedFuelConsumptionValues.expectedCurrentFuelConsumed,
                                    calculatedFuelChangeVolume));
        }

        return dataLoss;
    }

    public static boolean possibleDataLoss(final double calculatedFuelChangeVolume,
                                           final ExpectedFuelConsumptionValues expectedFuelConsumptionValues) {

        return Math.abs(calculatedFuelChangeVolume) > expectedFuelConsumptionValues.allowedDeviation
        && Math.abs(calculatedFuelChangeVolume) <= expectedFuelConsumptionValues.expectedMaxFuelConsumed
        && Math.abs(calculatedFuelChangeVolume) >= expectedFuelConsumptionValues.expectedMinFuelConsumed;
    }

    public static boolean checkRequiredFieldsPresent(Position startPosition, Position endPosition) {
        return startPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)
                && startPosition.getAttributes().containsKey(Position.KEY_ODOMETER)
                && endPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)
                && endPosition.getAttributes().containsKey(Position.KEY_ODOMETER);
    }

    public static ExpectedFuelConsumptionValues getExpectedFuelConsumptionValues(Position startPosition,
                                                                                 Position endPosition,
                                                                                 Optional<Long> maxCapacity) {

        double startTotalGPSDistanceInMeters = (double) startPosition.getAttributes().get(Position.KEY_TOTAL_DISTANCE);
        double endTotalGPSDistanceInMeters = (double) endPosition.getAttributes().get(Position.KEY_TOTAL_DISTANCE);

        int startOdometerInMeters = (int) startPosition.getAttributes().get(Position.KEY_ODOMETER);
        int endOdometerInMeters = (int) endPosition.getAttributes().get(Position.KEY_ODOMETER);

        double differenceTotalDistanceInMeters = endTotalGPSDistanceInMeters - startTotalGPSDistanceInMeters;
        double differenceOdometerInMeters = endOdometerInMeters - startOdometerInMeters;

        // max distance in KM
        double maximumDistanceTravelled = Math.max(differenceTotalDistanceInMeters, differenceOdometerInMeters) / 1000;
        double minimumAverageMileage = 1.5; // KM/L. This has to be a self learning value
        double maximumAverageMileage = 4.0; // KM/L. This has to be a self learning value
        double currentAverageMileage = 2.5; // KM/L. This has to be a self learning value

        double expectedMaxFuelConsumed = maximumDistanceTravelled / minimumAverageMileage;
        double expectedMinFuelConsumed = maximumDistanceTravelled / maximumAverageMileage;
        double expectedCurrentFuelConsumed = maximumDistanceTravelled / currentAverageMileage;

        double allowedDeviation = 1.0; // Default, if maxCapacity is absent.

        if (maxCapacity.isPresent()) {
            allowedDeviation = maxCapacity.get() * 0.01;
        }

        return new ExpectedFuelConsumptionValues(expectedMinFuelConsumed,
                                                 expectedMaxFuelConsumed,
                                                 expectedCurrentFuelConsumed,
                                                 allowedDeviation,
                                                 maximumDistanceTravelled);
    }

    public static class ExpectedFuelConsumptionValues {
        double maximumDistanceTravelled;
        double expectedMinFuelConsumed;
        double expectedMaxFuelConsumed;
        double expectedCurrentFuelConsumed;
        double allowedDeviation;

        public ExpectedFuelConsumptionValues(
                double expectedMinFuelConsumed,
                double expectedMaxFuelConsumed,
                double expectedCurrentFuelConsumed,
                double allowedDeviation,
                double maximumDistanceTravelled) {


            this.expectedCurrentFuelConsumed = expectedCurrentFuelConsumed;
            this.expectedMaxFuelConsumed = expectedMaxFuelConsumed;
            this.expectedMinFuelConsumed = expectedMinFuelConsumed;
            this.allowedDeviation = allowedDeviation;
            this.maximumDistanceTravelled = maximumDistanceTravelled;
        }

        @Override
        public String toString() {
            String maxDist = String.format("Maximum Distance Travelled: %f", maximumDistanceTravelled);
            String minFuel = String.format("Expected Min fuel consumed: %f", expectedMinFuelConsumed);
            String maxFuel = String.format("Expected max fuel consumed: %f", expectedMaxFuelConsumed);
            String currentFuel = String.format("Expected current fuel consumed %f", expectedCurrentFuelConsumed);
            String deviation = String.format("Allowed deviation: %f", allowedDeviation);

            return String.format("%s%n%s%n%s%n%s%n%s%n", maxDist, minFuel, maxFuel, currentFuel, deviation);
        }
    }
}
