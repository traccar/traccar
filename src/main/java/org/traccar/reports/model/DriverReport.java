package org.traccar.reports.model;

import java.util.Date;

public class DriverReport {

    private Date reportDate;

    public Date getReportDate() {
        return reportDate;
    }

    public void setReportDate(Date date) {
        this.reportDate = date;
    }

    private String driverUniqueId;

    public String getDriverUniqueId() {
        return driverUniqueId;
    }

    public void setDriverUniqueId(String driveruniqueid) {
        this.driverUniqueId = driveruniqueid;
    }

    private long deviceId;

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceid) {
        this.deviceId = deviceid;
    }
}
