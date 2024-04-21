/*
 * Copyright 2021 - 2022 Anton Tananaev (anton@traccar.org)
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

import org.traccar.api.BaseResource;
import org.traccar.api.signature.TokenManager;
import org.traccar.mail.MailManager;
import org.traccar.model.User;
import org.traccar.notification.TextTemplateFormatter;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.security.GeneralSecurityException;

@Path("password")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class PasswordResource extends BaseResource {

    @Inject
    private MailManager mailManager;

    @Inject
    private TokenManager tokenManager;

    @Inject
    private TextTemplateFormatter textTemplateFormatter;

    @Path("reset")
    @PermitAll
    @POST
    public Response reset(@FormParam("email") String email)
            throws StorageException, MessagingException, GeneralSecurityException, IOException {

        User user = storage.getObject(User.class, new Request(
                new Columns.All(), new Condition.Equals("email", email)));
        if (user != null) {
            var velocityContext = textTemplateFormatter.prepareContext(permissionsService.getServer(), user);
            var fullMessage = textTemplateFormatter.formatMessage(velocityContext, "passwordReset", "full");
            mailManager.sendMessage(user, true, fullMessage.getSubject(), fullMessage.getBody());
        }
        return Response.ok().build();
    }

    @Path("update")
    @PermitAll
    @POST
    public Response update(
            @FormParam("token") String token, @FormParam("password") String password)
            throws StorageException, GeneralSecurityException, IOException {

        long userId = tokenManager.verifyToken(token).getUserId();
        User user = storage.getObject(User.class, new Request(
                new Columns.All(), new Condition.Equals("id", userId)));
        if (user != null) {
            user.setPassword(password);
            storage.updateObject(user, new Request(
                    new Columns.Include("hashedPassword", "salt"),
                    new Condition.Equals("id", userId)));
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

}
