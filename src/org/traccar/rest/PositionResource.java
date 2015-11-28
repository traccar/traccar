package org.traccar.rest;

import org.traccar.Context;
import org.traccar.model.MiscFormatter;
import org.traccar.model.Position;
import org.traccar.rest.utils.SessionUtil;
import org.traccar.web.JsonConverter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by niko on 11/28/15.
 */
@Path("position")
@Produces(MediaType.APPLICATION_JSON)
public class PositionResource {

    @javax.ws.rs.core.Context
    HttpServletRequest req;

    @Path("get")
    @GET
    public Response get() throws Exception {
        long deviceId = Long.parseLong(req.getParameter("deviceId"));
        Context.getPermissionsManager().checkDevice(SessionUtil.getUserId(req), deviceId);

        return ResponseBuilder.getResponse(JsonConverter.arrayToJson(
                Context.getDataManager().getPositions(
                        SessionUtil.getUserId(req), deviceId,
                        JsonConverter.parseDate(req.getParameter("from")),
                        JsonConverter.parseDate(req.getParameter("to")))));
    }

    @Path("devices")
    @GET
    public Response devices() throws Exception {
        long userId = SessionUtil.getUserId(req);
        Map<String, Object> positions = new HashMap<>();

        for (String deviceIdString : req.getParameterValues("devicesId")) {
            Long deviceId = Long.parseLong(deviceIdString);

            Context.getPermissionsManager().checkDevice(userId, deviceId);

            Position position = Context.getConnectionManager().getLastPosition(deviceId);
            positions.put(deviceId.toString(), position);
        }

        return ResponseBuilder.getResponse(MiscFormatter.toJson(positions));
    }

}
