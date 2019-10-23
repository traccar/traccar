package org.traccar.timedistancematrix;

import java.util.ArrayList;
import java.util.List;

public class TimeDistanceRequest {
    private List<List<Double>> locations = new ArrayList<>();

    public List<List<Double>> getLocations() {
        return locations;
    }

    public void setLocations(List<List<Double>> locations) {
        this.locations = locations;
    }

    private List<Integer> sources = new ArrayList<>();

    public List<Integer> getSources() {
        return sources;
    }

    public void setSources(List<Integer> sources) {
        this.sources = sources;
    }

    private List<Integer> destinations = new ArrayList<>();

    public List<Integer> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<Integer> destinations) {
        this.destinations = destinations;
    }

    private List<String> metrics = new ArrayList<>();

    public List<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }
}
