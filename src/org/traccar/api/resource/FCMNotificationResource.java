package org.traccar.api.resource;

import org.traccar.Context;
import org.traccar.api.BaseObjectResource;
import org.traccar.model.FCMNotification;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.Collection;

@Path("fcmnotifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FCMNotificationResource extends BaseObjectResource<FCMNotification> {

    public FCMNotificationResource() {
        super(FCMNotification.class);
    }

    @GET
    public Collection<FCMNotification> get(
            @QueryParam("userId") long userId) throws SQLException {

        return Context.getDataManager().getFCMNotificationsForUser(userId);
    }

    @POST
    public Response add(FCMNotification entity) throws SQLException {

        if (getUserId() == entity.getUserId()) {
            Context.getPermissionsManager().checkDevice(entity.getUserId(),
                    entity.getDeviceId());


            Context.getFcmPushNotificationManager().addItem(entity);


        }
        Context.getFcmPushNotificationManager().refreshFCMNotificationItems();
        return Response.ok(entity).build();
    }
}
