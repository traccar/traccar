/*
 * Copyright 2016 - 2023 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.reports;

import org.jxls.util.JxlsHelper;
import org.traccar.api.security.PermissionsService;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.UnitsConverter;
import org.traccar.helper.model.DeviceUtil;
import org.traccar.helper.model.PositionUtil;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.reports.common.ReportUtils;
import org.traccar.reports.model.SummaryReportItem;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class SummaryReportProvider {

    private final Config config;
    private final ReportUtils reportUtils;
    private final PermissionsService permissionsService;
    private final Storage storage;

    @Inject
    public SummaryReportProvider(
            Config config, ReportUtils reportUtils, PermissionsService permissionsService, Storage storage) {
        this.config = config;
        this.reportUtils = reportUtils;
        this.permissionsService = permissionsService;
        this.storage = storage;
    }

    private Position getEdgePosition(long deviceId, Date from, Date to, boolean end) throws StorageException {
        return storage.getObject(Position.class, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("deviceId", deviceId),
                        new Condition.Between("fixTime", "from", from, "to", to)),
                new Order("fixTime", end, 1)));
    }

    private Collection<SummaryReportItem> calculateDeviceResult(
            Device device, Date from, Date to, boolean fast) throws StorageException {

        SummaryReportItem result = new SummaryReportItem();
        result.setDeviceId(device.getId());
        result.setDeviceName(device.getName());

        Position first = null;
        Position last = null;
        if (fast) {
            first = getEdgePosition(device.getId(), from, to, false);
            last = getEdgePosition(device.getId(), from, to, true);
        } else {
            var positions = PositionUtil.getPositions(storage, device.getId(), from, to);
            for (Position position : positions) {
                if (first == null) {
                    first = position;
                }
                if (position.getSpeed() > result.getMaxSpeed()) {
                    result.setMaxSpeed(position.getSpeed());
                }
                last = position;
            }
        }

        if (first != null && last != null) {
            boolean ignoreOdometer = config.getBoolean(Keys.REPORT_IGNORE_ODOMETER);
            result.setDistance(PositionUtil.calculateDistance(first, last, !ignoreOdometer));
            result.setSpentFuel(reportUtils.calculateFuel(first, last));

            long durationMilliseconds;
            if (first.hasAttribute(Position.KEY_HOURS) && last.hasAttribute(Position.KEY_HOURS)) {
                durationMilliseconds = last.getLong(Position.KEY_HOURS) - first.getLong(Position.KEY_HOURS);
                result.setEngineHours(durationMilliseconds);
            } else {
                durationMilliseconds = last.getFixTime().getTime() - first.getFixTime().getTime();
            }

            if (durationMilliseconds > 0) {
                result.setAverageSpeed(UnitsConverter.knotsFromMps(result.getDistance() * 1000 / durationMilliseconds));
            }

            if (!ignoreOdometer
                    && first.getDouble(Position.KEY_ODOMETER) != 0 && last.getDouble(Position.KEY_ODOMETER) != 0) {
                result.setStartOdometer(first.getDouble(Position.KEY_ODOMETER));
                result.setEndOdometer(last.getDouble(Position.KEY_ODOMETER));
            } else {
                result.setStartOdometer(first.getDouble(Position.KEY_TOTAL_DISTANCE));
                result.setEndOdometer(last.getDouble(Position.KEY_TOTAL_DISTANCE));
            }

            result.setStartTime(first.getFixTime());
            result.setEndTime(last.getFixTime());
            return List.of(result);
        }

        return List.of();
    }

    private Collection<SummaryReportItem> calculateDeviceResults(
            Device device, ZonedDateTime from, ZonedDateTime to, boolean daily) throws StorageException {

        boolean fast = Duration.between(from, to).toSeconds() > config.getLong(Keys.REPORT_FAST_THRESHOLD);
        var results = new ArrayList<SummaryReportItem>();
        if (daily) {
            while (from.truncatedTo(ChronoUnit.DAYS).isBefore(to.truncatedTo(ChronoUnit.DAYS))) {
                ZonedDateTime fromDay = from.truncatedTo(ChronoUnit.DAYS);
                ZonedDateTime nextDay = fromDay.plus(1, ChronoUnit.DAYS);
                results.addAll(calculateDeviceResult(
                        device, Date.from(from.toInstant()), Date.from(nextDay.toInstant()), fast));
                from = nextDay;
            }
            results.addAll(calculateDeviceResult(device, Date.from(from.toInstant()), Date.from(to.toInstant()), fast));
        } else {
            results.addAll(calculateDeviceResult(device, Date.from(from.toInstant()), Date.from(to.toInstant()), fast));
        }
        return results;
    }

    public Collection<SummaryReportItem> getObjects(
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to, boolean daily) throws StorageException {
        reportUtils.checkPeriodLimit(from, to);

        var tz = UserUtil.getTimezone(permissionsService.getServer(), permissionsService.getUser(userId)).toZoneId();

        ArrayList<SummaryReportItem> result = new ArrayList<>();
        for (Device device: DeviceUtil.getAccessibleDevices(storage, userId, deviceIds, groupIds)) {
            var deviceResults = calculateDeviceResults(
                    device, from.toInstant().atZone(tz), to.toInstant().atZone(tz), daily);
            for (SummaryReportItem summaryReport : deviceResults) {
                if (summaryReport.getStartTime() != null && summaryReport.getEndTime() != null) {
                    result.add(summaryReport);
                }
            }
        }
        return result;
    }

    public void getExcel(OutputStream outputStream,
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to, boolean daily) throws StorageException, IOException {
        Collection<SummaryReportItem> summaries = getObjects(userId, deviceIds, groupIds, from, to, daily);

        File file = Paths.get(config.getString(Keys.TEMPLATES_ROOT), "export", "summary.xlsx").toFile();
        try (InputStream inputStream = new FileInputStream(file)) {
            var context = reportUtils.initializeContext(userId);
            context.putVar("summaries", summaries);
            context.putVar("from", from);
            context.putVar("to", to);
            JxlsHelper.getInstance().setUseFastFormulaProcessor(false)
                    .processTemplate(inputStream, outputStream, context);
        }
    }
}
