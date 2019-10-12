package org.traccar.directions.matrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        return getMatrixResponse(this.url, this.key, sourceCoord, destCoord);
    }

    MatrixResponse getMatrixResponse(String url, String key,
                                             List<List<Double>> sourceCoord, ArrayList<Double> destCoord) {
        return null;
    }
}
