package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

public class ExpectedFuelConsumption {
    double expectedMinFuelConsumed;
    double expectedMaxFuelConsumed;
    double expectedCurrentFuelConsumed;
    double allowedDeviation;

    private long maxRunningTimeMillis;
    private double maximumDistanceTravelled;

    public ExpectedFuelConsumption(
            double expectedMinFuelConsumed,
            double expectedMaxFuelConsumed,
            double expectedCurrentFuelConsumed,
            double allowedDeviation) {

        this.expectedCurrentFuelConsumed = expectedCurrentFuelConsumed;
        this.expectedMaxFuelConsumed = expectedMaxFuelConsumed;
        this.expectedMinFuelConsumed = expectedMinFuelConsumed;
        this.allowedDeviation = allowedDeviation;
    }

    public long getMaxRunningTimeMillis() {
        return maxRunningTimeMillis;
    }

    public void setMaxRunningTimeMillis(long maxRunningTimeMillis) {
        this.maxRunningTimeMillis = maxRunningTimeMillis;
    }

    public double getMaximumDistanceTravelled() {
        return maximumDistanceTravelled;
    }

    public void setMaximumDistanceTravelled(double maximumDistanceTravelled) {
        this.maximumDistanceTravelled = maximumDistanceTravelled;
    }
}
