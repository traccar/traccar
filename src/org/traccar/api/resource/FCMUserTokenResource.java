package org.traccar.api.resource;

import org.traccar.Context;
import org.traccar.api.BaseObjectResource;
import org.traccar.database.FCMUserTokenManager;
import org.traccar.model.FCMUserToken;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

@Path("fcmusertoken")
@Consumes(MediaType.APPLICATION_JSON)
public class FCMUserTokenResource extends BaseObjectResource<FCMUserToken> {

    public FCMUserTokenResource() {
        super(FCMUserToken.class);
    }

    @POST
    public Response add(FCMUserToken entity) throws SQLException {
        if (entity.getUserId() == getUserId()) {
            FCMUserTokenManager fcmUserTokenManager = Context.getFcmUserTokenManager();
            fcmUserTokenManager.addItem(entity);
            fcmUserTokenManager.refreshFCMUserTokens();
        }
        return Response.ok().build();
    }

    @PUT
    public Response update(FCMUserToken entity) throws SQLException {
        if (entity.getUserId() == getUserId()) {
            FCMUserTokenManager fcmUserTokenManager = Context.getFcmUserTokenManager();
            fcmUserTokenManager.updateItem(entity);
            fcmUserTokenManager.refreshFCMUserTokens();
        }
        return Response.ok().build();
    }
}
