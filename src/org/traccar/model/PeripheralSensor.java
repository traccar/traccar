package org.traccar.model;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.traccar.Context;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.FuelConsumptionChecker;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.FuelDataConstants;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PeripheralSensor extends ExtendedModel {

    private long deviceId;
    private long peripheralSensorId;
    private String typeName;
    private String calibrationData;

    private static Map<String, String> gpsProtocolToFuelFieldMap = new ConcurrentHashMap<>();
    private static int minValuesForOutlierDetection;
    private static int minValuesForMovingAvg;
    private static int maxValuesForAlerts;

    static {
        gpsProtocolToFuelFieldMap.put("aquila", Context.getConfig().getString("aquila.fuel_analog"));
        gpsProtocolToFuelFieldMap.put("teltonika", Context.getConfig().getString("teltonika.fuel_analog"));

        minValuesForOutlierDetection =
                Context.getConfig().getInteger("processing.peripheralSensorData.minValuesForOutlierDetection");

        minValuesForMovingAvg = Context.getConfig()
                                       .getInteger("processing.peripheralSensorData.minValuesForMovingAverage");

        maxValuesForAlerts = Context.getConfig()
                                    .getInteger("processing.peripheralSensorData.maxValuesForAlerts");
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public long getPeripheralSensorId() {
        return peripheralSensorId;
    }

    public void setPeripheralSensorId(int peripheralSensorId) {
        this.peripheralSensorId = peripheralSensorId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getCalibrationData() {
        return calibrationData;
    }

    public void setCalibrationData(String calibrationData) {
        this.calibrationData = calibrationData;
    }

    public String getSensorFuelDataField(Position position) {

        String sensorDataField =
                this.getString(FuelDataConstants.SENSOR_FUEL_DATA_FIELD);

        if (sensorDataField == null) {
            // Default fall back to custom analog field.
            sensorDataField = gpsProtocolToFuelFieldMap.getOrDefault(position.getProtocol(), "adc1");
        }

        return sensorDataField;
    }

    public String getCalibFuelFieldName() {
        String calibFuelDataField =
                this.getString(FuelDataConstants.CALIB_FUEL_ON_POSITION_NAME);

        return calibFuelDataField == null?  Position.KEY_CALIBRATED_FUEL_LEVEL : calibFuelDataField;
    }

    public String getFuelDataFieldName() {
        String fuelDataField =
                this.getString(FuelDataConstants.SMOOTHED_FUEL_ON_POSITION_NAME);
        return fuelDataField == null? Position.KEY_FUEL_LEVEL: fuelDataField;
    };

    public String getFuelOutlierFieldName() {
        String defaultName = Position.KEY_FUEL_IS_OUTLIER;

        String fuelDataField =
                this.getString(FuelDataConstants.SMOOTHED_FUEL_ON_POSITION_NAME);

        return fuelDataField == null? defaultName : fuelDataField + "_is_outlier";
    }

    public int getOutlierWindowSize() {

        if (this.getAttributes().containsKey(FuelDataConstants.OUTLIER_WINDOW_SIZE_FIELD_NAME)) {
            return this.getInteger(FuelDataConstants.OUTLIER_WINDOW_SIZE_FIELD_NAME);
        }

        // Default if the field is missing.
        return minValuesForOutlierDetection;
    }

    public int getMovingAvgWindowSize() {
        if (this.getAttributes().containsKey(FuelDataConstants.MOVING_AVG_WINDOW_SIZE_FIELD_NAME)) {
            return  this.getInteger(FuelDataConstants.MOVING_AVG_WINDOW_SIZE_FIELD_NAME);
        }

        // Default if the field is missing.
        return minValuesForMovingAvg;
    }

    public int getEventsWindowSize() {

        if (this.getAttributes().containsKey(FuelDataConstants.ALERTS_WINDOW_SIZE_FIELD_NAME)) {
            return this.getInteger(FuelDataConstants.ALERTS_WINDOW_SIZE_FIELD_NAME);
        }

        // Default if the field is missing
        return maxValuesForAlerts;
    }

    public Optional<Double> getFillThreshold() {

        if (this.getAttributes().containsKey(FuelDataConstants.FILL_THRESHOLD_FIELD_NAME)) {
            return Optional.of(this.getDouble(FuelDataConstants.FILL_THRESHOLD_FIELD_NAME));
        }

        // Should make the caller fall back to activity threshold set on device, or default from the config file
        return Optional.empty();
    }

    public Optional<Double> getDrainThreshold() {
        if (this.getAttributes().containsKey(FuelDataConstants.DRAIN_THRESHOLD_FIELD_NAME)) {
            return Optional.of(this.getDouble(FuelDataConstants.DRAIN_THRESHOLD_FIELD_NAME));
        }

        // Should make the caller fall back to activity threshold set on device, or default from the config file
        return Optional.empty();
    }
}
