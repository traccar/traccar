package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

public class ExpectedFuelConsumption {
    double maximumDistanceTravelled;
    double expectedMinFuelConsumed;
    double expectedMaxFuelConsumed;
    double expectedCurrentFuelConsumed;
    double allowedDeviation;

    public ExpectedFuelConsumption(
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
