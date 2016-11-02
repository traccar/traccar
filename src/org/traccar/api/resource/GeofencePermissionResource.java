/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.GeofencePermission;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

@Path("permissions/geofences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GeofencePermissionResource extends BaseResource {

    @POST
    public Response add(GeofencePermission entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkUser(getUserId(), entity.getUserId());
        Context.getPermissionsManager().checkGeofence(getUserId(), entity.getGeofenceId());
        Context.getDataManager().linkGeofence(entity.getUserId(), entity.getGeofenceId());
        Context.getGeofenceManager().refreshUserGeofences();
        return Response.ok(entity).build();
    }

    @DELETE
    public Response remove(GeofencePermission entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkUser(getUserId(), entity.getUserId());
        Context.getPermissionsManager().checkGeofence(getUserId(), entity.getGeofenceId());
        Context.getDataManager().unlinkGeofence(entity.getUserId(), entity.getGeofenceId());
        Context.getGeofenceManager().refreshUserGeofences();
        return Response.noContent().build();
    }

}
