package org.traccar.directions.matrix;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.traccar.Context;

import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

public class OpenRouteServiceMatrix extends JsonMatrix {
    public OpenRouteServiceMatrix(String url, String key) {
        super(url, key);
    }

    @Override
    public JsonObject getMatrixResponse(String url, String key, MatrixRequest matrixRequest) {
        if (url == null) {
            url = "https://api.openrouteservice.org/v2/matrix/driving-car";
        }

        String requestBodyString = null;
        try {
            requestBodyString = JsonMatrixObjectMapper.getObjectMapper().writeValueAsString(matrixRequest);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Entity<String> requestBodyEntity = Entity.json(requestBodyString);

        Response request = Context.getClient().target(url)
                .request()
                .header("Authorization", key)
                .header("Accept",
                        "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8")
                .post(requestBodyEntity);

        return request.readEntity(JsonObject.class);

    }
}
