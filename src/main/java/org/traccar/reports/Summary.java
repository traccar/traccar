/*
 * Copyright 2016 - 2020 Anton Tananaev (anton@traccar.org)
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.jxls.util.JxlsHelper;
import org.traccar.Context;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.reports.model.SummaryReport;
import org.traccar.storage.StorageException;

public final class Summary {

    private Summary() {
    }

    private static SummaryReport calculateSummaryResult(long deviceId, Collection<Position> positions) {
        SummaryReport result = new SummaryReport();
        result.setDeviceId(deviceId);
        result.setDeviceName(Context.getIdentityManager().getById(deviceId).getName());
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
            boolean ignoreOdometer = Context.getDeviceManager()
                    .lookupAttributeBoolean(deviceId, "report.ignoreOdometer", false, false, true);
            result.setDistance(ReportUtils.calculateDistance(firstPosition, previousPosition, !ignoreOdometer));
            result.setSpentFuel(ReportUtils.calculateFuel(firstPosition, previousPosition));

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

    private static int getDay(long userId, Date date) {
        Calendar calendar = Calendar.getInstance(ReportUtils.getTimezone(userId));
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    private static Collection<SummaryReport> calculateSummaryResults(
            long userId, long deviceId, Date from, Date to, boolean daily) throws StorageException {

        ArrayList<Position> positions = new ArrayList<>(Context.getDataManager().getPositions(deviceId, from, to));

        ArrayList<SummaryReport> results = new ArrayList<>();
        if (daily && !positions.isEmpty()) {
            int startIndex = 0;
            int startDay = getDay(userId, positions.iterator().next().getFixTime());
            for (int i = 0; i < positions.size(); i++) {
                int currentDay = getDay(userId, positions.get(i).getFixTime());
                if (currentDay != startDay) {
                    results.add(calculateSummaryResult(deviceId, positions.subList(startIndex, i)));
                    startIndex = i;
                    startDay = currentDay;
                }
            }
            results.add(calculateSummaryResult(deviceId, positions.subList(startIndex, positions.size())));
        } else {
            results.add(calculateSummaryResult(deviceId, positions));
        }

        return results;
    }

    public static Collection<SummaryReport> getObjects(long userId, Collection<Long> deviceIds,
            Collection<Long> groupIds, Date from, Date to, boolean daily) throws StorageException {
        ReportUtils.checkPeriodLimit(from, to);
        ArrayList<SummaryReport> result = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            result.addAll(calculateSummaryResults(userId, deviceId, from, to, daily));
        }
        return result;
    }

    public static void getExcel(OutputStream outputStream,
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to, boolean daily) throws StorageException, IOException {
        ReportUtils.checkPeriodLimit(from, to);
        Collection<SummaryReport> summaries = getObjects(userId, deviceIds, groupIds, from, to, daily);
        String templatePath = Context.getConfig().getString("report.templatesPath",
                "templates/export/");
        try (InputStream inputStream = new FileInputStream(templatePath + "/summary.xlsx")) {
            org.jxls.common.Context jxlsContext = ReportUtils.initializeContext(userId);
            jxlsContext.putVar("summaries", summaries);
            jxlsContext.putVar("from", from);
            jxlsContext.putVar("to", to);
            JxlsHelper.getInstance().setUseFastFormulaProcessor(false)
                    .processTemplate(inputStream, outputStream, jxlsContext);
        }
    }
}
