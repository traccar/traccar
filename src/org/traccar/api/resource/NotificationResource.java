/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.api.resource;

import java.sql.SQLException;
import java.util.Collection;

import javax.mail.MessagingException;
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
import org.traccar.model.Event;
import org.traccar.model.Notification;
import org.traccar.notification.NotificationMail;
import org.traccar.notification.NotificationSms;

import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

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
        return Context.getNotificationManager().getAllUserNotifications(userId);
    }

    @POST
    public Response update(Notification entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkUser(getUserId(), entity.getUserId());
        Context.getNotificationManager().updateNotification(entity);
        return Response.ok(entity).build();
    }

    @Path("test")
    @POST
    public Response testMessage() throws MessagingException, RecoverablePduException,
            UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException {
        NotificationMail.sendMailSync(getUserId(), new Event("test", 0), null);
        NotificationSms.sendSmsSync(getUserId(), new Event("test", 0), null);
        return Response.noContent().build();
    }

}
