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

import java.util.Collection;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.model.User;

@Path("users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource extends BaseResource<User> {

    @GET
    @RolesAllowed(User.ROLE_ADMIN)
    @Override
    public Collection<User> getEntities() {
        return super.getEntities();
    }

    @GET
    @Path("{id}")
    @RolesAllowed(User.ROLE_USER)
    @Override
    public User getEntity(@PathParam("id") long id) {
        return super.getEntity(id);
    }

    @POST
    @RolesAllowed(User.ROLE_USER)
    @Override
    public Response postEntity(User entity) {
        return super.postEntity(entity);
    }

    @PUT
    @Path("{id}")
    @RolesAllowed(User.ROLE_USER)
    @Override
    public Response putEntity(@PathParam("id") long id, User entity) {
        return super.putEntity(id, entity);
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed(User.ROLE_USER)
    @Override
    public Response deleteEntity(@PathParam("id") long id) {
        return super.deleteEntity(id);
    }

}
