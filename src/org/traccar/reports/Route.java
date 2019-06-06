/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.util.WorkbookUtil;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.reports.model.DeviceReport;

public final class Route {

    private Route() {
    }

    public static Collection<Position> getObjects(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException {

        ReportUtils.checkPeriodLimit(from, to);
        ArrayList<Position> result = new ArrayList<>();

        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<Position> routePositions = Context.getDataManager().getPositionsForRoute(deviceId, from, to);
            List<Position> filteredRoutePositions =
                    routePositions.stream().filter(Route::positionFilter).collect(Collectors.toList());

            if (filteredRoutePositions.size() == 0 && routePositions.size() > 0) {
                List<Position> originalPositions = routePositions.stream().collect(Collectors.toList());
                int originalListSize = originalPositions.size();
                filteredRoutePositions.add(originalPositions.get(0));
                filteredRoutePositions.add(originalPositions.get(originalListSize - 1));
            }

            result.addAll(filteredRoutePositions);

        }
        return result;
    }

    private static boolean positionFilter(Position p) {
        return p.getAttributes().keySet().contains("distance")
                && (double) p.getAttributes().get("distance") > 50;

    }

    public static Collection<Position> getFuelObjects(long userId, Collection<Long> deviceIds,
                                                      Collection<Long> groupIds,
                                                      Date from, Date to) throws SQLException {
        ReportUtils.checkPeriodLimit(from, to);
        ArrayList<Position> fuelOnlyPositions = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            fuelOnlyPositions.addAll(Context.getDataManager().getPositionsForFuel(deviceId, from, to));
        }

        String deviceIdsString = deviceIds.stream().map(d -> d.toString()).collect(Collectors.joining(","));
        Log.info("[FUEL_INFO_REPORT] fuelOnlyPositions "
                 + fuelOnlyPositions.size() + " for deviceId = " + deviceIdsString);

        ArrayList<Position> result = new ArrayList<>();

        if (fuelOnlyPositions.size() > 1) {
            try {
                for (int positionIndex = 1; positionIndex < fuelOnlyPositions.size(); positionIndex++) {
                    Position previous = fuelOnlyPositions.get(positionIndex - 1);
                    Position current = fuelOnlyPositions.get(positionIndex);

                    if (current.getAttributes().containsKey("fuel")
                        && previous.getAttributes().containsKey("fuel")
                        && !current.getBoolean("fuelIsOutlier")
                        && !previous.getBoolean("fuelIsOutlier")) {

                        double previousFuel = (double) previous.getAttributes().get("fuel");
                        double currentFuel = (double) current.getAttributes().get("fuel");

                        double volumeChangeFromPrevious = Math.abs(previousFuel - currentFuel);
                        if (volumeChangeFromPrevious > 0.15) {
                            if (!result.contains(previous)) {
                                result.add(previous);
                            }
                            result.add(current);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (result.size() == 0 && fuelOnlyPositions.size() > 0 ) {
            result.add(fuelOnlyPositions.get(0));
            result.add(fuelOnlyPositions.get(fuelOnlyPositions.size() - 1));
        }

        Log.info("[FUEL_INFO_REPORT] Returning " + result.size()
                 + " positions for fuel info report for deviceId = " + deviceIdsString);
        return result;
    }

    public static Collection<Position> getSummaryObjects(long userId, Collection<Long> deviceIds,
                                                         Collection<Long> groupIds,
                                                         Date from,
                                                         Date to) throws SQLException {
        ReportUtils.checkPeriodLimit(from, to);
        ArrayList<Position> result = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);

            result.addAll(Context.getDataManager().getPositionsForSummary(deviceId, from, to));
        }
        return result;
    }

    public static void getExcel(OutputStream outputStream,
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException, IOException {
        ReportUtils.checkPeriodLimit(from, to);
        ArrayList<DeviceReport> devicesRoutes = new ArrayList<>();
        ArrayList<String> sheetNames = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<Position> positions = Context.getDataManager()
                    .getPositions(deviceId, from, to);
            DeviceReport deviceRoutes = new DeviceReport();
            Device device = Context.getIdentityManager().getById(deviceId);
            deviceRoutes.setDeviceName(device.getName());
            sheetNames.add(WorkbookUtil.createSafeSheetName(deviceRoutes.getDeviceName()));
            if (device.getGroupId() != 0) {
                Group group = Context.getGroupsManager().getById(device.getGroupId());
                if (group != null) {
                    deviceRoutes.setGroupName(group.getName());
                }
            }
            deviceRoutes.setObjects(positions);
            devicesRoutes.add(deviceRoutes);
        }
        String templatePath = Context.getConfig().getString("report.templatesPath",
                "templates/export/");
        try (InputStream inputStream = new FileInputStream(templatePath + "/route.xlsx")) {
            org.jxls.common.Context jxlsContext = ReportUtils.initializeContext(userId);
            jxlsContext.putVar("devices", devicesRoutes);
            jxlsContext.putVar("sheetNames", sheetNames);
            jxlsContext.putVar("from", from);
            jxlsContext.putVar("to", to);
            ReportUtils.processTemplateWithSheets(inputStream, outputStream, jxlsContext);
        }
    }
}
