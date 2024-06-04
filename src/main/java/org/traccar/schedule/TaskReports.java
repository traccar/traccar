/*
 * Copyright 2023 - 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar.schedule;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.helper.LogAction;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.BaseModel;
import org.traccar.model.Calendar;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Report;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.reports.EventsReportProvider;
import org.traccar.reports.RouteReportProvider;
import org.traccar.reports.StopsReportProvider;
import org.traccar.reports.SummaryReportProvider;
import org.traccar.reports.TripsReportProvider;
import org.traccar.reports.common.ReportMailer;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import com.google.inject.Injector;
import com.google.inject.servlet.RequestScoper;
import com.google.inject.servlet.ServletScopes;

import jakarta.inject.Inject;
import net.fortuna.ical4j.model.Period;

public class TaskReports implements ScheduleTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskReports.class);

    private static final long CHECK_PERIOD_MINUTES = 15;

    private final Storage storage;
    private final Injector injector;

    @Inject
    public TaskReports(Storage storage, Injector injector) {
        this.storage = storage;
        this.injector = injector;
    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        executor.scheduleAtFixedRate(this, CHECK_PERIOD_MINUTES, CHECK_PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        Date currentCheck = new Date();
        Date lastCheck = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(CHECK_PERIOD_MINUTES));

        try {
            for (Report report : storage.getObjects(Report.class, new Request(new Columns.All()))) {
                Calendar calendar = storage.getObject(Calendar.class, new Request(
                        new Columns.All(), new Condition.Equals("id", report.getCalendarId())));

                var lastEvents = calendar.findPeriods(lastCheck);
                var currentEvents = calendar.findPeriods(currentCheck);

                Set<Period> finishedEvents = new HashSet<>(lastEvents);
                finishedEvents.removeAll(currentEvents);
                for (Period period : finishedEvents) {
                    RequestScoper scope = ServletScopes.scopeRequest(Collections.emptyMap());
                    try (RequestScoper.CloseableScope ignored = scope.open()) {
                        executeReport(report, period.getStart(), period.getEnd());
                    }
                }
            }
        } catch (StorageException e) {
            LOGGER.warn("Scheduled reports error", e);
        }
    }

    // * CUSTOM CODE START * //
    private List<Group> getGroupsList(List<Long> groupIds) throws StorageException {
        List<Group> result = new LinkedList<>();
        for (Long groupId : groupIds) {
            result.addAll(storage.getObjects(Group.class, new Request(
                    new Columns.All(),
                    new Condition.Equals("id", groupId))));
        }
        return result;
    }

    private List<Device> getDevicesList(List<Long> deviceIds) throws StorageException {
        List<Device> result = new LinkedList<>();
        for (Long deviceId : deviceIds) {
            result.addAll(storage.getObjects(Device.class, new Request(
                    new Columns.All(),
                    new Condition.Equals("id", deviceId))));
        }
        return result;
    }

    private TimeZone getUserPreferredTimeZone(User user) throws StorageException {

        Server server = storage.getObject(
                Server.class, new Request(new Columns.All()));

        var tz = UserUtil.getTimezone(server, user);
        return tz;
    }

    // * CUSTOM CODE END * //

    private void executeReport(Report report, Date from, Date to) throws StorageException {

        var deviceIds = storage.getObjects(Device.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(Device.class, Report.class, report.getId())))
                .stream().map(BaseModel::getId).collect(Collectors.toList());
        var groupIds = storage.getObjects(Group.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(Group.class, Report.class, report.getId())))
                .stream().map(BaseModel::getId).collect(Collectors.toList());
        var users = storage.getObjects(User.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(User.class, Report.class, report.getId())));

        ReportMailer reportMailer = injector.getInstance(ReportMailer.class);

        for (User user : users) {
            // * CUSTOM CODE START * //
            TimeZone tz = TimeZone.getTimeZone("UTC");
            try {
                tz = getUserPreferredTimeZone(user);
            } catch (StorageException e) {
                e.printStackTrace();
            }
            // * CUSTOM CODE END * //

            LogAction.report(user.getId(), true, report.getType(), from, to, deviceIds, groupIds);
            switch (report.getType()) {
                case "events":
                    var eventsReportProvider = injector.getInstance(EventsReportProvider.class);
                    reportMailer.sendAsync(user.getId(), stream -> eventsReportProvider.getExcel(
                            stream, user.getId(), deviceIds, groupIds, List.of(), from, to),
                    // * CUSTOM CODE START * //
                            report.getType(), from,
                            to, getDevicesList(deviceIds), getGroupsList(groupIds), tz
                    // * CUSTOM CODE END * //
                    );
                    break;
                case "route":
                    var routeReportProvider = injector.getInstance(RouteReportProvider.class);
                    reportMailer.sendAsync(user.getId(), stream -> routeReportProvider.getExcel(
                            stream, user.getId(), deviceIds, groupIds, from, to),
                    // * CUSTOM CODE START * //
                            report.getType(), from, to,
                            getDevicesList(deviceIds), getGroupsList(groupIds), tz
                    // * CUSTOM CODE END * //
                    );
                    break;
                case "summary":
                    var summaryReportProvider = injector.getInstance(SummaryReportProvider.class);
                    reportMailer.sendAsync(user.getId(), stream -> summaryReportProvider.getExcel(
                            stream, user.getId(), deviceIds, groupIds, from, to, false),
                    // * CUSTOM CODE START * //
                            report.getType(), from, to, getDevicesList(deviceIds), getGroupsList(groupIds), tz
                    // * CUSTOM CODE END * //
                    );
                    break;
                case "trips":
                    var tripsReportProvider = injector.getInstance(TripsReportProvider.class);
                    reportMailer.sendAsync(user.getId(), stream -> tripsReportProvider.getExcel(
                            stream, user.getId(), deviceIds, groupIds, from, to),
                    // * CUSTOM CODE START * //
                            report.getType(), from, to,
                            getDevicesList(deviceIds), getGroupsList(groupIds), tz
                    // * CUSTOM CODE END * //
                    );
                    break;
                case "stops":
                    var stopsReportProvider = injector.getInstance(StopsReportProvider.class);
                    reportMailer.sendAsync(user.getId(), stream -> stopsReportProvider.getExcel(
                            stream, user.getId(), deviceIds, groupIds, from, to),
                    // * CUSTOM CODE START * //
                            report.getType(), from, to,
                            getDevicesList(deviceIds), getGroupsList(groupIds), tz
                    // * CUSTOM CODE END * //
                    );
                    break;
                default:
                    LOGGER.warn("Unsupported report type {}", report.getType());
                    break;
            }
        }
    }

}
