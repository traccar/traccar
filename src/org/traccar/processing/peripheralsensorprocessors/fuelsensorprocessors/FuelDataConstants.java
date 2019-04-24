package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

public class FuelDataConstants {

    public final static String SENSOR_FUEL_DATA_FIELD = "sensorField";

    // This field on the attributes of a fuel sensor will hold the name of the raw / calib fuel
    // data field we want to set on a position
    public final static String CALIB_FUEL_ON_POSITION_NAME = "calibField";

    // This field on the attributes of a fuel sensor will hold the name of the smoothed fuel
    // data field we want to set on a position
    public final static String SMOOTHED_FUEL_ON_POSITION_NAME = "fuelField";
}
