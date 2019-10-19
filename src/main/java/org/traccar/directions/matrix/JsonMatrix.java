package org.traccar.directions.matrix;

import javax.json.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class JsonMatrix implements Matrix {

    private final String url;
    private final String key;

    JsonMatrix(String url, String key) {
        this.url = url;
        this.key = key;
    }

    @Override
    public MatrixResponse getMatrix(List<List<Double>> sourceLocations, ArrayList<Double> destinationLocation) {

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

        MatrixRequest matrixRequest = new MatrixRequest();
        matrixRequest.setLocations(locations);
        matrixRequest.setSources(sourceIndexes);
        matrixRequest.setDestinations(destinationIndexes);
        matrixRequest.setMetrics(metrics);

        JsonObject resultJson = getMatrixResponse(this.url, this.key, matrixRequest);

        MatrixResponse result = new MatrixResponse();

        try {
            result = JsonMatrixObjectMapper
                        .getObjectMapper()
                        .readValue(resultJson.toString(), MatrixResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    JsonObject getMatrixResponse(String url, String key, MatrixRequest matrixRequest) {
        return null;
    }
}
