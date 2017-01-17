/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.database.GeofenceManager;
import org.traccar.model.Geofence;

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
import java.util.Set;

@Path("geofences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GeofenceResource extends BaseResource {

    @GET
    public Collection<Geofence> get(
            @QueryParam("all") boolean all, @QueryParam("userId") long userId, @QueryParam("groupId") long groupId,
            @QueryParam("deviceId") long deviceId, @QueryParam("refresh") boolean refresh) throws SQLException {

        GeofenceManager geofenceManager = Context.getGeofenceManager();
        if (refresh) {
            geofenceManager.refreshGeofences();
        }

        Set<Long> result = new HashSet<>();
        if (all) {
            if (Context.getPermissionsManager().isAdmin(getUserId())) {
                result.addAll(geofenceManager.getAllGeofencesIds());
            } else {
                Context.getPermissionsManager().checkManager(getUserId());
                result.addAll(geofenceManager.getManagedGeofencesIds(getUserId()));
            }
        } else {
            if (userId == 0) {
                userId = getUserId();
            }
            Context.getPermissionsManager().checkUser(getUserId(), userId);
            result.addAll(geofenceManager.getUserGeofencesIds(userId));
        }

        if (groupId != 0) {
            Context.getPermissionsManager().checkGroup(getUserId(), groupId);
            result.retainAll(geofenceManager.getGroupGeofencesIds(groupId));
        }

        if (deviceId != 0) {
            Context.getPermissionsManager().checkDevice(getUserId(), deviceId);
            result.retainAll(geofenceManager.getDeviceGeofencesIds(deviceId));
        }
        return geofenceManager.getGeofences(result);

    }

    @POST
    public Response add(Geofence entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getDataManager().addGeofence(entity);
        Context.getDataManager().linkGeofence(getUserId(), entity.getId());
        Context.getGeofenceManager().refreshGeofences();
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @PUT
    public Response update(Geofence entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkGeofence(getUserId(), entity.getId());
        Context.getGeofenceManager().updateGeofence(entity);
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkGeofence(getUserId(), id);
        Context.getDataManager().removeGeofence(id);
        Context.getGeofenceManager().refreshGeofences();
        return Response.noContent().build();
    }

}
