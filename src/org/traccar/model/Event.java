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

    public static final String TYPE_COMMAND_RESULT = "commandResult";

    public static final String TYPE_DEVICE_ONLINE = "deviceOnline";
    public static final String TYPE_DEVICE_OFFLINE = "deviceOffline";

    public static final String TYPE_DEVICE_MOVING = "deviceMoving";
    public static final String TYPE_DEVICE_STOPPED = "deviceStopped";

    public static final String TYPE_DEVICE_OVERSPEED = "deviceOverspeed";

    public static final String TYPE_GEOFENCE_ENTER = "geofenceEnter";
    public static final String TYPE_GEOFENCE_EXIT = "geofenceExit";

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
