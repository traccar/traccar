package org.traccar.reports.model;

import java.util.Date;

public class GeofenceReportItem {

    private long deviceId;

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }
    private long geofenceId;

    public long getGeofenceId() {
        return geofenceId;
    }

    public void setGeofenceId(long geofenceId) {
        this.geofenceId = geofenceId;
    }

    private Date enterTime;

    public Date getEnterTime() {
        return enterTime;
    }

    public void setEnterTime(Date enterTime) {
        this.enterTime = enterTime;
    }
    private Date exitTime;

    public Date getExitTime() {
        return exitTime;
    }

    public void setExitTime(Date exitTime) {
        this.exitTime = exitTime;
    }

    private String enterAddress;

    public String getEnterAddress() {
        return enterAddress;
    }

    public void setEnterAddress(String address) {
        this.enterAddress = address;
    }

    private String exitAddress;

    public String getExitAddress() {
        return exitAddress;
    }

    public void setExitAddress(String address) {
        this.exitAddress = address;
    }


    private long duration;

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
