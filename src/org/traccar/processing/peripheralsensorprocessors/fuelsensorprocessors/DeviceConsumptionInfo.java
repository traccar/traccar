package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import org.apache.commons.lang.StringUtils;
import org.traccar.Context;
import org.traccar.model.Device;

public class DeviceConsumptionInfo {
    private static final String CONSUMPTION_TYPE_ATTR = "consumption";
    private static final String DEFAULT_CONSUMPTION_TYPE = "odometer";

    private static final String MIN_AVG_CONSUMPTION_RATE_ATTR = "minAvgConsumptionRate";
    private static final String MAX_AVG_CONSUMPTION_RATE_ATTR = "maxAvgConsumptionRate";
    private static final String ASSUMED_AVG_CONSUMPTION_RATE_ATTR = "assumedAvgConsumptionRate";
    private static final String FUEL_ACTIVITY_THRESHOLD_ATTR = "fuelActivityThreshold";
    private static final String TRANSMISSION_FREQUENCY_ATTR = "transmissionFreq";

    private static final double DEFAULT_FUEL_ACTIVITY_THRESHOLD;
    private static final double MIN_AVG_CONSUMPTION_RATE;
    private static final double MAX_AVG_CONSUMPTION_RATE;
    private static final double ASSUMED_AVG_CONSUMPTION_RATE;
    private static final int DEFAULT_TRANSMISSION_FREQUENCY;


    static {
        DEFAULT_FUEL_ACTIVITY_THRESHOLD =
                Context.getConfig().getDouble("processing.peripheralSensorData.fuelLevelChangeThresholdLiters");

        DEFAULT_TRANSMISSION_FREQUENCY = Context.getConfig()
                         .getInteger("processing.peripheralSensorData.messageFrequency");

        MIN_AVG_CONSUMPTION_RATE = Context.getConfig().getDouble("processing.minimumAverageMileage");
        MAX_AVG_CONSUMPTION_RATE = Context.getConfig().getDouble("processing.maximumAverageMileage");
        ASSUMED_AVG_CONSUMPTION_RATE = Context.getConfig().getDouble("processing.currentAverageMileage");
    }

    private String deviceConsumptionType;
    private double minDeviceConsumptionRate;
    private double maxDeviceConsumptionRate;
    private double assumedDeviceConsumptionRate;
    private double fuelActivityThreshold;
    private int transmissionFrequency;

    public DeviceConsumptionInfo() {
        deviceConsumptionType = DEFAULT_CONSUMPTION_TYPE;
        minDeviceConsumptionRate = MIN_AVG_CONSUMPTION_RATE;
        maxDeviceConsumptionRate = MAX_AVG_CONSUMPTION_RATE;
        assumedDeviceConsumptionRate = ASSUMED_AVG_CONSUMPTION_RATE;
        fuelActivityThreshold = DEFAULT_FUEL_ACTIVITY_THRESHOLD;
        transmissionFrequency = DEFAULT_TRANSMISSION_FREQUENCY;
    }

    public DeviceConsumptionInfo(Device device) {
        this();

        if (StringUtils.isNotBlank(device.getString(CONSUMPTION_TYPE_ATTR))) {
            deviceConsumptionType = device.getString(CONSUMPTION_TYPE_ATTR);
        }

        if (device.getAttributes().containsKey(MIN_AVG_CONSUMPTION_RATE_ATTR)) {
            minDeviceConsumptionRate = device.getDouble(MIN_AVG_CONSUMPTION_RATE_ATTR);
        }

        if (device.getAttributes().containsKey(MAX_AVG_CONSUMPTION_RATE_ATTR)) {
            maxDeviceConsumptionRate = device.getDouble(MAX_AVG_CONSUMPTION_RATE_ATTR);
        }

        if (device.getAttributes().containsKey(ASSUMED_AVG_CONSUMPTION_RATE_ATTR)) {
            assumedDeviceConsumptionRate = device.getDouble(ASSUMED_AVG_CONSUMPTION_RATE_ATTR);
        }

        if (device.getAttributes().containsKey(FUEL_ACTIVITY_THRESHOLD_ATTR)) {
            fuelActivityThreshold = device.getDouble(FUEL_ACTIVITY_THRESHOLD_ATTR);
        }

        if (device.getAttributes().containsKey(TRANSMISSION_FREQUENCY_ATTR)) {
            transmissionFrequency = device.getInteger(TRANSMISSION_FREQUENCY_ATTR);
        }
    }

    public String getDeviceConsumptionType() {
        return deviceConsumptionType;
    }

    public double getMinDeviceConsumptionRate() {
        return minDeviceConsumptionRate;
    }

    public double getMaxDeviceConsumptionRate() {
        return maxDeviceConsumptionRate;
    }

    public double getAssumedDeviceConsumptionRate() {
        return assumedDeviceConsumptionRate;
    }

    public double getFuelActivityThreshold() {
        return fuelActivityThreshold;
    }

    public int getTransmissionFrequency() {
        return transmissionFrequency;
    }
}
