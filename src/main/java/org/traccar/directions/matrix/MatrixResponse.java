package org.traccar.directions.matrix;

import javax.json.JsonArray;

public class MatrixResponse {

    private JsonArray distances;

    public JsonArray getDistances() {
        return distances;
    }

    public void setDistances(JsonArray distances) {
        this.distances = distances;
    }

    private JsonArray durations;

    public JsonArray getDurations() {
        return durations;
    }

    public void setDurations(JsonArray durations) {
        this.durations = durations;
    }
}
