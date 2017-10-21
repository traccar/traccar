/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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

import java.sql.SQLException;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.traccar.Context;
import org.traccar.database.BaseObjectManager;
import org.traccar.database.ExtendedObjectManager;
import org.traccar.database.ManagableObjects;
import org.traccar.database.SimpleObjectManager;
import org.traccar.model.BaseModel;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.User;

public abstract class BaseObjectResource<T extends BaseModel> extends BaseResource {

    private Class<T> baseClass;

    public BaseObjectResource(Class<T> baseClass) {
        this.baseClass = baseClass;
    }

    protected final Class<T> getBaseClass() {
        return baseClass;
    }

    protected final Set<Long> getSimpleManagerItems(BaseObjectManager<T> manager, boolean all,  long userId) {
        Set<Long> result = null;
        if (all) {
            if (Context.getPermissionsManager().getUserAdmin(getUserId())) {
                result = manager.getAllItems();
            } else {
                Context.getPermissionsManager().checkManager(getUserId());
                result = ((ManagableObjects) manager).getManagedItems(getUserId());
            }
        } else {
            if (userId == 0) {
                userId = getUserId();
            }
            Context.getPermissionsManager().checkUser(getUserId(), userId);
            result = ((ManagableObjects) manager).getUserItems(userId);
        }
        return result;
    }

    @POST
    public Response add(T entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        if (baseClass.equals(Device.class)) {
            Context.getPermissionsManager().checkDeviceReadonly(getUserId());
            Context.getPermissionsManager().checkDeviceLimit(getUserId());
        } else if (baseClass.equals(Command.class)) {
            Context.getPermissionsManager().checkLimitCommands(getUserId());
        }

        BaseObjectManager<T> manager = Context.getManager(baseClass);
        manager.addItem(entity);

        Context.getDataManager().linkObject(User.class, getUserId(), baseClass, entity.getId(), true);

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
    public Response update(T entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        if (baseClass.equals(Device.class)) {
            Context.getPermissionsManager().checkDeviceReadonly(getUserId());
        } else if (baseClass.equals(User.class)) {
            User before = Context.getPermissionsManager().getUser(entity.getId());
            Context.getPermissionsManager().checkUserUpdate(getUserId(), before, (User) entity);
        } else if (baseClass.equals(Command.class)) {
            Context.getPermissionsManager().checkLimitCommands(getUserId());
        }
        Context.getPermissionsManager().checkPermission(baseClass, getUserId(), entity.getId());

        Context.getManager(baseClass).updateItem(entity);

        if (baseClass.equals(Group.class) || baseClass.equals(Device.class)) {
            Context.getPermissionsManager().refreshDeviceAndGroupPermissions();
            Context.getPermissionsManager().refreshAllExtendedPermissions();
        }
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        if (baseClass.equals(Device.class)) {
            Context.getPermissionsManager().checkDeviceReadonly(getUserId());
        } else if (baseClass.equals(Command.class)) {
            Context.getPermissionsManager().checkLimitCommands(getUserId());
        }
        Context.getPermissionsManager().checkPermission(baseClass, getUserId(), id);

        BaseObjectManager<T> manager = Context.getManager(baseClass);
        manager.removeItem(id);

        if (manager instanceof SimpleObjectManager) {
            ((SimpleObjectManager<T>) manager).refreshUserItems();
            if (manager instanceof ExtendedObjectManager) {
                ((ExtendedObjectManager<T>) manager).refreshExtendedPermissions();
            }
        }
        if (baseClass.equals(Group.class) || baseClass.equals(Device.class) || baseClass.equals(User.class)) {
            Context.getPermissionsManager().refreshDeviceAndGroupPermissions();
            if (baseClass.equals(User.class)) {
                Context.getPermissionsManager().refreshAllUsersPermissions();
            } else {
                Context.getPermissionsManager().refreshAllExtendedPermissions();
            }
        }
        return Response.noContent().build();
    }

}
