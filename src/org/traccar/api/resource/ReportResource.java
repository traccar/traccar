package org.traccar.api.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
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
import org.traccar.helper.DateUtil;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.reports.Events;
import org.traccar.reports.Summary;
import org.traccar.reports.Trips;
import org.traccar.reports.model.StopReport;
import org.traccar.reports.model.SummaryReport;
import org.traccar.reports.model.TripReport;
import org.traccar.reports.Route;
import org.traccar.reports.Stops;

@Path("reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReportResource extends BaseResource {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CONTENT_DISPOSITION_VALUE_XLSX = "attachment; filename=report.xlsx";

    @Path("route")
    @GET
    public Collection<Position> getRoute(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Route.getObjects(getUserId(), deviceIds, groupIds,
                DateUtil.parseDate(from), DateUtil.parseDate(to));
    }

    @Path("route")
    @GET
    @Produces(XLSX)
    public Response getRouteExcel(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Route.getExcel(stream, getUserId(), deviceIds, groupIds,
                DateUtil.parseDate(from), DateUtil.parseDate(to));

        return Response.ok(stream.toByteArray())
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE_XLSX).build();
    }

    @Path("events")
    @GET
    public Collection<Event> getEvents(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("type") final List<String> types,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Events.getObjects(getUserId(), deviceIds, groupIds, types,
                DateUtil.parseDate(from), DateUtil.parseDate(to));
    }

    @Path("events")
    @GET
    @Produces(XLSX)
    public Response getEventsExcel(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("type") final List<String> types,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Events.getExcel(stream, getUserId(), deviceIds, groupIds, types,
                DateUtil.parseDate(from), DateUtil.parseDate(to));

        return Response.ok(stream.toByteArray())
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE_XLSX).build();
    }

    @Path("summary")
    @GET
    public Collection<SummaryReport> getSummary(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Summary.getObjects(getUserId(), deviceIds, groupIds,
                DateUtil.parseDate(from), DateUtil.parseDate(to));
    }

    @Path("summary")
    @GET
    @Produces(XLSX)
    public Response getSummaryExcel(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Summary.getExcel(stream, getUserId(), deviceIds, groupIds,
                DateUtil.parseDate(from), DateUtil.parseDate(to));

        return Response.ok(stream.toByteArray())
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE_XLSX).build();
    }

    @Path("trips")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<TripReport> getTrips(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Trips.getObjects(getUserId(), deviceIds, groupIds,
                DateUtil.parseDate(from), DateUtil.parseDate(to));
    }

    @Path("trips")
    @GET
    @Produces(XLSX)
    public Response getTripsExcel(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Trips.getExcel(stream, getUserId(), deviceIds, groupIds,
                DateUtil.parseDate(from), DateUtil.parseDate(to));

        return Response.ok(stream.toByteArray())
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE_XLSX).build();
    }

    @Path("stops")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<StopReport> getStops(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Stops.getObjects(getUserId(), deviceIds, groupIds,
                DateUtil.parseDate(from), DateUtil.parseDate(to));
    }

    @Path("stops")
    @GET
    @Produces(XLSX)
    public Response getStopsExcel(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Stops.getExcel(stream, getUserId(), deviceIds, groupIds,
                DateUtil.parseDate(from), DateUtil.parseDate(to));

        return Response.ok(stream.toByteArray())
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE_XLSX).build();
    }


}
