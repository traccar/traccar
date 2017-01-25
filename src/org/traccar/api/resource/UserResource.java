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

import org.traccar.Context;
import org.traccar.api.BaseResource;
import org.traccar.model.User;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;

@Path("users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource extends BaseResource {

    @GET
    public Collection<User> get(@QueryParam("userId") long userId) throws SQLException {
        if (Context.getPermissionsManager().isAdmin(getUserId())) {
            if (userId != 0) {
                return Context.getPermissionsManager().getUsers(userId);
            } else {
                return Context.getPermissionsManager().getAllUsers();
            }
        } else if (Context.getPermissionsManager().isManager(getUserId())) {
            return Context.getPermissionsManager().getManagedUsers(getUserId());
        } else {
            throw new SecurityException("Admin or manager access required");
        }
    }

    @PermitAll
    @POST
    public Response add(User entity) throws SQLException {
        if (!Context.getPermissionsManager().isAdmin(getUserId())) {
            Context.getPermissionsManager().checkUserUpdate(getUserId(), new User(), entity);
            if (Context.getPermissionsManager().isManager(getUserId())) {
                Context.getPermissionsManager().checkUserLimit(getUserId());
            } else {
                Context.getPermissionsManager().checkRegistration(getUserId());
                entity.setDeviceLimit(Context.getConfig().getInteger("users.defaultDeviceLimit", -1));
                int expirationDays = Context.getConfig().getInteger("users.defaultExpirationDays");
                if (expirationDays > 0) {
                    entity.setExpirationTime(
                        new Date(System.currentTimeMillis() + (long) expirationDays * 24 * 3600 * 1000));
                }
            }
        }
        Context.getPermissionsManager().addUser(entity);
        if (Context.getPermissionsManager().isManager(getUserId())) {
            Context.getDataManager().linkUser(getUserId(), entity.getId());
        }
        Context.getPermissionsManager().refreshUserPermissions();
        if (Context.getNotificationManager() != null) {
            Context.getNotificationManager().refresh();
        }
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @PUT
    public Response update(User entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        User before = Context.getPermissionsManager().getUser(entity.getId());
        Context.getPermissionsManager().checkUser(getUserId(), entity.getId());
        Context.getPermissionsManager().checkUserUpdate(getUserId(), before, entity);
        Context.getPermissionsManager().updateUser(entity);
        if (Context.getNotificationManager() != null) {
            Context.getNotificationManager().refresh();
        }
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkUser(getUserId(), id);
        Context.getPermissionsManager().removeUser(id);
        if (Context.getGeofenceManager() != null) {
            Context.getGeofenceManager().refreshUserGeofences();
        }
        if (Context.getNotificationManager() != null) {
            Context.getNotificationManager().refresh();
        }
        return Response.noContent().build();
    }

}
