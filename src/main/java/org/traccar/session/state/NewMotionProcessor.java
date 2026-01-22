/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.reports.common.TripsConfig;

import java.util.Deque;

public final class NewMotionProcessor {

    private NewMotionProcessor() {
    }

    public static void updateState(NewMotionState state, Position position, TripsConfig tripsConfig) {

        state.setEvent(null);

        Deque<Position> positions = state.getPositions();
        if (positions.isEmpty()) {
            return;
        }

        double minimalDistance = tripsConfig.getMinimalTripDistance();
        long minimalDuration = tripsConfig.getMinimalTripDuration();
        for (var iterator = positions.descendingIterator(); iterator.hasNext(); ) {
            Position candidate = iterator.next();
            long duration = position.getFixTime().getTime() - candidate.getFixTime().getTime();
            double distance = DistanceCalculator.distance(
                    candidate.getLatitude(), candidate.getLongitude(),
                    position.getLatitude(), position.getLongitude());
            if (distance >= minimalDistance && duration <= minimalDuration) {
                if (!state.getMotionStreak()) {
                    state.setMotionStreak(true);
                    state.setEvent(newEvent(Event.TYPE_DEVICE_MOVING, position, position.getDeviceId()));
                }
                return;
            }
        }

        Position oldest = positions.peekFirst();
        long duration = position.getFixTime().getTime() - oldest.getFixTime().getTime();
        if (duration >= minimalDuration && state.getMotionStreak()) {
            state.setMotionStreak(false);
            state.setEvent(newEvent(Event.TYPE_DEVICE_STOPPED, position, position.getDeviceId()));
        }
    }

    private static Event newEvent(String type, Position position, long deviceId) {
        Event event = new Event(type, deviceId);
        event.setPositionId(position.getId());
        event.setEventTime(position.getFixTime());
        return event;
    }

}
