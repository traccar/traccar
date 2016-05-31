package org.traccar.api.resource;

import java.sql.SQLException;
import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.traccar.Context;
import org.traccar.api.BaseResource;
import org.traccar.model.Event;

@Path("events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class EventResource extends BaseResource {

    @Path("{id}")
    @GET
    public Event get(@PathParam("id") long id) throws SQLException {
        Event event = Context.getDataManager().getEvent(id);
        Context.getPermissionsManager().checkDevice(getUserId(), event.getDeviceId());
        return event;
    }

    @GET
    public Collection<Event> get(
            @QueryParam("deviceId") long deviceId, @QueryParam("type") String type,
            @QueryParam("interval") int interval) throws SQLException {
        Context.getPermissionsManager().checkDevice(getUserId(), deviceId);
        return Context.getDataManager().getLastEvents(deviceId, type, interval);
    }
}
