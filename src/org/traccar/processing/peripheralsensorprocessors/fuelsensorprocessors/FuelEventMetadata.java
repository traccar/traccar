package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import org.traccar.model.Position;

import java.util.Date;
import java.util.List;

/**
 * Created by saurako on 5/6/18.
 */
public class FuelEventMetadata {
    private double startLevel;
    private double endLevel;
    private double errorCheckStart;
    private double errorCheckEnd;
    private Date startTime;
    private Date endTime;
    private Position activityStartPosition;
    private Position activityEndPosition;
    private List<Position> activityWindow;

    public double getStartLevel() {
        return startLevel;
    }

    public void setStartLevel(final double startLevel) {
        this.startLevel = startLevel;
    }

    public double getEndLevel() {
        return endLevel;
    }

    public void setEndLevel(final double endLevel) {
        this.endLevel = endLevel;
    }

    public double getErrorCheckStart() {
        return errorCheckStart;
    }

    public void setErrorCheckStart(final double errorCheckStart) {
        this.errorCheckStart = errorCheckStart;
    }

    public double getErrorCheckEnd() {
        return errorCheckEnd;
    }

    public void setErrorCheckEnd(final double errorCheckEnd) {
        this.errorCheckEnd = errorCheckEnd;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(final Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(final Date endTime) {
        this.endTime = endTime;
    }

    public Position getActivityStartPosition() {
        return activityStartPosition;
    }

    public void setActivityStartPosition(final Position activityStartPosition) {
        this.activityStartPosition = activityStartPosition;
    }

    public Position getActivityEndPosition() {
        return activityEndPosition;
    }

    public void setActivityEndPosition(final Position activityEndPosition) {
        this.activityEndPosition = activityEndPosition;
    }

    public List<Position> getActivityWindow() {
        return activityWindow;
    }

    public void setActivityWindow(final List<Position> activityWindow) {
        this.activityWindow = activityWindow;
    }

    @Override
    public String toString() {
        return " startLevel: " + startLevel
                + " endLevel: " + endLevel
                + " errorCheckStart: " + errorCheckStart
                + " errorCheckEnd: " + errorCheckEnd
                + " startTime: " + startTime
                + " endTime: " + endTime;
    }
}
