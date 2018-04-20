package org.traccar.transforms.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Map;

@JsonClassDescription
public class FuelSensorCalibration {

    @JsonProperty("id")
    private long sensorId;

    @JsonProperty("calibrationDate")
    private Date calibrationDate;

    @JsonProperty("approximationBufferLength")
    private double approximationBufferLength;

    @JsonProperty("fillThreshold")
    private double fillThreshold;

    @JsonProperty("drainThreshold")
    private double drainThreshold;

    @JsonProperty("dutyConsumption")
    private double dutyConsumption;

    @JsonProperty("roughFilterLength")
    private double roughFilterLength;

    @JsonProperty("fineFilterLength")
    private double fineFilterLength;

    @JsonProperty("sensorPointsMap")
    private Map<Long, SensorPointsMap> sensorPointsMap;

    public Map<Long, SensorPointsMap> getSensorPointsMap() {
        return sensorPointsMap;
    }

    public void setSensorPointsMap(Map<Long, SensorPointsMap> sensorPointsMap) {
        this.sensorPointsMap = sensorPointsMap;
    }

    public long getSensorId() {
        return sensorId;
    }

    public void setSensorId(long sensorId) {
        this.sensorId = sensorId;
    }

    public Date getCalibrationDate() {
        return calibrationDate;
    }

    public void setCalibrationDate(Date calibrationDate) {
        this.calibrationDate = calibrationDate;
    }

    public double getApproximationBufferLength() {
        return approximationBufferLength;
    }

    public void setApproximationBufferLength(double approximationBufferLength) {
        this.approximationBufferLength = approximationBufferLength;
    }

    public double getFillThreshold() {
        return fillThreshold;
    }

    public void setFillThreshold(double fillThreshold) {
        this.fillThreshold = fillThreshold;
    }

    public double getDrainThreshold() {
        return drainThreshold;
    }

    public void setDrainThreshold(double drainThreshold) {
        this.drainThreshold = drainThreshold;
    }

    public double getDutyConsumption() {
        return dutyConsumption;
    }

    public void setDutyConsumption(double dutyConsumption) {
        this.dutyConsumption = dutyConsumption;
    }

    public double getRoughFilterLength() {
        return roughFilterLength;
    }

    public void setRoughFilterLength(double roughFilterLength) {
        this.roughFilterLength = roughFilterLength;
    }

    public double getFineFilterLength() {
        return fineFilterLength;
    }

    public void setFineFilterLength(double fineFilterLength) {
        this.fineFilterLength = fineFilterLength;
    }
}
