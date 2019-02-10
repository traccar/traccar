package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import org.traccar.model.Position;

import java.util.Date;

public class FuelActivity {

    public FuelActivity() { }

    public FuelActivity(FuelActivityType activityType,
                        double changeVolume,
                        Position activityStartPosition,
                        Position activityEndPosition) {

        this.activityType = activityType;
        this.changeVolume = changeVolume;
        this.activityStartTime = activityStartPosition.getDeviceTime();
        this.activityEndTime = activityEndPosition.getDeviceTime();
        this.activityStartPosition = activityStartPosition;
        this.activityEndPosition = activityEndPosition;
    }

    public enum FuelActivityType {
        NONE,
        FUEL_FILL,
        FUEL_DRAIN,
        PROBABLE_FILL,
        PROBABLE_DRAIN,
        EXPECTED_FILL
    }

    private FuelActivityType activityType = FuelActivityType.NONE;
    private double changeVolume = 0;
    private Date activityStartTime;
    private Date activityEndTime;
    private Position activityStartPosition;
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

    public Position getActivityStartPosition() {
        return activityStartPosition;
    }

    public void setActivityStartPosition(final Position activitystartPosition) {
        this.activityStartPosition = activitystartPosition;
    }

    public Position getActivityEndPosition() {
        return activityEndPosition;
    }

    public void setActivityEndPosition(final Position activityEndPosition) {
        this.activityEndPosition = activityEndPosition;
    }
}
