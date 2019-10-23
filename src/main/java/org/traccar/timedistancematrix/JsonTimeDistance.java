package org.traccar.timedistancematrix;

import javax.json.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class JsonTimeDistance implements TimeDistanceMatrix {

    private final String url;
    private final String key;

    JsonTimeDistance(String url, String key) {
        this.url = url;
        this.key = key;
    }

    @Override
    public TimeDistanceResponse getTimeDistanceMatrix(List<List<Double>> sourceLocations,
            ArrayList<Double> destinationLocation) {

        List<List<Double>> locations = new ArrayList<>(sourceLocations);
        locations.add(destinationLocation);

        List<Integer> sourceIndexes = new ArrayList<>();
        for (int i = 0; i < (locations.size() - 1); i++) {
            sourceIndexes.add(i);
        }

        List<Integer> destinationIndexes = new ArrayList<>();
        destinationIndexes.add(locations.size() - 1);

        List<String> metrics = new ArrayList<>();
        metrics.add("distance");
        metrics.add("duration");

        TimeDistanceRequest timeDistanceRequest = new TimeDistanceRequest();
        timeDistanceRequest.setLocations(locations);
        timeDistanceRequest.setSources(sourceIndexes);
        timeDistanceRequest.setDestinations(destinationIndexes);
        timeDistanceRequest.setMetrics(metrics);

        JsonObject resultJson = getTimeDistanceResponse(this.url, this.key, timeDistanceRequest);

        TimeDistanceResponse result = new TimeDistanceResponse();

        try {
            result = JsonTimeDistanceObjectMapper
                        .getObjectMapper()
                        .readValue(resultJson.toString(), TimeDistanceResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    JsonObject getTimeDistanceResponse(String url, String key, TimeDistanceRequest timeDistanceRequest) {
        return null;
    }
}
