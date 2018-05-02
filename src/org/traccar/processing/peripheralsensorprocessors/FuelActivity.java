package org.traccar.processing.peripheralsensorprocessors;

import java.util.Date;

public class FuelActivity {

    public enum FuelActivityType {
        NONE,
        FUEL_FILL,
        FUEL_DRAIN
    }

    private FuelActivityType activityType = FuelActivityType.NONE;
    private double changeVolume = 0;
    private Date activityStartTime;
    private Date activityEndTime;

    public FuelActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(FuelActivityType activityType) {
        this.activityType = activityType;
    }

    public double getChangeVolume() {
        return changeVolume;
    }

    public void setChangeVolume(double amount) {
        this.changeVolume = amount;
    }

    public Date getActivityStartTime() {
        return activityStartTime;
    }

    public void setActivityStartTime(Date activityStartTime) {
        this.activityStartTime = activityStartTime;
    }

    public Date getActivityEndTime() {
        return activityEndTime;
    }

    public void setActivityEndTime(Date activityEndTime) {
        this.activityEndTime = activityEndTime;
    }
}
