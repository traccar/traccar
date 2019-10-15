package org.traccar.directions.matrix;

import org.traccar.Context;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.StringWriter;
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

        JsonArrayBuilder locationsBuilder = Json.createArrayBuilder();
        for (List<Double> location : sourceCoord) {
            JsonArrayBuilder sourceCoordinatesBuilder = Json.createArrayBuilder();
            for (double point : location) {
                sourceCoordinatesBuilder.add(point);
            }
            JsonArray sourceCoordinates = sourceCoordinatesBuilder.build();
            locationsBuilder.add(sourceCoordinates);
        }
        JsonArrayBuilder destinationCoordinatesBuilder = Json.createArrayBuilder();

        for (double point : destCoord) {
            destinationCoordinatesBuilder.add(point);
        }
        JsonArray destinationCoordinates = destinationCoordinatesBuilder.build();

        locationsBuilder.add(destinationCoordinates);
        JsonArray locations = locationsBuilder.build();

        JsonArrayBuilder sourceIndexesBuilder = Json.createArrayBuilder();
        for (int i = 0; i < (locations.size() - 1); i++) {
            sourceIndexesBuilder.add(i);
        }
        JsonArray sourceIndexes = sourceIndexesBuilder.build();

        JsonArray destinationIndexes = Json.createArrayBuilder()
            .add(locations.size() - 1)
            .build();

        JsonObject requestBodyObject = Json.createObjectBuilder()
                .add("locations", locations)
                .add("destinations", destinationIndexes)
                .add("sources", sourceIndexes)
                .add("metrics", Json.createArrayBuilder()
                    .add("distance")
                    .add("duration"))
                .build();


        StringWriter stringWriter = new StringWriter();
        Json.createWriter(stringWriter).write(requestBodyObject);
        String requestBodyString = stringWriter.toString();

        Entity<String> requestBodyEntity = Entity.json(requestBodyString);

        Response request = Context.getClient().target(url)
                .request()
                .header("Authorization", key)
                .header("Accept",
                        "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8")
                .post(requestBodyEntity);

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
