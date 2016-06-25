package org.traccar.api.resource;

import java.sql.SQLException;
import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.traccar.Context;
import org.traccar.api.BaseResource;
import org.traccar.model.Notification;

@Path("users/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource extends BaseResource {

    @GET
    public Collection<Notification> get(@QueryParam("all") boolean all,
            @QueryParam("userId") long userId) throws SQLException {
        if (all) {
            return Context.getNotificationManager().getAllNotifications();
        }
        if (userId == 0) {
            userId = getUserId();
        }
        Context.getPermissionsManager().checkUser(getUserId(), userId);
        return Context.getNotificationManager().getUserNotifications(userId);
    }

    @POST
    public Response update(Notification entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkUser(getUserId(), entity.getUserId());
        Context.getNotificationManager().updateNotification(entity);
        return Response.ok(entity).build();
    }
}
