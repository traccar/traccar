package org.traccar.model;

import org.eclipse.jetty.util.StringUtil;
import org.traccar.Context;
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

    public void setDeviceId(Number deviceId) {
        this.deviceId = deviceId.longValue();
    }

    public long getPeripheralSensorId() {
        return peripheralSensorId;
    }

    public void setPeripheralSensorId(Number peripheralSensorId) {
        this.peripheralSensorId = peripheralSensorId.longValue();
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

        return StringUtil.isBlank(calibFuelDataField)?  Position.KEY_CALIBRATED_FUEL_LEVEL : calibFuelDataField;
    }

    public String getFuelDataFieldName() {
        String fuelDataField =
                this.getString(FuelDataConstants.SMOOTHED_FUEL_ON_POSITION_NAME);
        return StringUtil.isBlank(fuelDataField)? Position.KEY_FUEL_LEVEL: fuelDataField;
    };

    public String getFuelOutlierFieldName() {
        String defaultName = Position.KEY_FUEL_IS_OUTLIER;

        String fuelDataField =
                this.getString(FuelDataConstants.SMOOTHED_FUEL_ON_POSITION_NAME);

        return StringUtil.isBlank(fuelDataField)? defaultName : fuelDataField + "_is_outlier";
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

    public Optional<Double> getIgnOffDrainThreshold() {
        if (this.getAttributes().containsKey(FuelDataConstants.IGN_OFF_DRAIN_THRESHOLD_FIELD_NAME)) {
            return Optional.of(this.getDouble(FuelDataConstants.IGN_OFF_DRAIN_THRESHOLD_FIELD_NAME));
        }

        // Should make the caller fall back to activity threshold set on device, or default from the config file
        return Optional.empty();
    }

    public Optional<Double> getIgnOnDrainThreshold() {
        if (this.getAttributes().containsKey(FuelDataConstants.IGN_ON_DRAIN_THRESHOLD_FIELD_NAME)) {
            return Optional.of(this.getDouble(FuelDataConstants.IGN_ON_DRAIN_THRESHOLD_FIELD_NAME));
        }

        // Should make the caller fall back to activity threshold set on device, or default from the config file
        return Optional.empty();
    }

    public PeripheralSensor cloneMe(String finalCalibFieldName) {
        PeripheralSensor peripheralSensor = new PeripheralSensor();
        peripheralSensor.setDeviceId(this.getDeviceId());
        peripheralSensor.setPeripheralSensorId(this.getPeripheralSensorId());
        peripheralSensor.setTypeName(this.getTypeName());
        peripheralSensor.setCalibrationData(this.getCalibrationData());

        peripheralSensor.set(FuelDataConstants.CALIB_FUEL_ON_POSITION_NAME, finalCalibFieldName);
        peripheralSensor.set(FuelDataConstants.OUTLIER_WINDOW_SIZE_FIELD_NAME, this.getOutlierWindowSize());
        peripheralSensor.set(FuelDataConstants.MOVING_AVG_WINDOW_SIZE_FIELD_NAME, this.getMovingAvgWindowSize());
        peripheralSensor.set(FuelDataConstants.ALERTS_WINDOW_SIZE_FIELD_NAME, this.getEventsWindowSize());
        this.getFillThreshold().ifPresent(ft -> peripheralSensor.set(FuelDataConstants.FILL_THRESHOLD_FIELD_NAME, ft));
        this.getDrainThreshold().ifPresent(dt -> peripheralSensor.set(FuelDataConstants.DRAIN_THRESHOLD_FIELD_NAME, dt));
        this.getIgnOffDrainThreshold().ifPresent(igodt -> peripheralSensor.set(FuelDataConstants.IGN_OFF_DRAIN_THRESHOLD_FIELD_NAME, igodt));
        this.getIgnOffDrainThreshold().ifPresent(igodt -> peripheralSensor.set(FuelDataConstants.IGN_ON_DRAIN_THRESHOLD_FIELD_NAME, igodt));

        return peripheralSensor;
    }
}
