/*
 * Copyright 2016 - 2020 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 - 2018 Andrey Kunitsyn (andrey@traccar.org)
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.api.BaseResource;
import org.traccar.helper.LogAction;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportResource.class);

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CONTENT_DISPOSITION_VALUE_XLSX = "attachment; filename=report.xlsx";

    private interface ReportExecutor {
        void execute(ByteArrayOutputStream stream) throws SQLException, IOException;
    }

    private Response executeReport(
            long userId, boolean mail, ReportExecutor executor) throws SQLException, IOException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (mail) {
            new Thread(() -> {
                try {
                    executor.execute(stream);

                    MimeBodyPart attachment = new MimeBodyPart();

                    attachment.setFileName("report.xlsx");
                    attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(
                            stream.toByteArray(), "application/octet-stream")));

                    Context.getMailManager().sendMessage(
                            userId, "Report", "The report is in the attachment.", attachment);
                } catch (SQLException | IOException | MessagingException e) {
                    LOGGER.warn("Report failed", e);
                }
            }).start();
            return Response.noContent().build();
        } else {
            executor.execute(stream);
            return Response.ok(stream.toByteArray())
                    .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE_XLSX).build();
        }
    }

    @Path("route")
    @GET
    public Collection<Position> getRoute(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") Date from, @QueryParam("to") Date to) throws SQLException {
        LogAction.logReport(getUserId(), "route", from, to, deviceIds, groupIds);
        return Route.getObjects(getUserId(), deviceIds, groupIds, from, to);
    }

    @Path("route")
    @GET
    @Produces(XLSX)
    public Response getRouteExcel(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") Date from, @QueryParam("to") Date to, @QueryParam("mail") boolean mail)
            throws SQLException, IOException {
        return executeReport(getUserId(), mail, stream -> {
            LogAction.logReport(getUserId(), "route", from, to, deviceIds, groupIds);
            Route.getExcel(stream, getUserId(), deviceIds, groupIds, from, to);
        });
    }

    @Path("events")
    @GET
    public Collection<Event> getEvents(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("type") final List<String> types,
            @QueryParam("from") Date from, @QueryParam("to") Date to) throws SQLException {
        LogAction.logReport(getUserId(), "events", from, to, deviceIds, groupIds);
        return Events.getObjects(getUserId(), deviceIds, groupIds, types, from, to);
    }

    @Path("events")
    @GET
    @Produces(XLSX)
    public Response getEventsExcel(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("type") final List<String> types,
            @QueryParam("from") Date from, @QueryParam("to") Date to, @QueryParam("mail") boolean mail)
            throws SQLException, IOException {
        return executeReport(getUserId(), mail, stream -> {
            LogAction.logReport(getUserId(), "events", from, to, deviceIds, groupIds);
            Events.getExcel(stream, getUserId(), deviceIds, groupIds, types, from, to);
        });
    }

    @Path("summary")
    @GET
    public Collection<SummaryReport> getSummary(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") Date from, @QueryParam("to") Date to, @QueryParam("daily") boolean daily)
            throws SQLException {
        LogAction.logReport(getUserId(), "summary", from, to, deviceIds, groupIds);
        return Summary.getObjects(getUserId(), deviceIds, groupIds, from, to, daily);
    }

    @Path("summary")
    @GET
    @Produces(XLSX)
    public Response getSummaryExcel(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") Date from, @QueryParam("to") Date to, @QueryParam("daily") boolean daily,
            @QueryParam("mail") boolean mail)
            throws SQLException, IOException {
        return executeReport(getUserId(), mail, stream -> {
            LogAction.logReport(getUserId(), "summary", from, to, deviceIds, groupIds);
            Summary.getExcel(stream, getUserId(), deviceIds, groupIds, from, to, daily);
        });
    }

    @Path("trips")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<TripReport> getTrips(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") Date from, @QueryParam("to") Date to) throws SQLException {
        LogAction.logReport(getUserId(), "trips", from, to, deviceIds, groupIds);
        return Trips.getObjects(getUserId(), deviceIds, groupIds, from, to);
    }

    @Path("trips")
    @GET
    @Produces(XLSX)
    public Response getTripsExcel(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") Date from, @QueryParam("to") Date to, @QueryParam("mail") boolean mail)
            throws SQLException, IOException {
        return executeReport(getUserId(), mail, stream -> {
            LogAction.logReport(getUserId(), "trips", from, to, deviceIds, groupIds);
            Trips.getExcel(stream, getUserId(), deviceIds, groupIds, from, to);
        });
    }

    @Path("stops")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<StopReport> getStops(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") Date from, @QueryParam("to") Date to) throws SQLException {
        LogAction.logReport(getUserId(), "stops", from, to, deviceIds, groupIds);
        return Stops.getObjects(getUserId(), deviceIds, groupIds, from, to);
    }

    @Path("stops")
    @GET
    @Produces(XLSX)
    public Response getStopsExcel(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") Date from, @QueryParam("to") Date to, @QueryParam("mail") boolean mail)
            throws SQLException, IOException {
        return executeReport(getUserId(), mail, stream -> {
            LogAction.logReport(getUserId(), "stops", from, to, deviceIds, groupIds);
            Stops.getExcel(stream, getUserId(), deviceIds, groupIds, from, to);
        });
    }

}
