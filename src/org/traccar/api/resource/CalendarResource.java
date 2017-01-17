/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 - 2017 Andrey Kunitsyn (andrey@traccar.org)
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

import org.traccar.Context;
import org.traccar.api.BaseResource;
import org.traccar.model.Calendar;

@Path("calendars")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CalendarResource extends BaseResource {

    @GET
    public Collection<Calendar> get(
            @QueryParam("all") boolean all, @QueryParam("userId") long userId) throws SQLException {

        if (all) {
            if (Context.getPermissionsManager().isAdmin(getUserId())) {
                return Context.getCalendarManager().getAllCalendars();
            } else {
                Context.getPermissionsManager().checkManager(getUserId());
                return Context.getCalendarManager().getManagedCalendars(getUserId());
            }
        } else {
            if (userId == 0) {
                userId = getUserId();
            }
            Context.getPermissionsManager().checkUser(getUserId(), userId);
            return Context.getCalendarManager().getUserCalendars(userId);
        }
    }

    @POST
    public Response add(Calendar entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getCalendarManager().addCalendar(entity);
        Context.getDataManager().linkCalendar(getUserId(), entity.getId());
        Context.getCalendarManager().refreshUserCalendars();
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @PUT
    public Response update(Calendar entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkCalendar(getUserId(), entity.getId());
        Context.getCalendarManager().updateCalendar(entity);
        return Response.ok(entity).build();
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkCalendar(getUserId(), id);
        Context.getCalendarManager().removeCalendar(id);
        return Response.noContent().build();
    }
}
