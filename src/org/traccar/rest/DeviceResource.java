package org.traccar.rest;

import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.rest.utils.SessionUtil;
import org.traccar.web.DeviceServlet;
import org.traccar.web.JsonConverter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringReader;

/**
 * Created by niko on 11/28/15.
 */
@Path("device")
@Produces(MediaType.APPLICATION_JSON)
public class DeviceResource {

    @javax.ws.rs.core.Context
    HttpServletRequest req;

    @Path("get")
    @GET
    public Response get() throws Exception {
        if (Boolean.parseBoolean(req.getParameter("all"))) {
            Context.getPermissionsManager().checkAdmin(SessionUtil.getUserId(req));

            return ResponseBuilder.getResponse(JsonConverter.arrayToJson(
                    Context.getDataManager().getAllDevices()));
        } else {
            long userId;
            String userIdParam = req.getParameter("userId");
            if (userIdParam != null) {
                userId = Long.parseLong(userIdParam);
            } else {
                userId = new DeviceServlet().getUserId(req);
            }
            Context.getPermissionsManager().checkUser(SessionUtil.getUserId(req), userId);

            return ResponseBuilder.getResponse(JsonConverter.arrayToJson(
                    Context.getDataManager().getDevices(userId)));
        }
    }

    @Path("add")
    @POST
    public Response add(String deviceJson) throws Exception {
        Device device = JsonConverter.objectFromJson(new StringReader(deviceJson), new Device());
        long userId = SessionUtil.getUserId(req);
        Context.getDataManager().addDevice(device);
        Context.getDataManager().linkDevice(userId, device.getId());
        Context.getPermissionsManager().refresh();

        return ResponseBuilder.getResponse(JsonConverter.objectToJson(device));
    }

    @Path("update")
    @POST
    public Response update(String deviceJson) throws Exception {
        Device device = JsonConverter.objectFromJson(new StringReader(deviceJson), new Device());
        Context.getPermissionsManager().checkDevice(SessionUtil.getUserId(req), device.getId());
        Context.getDataManager().updateDevice(device);

        return ResponseBuilder.getResponse(true);
    }

    @Path("remove")
    @POST
    public Response remove(String deviceJson) throws Exception {
        Device device = JsonConverter.objectFromJson(new StringReader(deviceJson), new Device());
        Context.getPermissionsManager().checkDevice(SessionUtil.getUserId(req), device.getId());
        Context.getDataManager().removeDevice(device);
        Context.getPermissionsManager().refresh();

        return ResponseBuilder.getResponse(true);
    }

    @Path("link")
    @GET
    public Response link() throws Exception {
        Context.getPermissionsManager().checkAdmin(SessionUtil.getUserId(req));
        Context.getDataManager().linkDevice(
                Long.parseLong(req.getParameter("userId")),
                Long.parseLong(req.getParameter("deviceId")));
        Context.getPermissionsManager().refresh();

        return ResponseBuilder.getResponse(true);
    }

    @Path("unlink")
    @GET
    public Response unlink() throws Exception {
        Context.getPermissionsManager().checkAdmin(SessionUtil.getUserId(req));
        Context.getDataManager().unlinkDevice(
                Long.parseLong(req.getParameter("userId")),
                Long.parseLong(req.getParameter("deviceId")));
        Context.getPermissionsManager().refresh();

        return ResponseBuilder.getResponse(true);
    }
}
