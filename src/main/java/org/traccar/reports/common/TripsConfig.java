package org.traccar.reports.common;

import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;

public class TripsConfig {
    public TripsConfig(AttributeUtil.Provider attributeProvider) {
        this(
                AttributeUtil.lookup(attributeProvider, Keys.REPORT_TRIP_MINIMAL_TRIP_DISTANCE),
                AttributeUtil.lookup(attributeProvider, Keys.REPORT_TRIP_MINIMAL_TRIP_DURATION) * 1000,
                AttributeUtil.lookup(attributeProvider, Keys.REPORT_TRIP_MINIMAL_PARKING_DURATION) * 1000,
                AttributeUtil.lookup(attributeProvider, Keys.REPORT_TRIP_MINIMAL_NO_DATA_DURATION) * 1000,
                AttributeUtil.lookup(attributeProvider, Keys.REPORT_TRIP_USE_IGNITION),
                AttributeUtil.lookup(attributeProvider, Keys.REPORT_IGNORE_ODOMETER),
                AttributeUtil.lookup(attributeProvider, Keys.REPORT_IDLE_RPM_THRESHOLD),
                AttributeUtil.lookup(attributeProvider, Keys.REPORT_IDLE_MIN_DURATION),
                AttributeUtil.lookup(attributeProvider, Keys.REPORT_IDLE_MAX_GAP));
    }

    public TripsConfig(
            double minimalTripDistance, long minimalTripDuration, long minimalParkingDuration,
            long minimalNoDataDuration, boolean useIgnition, boolean ignoreOdometer, double idleRpmThreshold,
            long idleMinDuration, long idleMaxGap) {
        this.minimalTripDistance = minimalTripDistance;
        this.minimalTripDuration = minimalTripDuration;
        this.minimalParkingDuration = minimalParkingDuration;
        this.minimalNoDataDuration = minimalNoDataDuration;
        this.useIgnition = useIgnition;
        this.ignoreOdometer = ignoreOdometer;
        this.idleRpmThreshold = idleRpmThreshold;
        this.idleMinDuration = idleMinDuration;
        this.idleMaxGap = idleMaxGap;
    }

    private final double minimalTripDistance;
    public double getMinimalTripDistance() { return minimalTripDistance; }

    private final long minimalTripDuration;
    public long getMinimalTripDuration() { return minimalTripDuration; }

    private final long minimalParkingDuration;
    public long getMinimalParkingDuration() { return minimalParkingDuration; }

    private final long minimalNoDataDuration;
    public long getMinimalNoDataDuration() { return minimalNoDataDuration; }

    private final boolean useIgnition;
    public boolean getUseIgnition() { return useIgnition; }

    private final boolean ignoreOdometer;
    public boolean getIgnoreOdometer() { return ignoreOdometer; }

    private final double idleRpmThreshold;
    public double getIdleRpmThreshold() { return idleRpmThreshold; }

    private final long idleMinDuration;
    public long getIdleMinDuration() { return idleMinDuration; }

    private final long idleMaxGap;
    public long getIdleMaxGap() { return idleMaxGap; }
}