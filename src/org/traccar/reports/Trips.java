/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
 * Copyright 2016 Andrey Kunitsyn (abyss@fox5.ru)
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArrayBuilder;

import org.traccar.Context;
import org.traccar.model.Position;
import org.traccar.reports.model.TripReport;
import org.traccar.web.CsvBuilder;
import org.traccar.web.JsonConverter;

public final class Trips {

    private Trips() {
    }

    private static TripReport calculateTrip(ArrayList<Position> positions, int startIndex, int endIndex) {
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
        trip.setStartTime(startTrip.getFixTime());
        trip.setStartAddress(startTrip.getAddress());
        trip.setEndPositionId(endTrip.getId());
        trip.setEndTime(endTrip.getFixTime());
        trip.setEndAddress(endTrip.getAddress());
        boolean ignoreOdometer = Context.getDeviceManager()
                .lookupConfigBoolean(deviceId, "report.ignoreOdometer", false);
        trip.setDistance(ReportUtils.calculateDistance(startTrip, endTrip, !ignoreOdometer));
        trip.setDuration(tripDuration);
        trip.setAverageSpeed(speedSum / (endIndex - startIndex));
        trip.setMaxSpeed(speedMax);
        trip.setSpentFuel(ReportUtils.calculateFuel(startTrip, endTrip));

        return trip;
    }

    private static Collection<TripReport> detectTrips(long deviceId, Date from, Date to) throws SQLException {
        double speedThreshold = Context.getConfig().getDouble("event.motion.speedThreshold", 0.01);
        long minimalTripDuration = Context.getConfig().getLong("report.trip.minimalTripDuration", 300) * 1000;
        double minimalTripDistance = Context.getConfig().getLong("report.trip.minimalTripDistance", 500);
        long minimalParkingDuration = Context.getConfig().getLong("report.trip.minimalParkingDuration", 300) * 1000;
        boolean greedyParking = Context.getConfig().getBoolean("report.trip.greedyParking");
        Collection<TripReport> result = new ArrayList<>();

        ArrayList<Position> positions = new ArrayList<>(Context.getDataManager().getPositions(deviceId, from, to));
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
                            result.add(calculateTrip(positions, previousEndParkingIndex, startParkingIndex));
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

    public static String getJson(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException {
        JsonArrayBuilder json = Json.createArrayBuilder();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            for (TripReport tripReport : detectTrips(deviceId, from, to)) {
                json.add(JsonConverter.objectToJson(tripReport));
            }
        }
        return json.build().toString();
    }

    public static String getCsv(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException {
        CsvBuilder csv = new CsvBuilder();
        csv.addHeaderLine(new TripReport());
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            csv.addArray(detectTrips(deviceId, from, to));
        }
        return csv.build();
    }
}
