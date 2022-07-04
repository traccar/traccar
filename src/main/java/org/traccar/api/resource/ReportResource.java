/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.api.BaseResource;
import org.traccar.database.MailManager;
import org.traccar.helper.LogAction;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.model.UserRestrictions;
import org.traccar.reports.EventsReportProvider;
import org.traccar.reports.RouteReportProvider;
import org.traccar.reports.StopsReportProvider;
import org.traccar.reports.SummaryReportProvider;
import org.traccar.reports.TripsReportProvider;
import org.traccar.reports.model.StopReportItem;
import org.traccar.reports.model.SummaryReportItem;
import org.traccar.reports.model.TripReportItem;
import org.traccar.storage.StorageException;

import javax.activation.DataHandler;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Path("reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReportResource extends BaseResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportResource.class);

    private static final String EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    private EventsReportProvider eventsReportProvider;

    @Inject
    private RouteReportProvider routeReportProvider;

    @Inject
    private StopsReportProvider stopsReportProvider;

    @Inject
    private SummaryReportProvider summaryReportProvider;

    @Inject
    private TripsReportProvider tripsReportProvider;

    @Inject
    private MailManager mailManager;

    private interface ReportExecutor {
        void execute(OutputStream stream) throws StorageException, IOException;
    }

    private Response executeReport(
            long userId, boolean mail, ReportExecutor executor) {
        if (mail) {
            new Thread(() -> {
                try {
                    var stream = new ByteArrayOutputStream();
                    executor.execute(stream);

                    MimeBodyPart attachment = new MimeBodyPart();
                    attachment.setFileName("report.xlsx");
                    attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(
                            stream.toByteArray(), "application/octet-stream")));

                    User user = permissionsService.getUser(userId);
                    mailManager.sendMessage(user, "Report", "The report is in the attachment.", attachment);
                } catch (StorageException | IOException | MessagingException e) {
                    LOGGER.warn("Report failed", e);
                }
            }).start();
            return Response.noContent().build();
        } else {
            StreamingOutput stream = output -> {
                try {
                    executor.execute(output);
                } catch (StorageException e) {
                    throw new WebApplicationException(e);
                }
            };
            return Response.ok(stream)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.xlsx").build();
        }
    }

    @Path("route")
    @GET
    public Collection<Position> getRoute(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        LogAction.logReport(getUserId(), "route", from, to, deviceIds, groupIds);
        return routeReportProvider.getObjects(getUserId(), deviceIds, groupIds, from, to);
    }

    @Path("route")
    @GET
    @Produces(EXCEL)
    public Response getRouteExcel(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @QueryParam("mail") boolean mail) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        return executeReport(getUserId(), mail, stream -> {
            LogAction.logReport(getUserId(), "route", from, to, deviceIds, groupIds);
            routeReportProvider.getExcel(stream, getUserId(), deviceIds, groupIds, from, to);
        });
    }

    @Path("route/{type:xlsx|mail}")
    @GET
    @Produces(EXCEL)
    public Response getRouteExcel(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") final List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @PathParam("type") String type) throws StorageException {
        return getRouteExcel(deviceIds, groupIds, from, to, type.equals("mail"));
    }

    @Path("events")
    @GET
    public Collection<Event> getEvents(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("type") List<String> types,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        LogAction.logReport(getUserId(), "events", from, to, deviceIds, groupIds);
        return eventsReportProvider.getObjects(getUserId(), deviceIds, groupIds, types, from, to);
    }

    @Path("events")
    @GET
    @Produces(EXCEL)
    public Response getEventsExcel(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("type") List<String> types,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @QueryParam("mail") boolean mail) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        return executeReport(getUserId(), mail, stream -> {
            LogAction.logReport(getUserId(), "events", from, to, deviceIds, groupIds);
            eventsReportProvider.getExcel(stream, getUserId(), deviceIds, groupIds, types, from, to);
        });
    }

    @Path("events/{type:xlsx|mail}")
    @GET
    @Produces(EXCEL)
    public Response getEventsExcel(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("type") List<String> types,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @PathParam("type") String type) throws StorageException {
        return getEventsExcel(deviceIds, groupIds, types, from, to, type.equals("mail"));
    }

    @Path("summary")
    @GET
    public Collection<SummaryReportItem> getSummary(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @QueryParam("daily") boolean daily) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        LogAction.logReport(getUserId(), "summary", from, to, deviceIds, groupIds);
        return summaryReportProvider.getObjects(getUserId(), deviceIds, groupIds, from, to, daily);
    }

    @Path("summary")
    @GET
    @Produces(EXCEL)
    public Response getSummaryExcel(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @QueryParam("daily") boolean daily,
            @QueryParam("mail") boolean mail) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        return executeReport(getUserId(), mail, stream -> {
            LogAction.logReport(getUserId(), "summary", from, to, deviceIds, groupIds);
            summaryReportProvider.getExcel(stream, getUserId(), deviceIds, groupIds, from, to, daily);
        });
    }

    @Path("summary/{type:xlsx|mail}")
    @GET
    @Produces(EXCEL)
    public Response getSummaryExcel(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @QueryParam("daily") boolean daily,
            @PathParam("type") String type) throws StorageException {
        return getSummaryExcel(deviceIds, groupIds, from, to, daily, type.equals("mail"));
    }

    @Path("trips")
    @GET
    public Collection<TripReportItem> getTrips(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        LogAction.logReport(getUserId(), "trips", from, to, deviceIds, groupIds);
        return tripsReportProvider.getObjects(getUserId(), deviceIds, groupIds, from, to);
    }

    @Path("trips")
    @GET
    @Produces(EXCEL)
    public Response getTripsExcel(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @QueryParam("mail") boolean mail) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        return executeReport(getUserId(), mail, stream -> {
            LogAction.logReport(getUserId(), "trips", from, to, deviceIds, groupIds);
            tripsReportProvider.getExcel(stream, getUserId(), deviceIds, groupIds, from, to);
        });
    }

    @Path("trips/{type:xlsx|mail}")
    @GET
    @Produces(EXCEL)
    public Response getTripsExcel(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @PathParam("type") String type) throws StorageException {
        return getTripsExcel(deviceIds, groupIds, from, to, type.equals("mail"));
    }

    @Path("stops")
    @GET
    public Collection<StopReportItem> getStops(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        LogAction.logReport(getUserId(), "stops", from, to, deviceIds, groupIds);
        return stopsReportProvider.getObjects(getUserId(), deviceIds, groupIds, from, to);
    }

    @Path("stops")
    @GET
    @Produces(EXCEL)
    public Response getStopsExcel(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @QueryParam("mail") boolean mail) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        return executeReport(getUserId(), mail, stream -> {
            LogAction.logReport(getUserId(), "stops", from, to, deviceIds, groupIds);
            stopsReportProvider.getExcel(stream, getUserId(), deviceIds, groupIds, from, to);
        });
    }

    @Path("stops/{type:xlsx|mail}")
    @GET
    @Produces(EXCEL)
    public Response getStopsExcel(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @PathParam("type") String type) throws StorageException {
        return getStopsExcel(deviceIds, groupIds, from, to, type.equals("mail"));
    }

}
