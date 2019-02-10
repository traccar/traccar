package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import org.traccar.Context;
import org.traccar.helper.DistanceCalculator;
import org.traccar.helper.Log;
import org.traccar.model.Position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class FuelDataLossChecker {

    private static double MINIMUM_AVERAGE_MILEAGE;
    private static double MAXIMUM_AVERAGE_MILEAGE;
    private static double CURRENT_AVERAGE_MILEAGE;

    static {
        MINIMUM_AVERAGE_MILEAGE = Context.getConfig().getDouble("processing.minimumAverageMileage");
        MAXIMUM_AVERAGE_MILEAGE = Context.getConfig().getDouble("processing.maximumAverageMileage");
        CURRENT_AVERAGE_MILEAGE = Context.getConfig().getDouble("processing.currentAverageMileage");
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

        ExpectedFuelConsumption expectedFuelConsumption =
                getExpectedFuelConsumptionValues(startPosition, endPosition, maxCapacity);

        boolean dataLoss =
                isFuelConsumptionAsExpected(calculatedFuelChangeVolume,
                                            expectedFuelConsumption);

        if (dataLoss) {
            Log.debug(String.format("Data Loss: Distance covered %f, Exp fuel consumed: %f, actual fuel consumed: %f",
                                    expectedFuelConsumption.maximumDistanceTravelled,
                                    expectedFuelConsumption.expectedCurrentFuelConsumed,
                                    calculatedFuelChangeVolume));
        }

        return dataLoss;
    }

    public static boolean isFuelConsumptionAsExpected(final double calculatedFuelChangeVolume,
                                                      final ExpectedFuelConsumption expectedFuelConsumption) {

        return Math.abs(calculatedFuelChangeVolume) > expectedFuelConsumption.allowedDeviation
               && Math.abs(calculatedFuelChangeVolume) >= expectedFuelConsumption.expectedMinFuelConsumed
               && Math.abs(calculatedFuelChangeVolume) <= expectedFuelConsumption.expectedMaxFuelConsumed;
    }

    public static boolean checkRequiredFieldsPresent(Position startPosition, Position endPosition) {
        return startPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)
                && startPosition.getAttributes().containsKey(Position.KEY_ODOMETER)
                && endPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)
                && endPosition.getAttributes().containsKey(Position.KEY_ODOMETER);
    }

    public static ExpectedFuelConsumption getExpectedFuelConsumptionValues(Position startPosition,
                                                                           Position endPosition,
                                                                           Optional<Long> maxCapacity) {

        double startTotalGPSDistanceInMeters = (double) startPosition.getAttributes().get(Position.KEY_TOTAL_DISTANCE);
        double endTotalGPSDistanceInMeters = (double) endPosition.getAttributes().get(Position.KEY_TOTAL_DISTANCE);

        int startOdometerInMeters = (int) startPosition.getAttributes().get(Position.KEY_ODOMETER);
        int endOdometerInMeters = (int) endPosition.getAttributes().get(Position.KEY_ODOMETER);

        double differenceTotalDistanceInMeters;
        if (endTotalGPSDistanceInMeters > 0 && startTotalGPSDistanceInMeters > 0) {
            differenceTotalDistanceInMeters = endTotalGPSDistanceInMeters - startTotalGPSDistanceInMeters;
        } else {
            double distance = DistanceCalculator.distance(
                    endPosition.getLatitude(), endPosition.getLongitude(),
                    startPosition.getLatitude(), startPosition.getLongitude());

            differenceTotalDistanceInMeters = BigDecimal.valueOf(distance)
                                                        .setScale(2, RoundingMode.HALF_EVEN).doubleValue();
        }

        double differenceOdometerInMeters = endOdometerInMeters - startOdometerInMeters;

        // max distance in KM
        double maximumDistanceTravelled = Math.max(differenceTotalDistanceInMeters, differenceOdometerInMeters) / 1000;
        double expectedMaxFuelConsumed = maximumDistanceTravelled / MINIMUM_AVERAGE_MILEAGE;
        double expectedMinFuelConsumed = maximumDistanceTravelled / MAXIMUM_AVERAGE_MILEAGE;
        double expectedCurrentFuelConsumed = maximumDistanceTravelled / CURRENT_AVERAGE_MILEAGE;

        double allowedDeviation = 1.0; // Default, if maxCapacity is absent.

        if (maxCapacity.isPresent()) {
            allowedDeviation = maxCapacity.get() * 0.01;
        }

        return new ExpectedFuelConsumption(expectedMinFuelConsumed,
                                           expectedMaxFuelConsumed,
                                           expectedCurrentFuelConsumed,
                                           allowedDeviation,
                                           maximumDistanceTravelled);
    }
}
