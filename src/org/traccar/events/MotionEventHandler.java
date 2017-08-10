/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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
package org.traccar.events;

import java.util.Collection;
import java.util.Collections;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.DeviceState;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.reports.ReportUtils;
import org.traccar.reports.model.TripsConfig;

public class MotionEventHandler extends BaseEventHandler {

    private TripsConfig tripsConfig;

    public MotionEventHandler() {
        tripsConfig = ReportUtils.initTripsConfig();
    }

    public static Event updateMotionState(DeviceState deviceState, Position position, TripsConfig tripsConfig) {
        Event result = null;
        Boolean oldMotion = deviceState.getMotionState();

        long currentTime = position.getFixTime().getTime();
        boolean newMotion = position.getBoolean(Position.KEY_MOTION);
        if (newMotion != oldMotion) {
            if (deviceState.getMotionPosition() == null) {
                deviceState.setMotionPosition(position);
            }
        } else {
            deviceState.setMotionPosition(null);
        }

        Position motionPosition = deviceState.getMotionPosition();
        if (motionPosition != null) {
            long motionTime = motionPosition.getFixTime().getTime();
            double distance = ReportUtils.calculateDistance(motionPosition, position, false);
            if (newMotion) {
                if (motionTime + tripsConfig.getMinimalTripDuration() <= currentTime
                        || distance >= tripsConfig.getMinimalTripDistance()) {
                    result = new Event(Event.TYPE_DEVICE_MOVING, motionPosition.getDeviceId(),
                            motionPosition.getId());
                    deviceState.setMotionState(true);
                    deviceState.setMotionPosition(null);
                }
            } else {
                if (motionTime + tripsConfig.getMinimalParkingDuration() <= currentTime) {
                    result = new Event(Event.TYPE_DEVICE_STOPPED, motionPosition.getDeviceId(),
                            motionPosition.getId());
                    deviceState.setMotionState(false);
                    deviceState.setMotionPosition(null);
                }
            }
        }
        return result;
    }

    @Override
    protected Collection<Event> analyzePosition(Position position) {

        Device device = Context.getIdentityManager().getById(position.getDeviceId());
        if (device == null) {
            return null;
        }
        if (!Context.getIdentityManager().isLatestPosition(position) || !position.getValid()) {
            return null;
        }

        Event result = null;

        long deviceId = position.getDeviceId();
        DeviceState deviceState = Context.getDeviceManager().getDeviceState(deviceId);

        if (deviceState == null) {
            deviceState = new DeviceState();
            deviceState.setMotionState(position.getBoolean(Position.KEY_MOTION));
        } else if (deviceState.getMotionState() == null) {
            deviceState.setMotionState(position.getBoolean(Position.KEY_MOTION));
        } else {
            result = updateMotionState(deviceState, position, tripsConfig);
        }
        Context.getDeviceManager().setDeviceState(deviceId, deviceState);
        if (result != null) {
            return Collections.singleton(result);
        }
        return null;
    }

}
