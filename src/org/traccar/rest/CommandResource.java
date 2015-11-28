package org.traccar.rest;

import org.traccar.Context;
import org.traccar.model.Command;
import org.traccar.rest.utils.SessionUtil;
import org.traccar.web.CommandServlet;
import org.traccar.web.JsonConverter;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringReader;

/**
 * Created by niko on 11/28/15.
 */
@Path("command")
@Produces(MediaType.APPLICATION_JSON)
public class CommandResource {

    @javax.ws.rs.core.Context
    HttpServletRequest req;

    @Path("send")
    @POST
    public Response send(String commandJson) throws Exception {
        Command command = JsonConverter.objectFromJson(new StringReader(commandJson), new Command());
        Context.getPermissionsManager().checkDevice(SessionUtil.getUserId(req), command.getDeviceId());
        new CommandServlet().getActiveDevice(command.getDeviceId()).sendCommand(command);

        return ResponseBuilder.getResponse(true);
    }

    @Path("raw")
    @POST
    public Response raw(String commandJson) throws Exception {
        JsonObject json = Json.createReader(new StringReader(commandJson)).readObject();
        long deviceId = json.getJsonNumber("deviceId").longValue();
        String command = json.getString("command");
        Context.getPermissionsManager().checkDevice(SessionUtil.getUserId(req), deviceId);
        new CommandServlet().getActiveDevice(deviceId).write(command);

        return ResponseBuilder.getResponse(true);

    }
}
