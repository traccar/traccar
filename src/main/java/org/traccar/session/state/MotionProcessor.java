/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
            MotionState state, Position position, boolean newState, TripsConfig tripsConfig) {

        state.setEvent(null);

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
                    Event event = new Event(eventType, position);

                    state.setMotionStreak(newState);
                    state.setMotionTime(null);
                    state.setMotionDistance(0);
                    state.setEvent(event);

                }
            }
        } else {
            state.setMotionState(newState);
            if (state.getMotionStreak() == newState) {
                state.setMotionTime(null);
                state.setMotionDistance(0);
            } else {
                state.setMotionTime(position.getFixTime());
                state.setMotionDistance(position.getDouble(Position.KEY_TOTAL_DISTANCE));
            }
        }
    }

}
