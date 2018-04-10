package org.traccar.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

public class PeripheralSensor extends BaseModel {

    private long deviceId;
    private long peripheralSensorId;
    private String typeName;
    private String calibrationData;

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
}
