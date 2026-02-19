/*
 * Copyright 2016 - 2025 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.model.AttributeUtil;
import org.traccar.helper.model.DeviceUtil;
import org.traccar.helper.model.PositionUtil;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.reports.common.ReportUtils;
import org.traccar.reports.common.TripsConfig;
import org.traccar.reports.model.SummaryReportItem;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;

import jakarta.inject.Inject;
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

    /**
     * Calculates idle time for a device over a period based on position data.
     *
     * Idle time is defined as periods when:
     * - Engine is running (RPM > 0 OR ignition = true)
     * - Vehicle is not moving (motion = false)
     * - Duration meets minimum threshold (configurable, default 60 seconds)
     *
     * This follows industry standards (e.g., Geotab) for idle time detection.
     *
     * @param deviceId Device identifier
     * @param from Start time of period
     * @param to End time of period
     * @return Total idle time in milliseconds
     * @throws StorageException if database access fails
     */
    private long calculateIdleTime(long deviceId, Date from, Date to) throws StorageException {

        final long minIdleDuration = config.getLong(Keys.REPORT_IDLE_MIN_DURATION);
        final long maxGapDuration = config.getLong(Keys.REPORT_IDLE_MAX_GAP);
        final double idleRpmThreshold = config.getDouble(Keys.REPORT_IDLE_RPM_THRESHOLD);

        long idleTime = 0;
        var positions = PositionUtil.getPositions(storage, deviceId, from, to);

        if (positions.isEmpty()) {
            return 0;
        }

        Position previousPosition = null;
        boolean wasIdle = false;
        long idleStartTime = 0;

        for (Position position : positions) {
            boolean isIdle = false;


            Boolean ignition = position.getBoolean(Position.KEY_IGNITION);
            Boolean motion = position.getBoolean(Position.KEY_MOTION);
            Double rpm = position.getDouble(Position.KEY_RPM);

            // Enhanced validation: engine must be running (RPM > threshold or ignition=true)
            boolean engineRunning = false;
            if (rpm != null && rpm > idleRpmThreshold) {
                engineRunning = true;
            } else if (ignition != null && ignition) {
                engineRunning = true;
            }


            if (engineRunning && motion != null && !motion) {
                isIdle = true;
            }

            if (previousPosition != null) {
                long currentTime = position.getFixTime().getTime();
                long duration = currentTime - previousPosition.getFixTime().getTime();

                if (wasIdle && isIdle) {

                    if (duration > 0 && duration < maxGapDuration) {
                        idleTime += duration;
                    }
                } else if (wasIdle && !isIdle) {

                    long totalIdleDuration = currentTime - idleStartTime;
                    if (totalIdleDuration < minIdleDuration) {
                        // Remove idle time that doesn't meet minimum threshold
                        idleTime = Math.max(0, idleTime - totalIdleDuration);
                    }
                } else if (!wasIdle && isIdle) {
                    // Start new idle period
                    idleStartTime = currentTime;
                }
            }

            previousPosition = position;
            wasIdle = isIdle;
        }


        if (wasIdle && previousPosition != null) {
            long totalIdleDuration = previousPosition.getFixTime().getTime() - idleStartTime;
            if (totalIdleDuration < minIdleDuration) {
                // Remove idle time that doesn't meet minimum threshold
                idleTime = Math.max(0, idleTime - totalIdleDuration);
            }
        }

        // Ensure idle time is never negative
        return Math.max(0, idleTime);
    }

    private Collection<SummaryReportItem> calculateDeviceResult(
            Device device, Date from, Date to, boolean fast) throws StorageException {

        SummaryReportItem result = new SummaryReportItem();
        result.setDeviceId(device.getId());
        result.setDeviceName(device.getName());

        Position first = null;
        Position last = null;
        long idleTime = 0;

        if (fast) {
            first = PositionUtil.getEdgePosition(storage, device.getId(), from, to, false);
            last = PositionUtil.getEdgePosition(storage, device.getId(), from, to, true);
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
            TripsConfig tripsConfig = new TripsConfig(
                    new AttributeUtil.StorageProvider(config, storage, permissionsService, device));
            boolean ignoreOdometer = tripsConfig.getIgnoreOdometer();
            result.setDistance(PositionUtil.calculateDistance(first, last, !ignoreOdometer));
            result.setSpentFuel(reportUtils.calculateFuel(first, last, device));

            if (first.hasAttribute(Position.KEY_HOURS) && last.hasAttribute(Position.KEY_HOURS)) {
                result.setStartHours(first.getLong(Position.KEY_HOURS));
                result.setEndHours(last.getLong(Position.KEY_HOURS));
                long engineHours = result.getEngineHours();
                if (engineHours > 0) {
                    result.setAverageSpeed(UnitsConverter.knotsFromMps(result.getDistance() * 1000 / engineHours));
                }
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


            if (!fast) {
                idleTime = calculateIdleTime(device.getId(), from, to);
            }
            result.setIdleTime(idleTime);

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
                ZonedDateTime nextDay = fromDay.plusDays(1);
                results.addAll(calculateDeviceResult(
                        device, Date.from(from.toInstant()), Date.from(nextDay.toInstant()), fast));
                from = nextDay;
            }
        }
        results.addAll(calculateDeviceResult(device, Date.from(from.toInstant()), Date.from(to.toInstant()), fast));
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