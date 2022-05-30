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

import org.traccar.Context;
import org.traccar.database.BaseObjectManager;
import org.traccar.database.ExtendedObjectManager;
import org.traccar.database.SimpleObjectManager;
import org.traccar.helper.LogAction;
import org.traccar.model.BaseModel;
import org.traccar.model.Calendar;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Permission;
import org.traccar.model.User;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

public abstract class BaseObjectResource<T extends BaseModel> extends BaseResource {

    protected final Class<T> baseClass;

    public BaseObjectResource(Class<T> baseClass) {
        this.baseClass = baseClass;
    }

    @Path("{id}")
    @GET
    public Response getSingle(@PathParam("id") long id) throws StorageException {
        permissionsService.checkPermission(baseClass, getUserId(), id);
        T entity = storage.getObject(baseClass, new Request(
                new Columns.All(), new Condition.Equals("id", "id", id)));
        if (entity != null) {
            return Response.ok(entity).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    public Response add(T entity) throws StorageException {
        permissionsService.checkEdit(getUserId(), entity, true);

        BaseObjectManager<T> manager = Context.getManager(baseClass);
        manager.addItem(entity);
        LogAction.create(getUserId(), entity);

        storage.addPermission(new Permission(User.class, getUserId(), baseClass, entity.getId()));
        LogAction.link(getUserId(), User.class, getUserId(), baseClass, entity.getId());

        if (manager instanceof SimpleObjectManager) {
            ((SimpleObjectManager<T>) manager).refreshUserItems();
        } else if (baseClass.equals(Group.class) || baseClass.equals(Device.class)) {
            Context.getPermissionsManager().refreshDeviceAndGroupPermissions();
            Context.getPermissionsManager().refreshAllExtendedPermissions();
        }
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @PUT
    public Response update(T entity) throws StorageException {
        permissionsService.checkEdit(getUserId(), entity, false);
        permissionsService.checkPermission(baseClass, getUserId(), entity.getId());

        Context.getManager(baseClass).updateItem(entity);
        LogAction.edit(getUserId(), entity);

        if (baseClass.equals(Group.class) || baseClass.equals(Device.class)) {
            Context.getPermissionsManager().refreshDeviceAndGroupPermissions();
            Context.getPermissionsManager().refreshAllExtendedPermissions();
        }
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws StorageException {
        permissionsService.checkEdit(getUserId(), baseClass, false);
        permissionsService.checkPermission(baseClass, getUserId(), id);

        BaseObjectManager<T> manager = Context.getManager(baseClass);
        manager.removeItem(id);
        LogAction.remove(getUserId(), baseClass, id);

        if (manager instanceof SimpleObjectManager) {
            ((SimpleObjectManager<T>) manager).refreshUserItems();
            if (manager instanceof ExtendedObjectManager) {
                ((ExtendedObjectManager<T>) manager).refreshExtendedPermissions();
            }
        }
        if (baseClass.equals(Group.class) || baseClass.equals(Device.class) || baseClass.equals(User.class)) {
            if (baseClass.equals(Group.class)) {
                Context.getGroupsManager().refreshItems();
                Context.getDeviceManager().updateDeviceCache(true);
            }
            Context.getPermissionsManager().refreshDeviceAndGroupPermissions();
            if (baseClass.equals(User.class)) {
                Context.getPermissionsManager().refreshAllUsersPermissions();
            } else {
                Context.getPermissionsManager().refreshAllExtendedPermissions();
            }
        } else if (baseClass.equals(Calendar.class)) {
            Context.getGeofenceManager().refreshItems();
            Context.getNotificationManager().refreshItems();
        }
        return Response.noContent().build();
    }

}
