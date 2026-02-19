/*
 * Copyright 2022 - 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.session.state;

import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.reports.common.TripsConfig;

public final class MotionProcessor {

    private MotionProcessor() {
    }

    public static void updateState(
            MotionState state, Position last, Position position, boolean newState, TripsConfig tripsConfig) {

        state.setEvent(null);

        // Calculate idle time for this position if we have a previous position
        if (last != null) {
            calculateIdleTime(state, last, position, tripsConfig);
        }

        if (last != null) {
            long oldTime = last.getFixTime().getTime();
            long newTime = position.getFixTime().getTime();
            if (newTime - oldTime >= tripsConfig.getMinimalNoDataDuration() && state.getMotionStreak()) {
                state.setMotionStreak(false);
                state.setMotionState(false);
                state.setMotionPositionId(0);
                state.setMotionTime(null);
                state.setMotionDistance(0);

                Event event = new Event(Event.TYPE_DEVICE_STOPPED, last);
                event.set("idleTime", state.getIdleTime());
                state.setEvent(event);

                // Reset idle time for next period
                state.setIdleTime(0);
                state.setIdleState(false);
                state.setIdleStartTime(null);
                return;
            }
        }

        boolean oldState = state.getMotionState();
        if (oldState == newState) {
            if (state.getMotionTime() != null) {
                long oldTime = state.getMotionTime().getTime();
                long newTime = position.getFixTime().getTime();

                double distance = position.getDouble(Position.KEY_TOTAL_DISTANCE) - state.getMotionDistance();
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
                    Event event = new Event(eventType, position.getDeviceId());
                    event.setPositionId(state.getMotionPositionId());
                    event.setEventTime(state.getMotionTime());

                    state.setMotionStreak(newState);
                    state.setMotionPositionId(0);
                    state.setMotionTime(null);
                    state.setMotionDistance(0);
                    state.setEvent(event);

                    // Reset idle time for next period
                    state.setIdleTime(0);
                    state.setIdleState(false);
                    state.setIdleStartTime(null);

                }
            }
        } else {
            state.setMotionState(newState);
            if (state.getMotionStreak() == newState) {
                state.setMotionPositionId(0);
                state.setMotionTime(null);
                state.setMotionDistance(0);
            } else {
                state.setMotionPositionId(position.getId());
                state.setMotionTime(position.getFixTime());
                state.setMotionDistance(position.getDouble(Position.KEY_TOTAL_DISTANCE));
            }
        }
    }

    private static void calculateIdleTime(
            MotionState state, Position previous, Position current, TripsConfig tripsConfig) {

        // Detect if current position represents an idle state
        Boolean ignition = current.getBoolean(Position.KEY_IGNITION);
        Boolean motion = current.getBoolean(Position.KEY_MOTION);
        Double rpm = current.getDouble(Position.KEY_RPM);

        // Multi-sensor detection: engine running = RPM > threshold OR ignition = true
        double rpmThreshold = tripsConfig.getIdleRpmThreshold();
        boolean engineRunning = (rpm != null && rpm > rpmThreshold)
                || (ignition != null && ignition);

        // Idle = engine running + not moving
        boolean isIdle = engineRunning && motion != null && !motion;

        long currentTime = current.getFixTime().getTime();
        long previousTime = previous.getFixTime().getTime();
        long duration = currentTime - previousTime;

        boolean wasIdle = state.getIdleState();

        if (wasIdle && isIdle) {
            // Continue existing idle period
            long maxGap = tripsConfig.getIdleMaxGap();

            // Only count if gap between positions is reasonable (not offline period)
            if (duration > 0 && duration < maxGap) {
                state.setIdleTime(state.getIdleTime() + duration);
            }
        } else if (!wasIdle && isIdle) {
            // Start new idle period
            state.setIdleState(true);
            state.setIdleStartTime(current.getFixTime());
        } else if (wasIdle && !isIdle) {
            // End idle period - validate it meets minimum duration
            if (state.getIdleStartTime() != null) {
                long totalIdleDuration = currentTime - state.getIdleStartTime().getTime();
                long minDuration = tripsConfig.getIdleMinDuration();

                if (totalIdleDuration < minDuration) {
                    // Period too short - subtract what we've counted
                    long correctedIdle = Math.max(0, state.getIdleTime() - totalIdleDuration);
                    state.setIdleTime(correctedIdle);
                }
            }
            state.setIdleState(false);
            state.setIdleStartTime(null);
        }
    }

}