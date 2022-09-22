/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2018 Andrey Kunitsyn (andrey@traccar.org)
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

import java.util.Collections;
import java.util.Map;

import io.netty.channel.ChannelHandler;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.session.ConnectionManager;
import org.traccar.session.DeviceState;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import javax.inject.Inject;

@ChannelHandler.Sharable
public class OverspeedEventHandler extends BaseEventHandler {

    public static final String ATTRIBUTE_SPEED = "speed";

    private final ConnectionManager connectionManager;
    private final CacheManager cacheManager;

    private final long minimalDuration;
    private final boolean preferLowest;

    @Inject
    public OverspeedEventHandler(Config config, ConnectionManager connectionManager, CacheManager cacheManager) {
        this.connectionManager = connectionManager;
        this.cacheManager = cacheManager;
        minimalDuration = config.getLong(Keys.EVENT_OVERSPEED_MINIMAL_DURATION) * 1000;
        preferLowest = config.getBoolean(Keys.EVENT_OVERSPEED_PREFER_LOWEST);
    }

    public Map<Event, Position> updateOverspeedState(
            DeviceState deviceState, Position position, double speedLimit, long geofenceId) {

        boolean oldState = deviceState.getOverspeedState();
        if (oldState) {
            boolean newState = position.getSpeed() > speedLimit;
            if (newState) {
                if (deviceState.getOverspeedTime() != null) {
                    long oldTime = deviceState.getOverspeedTime().getTime();
                    long newTime = position.getFixTime().getTime();
                    if (newTime - oldTime > minimalDuration) {

                        Event event = new Event(Event.TYPE_DEVICE_OVERSPEED, position);
                        event.set(ATTRIBUTE_SPEED, position.getSpeed());
                        event.set(Position.KEY_SPEED_LIMIT, speedLimit);
                        event.setGeofenceId(deviceState.getOverspeedGeofenceId());

                        deviceState.setOverspeedTime(null);
                        deviceState.setOverspeedGeofenceId(0);

                        return Collections.singletonMap(event, position);

                    }
                }
            } else {
                deviceState.setOverspeedState(false);
                deviceState.setOverspeedTime(null);
                deviceState.setOverspeedGeofenceId(0);
            }
        } else if (position != null && position.getSpeed() > speedLimit) {
            deviceState.setOverspeedState(true);
            deviceState.setOverspeedTime(position.getFixTime());
            deviceState.setOverspeedGeofenceId(geofenceId);
        }

        return null;
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {

        long deviceId = position.getDeviceId();
        Device device = cacheManager.getObject(Device.class, position.getDeviceId());
        if (device == null) {
            return null;
        }
        if (!PositionUtil.isLatest(cacheManager, position) || !position.getValid()) {
            return null;
        }

        double speedLimit = AttributeUtil.lookup(cacheManager, Keys.EVENT_OVERSPEED_LIMIT, deviceId);

        double positionSpeedLimit = position.getDouble(Position.KEY_SPEED_LIMIT);
        if (positionSpeedLimit > 0) {
            speedLimit = positionSpeedLimit;
        }

        double geofenceSpeedLimit = 0;
        long overspeedGeofenceId = 0;

        if (device.getGeofenceIds() != null) {
            for (long geofenceId : device.getGeofenceIds()) {
                Geofence geofence = cacheManager.getObject(Geofence.class, geofenceId);
                if (geofence != null) {
                    double currentSpeedLimit = geofence.getDouble(Keys.EVENT_OVERSPEED_LIMIT.getKey());
                    if (currentSpeedLimit > 0 && geofenceSpeedLimit == 0
                            || preferLowest && currentSpeedLimit < geofenceSpeedLimit
                            || !preferLowest && currentSpeedLimit > geofenceSpeedLimit) {
                        geofenceSpeedLimit = currentSpeedLimit;
                        overspeedGeofenceId = geofenceId;
                    }
                }
            }
        }
        if (geofenceSpeedLimit > 0) {
            speedLimit = geofenceSpeedLimit;
        }

        if (speedLimit == 0) {
            return null;
        }

        DeviceState deviceState = connectionManager.getDeviceState(deviceId);
        var result = updateOverspeedState(deviceState, position, speedLimit, overspeedGeofenceId);
        connectionManager.setDeviceState(deviceId, deviceState);
        return result;
    }

}
