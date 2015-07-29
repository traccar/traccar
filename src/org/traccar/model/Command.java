package org.traccar.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Command implements Factory {

    @Override
    public Command create() {
        return new Command();
    }

    private long deviceId;
    public long getDeviceId() { return deviceId; }
    public void setDeviceId(long deviceId) { this.deviceId = deviceId; }

    private String type;
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    private Map<String, Object> other = new LinkedHashMap<>();
    public Map<String, Object> getOther() { return other; }
    public void setOther(Map<String, Object> other) { this.other = other; }

    public static final String TYPE_POSITION_SINGLE = "positionSingle";
    public static final String TYPE_POSITION_PERIODIC = "positionPeriodic";
    public static final String TYPE_POSITION_STOP = "positionStop";
    public static final String TYPE_ENGINE_STOP = "engineStop";
    public static final String TYPE_ENGINE_RESUME = "engineResume";
    
    public static final String KEY_UNIQUE_ID = "uniqueId";
    public static final String KEY_FREQUENCY = "frequency";
    public static final String KEY_DEVICE_PASSWORD = "devicePassword";
    

}
