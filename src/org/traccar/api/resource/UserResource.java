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

import java.sql.SQLException;
import java.util.Collection;
import javax.annotation.security.PermitAll;
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

import org.traccar.Context;
import org.traccar.api.BaseResource;
import org.traccar.model.User;

@Path("users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource extends BaseResource {

    @GET
    public Collection<User> get() {
        try {
            return Context.getDataManager().getUsers();
        } catch (SQLException e) {
            throw new WebApplicationException(e);
        }
    }

    @PermitAll
    @POST
    public Response add(User entity) {
        try {
            Context.getDataManager().addUser(entity);
            return Response.ok(entity).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e);
        }
    }

    @Path("{id}")
    @PUT
    public Response update(@PathParam("id") long id, User entity) {
        try {
            entity.setId(id);
            Context.getDataManager().updateUser(entity);
            return Response.ok(entity).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e);
        }
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) {
        try {
            Context.getDataManager().removeUser(id);
            return Response.noContent().build();
        } catch (SQLException e) {
            throw new WebApplicationException(e);
        }
    }

}
