package org.traccar.directions.matrix;

import org.traccar.Context;

import javax.json.JsonObject;
import javax.ws.rs.client.Invocation;
import java.util.ArrayList;
import java.util.List;

public class LocationIQMatrix extends JsonMatrix {
    public LocationIQMatrix(String url, String key) {
        super(url, key);
    }

    @Override
    public MatrixResponse getMatrixResponse(String url, String key,
                                             List<List<Double>> sourceCoord, ArrayList<Double> destCoord) {
        if (url == null) {
            url = "https://us1.locationiq.com/v1/matrix/driving/";
        }

        String annotations = "distance,duration";
        String destinationCoordinates = "";
        String destinationIndexes = "0";
        String sourceCoordinates = "";
        String sourceIndexes = "";

        int destinationIterator = 1;
        for (double point : destCoord) {
            destinationCoordinates += point;
            if (destinationIterator < destCoord.size()) {
                destinationCoordinates += ",";
            }
            destinationIterator++;
        }

        int sourceIterator = 1;
        for (List<Double> coord : sourceCoord) {
            int coordIterator = 1;
            for (double point : coord) {
                sourceCoordinates += point;
                if (coordIterator < coord.size()) {
                    sourceCoordinates += ",";
                }
                coordIterator++;
            }
            sourceIndexes += sourceIterator;
            if (sourceIterator < sourceCoord.size()) {
                sourceCoordinates += ";";
                sourceIndexes += ";";
            }
            sourceIterator++;
        }

        String finalUrl = String.format(
                "%s%s;%s?destinations=%s&sources=%s&annotations=%s&key=%s",
                url,
                destinationCoordinates, sourceCoordinates,
                destinationIndexes, sourceIndexes,
                annotations,
                key);

        Invocation.Builder request = Context.getClient().target(finalUrl)
                .request();

        JsonObject aResult = request.get(JsonObject.class);

        MatrixResponse result = new MatrixResponse();

        for (int dev = 0; dev < aResult.getJsonArray("distances").size(); dev++) {
            result.setDistance(aResult
                    .getJsonArray("distances")
                    .getJsonArray(dev)
                    .getJsonNumber(0)
                    .doubleValue());
        }

        for (int dev = 0; dev < aResult.getJsonArray("durations").size(); dev++) {
            result.setDuration(aResult
                    .getJsonArray("durations")
                    .getJsonArray(dev)
                    .getJsonNumber(0)
                    .intValue());
        }

        return result;
    }
}
