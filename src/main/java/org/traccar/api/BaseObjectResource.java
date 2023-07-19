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

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

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
    public Response add(T entity) throws StorageException {
        permissionsService.checkEdit(getUserId(), entity, true);

        entity.setId(storage.addObject(entity, new Request(new Columns.Exclude("id"))));
        LogAction.create(getUserId(), entity);

        if (getUserId() != ServiceAccountUser.ID) {
            storage.addPermission(new Permission(User.class, getUserId(), baseClass, entity.getId()));
            cacheManager.invalidatePermission(true, User.class, getUserId(), baseClass, entity.getId());
            connectionManager.invalidatePermission(true, User.class, getUserId(), baseClass, entity.getId());
            LogAction.link(getUserId(), User.class, getUserId(), baseClass, entity.getId());
        }

        return Response.ok(entity).build();
    }

    @Path("{id}")
    @PUT
    public Response update(T entity) throws StorageException {
        permissionsService.checkEdit(getUserId(), entity, false);
        permissionsService.checkPermission(baseClass, getUserId(), entity.getId());

        if (entity instanceof User) {
            User before = storage.getObject(User.class, new Request(
                    new Columns.All(), new Condition.Equals("id", entity.getId())));
            permissionsService.checkUserUpdate(getUserId(), before, (User) entity);
        } else if (entity instanceof Group) {
            Group group = (Group) entity;
            if (group.getId() == group.getGroupId()) {
                throw new IllegalArgumentException("Cycle in group hierarchy");
            }
        }

        storage.updateObject(entity, new Request(
                new Columns.Exclude("id"),
                new Condition.Equals("id", entity.getId())));
        if (entity instanceof User) {
            User user = (User) entity;
            if (user.getHashedPassword() != null) {
                storage.updateObject(entity, new Request(
                        new Columns.Include("hashedPassword", "salt"),
                        new Condition.Equals("id", entity.getId())));
            }
        }
        cacheManager.updateOrInvalidate(true, entity);
        LogAction.edit(getUserId(), entity);

        return Response.ok(entity).build();
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws StorageException {
        permissionsService.checkEdit(getUserId(), baseClass, false);
        permissionsService.checkPermission(baseClass, getUserId(), id);

        storage.removeObject(baseClass, new Request(new Condition.Equals("id", id)));
        cacheManager.invalidate(baseClass, id);

        LogAction.remove(getUserId(), baseClass, id);

        return Response.noContent().build();
    }

}
