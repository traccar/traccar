/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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
            @QueryParam("from") String from, @QueryParam("to") String to) throws SQLException {
        return Route.getObjects(getUserId(), deviceIds, groupIds,
                DateUtil.parseDate(from), DateUtil.parseDate(to));
    }

    @Path("route")
    @GET
    @Produces(XLSX)
    public Response getRouteExcel(
            @QueryParam("deviceId") final List<Long> deviceIds, @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") String from, @QueryParam("to") String to, @QueryParam("mail") boolean mail)
            throws SQLException, IOException {
        return executeReport(getUserId(), mail, stream -> {
            Route.getExcel(stream, getUserId(), deviceIds, groupIds,
                    DateUtil.parseDate(from), DateUtil.parseDate(to));
        });
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
            @QueryParam("from") String from, @QueryParam("to") String to, @QueryParam("mail") boolean mail)
            throws SQLException, IOException {
        return executeReport(getUserId(), mail, stream -> {
            Events.getExcel(stream, getUserId(), deviceIds, groupIds, types,
                    DateUtil.parseDate(from), DateUtil.parseDate(to));
        });
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
            @QueryParam("from") String from, @QueryParam("to") String to, @QueryParam("mail") boolean mail)
            throws SQLException, IOException {
        return executeReport(getUserId(), mail, stream -> {
            Summary.getExcel(stream, getUserId(), deviceIds, groupIds,
                    DateUtil.parseDate(from), DateUtil.parseDate(to));
        });
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
            @QueryParam("from") String from, @QueryParam("to") String to, @QueryParam("mail") boolean mail)
            throws SQLException, IOException {
        return executeReport(getUserId(), mail, stream -> {
            Trips.getExcel(stream, getUserId(), deviceIds, groupIds,
                    DateUtil.parseDate(from), DateUtil.parseDate(to));
        });
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
            @QueryParam("from") String from, @QueryParam("to") String to, @QueryParam("mail") boolean mail)
            throws SQLException, IOException {
        return executeReport(getUserId(), mail, stream -> {
            Stops.getExcel(stream, getUserId(), deviceIds, groupIds,
                    DateUtil.parseDate(from), DateUtil.parseDate(to));
        });
    }

}
