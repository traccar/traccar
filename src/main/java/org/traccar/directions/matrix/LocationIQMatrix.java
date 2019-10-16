package org.traccar.directions.matrix;

import org.traccar.Context;

import javax.json.JsonObject;
import javax.ws.rs.client.Invocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

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

        List<List<Double>> locationsArray = new ArrayList<>(sourceCoord);
        locationsArray.add(destCoord);

        StringJoiner locationsStringJoiner = new StringJoiner(";");
        for (List<Double> coordinates : locationsArray) {
            StringJoiner locationStringJoiner = new StringJoiner(",");
            for (Double point : coordinates) {
                locationStringJoiner.add(Objects.toString(point));
            }
            locationsStringJoiner.add(locationStringJoiner.toString());
        }
        String locations = locationsStringJoiner.toString();

        StringJoiner sourceIndexesJoiner = new StringJoiner(";");
        for (int i = 0; i < (locationsArray.size() - 1); i++) {
            sourceIndexesJoiner.add(Objects.toString(i));
        }
        String sourceIndexes = sourceIndexesJoiner.toString();

        String destinationIndexes = Objects.toString(locationsArray.size() - 1);

        String finalUrl = String.format(
                "%s%s?destinations=%s&sources=%s&annotations=%s&key=%s",
                url,
                locations,
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
