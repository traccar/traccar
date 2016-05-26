package org.traccar.model;

import java.util.Date;

public class Event extends Message {

    public Event(String type, long deviceId, long positionId) {
        this.setType(type);
        this.setDeviceId(deviceId);
        this.setPositionId(positionId);
        this.eventTime = new Date();
    }

    public Event(String type, long deviceId) {
        this.setType(type);
        this.setDeviceId(deviceId);
        this.eventTime = new Date();
    }

    public Event() {
    }

    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public static final String COMMAND_RESULT = "command-result";

    public static final String DEVICE_ONLINE = "device-online";
    public static final String DEVICE_OFFLINE = "device-offline";

    public static final String DEVICE_MOVING = "device-moving";
    public static final String DEVICE_STOPPED = "device-stopped";

    public static final String DEVICE_OVERSPEED = "device-overspeed";

    public static final String GEOFENCE_ENTER = "geofence-enter";
    public static final String GEOFENCE_EXIT = "geofence-exit";

    private Date eventTime;

    public Date getEventTime() {
        if (eventTime != null) {
            return new Date(eventTime.getTime());
        } else {
            return null;
        }
    }

    public void setEventTime(Date eventTime) {
        if (eventTime != null) {
            this.eventTime = new Date(eventTime.getTime());
        } else {
            this.eventTime = null;
        }
    }

    private long positionId;

    public long getPositionId() {
        return positionId;
    }

    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

}
