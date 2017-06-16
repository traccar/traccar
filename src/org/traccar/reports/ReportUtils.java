/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 - 2017 Andrey Kunitsyn (andrey@traccar.org)
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

import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.jxls.area.Area;
import org.jxls.builder.xls.XlsCommentAreaBuilder;
import org.jxls.common.CellRef;
import org.jxls.formula.StandardFormulaProcessor;
import org.jxls.transform.Transformer;
import org.jxls.transform.poi.PoiTransformer;
import org.jxls.util.TransformerFactory;
import org.traccar.Context;
import org.traccar.model.Position;
import org.traccar.reports.model.BaseReport;
import org.traccar.reports.model.StopReport;
import org.traccar.reports.model.TripReport;
import org.traccar.reports.model.TripsConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class ReportUtils {

    private ReportUtils() {
    }

    public static String getDistanceUnit(long userId) {
        return (String) Context.getPermissionsManager().lookupPreference(userId, "distanceUnit", "km");
    }

    public static String getSpeedUnit(long userId) {
        return (String) Context.getPermissionsManager().lookupPreference(userId, "speedUnit", "kn");
    }

    public static TimeZone getTimezone(long userId) {
        String timezone = (String) Context.getPermissionsManager().lookupPreference(userId, "timezone", null);
        return timezone != null ? TimeZone.getTimeZone(timezone) : TimeZone.getDefault();
    }

    public static Collection<Long> getDeviceList(Collection<Long> deviceIds, Collection<Long> groupIds) {
        Collection<Long> result = new ArrayList<>();
        result.addAll(deviceIds);
        for (long groupId : groupIds) {
            result.addAll(Context.getPermissionsManager().getGroupDevices(groupId));
        }
        return result;
    }

    public static double calculateDistance(Position firstPosition, Position lastPosition) {
        return calculateDistance(firstPosition, lastPosition, true);
    }

    public static double calculateDistance(Position firstPosition, Position lastPosition, boolean useOdometer) {
        double distance = 0.0;
        double firstOdometer = firstPosition.getDouble(Position.KEY_ODOMETER);
        double lastOdometer = lastPosition.getDouble(Position.KEY_ODOMETER);

        if (useOdometer && (firstOdometer != 0.0 || lastOdometer != 0.0)) {
            distance = lastOdometer - firstOdometer;
        } else if (firstPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)
                && lastPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)) {
            distance = lastPosition.getDouble(Position.KEY_TOTAL_DISTANCE)
                    - firstPosition.getDouble(Position.KEY_TOTAL_DISTANCE);
        }
        return distance;
    }

    public static String calculateFuel(Position firstPosition, Position lastPosition) {

        if (firstPosition.getAttributes().get(Position.KEY_FUEL_LEVEL) != null
                && lastPosition.getAttributes().get(Position.KEY_FUEL_LEVEL) != null) {

            BigDecimal value = new BigDecimal(firstPosition.getDouble(Position.KEY_FUEL_LEVEL)
                    - lastPosition.getDouble(Position.KEY_FUEL_LEVEL));
            return value.setScale(1, RoundingMode.HALF_EVEN).toString();
        }
        return null;
    }

    public static org.jxls.common.Context initializeContext(long userId) {
        org.jxls.common.Context jxlsContext = PoiTransformer.createInitialContext();
        jxlsContext.putVar("distanceUnit", getDistanceUnit(userId));
        jxlsContext.putVar("speedUnit", getSpeedUnit(userId));
        jxlsContext.putVar("webUrl", Context.getVelocityEngine().getProperty("web.url"));
        jxlsContext.putVar("dateTool", new DateTool());
        jxlsContext.putVar("numberTool", new NumberTool());
        jxlsContext.putVar("timezone", getTimezone(userId));
        jxlsContext.putVar("locale", Locale.getDefault());
        jxlsContext.putVar("bracketsRegex", "[\\{\\}\"]");
        return jxlsContext;
    }

    public static void processTemplateWithSheets(InputStream templateStream, OutputStream targetStream,
            org.jxls.common.Context jxlsContext) throws IOException {
        Transformer transformer = TransformerFactory.createTransformer(templateStream, targetStream);
        List<Area> xlsAreas = new XlsCommentAreaBuilder(transformer).build();
        for (Area xlsArea : xlsAreas) {
            xlsArea.applyAt(new CellRef(xlsArea.getStartCellRef().getCellName()), jxlsContext);
            xlsArea.setFormulaProcessor(new StandardFormulaProcessor());
            xlsArea.processFormulas();
        }
        transformer.deleteSheet(xlsAreas.get(0).getStartCellRef().getSheetName());
        transformer.write();
    }

    public static TripsConfig initTripsConfig() {
        return new TripsConfig(
                Context.getConfig().getLong("report.trip.minimalTripDuration", 300) * 1000,
                Context.getConfig().getLong("report.trip.minimalTripDistance", 500),
                Context.getConfig().getLong("report.trip.minimalParkingDuration", 300) * 1000,
                Context.getConfig().getBoolean("report.trip.greedyParking"),
                Context.getConfig().getLong("report.trip.minimalNoDataDuration", 3600) * 1000);
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

        long tripDuration = endTrip.getFixTime().getTime() - startTrip.getFixTime().getTime();
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

        trip.setDistance(calculateDistance(startTrip, endTrip, !ignoreOdometer));
        trip.setDuration(tripDuration);
        trip.setAverageSpeed(speedSum / (endIndex - startIndex));
        trip.setMaxSpeed(speedMax);
        trip.setSpentFuel(calculateFuel(startTrip, endTrip));

        return trip;
    }

    private static StopReport calculateStop(ArrayList<Position> positions, int startIndex, int endIndex) {
        Position startStop = positions.get(startIndex);
        Position endStop = positions.get(endIndex);

        StopReport stop = new StopReport();

        long deviceId = startStop.getDeviceId();
        stop.setDeviceId(deviceId);
        stop.setDeviceName(Context.getIdentityManager().getDeviceById(deviceId).getName());

        stop.setPositionId(startStop.getId());
        stop.setLatitude(startStop.getLatitude());
        stop.setLongitude(startStop.getLongitude());
        stop.setStartTime(startStop.getFixTime());
        stop.setAddress(startStop.getAddress());
        stop.setEndTime(endStop.getFixTime());

        long stopDuration = endStop.getFixTime().getTime() - startStop.getFixTime().getTime();
        stop.setDuration(stopDuration);
        stop.setSpentFuel(calculateFuel(startStop, endStop));

        long engineHours = 0;
        for (int i = startIndex + 1; i <= endIndex; i++) {
            if (positions.get(i).getBoolean(Position.KEY_IGNITION)
                    && positions.get(i - 1).getBoolean(Position.KEY_IGNITION)) {
                engineHours += positions.get(i).getFixTime().getTime() - positions.get(i - 1).getFixTime().getTime();
            }
        }
        stop.setEngineHours(engineHours);

        return stop;

    }

    private static boolean isMoving(ArrayList<Position> positions, int index,
            TripsConfig tripsConfig, double speedThreshold) {
        if (tripsConfig.getMinimalNoDataDuration() > 0 && index < positions.size() - 1
                && positions.get(index + 1).getFixTime().getTime() - positions.get(index).getFixTime().getTime()
                >= tripsConfig.getMinimalNoDataDuration()) {
            return false;
        }
        if (positions.get(index).getAttributes().containsKey(Position.KEY_MOTION)
                && positions.get(index).getAttributes().get(Position.KEY_MOTION) instanceof Boolean) {
            return positions.get(index).getBoolean(Position.KEY_MOTION);
        } else {
            return positions.get(index).getSpeed() > speedThreshold;
        }
    }

    public static Collection<BaseReport> detectTripsAndStops(TripsConfig tripsConfig, boolean ignoreOdometer,
            double speedThreshold, Collection<Position> positionCollection, boolean trips) {

        Collection<BaseReport> result = new ArrayList<>();

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
                isMoving = isMoving(positions, i, tripsConfig, speedThreshold);
                isLast = i == positions.size() - 1;

                if ((isMoving || isLast) && startParkingIndex != -1) {
                    if (!skipped || previousEndParkingIndex == 0) {
                        previousEndParkingIndex = endParkingIndex;
                    }
                    endParkingIndex = i;
                }
                if (!isMoving && startParkingIndex == -1) {
                    if (tripsConfig.getGreedyParking()) {
                        long tripDuration = positions.get(i).getFixTime().getTime()
                                - positions.get(endParkingIndex).getFixTime().getTime();
                        double tripDistance = ReportUtils.calculateDistance(positions.get(endParkingIndex),
                                positions.get(i), false);
                        tripFiltered = tripDuration < tripsConfig.getMinimalTripDuration()
                                && tripDistance < tripsConfig.getMinimalTripDistance();
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
                        tripFiltered = tripDuration < tripsConfig.getMinimalTripDuration()
                                && tripDistance < tripsConfig.getMinimalTripDistance();
                        startParkingIndex = i;
                    }
                }
                if (startParkingIndex != -1 && (endParkingIndex > startParkingIndex || isLast)) {
                    long parkingDuration = positions.get(endParkingIndex).getFixTime().getTime()
                            - positions.get(startParkingIndex).getFixTime().getTime();
                    if ((parkingDuration >= tripsConfig.getMinimalParkingDuration() || isLast)
                            && previousEndParkingIndex < startParkingIndex) {
                        if (!tripFiltered) {
                            if (trips) {
                                result.add(calculateTrip(
                                        positions, previousEndParkingIndex, startParkingIndex, ignoreOdometer));
                            } else {
                                if (result.isEmpty() && previousEndParkingIndex > previousStartParkingIndex) {
                                    long previousParkingDuration = positions.get(previousEndParkingIndex)
                                            .getFixTime().getTime() - positions.get(previousStartParkingIndex)
                                            .getFixTime().getTime();
                                    if (previousParkingDuration >= tripsConfig.getMinimalParkingDuration()) {
                                        result.add(calculateStop(positions, previousStartParkingIndex,
                                                previousEndParkingIndex));
                                    }
                                }
                                result.add(calculateStop(positions, startParkingIndex, isLast ? i : endParkingIndex));
                            }
                        }
                        previousEndParkingIndex = endParkingIndex;
                        skipped = false;
                    } else {
                        skipped = true;
                    }
                    startParkingIndex = -1;
                }
            }
            if (result.isEmpty() && !trips) {
                int end = isMoving && !tripsConfig.getGreedyParking()
                        ? Math.max(endParkingIndex, previousEndParkingIndex) : positions.size() - 1;
                long parkingDuration = positions.get(end).getFixTime().getTime()
                        - positions.get(previousStartParkingIndex).getFixTime().getTime();
                if (parkingDuration >= tripsConfig.getMinimalParkingDuration()) {
                    result.add(calculateStop(positions, previousStartParkingIndex, end));
                }
            }
        }

        return result;
    }
}
