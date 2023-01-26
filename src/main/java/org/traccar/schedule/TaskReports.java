/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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

import net.fortuna.ical4j.model.component.VEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.BaseModel;
import org.traccar.model.Calendar;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Report;
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

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TaskReports implements ScheduleTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskReports.class);

    private static final long CHECK_PERIOD_MINUTES = 1;

    private final Storage storage;
    private final ReportMailer reportMailer;

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
    public TaskReports(Storage storage, ReportMailer reportMailer) {
        this.storage = storage;
        this.reportMailer = reportMailer;
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

                var lastEvents = calendar.findEvents(lastCheck);
                var currentEvents = calendar.findEvents(currentCheck);

                if (!lastEvents.isEmpty() && currentEvents.isEmpty()) {
                    VEvent event = lastEvents.iterator().next();
                    Date from = event.getStartDate().getDate();
                    Date to = event.getEndDate().getDate();
                    executeReport(report, from, to);
                }
            }
        } catch (StorageException e) {
            LOGGER.warn("Scheduled reports error", e);
        }
    }

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

        for (User user : users) {
            switch (report.getType()) {
                case "events":
                    reportMailer.sendAsync(user.getId(), stream -> eventsReportProvider.getExcel(
                            stream, user.getId(), deviceIds, groupIds, List.of(), from, to));
                    break;
                case "route":
                    reportMailer.sendAsync(user.getId(), stream -> routeReportProvider.getExcel(
                            stream, user.getId(), deviceIds, groupIds, from, to));
                    break;
                case "summary":
                    reportMailer.sendAsync(user.getId(), stream -> summaryReportProvider.getExcel(
                            stream, user.getId(), deviceIds, groupIds, from, to, false));
                    break;
                case "trips":
                    reportMailer.sendAsync(user.getId(), stream -> tripsReportProvider.getExcel(
                            stream, user.getId(), deviceIds, groupIds, from, to));
                    break;
                case "stops":
                    reportMailer.sendAsync(user.getId(), stream -> stopsReportProvider.getExcel(
                            stream, user.getId(), deviceIds, groupIds, from, to));
                    break;
                default:
                    LOGGER.warn("Unsupported report type {}", report.getType());
                    break;
            }
        }
    }

}
