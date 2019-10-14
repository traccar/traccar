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

        int destinationsLocationIq = 0;
        String annotations = "distance,duration";

        String coordLocationIq = "";
        String sourcesLocationIq = "";

        int i = 1;
        for (List<Double> coord : sourceCoord) {
            int j = 1;
            for (double point : coord) {
                coordLocationIq += point;
                if (j < coord.size()) {
                    coordLocationIq += ",";
                }
                j++;
            }
            sourcesLocationIq += i;
            if (i < sourceCoord.size()) {
                coordLocationIq += ";";
                sourcesLocationIq += ";";
            }
            i++;
        }

        String destLocationIq = "";
        int j = 1;
        for (double point : destCoord) {
            destLocationIq += point;
            if (j < destCoord.size()) {
                destLocationIq += ",";
            }
            j++;
        }

        String finalUrl = String.format(
                "%s%s;%s?destinations=%d&sources=%s&annotations=%s&key=%s",
                url, destLocationIq, coordLocationIq, destinationsLocationIq, sourcesLocationIq, annotations, key);

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
