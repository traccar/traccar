/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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

import org.apache.velocity.VelocityContext;
import org.traccar.Context;
import org.traccar.api.BaseObjectResource;
import org.traccar.config.Keys;
import org.traccar.database.UsersManager;
import org.traccar.helper.Hashing;
import org.traccar.helper.LogAction;
import org.traccar.model.ManagedUser;
import org.traccar.model.User;
import org.traccar.notification.FullMessage;
import org.traccar.notification.TextTemplateFormatter;

import javax.annotation.security.PermitAll;
import javax.mail.MessagingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

@Path("users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource extends BaseObjectResource<User> {

    public UserResource() {
        super(User.class);
    }

    @GET
    public Collection<User> get(@QueryParam("userId") long userId) throws SQLException {
        UsersManager usersManager = Context.getUsersManager();
        Set<Long> result;
        if (Context.getPermissionsManager().getUserAdmin(getUserId())) {
            if (userId != 0) {
                result = usersManager.getUserItems(userId);
            } else {
                result = usersManager.getAllItems();
            }
        } else if (Context.getPermissionsManager().getUserManager(getUserId())) {
            result = usersManager.getManagedItems(getUserId());
        } else {
            throw new SecurityException("Admin or manager access required");
        }
        return usersManager.getItems(result);
    }

    @Override
    @PermitAll
    @POST
    public Response add(User entity) throws SQLException {
        if (!Context.getPermissionsManager().getUserAdmin(getUserId())) {
            Context.getPermissionsManager().checkUserUpdate(getUserId(), new User(), entity);
            if (Context.getPermissionsManager().getUserManager(getUserId())) {
                Context.getPermissionsManager().checkUserLimit(getUserId());
            } else {
                Context.getPermissionsManager().checkRegistration(getUserId());
                entity.setDeviceLimit(Context.getConfig().getInteger(Keys.USERS_DEFAULT_DEVICE_LIMIT));
                int expirationDays = Context.getConfig().getInteger(Keys.USERS_DEFAULT_EXPIRATION_DAYS);
                if (expirationDays > 0) {
                    entity.setExpirationTime(
                        new Date(System.currentTimeMillis() + (long) expirationDays * 24 * 3600 * 1000));
                }
            }
        }
        if (!Context.getConfig().getBoolean(Keys.USERS_EMAIL_VALIDATION_ENABLE)) {
            Context.getUsersManager().addItem(entity);
            LogAction.create(getUserId(), entity);
            if (Context.getPermissionsManager().getUserManager(getUserId())) {
                Context.getDataManager().linkObject(User.class, getUserId(), ManagedUser.class, entity.getId(), true);
                LogAction.link(getUserId(), User.class, getUserId(), ManagedUser.class, entity.getId());
            }
            Context.getUsersManager().refreshUserItems();
        } else {
            VelocityContext velocityContext = TextTemplateFormatter.prepareContext(entity);
            velocityContext.put("signature", Hashing.createHash(entity.getEmail()).getHash());
            velocityContext.put("salt", Hashing.createHash(entity.getEmail()).getSalt());
            FullMessage message = TextTemplateFormatter.formatFullMessage(velocityContext, "validateEmail");
            try {
                Context.getMailManager().sendMessage(entity, message.getSubject(), message.getBody());
            } catch (MessagingException e) {
                return Response.serverError().build();
            }
        }
        return Response.ok(entity).build();
    }

}
