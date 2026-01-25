/*
 * Copyright 2016 - 2026 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.handler.events;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.reports.common.TripsConfig;
import org.traccar.session.cache.CacheManager;
import org.traccar.session.state.MotionProcessor;
import org.traccar.session.state.MotionState;
import org.traccar.session.state.NewMotionProcessor;
import org.traccar.session.state.NewMotionState;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

public class MotionEventHandler extends BaseEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MotionEventHandler.class);

    private static final String KEY_MOTION_TIME = "motionTime";
    private static final String KEY_MOTION_LAT = "motionLat";
    private static final String KEY_MOTION_LON = "motionLon";

    private final Config config;
    private final CacheManager cacheManager;
    private final Storage storage;

    @Inject
    public MotionEventHandler(Config config, CacheManager cacheManager, Storage storage) {
        this.config = config;
        this.cacheManager = cacheManager;
        this.storage = storage;
    }

    @Override
    public void onPosition(Position position, Callback callback) {

        long deviceId = position.getDeviceId();
        Device device = cacheManager.getObject(Device.class, deviceId);
        if (device == null || !PositionUtil.isLatest(cacheManager, position)) {
            return;
        }

        var attributeProvider = new AttributeUtil.CacheProvider(cacheManager, deviceId);
        if (config.getBoolean(Keys.REPORT_TRIP_NEW_LOGIC)) {
            double minDistance = AttributeUtil.lookup(attributeProvider, Keys.REPORT_TRIP_MIN_DISTANCE);
            long minDuration = AttributeUtil.lookup(attributeProvider, Keys.REPORT_TRIP_MIN_DURATION) * 1000;
            handleNewLogic(device, position, minDistance, minDuration, callback);
        } else {
            TripsConfig tripsConfig = new TripsConfig(attributeProvider);
            handleOldLogic(device, position, tripsConfig, callback);
        }
    }

    private void handleNewLogic(
            Device device, Position position, double minDistance, long minDuration, Callback callback) {
        NewMotionState state = new NewMotionState();
        state.setMotionStreak(device.getMotionStreak());
        state.setPositions(cacheManager.getPositions(device.getId()));
        NewMotionProcessor.updateState(state, position, minDistance, minDuration);
        if (state.isChanged()) {
            device.setMotionStreak(state.getMotionStreak());
            device.set(KEY_MOTION_TIME, state.getEventTime().getTime());
            device.set(KEY_MOTION_LAT, state.getEventLatitude());
            device.set(KEY_MOTION_LON, state.getEventLongitude());
            try {
                storage.updateObject(device, new Request(
                        new Columns.Include("motionStreak", "attributes"),
                        new Condition.Equals("id", device.getId())));
            } catch (StorageException e) {
                LOGGER.warn("Update device motion error", e);
            }
        }
        for (var event : state.getEvents()) {
            callback.eventDetected(event);
        }
    }

    private void handleOldLogic(Device device, Position position, TripsConfig tripsConfig, Callback callback) {
        MotionState state = MotionState.fromDevice(device);
        Position last = cacheManager.getPosition(device.getId());
        MotionProcessor.updateState(state, last, position, position.getBoolean(Position.KEY_MOTION), tripsConfig);
        if (state.isChanged()) {
            state.toDevice(device);
            try {
                storage.updateObject(device, new Request(
                        new Columns.Include(
                                "motionStreak", "motionState", "motionPositionId", "motionTime", "motionDistance"),
                        new Condition.Equals("id", device.getId())));
            } catch (StorageException e) {
                LOGGER.warn("Update device motion error", e);
            }
        }
        if (state.getEvent() != null) {
            callback.eventDetected(state.getEvent());
        }
    }

}
