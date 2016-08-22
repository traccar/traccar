package org.traccar.reports.model;

import java.util.Date;

public class TripReport extends BaseReport {

    private long startPositionId;
    public long getStartPositionId() {
        return startPositionId;
    }
    public void setStartPositionId(long startPositionId) {
        this.startPositionId = startPositionId;
    }

    private long endPositionId;
    public long getEndPositionId() {
        return endPositionId;
    }
    public void setEndPositionId(long endPositionId) {
        this.endPositionId = endPositionId;
    }

    private Date startTime;
    public Date getStartTime() {
        if (startTime != null) {
            return new Date(startTime.getTime());
        } else {
            return null;
        }
    }
    public void setStartTime(Date startTime) {
        if (startTime != null) {
            this.startTime = new Date(startTime.getTime());
        } else {
            this.startTime = null;
        }
    }

    private String startAddress;
    public String getStartAddress() {
        return startAddress;
    }
    public void setStartAddress(String address) {
        this.startAddress = address;
    }

    private Date endTime;
    public Date getEndTime() {
        if (endTime != null) {
            return new Date(endTime.getTime());
        } else {
            return null;
        }
    }
    public void setEndTime(Date endTime) {
        if (endTime != null) {
            this.endTime = new Date(endTime.getTime());
        } else {
            this.endTime = null;
        }
    }

    private String endAddress;
    public String getEndAddress() {
        return endAddress;
    }
    public void setEndAddress(String address) {
        this.endAddress = address;
    }

    private long duration;
    public long getDuration() {
        return duration;
    }
    public void setDuration(long duration) {
        this.duration = duration;
    }

    private String spentFuel;
    public String getSpentFuel() {
        return spentFuel;
    }
    public void setSpentFuel(String spentFuel) {
        this.spentFuel = spentFuel;
    }

}
