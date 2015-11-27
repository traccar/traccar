package org.traccar.rest;

import org.traccar.Context;
import org.traccar.model.User;
import org.traccar.web.JsonConverter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.security.AccessControlException;

import static org.traccar.web.BaseServlet.USER_KEY;

/**
 * Created by niko on 11/26/15.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class MainResource {

    @javax.ws.rs.core.Context
    HttpServletRequest req;

    @Path("login")
    @POST
    public Response logOn() throws Exception{
        User user = Context.getDataManager().login(
                req.getParameter("email"), req.getParameter("password"));
        if (user != null) {
            req.getSession().setAttribute(USER_KEY, user.getId());
            return ResponseBuilder.getResponse(JsonConverter.objectToJson(user));
        } else {
            return ResponseBuilder.getResponse(false);
        }
    }
}