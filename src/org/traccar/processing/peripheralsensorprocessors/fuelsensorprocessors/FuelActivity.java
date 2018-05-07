package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import org.traccar.model.Position;

import java.util.Date;

public class FuelActivity {

    public FuelActivity() {}

    public enum FuelActivityType {
        NONE,
        FUEL_FILL,
        FUEL_DRAIN
    }

    private FuelActivityType activityType = FuelActivityType.NONE;
    private double changeVolume = 0;
    private Date activityStartTime;
    private Date activityEndTime;
    private Position activitystartPosition;
    private Position activityEndPosition;

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

    public Position getActivitystartPosition() {
        return activitystartPosition;
    }

    public void setActivitystartPosition(final Position activitystartPosition) {
        this.activitystartPosition = activitystartPosition;
    }

    public Position getActivityEndPosition() {
        return activityEndPosition;
    }

    public void setActivityEndPosition(final Position activityEndPosition) {
        this.activityEndPosition = activityEndPosition;
    }
}
