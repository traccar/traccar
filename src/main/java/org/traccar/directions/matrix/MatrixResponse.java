package org.traccar.directions.matrix;

import java.util.ArrayList;
import java.util.List;

public class MatrixResponse {

    private List<Double> distances = new ArrayList<>();

    public List<Double> getDistances() {
        return distances;
    }

    public void setDistances(List<Double> distances) {
        this.distances = distances;
    }

    public double getDistance(int index) {
        return this.distances.get(index);
    }

    public void setDistance(double distance) {
        this.distances.add(distance);
    }

    private List<Integer> durations = new ArrayList<>();

    public List<Integer> getDurations() {
        return durations;
    }

    public void setDurations(List<Integer> durations) {
        this.durations = durations;
    }

    public int getDuration(int index) {
        return this.durations.get(index);
    }

    public void setDuration(int duration) {
        this.durations.add(duration);
    }
}
