package org.traccar.timedistancematrix;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.traccar.Context;

import javax.json.JsonObject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Entity;

public class OpenRouteServiceTimeDistance extends JsonTimeDistance {
    public OpenRouteServiceTimeDistance(String url, String key) {
        super(url, key);
    }

    @Override
    public JsonObject getTimeDistanceResponse(String url, String key, TimeDistanceRequest timeDistanceRequest)
            throws ClientErrorException {
        if (url == null) {
            url = "https://api.openrouteservice.org/v2/matrix/driving-car";
        }

        String requestBodyString = null;
        try {
            requestBodyString = Context.getObjectMapper().writeValueAsString(timeDistanceRequest);
        } catch (JsonProcessingException e) {
            LOGGER.warn(String.valueOf(e));
        }

        Entity<String> requestBodyEntity = Entity.json(requestBodyString);

        return Context
                .getClient()
                .target(url)
                .request()
                .header("Authorization", key)
                .header("Accept",
                        "application/json; charset=utf-8")
                .post(requestBodyEntity)
                .readEntity(JsonObject.class);

    }
}
