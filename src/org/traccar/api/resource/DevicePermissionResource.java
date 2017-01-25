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
import org.traccar.model.DevicePermission;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

@Path("permissions/devices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DevicePermissionResource extends BaseResource {

    @POST
    public Response add(DevicePermission entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkUser(getUserId(), entity.getUserId());
        Context.getPermissionsManager().checkDevice(getUserId(), entity.getDeviceId());
        Context.getDataManager().linkDevice(entity.getUserId(), entity.getDeviceId());
        Context.getPermissionsManager().refreshPermissions();
        if (Context.getGeofenceManager() != null) {
            Context.getGeofenceManager().refresh();
        }
        return Response.ok(entity).build();
    }

    @DELETE
    public Response remove(DevicePermission entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        if (getUserId() != entity.getUserId()) {
            Context.getPermissionsManager().checkUser(getUserId(), entity.getUserId());
        } else {
            Context.getPermissionsManager().checkAdmin(getUserId());
        }
        Context.getPermissionsManager().checkDevice(getUserId(), entity.getDeviceId());
        Context.getDataManager().unlinkDevice(entity.getUserId(), entity.getDeviceId());
        Context.getPermissionsManager().refreshPermissions();
        if (Context.getGeofenceManager() != null) {
            Context.getGeofenceManager().refresh();
        }
        return Response.noContent().build();
    }

}
