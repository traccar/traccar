package org.traccar.api.resource;

import java.sql.SQLException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.traccar.api.BaseResource;
import org.traccar.reports.ReportUtils;
import org.traccar.reports.Route;
import org.traccar.web.JsonConverter;

@Path("reports")
public class ReportResource extends BaseResource {

    @Path("route")
    @GET
    public Response getRoute(@Context HttpHeaders headers,
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        MultivaluedMap<String, String> headerParams = headers.getRequestHeaders();
        String accept = headerParams.getFirst("Accept");
        if (accept.equals("application/ms-excel")) {
            ResponseBuilder response = Response.ok(ReportUtils.getOut(Route.getCsv(getUserId(), deviceIds, groupIds,
                    JsonConverter.parseDate(from), JsonConverter.parseDate(to))));
            response.type("application/ms-excel");
            return response.build();
        }
        ResponseBuilder response = Response.ok(Route.getJson(getUserId(), deviceIds, groupIds,
                JsonConverter.parseDate(from), JsonConverter.parseDate(to)));
        response.type(MediaType.APPLICATION_JSON);
        return response.build();
    }

}
