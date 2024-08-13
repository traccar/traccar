/*
 * Copyright 2017 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 - 2018 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.api;

import org.traccar.api.security.ServiceAccountUser;
import org.traccar.model.ObjectOperation;
import org.traccar.helper.LogAction;
import org.traccar.model.BaseModel;
import org.traccar.model.Group;
import org.traccar.model.Permission;
import org.traccar.model.User;
import org.traccar.session.ConnectionManager;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

public abstract class BaseObjectResource<T extends BaseModel> extends BaseResource {

    @Inject
    private CacheManager cacheManager;

    @Inject
    private ConnectionManager connectionManager;

    protected final Class<T> baseClass;

    public BaseObjectResource(Class<T> baseClass) {
        this.baseClass = baseClass;
    }

    @Path("{id}")
    @GET
    public Response getSingle(@PathParam("id") long id) throws StorageException {
        permissionsService.checkPermission(baseClass, getUserId(), id);
        T entity = storage.getObject(baseClass, new Request(
                new Columns.All(), new Condition.Equals("id", id)));
        if (entity != null) {
            return Response.ok(entity).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    public Response add(T entity) throws Exception {
        permissionsService.checkEdit(getUserId(), entity, true, false);

        entity.setId(storage.addObject(entity, new Request(new Columns.Exclude("id"))));
        LogAction.create(getUserId(), entity);

        if (getUserId() != ServiceAccountUser.ID) {
            storage.addPermission(new Permission(User.class, getUserId(), baseClass, entity.getId()));
            cacheManager.invalidatePermission(true, User.class, getUserId(), baseClass, entity.getId(), true);
            connectionManager.invalidatePermission(true, User.class, getUserId(), baseClass, entity.getId(), true);
            LogAction.link(getUserId(), User.class, getUserId(), baseClass, entity.getId());
        }

        return Response.ok(entity).build();
    }

    @Path("{id}")
    @PUT
    public Response update(T entity) throws Exception {
        permissionsService.checkPermission(baseClass, getUserId(), entity.getId());

        boolean skipReadonly = false;
        if (entity instanceof User after) {
            User before = storage.getObject(User.class, new Request(
                    new Columns.All(), new Condition.Equals("id", entity.getId())));
            permissionsService.checkUserUpdate(getUserId(), before, (User) entity);
            skipReadonly = permissionsService.getUser(getUserId())
                    .compare(after, "notificationTokens", "termsAccepted");
        } else if (entity instanceof Group group) {
            if (group.getId() == group.getGroupId()) {
                throw new IllegalArgumentException("Cycle in group hierarchy");
            }
        }

        permissionsService.checkEdit(getUserId(), entity, false, skipReadonly);

        storage.updateObject(entity, new Request(
                new Columns.Exclude("id"),
                new Condition.Equals("id", entity.getId())));
        if (entity instanceof User user) {
            if (user.getHashedPassword() != null) {
                storage.updateObject(entity, new Request(
                        new Columns.Include("hashedPassword", "salt"),
                        new Condition.Equals("id", entity.getId())));
            }
        }
        cacheManager.invalidateObject(true, entity.getClass(), entity.getId(), ObjectOperation.UPDATE);
        LogAction.edit(getUserId(), entity);

        return Response.ok(entity).build();
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws Exception {
        permissionsService.checkPermission(baseClass, getUserId(), id);
        permissionsService.checkEdit(getUserId(), baseClass, false, false);

        storage.removeObject(baseClass, new Request(new Condition.Equals("id", id)));
        cacheManager.invalidateObject(true, baseClass, id, ObjectOperation.DELETE);

        LogAction.remove(getUserId(), baseClass, id);

        return Response.noContent().build();
    }

}
