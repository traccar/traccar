package org.traccar.rest;

import org.traccar.helper.Log;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created by niko on 11/27/15.
 */
public class ResponseBuilder {
    public static Response getResponse(JsonStructure json) throws IOException {
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("success", true);
        result.add("data", json);
        return Response.ok().entity(result.build().toString()).build();
    }

    public static Response getResponse(boolean success) throws IOException {
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("success", success);
        return Response.ok().entity(result.build().toString()).build();
    }

    public static Response getResponse(int status, Exception error) {
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("success", false);
        result.add("error", Log.exceptionStack(error));
        return Response.status(status).entity(result.build().toString()).build();
    }
}