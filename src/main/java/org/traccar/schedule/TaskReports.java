/*
 * Copyright 2023 - 2025 Anton Tananaev (anton@traccar.org)
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

import com.google.inject.Injector;
import com.google.inject.servlet.RequestScoper;
import com.google.inject.servlet.ServletScopes;
import net.fortuna.ical4j.model.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.helper.DateUtil;
import org.traccar.helper.LogAction;
import org.traccar.model.BaseModel;
import org.traccar.model.Calendar;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Report;
import org.traccar.model.User;
import org.traccar.reports.common.ReportMailer;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TaskReports extends SingleScheduleTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskReports.class);

    private static final long CHECK_PERIOD_MINUTES = 15;

    private final LogAction actionLogger;
    private final Storage storage;
    private final Injector injector;

    @Inject
    public TaskReports(LogAction actionLogger, Storage storage, Injector injector) {
        this.actionLogger = actionLogger;
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

                Set<Period<Instant>> finishedEvents = new HashSet<>(lastEvents);
                finishedEvents.removeAll(currentEvents);
                for (Period<Instant> period : finishedEvents) {
                    RequestScoper scope = ServletScopes.scopeRequest(Collections.emptyMap());
                    try (RequestScoper.CloseableScope ignored = scope.open()) {
                        executeReport(report, Date.from(period.getStart()), Date.from(period.getEnd()));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Scheduled reports error", e);
        }
    }

    private void executeReport(Report report, Date from, Date to) throws StorageException {

        var deviceIds = storage.getObjects(Device.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(Device.class, Report.class, report.getId())))
                .stream().map(BaseModel::getId).toList();
        var deviceIdsPart = deviceIds.stream()
                .map(id -> "deviceId=" + id)
                .collect(Collectors.joining("&"));

        var groupIds = storage.getObjects(Group.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(Group.class, Report.class, report.getId())))
                .stream().map(BaseModel::getId).toList();
        var groupIdsPart = groupIds.stream()
                .map(id -> "groupId=" + id)
                .collect(Collectors.joining("&"));

        var users = storage.getObjects(User.class, new Request(
                new Columns.All(),
                new Condition.Permission(User.class, Report.class, report.getId())));

        StringBuilder url = new StringBuilder("/reports/");
        url.append(report.getType()).append('?');
        if (!deviceIdsPart.isEmpty()) {
            url.append(deviceIdsPart).append('&');
        }
        if (!groupIdsPart.isEmpty()) {
            url.append(groupIdsPart).append('&');
        }
        url.append("from=").append(URLEncoder.encode(DateUtil.formatDate(from, true), StandardCharsets.UTF_8));
        url.append('&');
        url.append("to=").append(URLEncoder.encode(DateUtil.formatDate(to, true), StandardCharsets.UTF_8));

        ReportMailer reportMailer = injector.getInstance(ReportMailer.class);
        for (User user : users) {
            actionLogger.report(null, user.getId(), true, report.getType(), from, to, deviceIds, groupIds);
            reportMailer.sendAsync(user, url.toString());
        }
    }

}
