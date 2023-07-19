/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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

import org.traccar.api.BaseObjectResource;
import org.traccar.config.Config;
import org.traccar.helper.LogAction;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.ManagedUser;
import org.traccar.model.Permission;
import org.traccar.model.User;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Path("users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource extends BaseObjectResource<User> {

    @Inject
    private Config config;

    public UserResource() {
        super(User.class);
    }

    @GET
    public Collection<User> get(@QueryParam("userId") long userId) throws StorageException {
        if (userId > 0) {
            permissionsService.checkUser(getUserId(), userId);
            return storage.getObjects(baseClass, new Request(
                    new Columns.All(),
                    new Condition.Permission(User.class, userId, ManagedUser.class).excludeGroups()));
        } else if (permissionsService.notAdmin(getUserId())) {
            return storage.getObjects(baseClass, new Request(
                    new Columns.All(),
                    new Condition.Permission(User.class, getUserId(), ManagedUser.class).excludeGroups()));
        } else {
            return storage.getObjects(baseClass, new Request(new Columns.All()));
        }
    }

    @Override
    @PermitAll
    @POST
    public Response add(User entity) throws StorageException {
        User currentUser = getUserId() > 0 ? permissionsService.getUser(getUserId()) : null;
        if (currentUser == null || !currentUser.getAdministrator()) {
            permissionsService.checkUserUpdate(getUserId(), new User(), entity);
            if (currentUser != null && currentUser.getUserLimit() != 0) {
                int userLimit = currentUser.getUserLimit();
                if (userLimit > 0) {
                    int userCount = storage.getObjects(baseClass, new Request(
                            new Columns.All(),
                            new Condition.Permission(User.class, getUserId(), ManagedUser.class).excludeGroups()))
                            .size();
                    if (userCount >= userLimit) {
                        throw new SecurityException("Manager user limit reached");
                    }
                }
            } else {
                if (!permissionsService.getServer().getRegistration()) {
                    throw new SecurityException("Registration disabled");
                }
                UserUtil.setUserDefaults(entity, config);
            }
        }

        if (UserUtil.isEmpty(storage)) {
            entity.setAdministrator(true);
        }

        entity.setId(storage.addObject(entity, new Request(new Columns.Exclude("id"))));
        storage.updateObject(entity, new Request(
                new Columns.Include("hashedPassword", "salt"),
                new Condition.Equals("id", entity.getId())));

        LogAction.create(getUserId(), entity);

        if (currentUser != null && currentUser.getUserLimit() != 0) {
            storage.addPermission(new Permission(User.class, getUserId(), ManagedUser.class, entity.getId()));
            LogAction.link(getUserId(), User.class, getUserId(), ManagedUser.class, entity.getId());
        }
        return Response.ok(entity).build();
    }

}
