package org.traccar.directions.matrix;

import org.traccar.Context;

import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class OpenRouteServiceMatrix extends JsonMatrix {
    public OpenRouteServiceMatrix(String url, String key) {
        super(url, key);
    }

    @Override
    public MatrixResponse getMatrixResponse(String url, String key,
                                             List<List<Double>> sourceCoord, ArrayList<Double> destCoord) {
        if (url == null) {
            url = "https://api.openrouteservice.org/v2/matrix/driving-car";
        }

        List<List<Double>> coordOpenRoute = sourceCoord;
        coordOpenRoute.add(destCoord);
        int destinationsOpenRoute = coordOpenRoute.size() - 1;

        List<Integer> sourcesOpenRoute = new ArrayList<>();
        int i = 0;
        for (i = 0; i < (coordOpenRoute.size() - 1); i++) {
            sourcesOpenRoute.add(i);
        }

        String payload1 = String.format("{\"locations\":%s,", coordOpenRoute);

        String payload2 = String.format("\"sources\":%s,\"destinations\":[%d],",
                sourcesOpenRoute, destinationsOpenRoute);

        String payload3 = "\"metrics\":[\"distance\",\"duration\"]}";

        Entity<String> payload = Entity.json(payload1 + payload2 + payload3);

        Response request = Context.getClient().target(url)
                .request()
                .header("Authorization", key)
                .header("Accept",
                        "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8")
                .post(payload);

        JsonObject aResult = request.readEntity(JsonObject.class);

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
