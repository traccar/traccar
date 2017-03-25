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
import org.traccar.model.Device;
import org.traccar.model.DeviceTotalDistance;

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

@Path("devices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeviceResource extends BaseResource {

    @GET
    public Collection<Device> get(
            @QueryParam("all") boolean all, @QueryParam("userId") long userId) throws SQLException {
        if (all) {
            if (Context.getPermissionsManager().isAdmin(getUserId())) {
                return Context.getDeviceManager().getAllDevices();
            } else {
                Context.getPermissionsManager().checkManager(getUserId());
                return Context.getDeviceManager().getManagedDevices(getUserId());
            }
        } else {
            if (userId == 0) {
                userId = getUserId();
            }
            Context.getPermissionsManager().checkUser(getUserId(), userId);
            return Context.getDeviceManager().getDevices(userId);
        }
    }

    @POST
    public Response add(Device entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkDeviceReadonly(getUserId());
        Context.getPermissionsManager().checkDeviceLimit(getUserId());
        Context.getDeviceManager().addDevice(entity);
        Context.getDataManager().linkDevice(getUserId(), entity.getId());
        Context.getPermissionsManager().refreshPermissions();
        if (Context.getGeofenceManager() != null) {
            Context.getGeofenceManager().refresh();
        }
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @PUT
    public Response update(Device entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkDeviceReadonly(getUserId());
        Context.getPermissionsManager().checkDevice(getUserId(), entity.getId());
        Context.getDeviceManager().updateDevice(entity);
        Context.getPermissionsManager().refreshPermissions();
        if (Context.getGeofenceManager() != null) {
            Context.getGeofenceManager().refresh();
        }
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkDeviceReadonly(getUserId());
        Context.getPermissionsManager().checkDevice(getUserId(), id);
        Context.getDeviceManager().removeDevice(id);
        Context.getPermissionsManager().refreshPermissions();
        if (Context.getGeofenceManager() != null) {
            Context.getGeofenceManager().refresh();
        }
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
