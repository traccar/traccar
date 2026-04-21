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

public final class OverspeedProcessor {

    public static final String ATTRIBUTE_SPEED = "speed";

    private OverspeedProcessor() {
    }

    public static void updateState(
            OverspeedState state, Position position,
            double speedLimit, double multiplier, long minimalDuration, long geofenceId) {

        state.setEvent(null);

        boolean oldState = state.getOverspeedState();
        if (oldState) {
            boolean newState = position.getSpeed() > speedLimit * multiplier;
            if (newState) {
                checkEvent(state, position, speedLimit, minimalDuration);
            } else {
                state.setOverspeedState(false);
                state.setOverspeedTime(null);
                state.setOverspeedGeofenceId(0);
            }
        } else if (position != null && position.getSpeed() > speedLimit * multiplier) {
            state.setOverspeedState(true);
            state.setOverspeedTime(position.getFixTime());
            state.setOverspeedGeofenceId(geofenceId);

            checkEvent(state, position, speedLimit, minimalDuration);
        }
    }

    private static void checkEvent(OverspeedState state, Position position, double speedLimit, long minimalDuration) {
        if (state.getOverspeedTime() != null) {
            long oldTime = state.getOverspeedTime().getTime();
            long newTime = position.getFixTime().getTime();
            if (newTime - oldTime >= minimalDuration) {

                Event event = new Event(Event.TYPE_DEVICE_OVERSPEED, position);
                event.set(ATTRIBUTE_SPEED, position.getSpeed());
                event.set(Position.KEY_SPEED_LIMIT, speedLimit);
                event.setGeofenceId(state.getOverspeedGeofenceId());

                state.setOverspeedTime(null);
                state.setOverspeedGeofenceId(0);
                state.setEvent(event);

            }
        }
    }
}
