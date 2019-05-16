package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

public class FuelDataConstants {

    public final static String SENSOR_FUEL_DATA_FIELD = "sensorField";

    // This field on the attributes of a fuel sensor will hold the name of the raw / calib fuel
    // data field we want to set on a position
    public final static String CALIB_FUEL_ON_POSITION_NAME = "calibField";

    // This field on the attributes of a fuel sensor will hold the name of the smoothed fuel
    // data field we want to set on a position
    public final static String SMOOTHED_FUEL_ON_POSITION_NAME = "fuelField";

    public final static String OUTLIER_WINDOW_SIZE_FIELD_NAME = "outlier_window";

    public final static String MOVING_AVG_WINDOW_SIZE_FIELD_NAME = "mavg_window";

    public final static String ALERTS_WINDOW_SIZE_FIELD_NAME = "alerts_window";

    public final static String FILL_THRESHOLD_FIELD_NAME = "fill_threshold";

    public final static String DRAIN_THRESHOLD_FIELD_NAME = "drain_threshold";
}
