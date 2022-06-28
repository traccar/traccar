/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.model.PositionUtil;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.reports.common.ReportUtils;
import org.traccar.reports.model.SummaryReportItem;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

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

    private SummaryReportItem calculateSummaryResult(Device device, Collection<Position> positions) {
        SummaryReportItem result = new SummaryReportItem();
        result.setDeviceId(device.getId());
        result.setDeviceName(device.getName());
        if (positions != null && !positions.isEmpty()) {
            Position firstPosition = null;
            Position previousPosition = null;
            for (Position position : positions) {
                if (firstPosition == null) {
                    firstPosition = position;
                }
                previousPosition = position;
                if (position.getSpeed() > result.getMaxSpeed()) {
                    result.setMaxSpeed(position.getSpeed());
                }
            }
            boolean ignoreOdometer = config.getBoolean(Keys.REPORT_IGNORE_ODOMETER);
            result.setDistance(PositionUtil.calculateDistance(firstPosition, previousPosition, !ignoreOdometer));
            result.setSpentFuel(reportUtils.calculateFuel(firstPosition, previousPosition));

            long durationMilliseconds;
            if (firstPosition.getAttributes().containsKey(Position.KEY_HOURS)
                    && previousPosition.getAttributes().containsKey(Position.KEY_HOURS)) {
                durationMilliseconds =
                        previousPosition.getLong(Position.KEY_HOURS) - firstPosition.getLong(Position.KEY_HOURS);
                result.setEngineHours(durationMilliseconds);
            } else {
                durationMilliseconds =
                        previousPosition.getFixTime().getTime() - firstPosition.getFixTime().getTime();
            }

            if (durationMilliseconds > 0) {
                result.setAverageSpeed(
                        UnitsConverter.knotsFromMps(result.getDistance() * 1000 / durationMilliseconds));
            }

            if (!ignoreOdometer
                    && firstPosition.getDouble(Position.KEY_ODOMETER) != 0
                    && previousPosition.getDouble(Position.KEY_ODOMETER) != 0) {
                result.setStartOdometer(firstPosition.getDouble(Position.KEY_ODOMETER));
                result.setEndOdometer(previousPosition.getDouble(Position.KEY_ODOMETER));
            } else {
                result.setStartOdometer(firstPosition.getDouble(Position.KEY_TOTAL_DISTANCE));
                result.setEndOdometer(previousPosition.getDouble(Position.KEY_TOTAL_DISTANCE));
            }

            result.setStartTime(firstPosition.getFixTime());
            result.setEndTime(previousPosition.getFixTime());
        }
        return result;
    }

    private int getDay(long userId, Date date) throws StorageException {
        Calendar calendar = Calendar.getInstance(UserUtil.getTimezone(
                permissionsService.getServer(), permissionsService.getUser(userId)));
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    private Collection<SummaryReportItem> calculateSummaryResults(
            long userId, Device device, Date from, Date to, boolean daily) throws StorageException {

        var positions = PositionUtil.getPositions(storage, device.getId(), from, to);
        var results = new ArrayList<SummaryReportItem>();
        if (daily && !positions.isEmpty()) {
            int startIndex = 0;
            int startDay = getDay(userId, positions.iterator().next().getFixTime());
            for (int i = 0; i < positions.size(); i++) {
                int currentDay = getDay(userId, positions.get(i).getFixTime());
                if (currentDay != startDay) {
                    results.add(calculateSummaryResult(device, positions.subList(startIndex, i)));
                    startIndex = i;
                    startDay = currentDay;
                }
            }
            results.add(calculateSummaryResult(device, positions.subList(startIndex, positions.size())));
        } else {
            results.add(calculateSummaryResult(device, positions));
        }

        return results;
    }

    public Collection<SummaryReportItem> getObjects(
            long userId, Collection<Long> deviceIds,
            Collection<Long> groupIds, Date from, Date to, boolean daily) throws StorageException {
        reportUtils.checkPeriodLimit(from, to);

        ArrayList<SummaryReportItem> result = new ArrayList<>();
        for (Device device: reportUtils.getAccessibleDevices(userId, deviceIds, groupIds)) {
            Collection<SummaryReportItem> deviceResults = calculateSummaryResults(userId, device, from, to, daily);
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
