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
import org.traccar.database.MailManager;
import org.traccar.model.User;
import org.traccar.notification.TextTemplateFormatter;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("password")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class PasswordResource extends BaseResource {

    private static final String PASSWORD_RESET_TOKEN = "passwordToken";

    @Inject
    private MailManager mailManager;

    @Inject
    private TextTemplateFormatter textTemplateFormatter;

    @Path("reset")
    @PermitAll
    @POST
    public Response reset(@FormParam("email") String email) throws StorageException, MessagingException {
        User user = storage.getObject(User.class, new Request(
                new Columns.All(), new Condition.Equals("email", "email", email)));
        if (user != null) {
            String token = UUID.randomUUID().toString().replaceAll("-", "");
            user.set(PASSWORD_RESET_TOKEN, token);
            storage.updateObject(user, new Request(new Columns.Exclude("id"), new Condition.Equals("id", "id")));

            var velocityContext = textTemplateFormatter.prepareContext(permissionsService.getServer(), user);
            velocityContext.put("token", token);
            var fullMessage = textTemplateFormatter.formatMessage(velocityContext, "passwordReset", "full");
            mailManager.sendMessage(user, fullMessage.getSubject(), fullMessage.getBody());
        }
        return Response.ok().build();
    }

    @Path("update")
    @PermitAll
    @POST
    public Response update(
            @FormParam("token") String token, @FormParam("password") String password) throws StorageException {
        User user = storage.getObjects(User.class, new Request(new Columns.All())).stream()
                .filter(it -> token.equals(it.getString(PASSWORD_RESET_TOKEN)))
                .findFirst().orElse(null);
        if (user != null) {
            user.getAttributes().remove(PASSWORD_RESET_TOKEN);
            user.setPassword(password);
            storage.updateObject(user, new Request(new Columns.Exclude("id"), new Condition.Equals("id", "id")));
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

}
