package org.traccar.timedistancematrix;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;

import javax.json.JsonObject;
import javax.ws.rs.ClientErrorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class JsonTimeDistance implements TimeDistanceMatrix {

    static final Logger LOGGER = LoggerFactory.getLogger(JsonTimeDistance.class);

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

        JsonObject resultJson = null;
        try {
            resultJson = getTimeDistanceResponse(this.url, this.key, timeDistanceRequest);
        } catch (ClientErrorException e) {
            LOGGER.warn("Time distance network error", e);
        }

        TimeDistanceResponse result = null;
        try {
            result = Context.getObjectMapper().readerFor(TimeDistanceResponse.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(resultJson.toString());
        } catch (IOException e) {
            LOGGER.warn("Time distance response error", e);
        }

        return result;
    }

    JsonObject getTimeDistanceResponse(String url, String key, TimeDistanceRequest timeDistanceRequest)
            throws ClientErrorException {
        return null;
    }
}
