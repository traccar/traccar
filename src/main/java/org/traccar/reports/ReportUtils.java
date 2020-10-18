/*
 * Copyright 2016 - 2020 Anton Tananaev (anton@traccar.org)
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
import org.traccar.database.DeviceManager;
import org.traccar.database.IdentityManager;
import org.traccar.handler.events.MotionEventHandler;
import org.traccar.model.DeviceState;
import org.traccar.model.Driver;
import org.traccar.model.Event;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public final class ReportUtils {

    private ReportUtils() {
    }

    public static void checkPeriodLimit(Date from, Date to) {
        long limit = Context.getConfig().getLong("report.periodLimit") * 1000;
        if (limit > 0 && to.getTime() - from.getTime() > limit) {
            throw new IllegalArgumentException("Time period exceeds the limit");
        }
    }

    public static String getDistanceUnit(long userId) {
        return (String) Context.getPermissionsManager().lookupAttribute(userId, "distanceUnit", "km");
    }

    public static String getSpeedUnit(long userId) {
        return (String) Context.getPermissionsManager().lookupAttribute(userId, "speedUnit", "kn");
    }

    public static String getVolumeUnit(long userId) {
        return (String) Context.getPermissionsManager().lookupAttribute(userId, "volumeUnit", "ltr");
    }

    public static TimeZone getTimezone(long userId) {
        String timezone = (String) Context.getPermissionsManager().lookupAttribute(userId, "timezone", null);
        return timezone != null ? TimeZone.getTimeZone(timezone) : TimeZone.getDefault();
    }

    public static Collection<Long> getDeviceList(Collection<Long> deviceIds, Collection<Long> groupIds) {
        Collection<Long> result = new ArrayList<>(deviceIds);
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

        if (useOdometer && firstOdometer != 0.0 && lastOdometer != 0.0) {
            distance = lastOdometer - firstOdometer;
        } else if (firstPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)
                && lastPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)) {
            distance = lastPosition.getDouble(Position.KEY_TOTAL_DISTANCE)
                    - firstPosition.getDouble(Position.KEY_TOTAL_DISTANCE);
        }
        return distance;
    }

    public static double calculateFuel(Position firstPosition, Position lastPosition) {

        if (firstPosition.getAttributes().get(Position.KEY_FUEL_LEVEL) != null
                && lastPosition.getAttributes().get(Position.KEY_FUEL_LEVEL) != null) {

            BigDecimal value = BigDecimal.valueOf(firstPosition.getDouble(Position.KEY_FUEL_LEVEL)
                    - lastPosition.getDouble(Position.KEY_FUEL_LEVEL));
            return value.setScale(1, RoundingMode.HALF_EVEN).doubleValue();
        }
        return 0;
    }

    public static String findDriver(Position firstPosition, Position lastPosition) {
        if (firstPosition.getAttributes().containsKey(Position.KEY_DRIVER_UNIQUE_ID)) {
            return firstPosition.getString(Position.KEY_DRIVER_UNIQUE_ID);
        } else if (lastPosition.getAttributes().containsKey(Position.KEY_DRIVER_UNIQUE_ID)) {
            return lastPosition.getString(Position.KEY_DRIVER_UNIQUE_ID);
        }
        return null;
    }

    public static String findDriverName(String driverUniqueId) {
        if (driverUniqueId != null && Context.getDriversManager() != null) {
            Driver driver = Context.getDriversManager().getDriverByUniqueId(driverUniqueId);
            if (driver != null) {
                return driver.getName();
            }
        }
        return null;
    }

    public static org.jxls.common.Context initializeContext(long userId) {
        org.jxls.common.Context jxlsContext = PoiTransformer.createInitialContext();
        jxlsContext.putVar("distanceUnit", getDistanceUnit(userId));
        jxlsContext.putVar("speedUnit", getSpeedUnit(userId));
        jxlsContext.putVar("volumeUnit", getVolumeUnit(userId));
        jxlsContext.putVar("webUrl", Context.getVelocityEngine().getProperty("web.url"));
        jxlsContext.putVar("dateTool", new DateTool());
        jxlsContext.putVar("numberTool", new NumberTool());
        jxlsContext.putVar("timezone", getTimezone(userId));
        jxlsContext.putVar("locale", Locale.getDefault());
        jxlsContext.putVar("bracketsRegex", "[\\{\\}\"]");
        return jxlsContext;
    }

    public static void processTemplateWithSheets(
            InputStream templateStream, OutputStream targetStream,
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
        trip.setDeviceName(Context.getIdentityManager().getById(deviceId).getName());

        trip.setStartPositionId(startTrip.getId());
        trip.setStartLat(startTrip.getLatitude());
        trip.setStartLon(startTrip.getLongitude());
        trip.setStartTime(startTrip.getFixTime());
        String startAddress = startTrip.getAddress();
        if (startAddress == null && Context.getGeocoder() != null
                && Context.getConfig().getBoolean("geocoder.onRequest")) {
            startAddress = Context.getGeocoder().getAddress(startTrip.getLatitude(), startTrip.getLongitude(), null);
        }
        trip.setStartAddress(startAddress);

        trip.setEndPositionId(endTrip.getId());
        trip.setEndLat(endTrip.getLatitude());
        trip.setEndLon(endTrip.getLongitude());
        trip.setEndTime(endTrip.getFixTime());
        String endAddress = endTrip.getAddress();
        if (endAddress == null && Context.getGeocoder() != null
                && Context.getConfig().getBoolean("geocoder.onRequest")) {
            endAddress = Context.getGeocoder().getAddress(endTrip.getLatitude(), endTrip.getLongitude(), null);
        }
        trip.setEndAddress(endAddress);

        trip.setDistance(calculateDistance(startTrip, endTrip, !ignoreOdometer));
        trip.setDuration(tripDuration);
        trip.setAverageSpeed(speedSum / (endIndex - startIndex));
        trip.setMaxSpeed(speedMax);
        trip.setSpentFuel(calculateFuel(startTrip, endTrip));

        trip.setDriverUniqueId(findDriver(startTrip, endTrip));
        trip.setDriverName(findDriverName(trip.getDriverUniqueId()));

        if (!ignoreOdometer
                && startTrip.getDouble(Position.KEY_ODOMETER) != 0
                && endTrip.getDouble(Position.KEY_ODOMETER) != 0) {
            trip.setStartOdometer(startTrip.getDouble(Position.KEY_ODOMETER));
            trip.setEndOdometer(endTrip.getDouble(Position.KEY_ODOMETER));
        } else {
            trip.setStartOdometer(startTrip.getDouble(Position.KEY_TOTAL_DISTANCE));
            trip.setEndOdometer(endTrip.getDouble(Position.KEY_TOTAL_DISTANCE));
        }

        return trip;
    }

    private static StopReport calculateStop(
            ArrayList<Position> positions, int startIndex, int endIndex, boolean ignoreOdometer) {

        Position startStop = positions.get(startIndex);
        Position endStop = positions.get(endIndex);

        StopReport stop = new StopReport();

        long deviceId = startStop.getDeviceId();
        stop.setDeviceId(deviceId);
        stop.setDeviceName(Context.getIdentityManager().getById(deviceId).getName());

        stop.setPositionId(startStop.getId());
        stop.setLatitude(startStop.getLatitude());
        stop.setLongitude(startStop.getLongitude());
        stop.setStartTime(startStop.getFixTime());
        String address = startStop.getAddress();
        if (address == null && Context.getGeocoder() != null
                && Context.getConfig().getBoolean("geocoder.onRequest")) {
            address = Context.getGeocoder().getAddress(stop.getLatitude(), stop.getLongitude(), null);
        }
        stop.setAddress(address);

        stop.setEndTime(endStop.getFixTime());

        long stopDuration = endStop.getFixTime().getTime() - startStop.getFixTime().getTime();
        stop.setDuration(stopDuration);
        stop.setSpentFuel(calculateFuel(startStop, endStop));

        if (startStop.getAttributes().containsKey(Position.KEY_HOURS)
                && endStop.getAttributes().containsKey(Position.KEY_HOURS)) {
            stop.setEngineHours(endStop.getLong(Position.KEY_HOURS) - startStop.getLong(Position.KEY_HOURS));
        }

        if (!ignoreOdometer
                && startStop.getDouble(Position.KEY_ODOMETER) != 0
                && endStop.getDouble(Position.KEY_ODOMETER) != 0) {
            stop.setStartOdometer(startStop.getDouble(Position.KEY_ODOMETER));
            stop.setEndOdometer(endStop.getDouble(Position.KEY_ODOMETER));
        } else {
            stop.setStartOdometer(startStop.getDouble(Position.KEY_TOTAL_DISTANCE));
            stop.setEndOdometer(endStop.getDouble(Position.KEY_TOTAL_DISTANCE));
        }

        return stop;

    }

    private static <T extends BaseReport> T calculateTripOrStop(
            ArrayList<Position> positions, int startIndex, int endIndex, boolean ignoreOdometer, Class<T> reportClass) {

        if (reportClass.equals(TripReport.class)) {
            return (T) calculateTrip(positions, startIndex, endIndex, ignoreOdometer);
        } else {
            return (T) calculateStop(positions, startIndex, endIndex, ignoreOdometer);
        }
    }

    private static boolean isMoving(ArrayList<Position> positions, int index, TripsConfig tripsConfig) {
        if (tripsConfig.getMinimalNoDataDuration() > 0) {
            boolean beforeGap = index < positions.size() - 1
                    && positions.get(index + 1).getFixTime().getTime() - positions.get(index).getFixTime().getTime()
                    >= tripsConfig.getMinimalNoDataDuration();
            boolean afterGap = index > 0
                    && positions.get(index).getFixTime().getTime() - positions.get(index - 1).getFixTime().getTime()
                    >= tripsConfig.getMinimalNoDataDuration();
            if (beforeGap || afterGap) {
                return false;
            }
        }
        if (positions.get(index).getAttributes().containsKey(Position.KEY_MOTION)
                && positions.get(index).getAttributes().get(Position.KEY_MOTION) instanceof Boolean) {
            return positions.get(index).getBoolean(Position.KEY_MOTION);
        } else {
            return positions.get(index).getSpeed() > tripsConfig.getSpeedThreshold();
        }
    }

    public static <T extends BaseReport> Collection<T> detectTripsAndStops(
            IdentityManager identityManager, DeviceManager deviceManager,
            Collection<Position> positionCollection,
            TripsConfig tripsConfig, boolean ignoreOdometer, Class<T> reportClass) {

        Collection<T> result = new ArrayList<>();

        ArrayList<Position> positions = new ArrayList<>(positionCollection);
        if (!positions.isEmpty()) {
            boolean trips = reportClass.equals(TripReport.class);
            MotionEventHandler  motionHandler = new MotionEventHandler(identityManager, deviceManager, tripsConfig);
            DeviceState deviceState = new DeviceState();
            deviceState.setMotionState(isMoving(positions, 0, tripsConfig));
            int startEventIndex = trips == deviceState.getMotionState() ? 0 : -1;
            int startNoEventIndex = -1;
            for (int i = 0; i < positions.size(); i++) {
                Map<Event, Position> event = motionHandler.updateMotionState(deviceState, positions.get(i),
                        isMoving(positions, i, tripsConfig));
                if (startEventIndex == -1
                        && (trips != deviceState.getMotionState() && deviceState.getMotionPosition() != null
                        || trips == deviceState.getMotionState() && event != null)) {
                    startEventIndex = i;
                    startNoEventIndex = -1;
                } else if (trips != deviceState.getMotionState() && startEventIndex != -1
                        && deviceState.getMotionPosition() == null && event == null) {
                    startEventIndex = -1;
                }
                if (startNoEventIndex == -1
                        && (trips == deviceState.getMotionState() && deviceState.getMotionPosition() != null
                        || trips != deviceState.getMotionState() && event != null)) {
                    startNoEventIndex = i;
                } else if (startNoEventIndex != -1 && deviceState.getMotionPosition() == null && event == null) {
                    startNoEventIndex = -1;
                }
                if (startEventIndex != -1 && startNoEventIndex != -1 && event != null
                        && trips != deviceState.getMotionState()) {
                    result.add(calculateTripOrStop(positions, startEventIndex, startNoEventIndex,
                            ignoreOdometer, reportClass));
                    startEventIndex = -1;
                }
            }
            if (startEventIndex != -1 && (startNoEventIndex != -1 || !trips)) {
                result.add(calculateTripOrStop(positions, startEventIndex,
                            startNoEventIndex != -1 ? startNoEventIndex : positions.size() - 1,
                            ignoreOdometer, reportClass));
            }
        }

        return result;
    }

}
