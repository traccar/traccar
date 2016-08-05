package org.traccar.reports.model;

public class GeneralReport {

    private String deviceName;
    public String getDeviceName() {
        return deviceName;
    }
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    private double distance = 0;
    public double getDistance() {
        return distance;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void addDistance(double distance) {
        this.distance += distance;
    }

    private double averageSpeed = 0;
    public double getAverageSpeed() {
        return averageSpeed;
    }
    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    private double maxSpeed = 0;
    public double getMaxSpeed() {
        return maxSpeed;
    }
    public void setMaxSpeed(double maxSpeed) {
        if (maxSpeed > this.maxSpeed) {
            this.maxSpeed = maxSpeed;
        }
    }
}
