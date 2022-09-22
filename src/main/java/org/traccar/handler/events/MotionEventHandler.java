/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.ChannelHandler;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.reports.common.TripsConfig;
import org.traccar.session.ConnectionManager;
import org.traccar.session.DeviceState;
import org.traccar.session.cache.CacheManager;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

@ChannelHandler.Sharable
public class MotionEventHandler extends BaseEventHandler {

    private final CacheManager cacheManager;
    private final ConnectionManager connectionManager;
    private final TripsConfig tripsConfig;

    @Inject
    public MotionEventHandler(
            CacheManager cacheManager, ConnectionManager connectionManager, TripsConfig tripsConfig) {
        this.cacheManager = cacheManager;
        this.connectionManager = connectionManager;
        this.tripsConfig = tripsConfig;
    }

    public Map<Event, Position> updateMotionState(DeviceState deviceState, Position position, boolean newState) {

        boolean oldState = deviceState.getMotionState();
        if (oldState == newState) {
            if (deviceState.getMotionTime() != null) {
                long oldTime = deviceState.getMotionTime().getTime();
                long newTime = position.getFixTime().getTime();

                double distance = position.getDouble(Position.KEY_TOTAL_DISTANCE) - deviceState.getMotionDistance();
                Boolean ignition = null;
                if (tripsConfig.getUseIgnition() && position.hasAttribute(Position.KEY_IGNITION)) {
                    ignition = position.getBoolean(Position.KEY_IGNITION);
                }

                boolean generateEvent = false;
                if (newState) {
                    if (newTime - oldTime >= tripsConfig.getMinimalTripDuration()
                            || distance >= tripsConfig.getMinimalTripDistance()) {
                        generateEvent = true;
                    }
                } else {
                    if (newTime - oldTime >= tripsConfig.getMinimalParkingDuration()
                            || ignition != null && !ignition) {
                        generateEvent = true;
                    }
                }

                if (generateEvent) {

                    String eventType = newState ? Event.TYPE_DEVICE_MOVING : Event.TYPE_DEVICE_STOPPED;
                    Event event = new Event(eventType, position);

                    deviceState.setMotionTime(null);
                    deviceState.setMotionDistance(0);

                    return Collections.singletonMap(event, position);

                }
            }
        } else {
            deviceState.setMotionState(newState);
            deviceState.setMotionTime(position.getFixTime());
            deviceState.setMotionDistance(position.getDouble(Position.KEY_TOTAL_DISTANCE));
        }

        return null;
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {

        long deviceId = position.getDeviceId();
        Device device = cacheManager.getObject(Device.class, deviceId);
        if (device == null) {
            return null;
        }
        if (!PositionUtil.isLatest(cacheManager, position)
                || !tripsConfig.getProcessInvalidPositions() && !position.getValid()) {
            return null;
        }

        DeviceState deviceState = connectionManager.getDeviceState(deviceId);
        var result = updateMotionState(deviceState, position, position.getBoolean(Position.KEY_MOTION));
        connectionManager.setDeviceState(deviceId, deviceState);
        return result;
    }

}
