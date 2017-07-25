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
import org.traccar.database.DeviceManager;
import org.traccar.model.Device;
import org.traccar.model.DeviceTotalDistance;
import org.traccar.model.User;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Path("devices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeviceResource extends BaseResource {

    @GET
    public Collection<Device> get(
            @QueryParam("all") boolean all, @QueryParam("userId") long userId,
            @QueryParam("uniqueId") List<String> uniqueIds,
            @QueryParam("id") List<Long> deviceIds) throws SQLException {
        DeviceManager deviceManager = Context.getDeviceManager();
        Set<Long> result = null;
        if (all) {
            if (Context.getPermissionsManager().isAdmin(getUserId())) {
                result = deviceManager.getAllItems();
            } else {
                Context.getPermissionsManager().checkManager(getUserId());
                result = deviceManager.getManagedItems(getUserId());
            }
        } else if (uniqueIds.isEmpty() && deviceIds.isEmpty()) {
            if (userId == 0) {
                userId = getUserId();
            }
            Context.getPermissionsManager().checkUser(getUserId(), userId);
            result = deviceManager.getUserItems(userId);
        } else {
            result = new HashSet<Long>();
            for (String uniqueId : uniqueIds) {
                Device device = deviceManager.getDeviceByUniqueId(uniqueId);
                Context.getPermissionsManager().checkDevice(getUserId(), device.getId());
                result.add(device.getId());
            }
            for (Long deviceId : deviceIds) {
                Context.getPermissionsManager().checkDevice(getUserId(), deviceId);
                result.add(deviceId);
            }
        }
        return deviceManager.getItems(Device.class, result);
    }

    @POST
    public Response add(Device entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkDeviceReadonly(getUserId());
        Context.getPermissionsManager().checkDeviceLimit(getUserId());
        Context.getDeviceManager().addItem(entity);
        Context.getDataManager().linkObject(User.class, getUserId(), entity.getClass(), entity.getId(), true);
        Context.getPermissionsManager().refreshDeviceAndGroupPermissions();
        Context.getPermissionsManager().refreshAllExtendedPermissions();
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @PUT
    public Response update(Device entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkDeviceReadonly(getUserId());
        Context.getPermissionsManager().checkDevice(getUserId(), entity.getId());
        Context.getDeviceManager().updateItem(entity);
        Context.getPermissionsManager().refreshDeviceAndGroupPermissions();
        Context.getPermissionsManager().refreshAllExtendedPermissions();
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkDeviceReadonly(getUserId());
        Context.getPermissionsManager().checkDevice(getUserId(), id);
        Context.getDeviceManager().removeItem(id);
        Context.getPermissionsManager().refreshDeviceAndGroupPermissions();
        Context.getPermissionsManager().refreshAllExtendedPermissions();
        Context.getAliasesManager().removeDevice(id);
        return Response.noContent().build();
    }

    @Path("{id}/distance")
    @PUT
    public Response updateTotalDistance(DeviceTotalDistance entity) throws SQLException {
        Context.getPermissionsManager().checkAdmin(getUserId());
        Context.getDeviceManager().resetTotalDistance(entity);
        return Response.noContent().build();
    }

}
