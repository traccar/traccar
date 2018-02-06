package org.traccar.api.resource;

import org.traccar.Context;
import org.traccar.api.BaseObjectResource;
import org.traccar.model.FCMPushNotification;

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

@Path("fcmpushnotifications")
public class FCMNotificationResource extends BaseObjectResource<FCMPushNotification> {

    public FCMNotificationResource() {
        super(FCMPushNotification.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<FCMPushNotification> get(
            @QueryParam("userId") long userId) throws SQLException {

        return Context.getDataManager().getFCMPushNotificationsForUser(userId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(Collection<FCMPushNotification> entityList) throws SQLException {

        for (FCMPushNotification entity : entityList) {
            if (getUserId() == entity.getUserId()) {
                Context.getPermissionsManager().checkDevice(entity.getUserId(),
                        entity.getDeviceId());

                // We need a addItemsList in DataManager to avoid multiple writes.
                Context.getFcmPushNotificationManager().addItem(entity);
            }
        }
        Context.getFcmPushNotificationManager().refreshFCMNotificationsMap();
        return Response.ok(entityList).build();
    }
}
