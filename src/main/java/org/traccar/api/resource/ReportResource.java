/*
 * Copyright 2016 - 2023 Anton Tananaev (anton@traccar.org)
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

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.traccar.api.SimpleObjectResource;
import org.traccar.helper.LogAction;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.model.Report;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.model.UserRestrictions;
import org.traccar.reports.CombinedReportProvider;
import org.traccar.reports.DevicesReportProvider;
import org.traccar.reports.EventsReportProvider;
import org.traccar.reports.RouteReportProvider;
import org.traccar.reports.StopsReportProvider;
import org.traccar.reports.SummaryReportProvider;
import org.traccar.reports.TripsReportProvider;
import org.traccar.reports.common.ReportExecutor;
import org.traccar.reports.common.ReportMailer;
import org.traccar.reports.model.CombinedReportItem;
import org.traccar.reports.model.StopReportItem;
import org.traccar.reports.model.SummaryReportItem;
import org.traccar.reports.model.TripReportItem;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@Path("reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReportResource extends SimpleObjectResource<Report> {

    private static final String EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    private CombinedReportProvider combinedReportProvider;

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
    private DevicesReportProvider devicesReportProvider;

    @Inject
    private ReportMailer reportMailer;

    public ReportResource() {
        super(Report.class);
    }

    // * CUSTOM CODE START * //

    private List<Group> getGroupsList(List<Long> groupIds) throws StorageException {
        List<Group> result = new LinkedList<>();
        for (Long groupId : groupIds) {
            result.addAll(storage.getObjects(Group.class, new Request(
                    new Columns.All(),
                    new Condition.And(
                            new Condition.Equals("id", groupId),
                            new Condition.Permission(User.class, getUserId(), Group.class)))));
        }
        return result;
    }

    private List<Device> getDevicesList(List<Long> deviceIds) throws StorageException {
        List<Device> result = new LinkedList<>();
        for (Long deviceId : deviceIds) {
            result.addAll(storage.getObjects(Device.class, new Request(
                    new Columns.All(),
                    new Condition.And(
                            new Condition.Equals("id", deviceId),
                            new Condition.Permission(User.class, getUserId(), Device.class)))));
        }
        return result;
    }

    private TimeZone getUserPreferredTimeZone(long userId) throws StorageException {

        User user = permissionsService.getUser(userId);

        Server server = storage.getObject(
                Server.class, new Request(new Columns.All()));

        var tz = UserUtil.getTimezone(server, user);
        return tz;
    }

    private Response executeReport(long userId, boolean mail, ReportExecutor executor, String reportType,
            Date fromDate, Date toDate, List<Device> devices, List<Group> groups) {

        TimeZone tz = TimeZone.getTimeZone("UTC");

        try {
            tz = getUserPreferredTimeZone(userId);
        } catch (StorageException e) {
            e.printStackTrace();
        }

        if (mail) {
            reportMailer.sendAsync(userId, executor, reportType, fromDate, toDate, devices, groups, tz);
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

    // * CUSTOM CODE END * //

    private Response executeReport(long userId, boolean mail, ReportExecutor executor) {
        if (mail) {
            reportMailer.sendAsync(userId, executor);
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

    @Path("combined")
    @GET
    public Collection<CombinedReportItem> getCombined(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        LogAction.logReport(getUserId(), false, "combined", from, to, deviceIds, groupIds);
        return combinedReportProvider.getObjects(getUserId(), deviceIds, groupIds, from, to);
    }

    @Path("route")
    @GET
    public Collection<Position> getRoute(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        LogAction.logReport(getUserId(), false, "route", from, to, deviceIds, groupIds);
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
            LogAction.logReport(getUserId(), false, "route", from, to, deviceIds, groupIds);
            routeReportProvider.getExcel(stream, getUserId(), deviceIds, groupIds, from, to);
        }, "route", from, to, getDevicesList(deviceIds), getGroupsList(groupIds));
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
        LogAction.logReport(getUserId(), false, "events", from, to, deviceIds, groupIds);
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
            LogAction.logReport(getUserId(), false, "events", from, to, deviceIds, groupIds);
            eventsReportProvider.getExcel(stream, getUserId(), deviceIds, groupIds, types, from, to);
        }, "events", from, to, getDevicesList(deviceIds), getGroupsList(groupIds));
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
        LogAction.logReport(getUserId(), false, "summary", from, to, deviceIds, groupIds);
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
            LogAction.logReport(getUserId(), false, "summary", from, to, deviceIds, groupIds);
            summaryReportProvider.getExcel(stream, getUserId(), deviceIds, groupIds, from, to, daily);
        }, "summary" + (daily ? "-daily" : ""), from, to, getDevicesList(deviceIds), getGroupsList(groupIds));
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
        LogAction.logReport(getUserId(), false, "trips", from, to, deviceIds, groupIds);
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
            LogAction.logReport(getUserId(), false, "trips", from, to, deviceIds, groupIds);
            tripsReportProvider.getExcel(stream, getUserId(), deviceIds, groupIds, from, to);
        }, "trips", from, to, getDevicesList(deviceIds), getGroupsList(groupIds));
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
        LogAction.logReport(getUserId(), false, "stops", from, to, deviceIds, groupIds);
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
            LogAction.logReport(getUserId(), false, "stops", from, to, deviceIds, groupIds);
            stopsReportProvider.getExcel(stream, getUserId(), deviceIds, groupIds, from, to);
        }, "stops", from, to, getDevicesList(deviceIds), getGroupsList(groupIds));
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

    @Path("devices/{type:xlsx|mail}")
    @GET
    @Produces(EXCEL)
    public Response geDevicesExcel(
            @PathParam("type") String type) throws StorageException {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        return executeReport(getUserId(), type.equals("mail"), stream -> {
            devicesReportProvider.getExcel(stream, getUserId());
        });
    }

}
