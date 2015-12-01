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
import org.traccar.model.User;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

@Path("session")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class SessionResource extends BaseResource {

    public static final String USER_ID_KEY = "userId";

    @javax.ws.rs.core.Context
    private HttpServletRequest req;

    @PermitAll
    @GET
    public User get() {
        try {
            Long userId = (Long) req.getSession().getAttribute(USER_ID_KEY);
            if (userId != null) {
                return Context.getDataManager().getUser(userId);
            } else {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e);
        }
    }

    @PermitAll
    @POST
    public User add(@FormParam("email") String email, @FormParam("password") String password) {
        try {
            User user = Context.getDataManager().login(email, password);
            if (user != null) {
                req.getSession().setAttribute(USER_ID_KEY, user.getId());
                return user;
            } else {
                throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e);
        }
    }

    @DELETE
    public Response remove() {
        req.getSession().removeAttribute(USER_ID_KEY);
        return Response.noContent().build();
    }

}
