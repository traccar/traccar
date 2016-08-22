package org.traccar.api.resource;

import java.sql.SQLException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.traccar.api.BaseResource;
import org.traccar.reports.Events;
import org.traccar.reports.Summary;
import org.traccar.reports.Trips;
import org.traccar.reports.Route;
import org.traccar.web.JsonConverter;

@Path("reports")
@Consumes(MediaType.APPLICATION_JSON)
public class ReportResource extends BaseResource {

    public static final String TEXT_CSV = "text/csv";
    public static final String CONTENT_DISPOSITION_VALUE = "attachment; filename=report.csv";

    @Path("route")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRouteJson(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Response.ok(Route.getJson(getUserId(), deviceIds, groupIds,
                JsonConverter.parseDate(from), JsonConverter.parseDate(to))).build();
    }

    @Path("route")
    @GET
    @Produces(TEXT_CSV)
    public Response getRouteCsv(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Response.ok(Route.getCsv(getUserId(), deviceIds, groupIds,
                JsonConverter.parseDate(from), JsonConverter.parseDate(to)))
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE)
                .build();
    }

    @Path("events")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEventsJson(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("type") final List<String> types,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Response.ok(Events.getJson(getUserId(), deviceIds, groupIds, types,
                JsonConverter.parseDate(from), JsonConverter.parseDate(to))).build();
    }

    @Path("events")
    @GET
    @Produces(TEXT_CSV)
    public Response getEventsCsv(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("type") final List<String> types,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Response.ok(Events.getCsv(getUserId(), deviceIds, groupIds,
                types, JsonConverter.parseDate(from), JsonConverter.parseDate(to)))
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE)
                .build();
    }

    @Path("summary")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSummaryJson(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Response.ok(Summary.getJson(getUserId(), deviceIds, groupIds,
                JsonConverter.parseDate(from), JsonConverter.parseDate(to))).build();
    }

    @Path("summary")
    @GET
    @Produces(TEXT_CSV)
    public Response getSummaryCsv(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Response.ok(Summary.getCsv(getUserId(), deviceIds, groupIds,
                JsonConverter.parseDate(from), JsonConverter.parseDate(to)))
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE)
                .build();
    }

    @Path("trips")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTripsJson(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Response.ok(Trips.getJson(getUserId(), deviceIds, groupIds,
                JsonConverter.parseDate(from), JsonConverter.parseDate(to))).build();
    }

    @Path("trips")
    @GET
    @Produces(TEXT_CSV)
    public Response getTripsCsv(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Response.ok(Trips.getCsv(getUserId(), deviceIds, groupIds,
                JsonConverter.parseDate(from), JsonConverter.parseDate(to)))
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE)
                .build();
    }

}
