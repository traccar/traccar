package org.traccar.api.resource;

import org.traccar.Context;
import org.traccar.api.BaseObjectResource;
import org.traccar.model.FCMPushNotificationType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.Collection;

@Path("fcmnotificationtypes")
public class FCMNotificationTypeResource extends BaseObjectResource<FCMPushNotificationType> {

    public FCMNotificationTypeResource() {
        super(FCMPushNotificationType.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<FCMPushNotificationType> get() throws SQLException {
        return Context.getDataManager().getFCMPushNotificationTypes();
    }
}
