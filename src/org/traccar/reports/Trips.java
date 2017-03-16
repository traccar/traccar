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

import org.apache.poi.ss.util.WorkbookUtil;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.reports.model.DeviceReport;
import org.traccar.reports.model.TripReport;

public final class Trips {

    private Trips() {
    }

    private static TripReport calculateTrip(
            ArrayList<Position> positions, int startIndex, int endIndex, boolean ignoreOdometer) {
        Position startTrip = positions.get(startIndex);
        Position endTrip = positions.get(endIndex);

        double speedMax = 0.0;
        double speedSum = 0.0;
        for (int i = startIndex; i <= endIndex; i++) {
            double speed = positions.get(i).getSpeed();
            speedSum += speed;
            if (speed > speedMax) {
                speedMax = speed;
            }
        }

        TripReport trip = new TripReport();

        long tripDuration = endTrip.getFixTime().getTime() - positions.get(startIndex).getFixTime().getTime();
        long deviceId = startTrip.getDeviceId();
        trip.setDeviceId(deviceId);
        trip.setDeviceName(Context.getIdentityManager().getDeviceById(deviceId).getName());

        trip.setStartPositionId(startTrip.getId());
        trip.setStartLat(startTrip.getLatitude());
        trip.setStartLon(startTrip.getLongitude());
        trip.setStartTime(startTrip.getFixTime());
        trip.setStartAddress(startTrip.getAddress());

        trip.setEndPositionId(endTrip.getId());
        trip.setEndLat(endTrip.getLatitude());
        trip.setEndLon(endTrip.getLongitude());
        trip.setEndTime(endTrip.getFixTime());
        trip.setEndAddress(endTrip.getAddress());

        trip.setDistance(ReportUtils.calculateDistance(startTrip, endTrip, !ignoreOdometer));
        trip.setDuration(tripDuration);
        trip.setAverageSpeed(speedSum / (endIndex - startIndex));
        trip.setMaxSpeed(speedMax);
        trip.setSpentFuel(ReportUtils.calculateFuel(startTrip, endTrip));

        return trip;
    }

    protected static Collection<TripReport> detectTrips(
            double speedThreshold, double minimalTripDistance,
            long minimalTripDuration, long minimalParkingDuration, boolean greedyParking, boolean ignoreOdometer,
            Collection<Position> positionCollection) {

        Collection<TripReport> result = new ArrayList<>();

        ArrayList<Position> positions = new ArrayList<>(positionCollection);
        if (positions != null && !positions.isEmpty()) {
            int previousStartParkingIndex = 0;
            int startParkingIndex = -1;
            int previousEndParkingIndex = 0;
            int endParkingIndex = 0;

            boolean isMoving = false;
            boolean isLast = false;
            boolean skipped = false;
            boolean tripFiltered = false;

            for (int i = 0; i < positions.size(); i++) {
                isMoving = positions.get(i).getSpeed() > speedThreshold;
                isLast = i == positions.size() - 1;

                if ((isMoving || isLast) && startParkingIndex != -1) {
                    if (!skipped || previousEndParkingIndex == 0) {
                        previousEndParkingIndex = endParkingIndex;
                    }
                    endParkingIndex = i;
                }
                if (!isMoving && startParkingIndex == -1) {
                    if (greedyParking) {
                        long tripDuration = positions.get(i).getFixTime().getTime()
                                - positions.get(endParkingIndex).getFixTime().getTime();
                        double tripDistance = ReportUtils.calculateDistance(positions.get(endParkingIndex),
                                positions.get(i), false);
                        tripFiltered = tripDuration < minimalTripDuration && tripDistance < minimalTripDistance;
                        if (tripFiltered) {
                            startParkingIndex = previousStartParkingIndex;
                            endParkingIndex = previousEndParkingIndex;
                            tripFiltered = false;
                        } else {
                            previousStartParkingIndex = i;
                            startParkingIndex = i;
                        }
                    } else {
                        long tripDuration = positions.get(i).getFixTime().getTime()
                                - positions.get(previousEndParkingIndex).getFixTime().getTime();
                        double tripDistance = ReportUtils.calculateDistance(positions.get(previousEndParkingIndex),
                                positions.get(i), false);
                        tripFiltered = tripDuration < minimalTripDuration && tripDistance < minimalTripDistance;
                        startParkingIndex = i;
                    }
                }
                if (startParkingIndex != -1 && (endParkingIndex > startParkingIndex || isLast)) {
                    long parkingDuration = positions.get(endParkingIndex).getFixTime().getTime()
                            - positions.get(startParkingIndex).getFixTime().getTime();
                    if ((parkingDuration >= minimalParkingDuration || isLast)
                            && previousEndParkingIndex < startParkingIndex) {
                        if (!tripFiltered) {
                            result.add(calculateTrip(
                                    positions, previousEndParkingIndex, startParkingIndex, ignoreOdometer));
                        }
                        previousEndParkingIndex = endParkingIndex;
                        skipped = false;
                    } else {
                        skipped = true;
                    }
                    startParkingIndex = -1;
                }
            }
        }

        return result;
    }

    private static Collection<TripReport> detectTrips(long deviceId, Date from, Date to) throws SQLException {
        double speedThreshold = Context.getConfig().getDouble("event.motion.speedThreshold", 0.01);
        long minimalTripDuration = Context.getConfig().getLong("report.trip.minimalTripDuration", 300) * 1000;
        double minimalTripDistance = Context.getConfig().getLong("report.trip.minimalTripDistance", 500);
        long minimalParkingDuration = Context.getConfig().getLong("report.trip.minimalParkingDuration", 300) * 1000;
        boolean greedyParking = Context.getConfig().getBoolean("report.trip.greedyParking");

        boolean ignoreOdometer = Context.getDeviceManager()
                .lookupAttributeBoolean(deviceId, "report.ignoreOdometer", false, true);

        return detectTrips(
                speedThreshold, minimalTripDistance, minimalTripDuration,
                minimalParkingDuration, greedyParking, ignoreOdometer,
                Context.getDataManager().getPositions(deviceId, from, to));
    }

    public static Collection<TripReport> getObjects(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException {
        ArrayList<TripReport> result = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            result.addAll(detectTrips(deviceId, from, to));
        }
        return result;
    }

    public static void getExcel(OutputStream outputStream,
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException, IOException {
        ArrayList<DeviceReport> devicesTrips = new ArrayList<>();
        ArrayList<String> sheetNames = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<TripReport> trips = detectTrips(deviceId, from, to);
            DeviceReport deviceTrips = new DeviceReport();
            Device device = Context.getIdentityManager().getDeviceById(deviceId);
            deviceTrips.setDeviceName(device.getName());
            sheetNames.add(WorkbookUtil.createSafeSheetName(deviceTrips.getDeviceName()));
            if (device.getGroupId() != 0) {
                Group group = Context.getDeviceManager().getGroupById(device.getGroupId());
                if (group != null) {
                    deviceTrips.setGroupName(group.getName());
                }
            }
            deviceTrips.setObjects(trips);
            devicesTrips.add(deviceTrips);
        }
        String templatePath = Context.getConfig().getString("report.templatesPath",
                "templates/export/");
        try (InputStream inputStream = new FileInputStream(templatePath + "/trips.xlsx")) {
            org.jxls.common.Context jxlsContext = ReportUtils.initializeContext(userId);
            jxlsContext.putVar("devices", devicesTrips);
            jxlsContext.putVar("sheetNames", sheetNames);
            jxlsContext.putVar("from", from);
            jxlsContext.putVar("to", to);
            ReportUtils.processTemplateWithSheets(inputStream, outputStream, jxlsContext);
        }
    }

}
