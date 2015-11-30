package org.traccar.rest;

import org.traccar.Context;
import org.traccar.model.User;
import org.traccar.web.JsonConverter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.text.ParseException;

import static org.traccar.web.BaseServlet.USER_KEY;

/**
 * Created by niko on 11/26/15.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class MainResource {

    @javax.ws.rs.core.Context
    HttpServletRequest req;

    @Path("session")
    @GET
    public Response session() throws SQLException, IOException {
        Long userId = (Long) req.getSession().getAttribute(USER_KEY);
        if (userId != null) {
            return ResponseBuilder.getResponse(JsonConverter.objectToJson(
                    Context.getDataManager().getUser(userId)));
        }
        return ResponseBuilder.getResponse(false);
    }

    @Path("login")
    @POST
    public Response logOn(@FormParam("email") String email,
                          @FormParam("password") String password) throws Exception{
        User user = Context.getDataManager().login(
                email, password);
        if (user != null) {
            req.getSession().setAttribute(USER_KEY, user.getId());
            return ResponseBuilder.getResponse(JsonConverter.objectToJson(user));
        }
        return ResponseBuilder.getResponse(false);
    }

    @Path("logout")
    @GET
    public Response logout() throws IOException {
        req.getSession().removeAttribute(USER_KEY);
        return ResponseBuilder.getResponse(true);
    }

    @Path("register")
    @POST
    public Response register(String u) throws IOException, ParseException, SQLException {

        User user = JsonConverter.objectFromJson(new StringReader(u), new User());
        Context.getDataManager().addUser(user);
        return ResponseBuilder.getResponse(true);
    }
}