package org.traccar.directions.timeDistance;

import java.util.ArrayList;
import java.util.List;

public class TimeDistanceResponse {

    private List<List<Double>> distances = new ArrayList<>();

    public List<List<Double>> getDistances() {
        return distances;
    }

    public void setDistances(List<List<Double>> distances) {
        this.distances = distances;
    }

    public double getDistance(int index) {
        return this.distances.get(index).get(0);
    }

    private List<List<Integer>> durations = new ArrayList<>();

    public List<List<Integer>> getDurations() {
        return durations;
    }

    public void setDurations(List<List<Integer>> durations) {
        this.durations = durations;
    }

    public int getDuration(int index) {
        return this.durations.get(index).get(0);
    }
}
