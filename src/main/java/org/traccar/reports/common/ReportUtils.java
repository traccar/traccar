/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.reports.common;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.jxls.area.Area;
import org.jxls.builder.xls.XlsCommentAreaBuilder;
import org.jxls.common.CellRef;
import org.jxls.formula.StandardFormulaProcessor;
import org.jxls.transform.Transformer;
import org.jxls.transform.poi.PoiTransformer;
import org.jxls.util.TransformerFactory;
import org.traccar.api.security.PermissionsService;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.geocoder.Geocoder;
import org.traccar.handler.events.MotionEventHandler;
import org.traccar.helper.UnitsConverter;
import org.traccar.helper.model.PositionUtil;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.BaseModel;
import org.traccar.model.Device;
import org.traccar.model.Driver;
import org.traccar.model.Event;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.reports.model.BaseReportItem;
import org.traccar.reports.model.StopReportItem;
import org.traccar.reports.model.TripReportItem;
import org.traccar.session.DeviceState;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReportUtils {

    private final Config config;
    private final Storage storage;
    private final PermissionsService permissionsService;
    private final TripsConfig tripsConfig;
    private final VelocityEngine velocityEngine;
    private final Geocoder geocoder;

    @Inject
    public ReportUtils(
            Config config, Storage storage, PermissionsService permissionsService,
            TripsConfig tripsConfig, VelocityEngine velocityEngine, @Nullable Geocoder geocoder) {
        this.config = config;
        this.storage = storage;
        this.permissionsService = permissionsService;
        this.tripsConfig = tripsConfig;
        this.velocityEngine = velocityEngine;
        this.geocoder = geocoder;
    }

    public <T extends BaseModel> T getObject(
            long userId, Class<T> clazz, long objectId) throws StorageException, SecurityException {
        return storage.getObject(clazz, new Request(
                new Columns.Include("id"),
                new Condition.And(
                        new Condition.Equals("id", "id", objectId),
                        new Condition.Permission(User.class, userId, clazz))));
    }

    public void checkPeriodLimit(Date from, Date to) {
        long limit = config.getLong(Keys.REPORT_PERIOD_LIMIT) * 1000;
        if (limit > 0 && to.getTime() - from.getTime() > limit) {
            throw new IllegalArgumentException("Time period exceeds the limit");
        }
    }

    public Collection<Device> getAccessibleDevices(
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds) throws StorageException {

        var devices = storage.getObjects(Device.class, new Request(
                new Columns.All(),
                new Condition.Permission(User.class, userId, Device.class)));
        var deviceById = devices.stream()
                .collect(Collectors.toUnmodifiableMap(Device::getId, x -> x));
        var devicesByGroup = devices.stream()
                .filter(x -> x.getGroupId() > 0)
                .collect(Collectors.groupingBy(Device::getGroupId));

        var groups = storage.getObjects(Group.class, new Request(
                new Columns.All(),
                new Condition.Permission(User.class, userId, Group.class)));
        var groupsByGroup = groups.stream()
                .filter(x -> x.getGroupId() > 0)
                .collect(Collectors.groupingBy(Group::getGroupId));

        var results = deviceIds.stream()
                .map(deviceById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        var groupQueue = new LinkedList<>(groupIds);
        while (!groupQueue.isEmpty()) {
            long groupId = groupQueue.pop();
            results.addAll(devicesByGroup.getOrDefault(groupId, Collections.emptyList()));
            groupQueue.addAll(groupsByGroup.getOrDefault(groupId, Collections.emptyList())
                    .stream().map(Group::getId).collect(Collectors.toUnmodifiableList()));
        }

        return results;
    }

    public double calculateFuel(Position firstPosition, Position lastPosition) {

        if (firstPosition.getAttributes().get(Position.KEY_FUEL_LEVEL) != null
                && lastPosition.getAttributes().get(Position.KEY_FUEL_LEVEL) != null) {

            BigDecimal value = BigDecimal.valueOf(firstPosition.getDouble(Position.KEY_FUEL_LEVEL)
                    - lastPosition.getDouble(Position.KEY_FUEL_LEVEL));
            return value.setScale(1, RoundingMode.HALF_EVEN).doubleValue();
        }
        return 0;
    }

    public String findDriver(Position firstPosition, Position lastPosition) {
        if (firstPosition.getAttributes().containsKey(Position.KEY_DRIVER_UNIQUE_ID)) {
            return firstPosition.getString(Position.KEY_DRIVER_UNIQUE_ID);
        } else if (lastPosition.getAttributes().containsKey(Position.KEY_DRIVER_UNIQUE_ID)) {
            return lastPosition.getString(Position.KEY_DRIVER_UNIQUE_ID);
        }
        return null;
    }

    public String findDriverName(String driverUniqueId) throws StorageException {
        if (driverUniqueId != null) {
            Driver driver = storage.getObject(Driver.class, new Request(
                    new Columns.All(),
                    new Condition.Equals("uniqueId", "uniqueId", driverUniqueId)));
            if (driver != null) {
                return driver.getName();
            }
        }
        return null;
    }

    public org.jxls.common.Context initializeContext(long userId) throws StorageException {
        var server = permissionsService.getServer();
        var user = permissionsService.getUser(userId);
        var context = PoiTransformer.createInitialContext();
        context.putVar("distanceUnit", UserUtil.getDistanceUnit(server, user));
        context.putVar("speedUnit", UserUtil.getSpeedUnit(server, user));
        context.putVar("volumeUnit", UserUtil.getVolumeUnit(server, user));
        context.putVar("webUrl", velocityEngine.getProperty("web.url"));
        context.putVar("dateTool", new DateTool());
        context.putVar("numberTool", new NumberTool());
        context.putVar("timezone", UserUtil.getTimezone(server, user));
        context.putVar("locale", Locale.getDefault());
        context.putVar("bracketsRegex", "[\\{\\}\"]");
        return context;
    }

    public void processTemplateWithSheets(
            InputStream templateStream, OutputStream targetStream, org.jxls.common.Context context) throws IOException {

        Transformer transformer = TransformerFactory.createTransformer(templateStream, targetStream);
        List<Area> xlsAreas = new XlsCommentAreaBuilder(transformer).build();
        for (Area xlsArea : xlsAreas) {
            xlsArea.applyAt(new CellRef(xlsArea.getStartCellRef().getCellName()), context);
            xlsArea.setFormulaProcessor(new StandardFormulaProcessor());
            xlsArea.processFormulas();
        }
        transformer.deleteSheet(xlsAreas.get(0).getStartCellRef().getSheetName());
        transformer.write();
    }

    private TripReportItem calculateTrip(
            Device device, ArrayList<Position> positions, int startIndex, int endIndex,
            boolean ignoreOdometer) throws StorageException {

        Position startTrip = positions.get(startIndex);
        Position endTrip = positions.get(endIndex);

        double speedMax = 0;
        for (int i = startIndex; i <= endIndex; i++) {
            double speed = positions.get(i).getSpeed();
            if (speed > speedMax) {
                speedMax = speed;
            }
        }

        TripReportItem trip = new TripReportItem();

        long tripDuration = endTrip.getFixTime().getTime() - startTrip.getFixTime().getTime();
        long deviceId = startTrip.getDeviceId();
        trip.setDeviceId(deviceId);
        trip.setDeviceName(device.getName());

        trip.setStartPositionId(startTrip.getId());
        trip.setStartLat(startTrip.getLatitude());
        trip.setStartLon(startTrip.getLongitude());
        trip.setStartTime(startTrip.getFixTime());
        String startAddress = startTrip.getAddress();
        if (startAddress == null && geocoder != null && config.getBoolean(Keys.GEOCODER_ON_REQUEST)) {
            startAddress = geocoder.getAddress(startTrip.getLatitude(), startTrip.getLongitude(), null);
        }
        trip.setStartAddress(startAddress);

        trip.setEndPositionId(endTrip.getId());
        trip.setEndLat(endTrip.getLatitude());
        trip.setEndLon(endTrip.getLongitude());
        trip.setEndTime(endTrip.getFixTime());
        String endAddress = endTrip.getAddress();
        if (endAddress == null && geocoder != null && config.getBoolean(Keys.GEOCODER_ON_REQUEST)) {
            endAddress = geocoder.getAddress(endTrip.getLatitude(), endTrip.getLongitude(), null);
        }
        trip.setEndAddress(endAddress);

        trip.setDistance(PositionUtil.calculateDistance(startTrip, endTrip, !ignoreOdometer));
        trip.setDuration(tripDuration);
        if (tripDuration > 0) {
            trip.setAverageSpeed(UnitsConverter.knotsFromMps(trip.getDistance() * 1000 / tripDuration));
        }
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

    private StopReportItem calculateStop(
            Device device, ArrayList<Position> positions, int startIndex, int endIndex, boolean ignoreOdometer) {

        Position startStop = positions.get(startIndex);
        Position endStop = positions.get(endIndex);

        StopReportItem stop = new StopReportItem();

        long deviceId = startStop.getDeviceId();
        stop.setDeviceId(deviceId);
        stop.setDeviceName(device.getName());

        stop.setPositionId(startStop.getId());
        stop.setLatitude(startStop.getLatitude());
        stop.setLongitude(startStop.getLongitude());
        stop.setStartTime(startStop.getFixTime());
        String address = startStop.getAddress();
        if (address == null && geocoder != null && config.getBoolean(Keys.GEOCODER_ON_REQUEST)) {
            address = geocoder.getAddress(stop.getLatitude(), stop.getLongitude(), null);
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

    @SuppressWarnings("unchecked")
    private <T extends BaseReportItem> T calculateTripOrStop(
            Device device, ArrayList<Position> positions, int startIndex, int endIndex,
            boolean ignoreOdometer, Class<T> reportClass) throws StorageException {

        if (reportClass.equals(TripReportItem.class)) {
            return (T) calculateTrip(device, positions, startIndex, endIndex, ignoreOdometer);
        } else {
            return (T) calculateStop(device, positions, startIndex, endIndex, ignoreOdometer);
        }
    }

    private boolean isMoving(ArrayList<Position> positions, int index, TripsConfig tripsConfig) {
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
        return positions.get(index).getBoolean(Position.KEY_MOTION);
    }

    public <T extends BaseReportItem> Collection<T> detectTripsAndStops(
            Device device, Collection<Position> positionCollection, boolean ignoreOdometer,
            Class<T> reportClass) throws StorageException {

        Collection<T> result = new ArrayList<>();

        ArrayList<Position> positions = new ArrayList<>(positionCollection);
        if (!positions.isEmpty()) {
            boolean trips = reportClass.equals(TripReportItem.class);
            MotionEventHandler motionHandler = new MotionEventHandler(null, null, tripsConfig);
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
                    result.add(calculateTripOrStop(
                            device, positions, startEventIndex, startNoEventIndex, ignoreOdometer, reportClass));
                    startEventIndex = -1;
                }
            }
            if (startEventIndex != -1 && (startNoEventIndex != -1 || !trips)) {
                int endIndex = startNoEventIndex != -1 ? startNoEventIndex : positions.size() - 1;
                result.add(calculateTripOrStop(
                        device, positions, startEventIndex, endIndex, ignoreOdometer, reportClass));
            }
        }

        return result;
    }

}
