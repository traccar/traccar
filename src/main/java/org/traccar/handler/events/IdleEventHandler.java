/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;
import org.traccar.session.state.MotionState;
import org.traccar.reports.common.TripsConfig;

public class IdleEventHandler extends BaseEventHandler {

    private final CacheManager cacheManager;
    private final TripsConfig tripsConfig;

    @Inject
    public IdleEventHandler(CacheManager cacheManager, TripsConfig tripsConfig) {
        this.cacheManager = cacheManager;
        this.tripsConfig = tripsConfig;
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        Device device = cacheManager.getObject(Device.class, position.getDeviceId());
        if (device == null || !PositionUtil.isLatest(cacheManager, position)) {
            return;
        }

        Position lastPosition = cacheManager.getPosition(position.getDeviceId());
        if (lastPosition == null) {
            return;
        }

        // Check if current position indicates idle state
        Boolean ignition = position.getBoolean(Position.KEY_IGNITION);
        Boolean motion = position.getBoolean(Position.KEY_MOTION);
        Double rpm = position.getDouble(Position.KEY_RPM);

        // Multi-sensor detection: engine running = RPM > threshold OR ignition = true
        double rpmThreshold = tripsConfig.getIdleRpmThreshold();
        boolean engineRunning = (rpm != null && rpm > rpmThreshold)
                || (ignition != null && ignition);

        // Idle = engine running + not moving
        boolean isIdle = engineRunning && motion != null && !motion;

        // Get previous state from MotionState
        MotionState state = new MotionState();
        
        // Check if we transitioned into idle state
        if (isIdle) {
            // Check if previous position was not idle
            Boolean lastIgnition = lastPosition.getBoolean(Position.KEY_IGNITION);
            Boolean lastMotion = lastPosition.getBoolean(Position.KEY_MOTION);
            Double lastRpm = lastPosition.getDouble(Position.KEY_RPM);
            
            boolean lastEngineRunning = (lastRpm != null && lastRpm > rpmThreshold)
                    || (lastIgnition != null && lastIgnition);
            boolean lastWasIdle = lastEngineRunning && lastMotion != null && !lastMotion;

            // If we transitioned from not-idle to idle, generate event
            if (!lastWasIdle) {
                Event idleEvent = new Event(Event.TYPE_DEVICE_IDLE, position);
                idleEvent.set("idleStartTime", position.getFixTime().getTime());
                callback.eventDetected(idleEvent);
            }
        }
    }
}
