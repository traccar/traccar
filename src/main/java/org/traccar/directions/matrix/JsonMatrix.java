package org.traccar.directions.matrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class JsonMatrix implements Matrix {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMatrix.class);

    private final String url;
    private final String key;

    JsonMatrix(String url, String key) {
        this.url = url;
        this.key = key;
    }

    @Override
    public MatrixResponse getMatrix(List<List<Double>> sourceCoord, ArrayList<Double> destCoord) {

        List<List<Double>> locations = new ArrayList<>(sourceCoord);
        locations.add(destCoord);

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
