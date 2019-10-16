package org.traccar.directions.matrix;

import org.traccar.Context;

import javax.json.JsonObject;
import javax.ws.rs.client.Invocation;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class LocationIQMatrix extends JsonMatrix {
    public LocationIQMatrix(String url, String key) {
        super(url, key);
    }

    @Override
    public JsonObject getMatrixResponse(String url, String key, MatrixRequest matrixRequest) {
        if (url == null) {
            url = "https://us1.locationiq.com/v1/matrix/driving/";
        }
        String metrics = String.join(",", matrixRequest.getMetrics());

        StringJoiner locationsStringJoiner = new StringJoiner(";");
        for (List<Double> coordinates : matrixRequest.getLocations()) {
            StringJoiner locationStringJoiner = new StringJoiner(",");
            for (Double point : coordinates) {
                locationStringJoiner.add(Objects.toString(point));
            }
            locationsStringJoiner.add(locationStringJoiner.toString());
        }
        String locations = locationsStringJoiner.toString();

        StringJoiner sourcesJoiner = new StringJoiner(";");
        for (int source : matrixRequest.getSources()) {
            sourcesJoiner.add(Objects.toString(source));
        }
        String sources = sourcesJoiner.toString();

        String destinations = Objects.toString(matrixRequest.getDestinations().get(0));

        String finalUrl = String.format(
                "%s%s?destinations=%s&sources=%s&annotations=%s&key=%s",
                url,
                locations,
                destinations, sources,
                metrics,
                key);

        Invocation.Builder request = Context.getClient().target(finalUrl)
                .request();

        return request.get(JsonObject.class);
    }
}
