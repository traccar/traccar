package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import org.traccar.model.Position;

import java.util.Date;

/**
 * Created by saurako on 5/6/18.
 */
public class FuelEventMetadata {
    public double startLevel;
    public double endLevel;
    public double errorCheckStart;
    public double errorCheckEnd;
    public Date startTime;
    public Date endTime;
    public Position activityStartPosition;
    public Position activityEndPosition;
}
