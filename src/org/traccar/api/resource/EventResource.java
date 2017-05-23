package org.traccar.api.resource;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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

    @GET
    public Event get(@QueryParam("id") long id) throws SQLException {
        Event event = Context.getDataManager().getEvent(id);
        Context.getPermissionsManager().checkDevice(getUserId(), event.getDeviceId());
        if (event.getGeofenceId() != 0) {
            Context.getPermissionsManager().checkGeofence(getUserId(), event.getGeofenceId());
        }
        return event;
    }

}
