/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.sql.SQLException;
import java.util.Collection;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.traccar.model.Device;

@Path("devices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeviceResource extends BaseResource {

    @GET
    public Collection<Device> get() {
        try {
            return Context.getDataManager().getAllDevices();
        } catch (SQLException e) {
            throw new WebApplicationException(e);
        }
    }

    @POST
    public Response add(Device entity) {
        try {
            Context.getDataManager().addDevice(entity);
            return Response.ok(entity).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e);
        }
    }

    @Path("{id}")
    @PUT
    public Response update(@PathParam("id") long id, Device entity) {
        try {
            entity.setId(id);
            Context.getDataManager().updateDevice(entity);
            return Response.ok(entity).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e);
        }
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) {
        try {
            Context.getDataManager().removeDevice(id);
            return Response.noContent().build();
        } catch (SQLException e) {
            throw new WebApplicationException(e);
        }
    }

}
