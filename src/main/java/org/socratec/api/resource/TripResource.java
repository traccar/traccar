/*
 * Copyright 2024 Socratec Telematic GmbH
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
package org.socratec.api.resource;

import org.traccar.api.SimpleObjectResource;
import org.traccar.model.UserRestrictions;
import org.traccar.storage.StorageException;
import org.socratec.model.Trip;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;

@Path("trips")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TripResource extends SimpleObjectResource<Trip> {

    public TripResource() {
        super(Trip.class, "startTime");
    }

    @Override
    @GET
    public Collection<Trip> get(
            @QueryParam("all") boolean all,
            @QueryParam("userId") long userId
    ) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        return super.get(all, userId);
    }

    @Override
    @POST
    public Response add(Trip entity) {
        return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }

    @Override
    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) {
        return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }
}
