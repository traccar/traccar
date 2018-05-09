package org.traccar.transforms.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SensorPointsMap {
    @JsonProperty("fuelLevel")
    private Long fuelLevel;

    @JsonProperty("pointsPerLitre")
    private Double pointsPerLitre;

    public SensorPointsMap() {
    }

    public Long getFuelLevel() {
        return fuelLevel;
    }

    public void setFuelLevel(Long fuelLevel) {
        this.fuelLevel = fuelLevel;
    }

    public Double getPointsPerLitre() {
        return pointsPerLitre;
    }

    public void setPointsPerLitre(Double pointsPerLitre) {
        this.pointsPerLitre = pointsPerLitre;
    }
}
