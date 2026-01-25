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

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class NewMotionProcessor {

    private NewMotionProcessor() {
    }

    public static void updateState(
            NewMotionState state, Position position, double minDistance, long minDuration) {

        List<Event> events = new ArrayList<>();
        state.setEvents(events);

        Deque<Position> positions = state.getPositions();
        if (positions.isEmpty()) {
            return;
        }

        double targetSpeed = minDuration > 0 ? minDistance / minDuration : 0;
        for (var iterator = positions.descendingIterator(); iterator.hasNext();) {
            Position candidate = iterator.next();
            long duration = position.getFixTime().getTime() - candidate.getFixTime().getTime();
            double distance = DistanceCalculator.distance(
                    candidate.getLatitude(), candidate.getLongitude(),
                    position.getLatitude(), position.getLongitude());
            if (distance >= minDistance) {
                if (duration <= minDuration || distance >= targetSpeed * duration) {
                    if (!state.getMotionStreak()) {
                        state.setMotionStreak(true);
                        addEvent(state, events, Event.TYPE_DEVICE_MOVING, position);
                    }
                } else {
                    if (state.getMotionStreak()) {
                        addEvent(state, events, Event.TYPE_DEVICE_STOPPED, candidate);
                    }
                    addEvent(state, events, Event.TYPE_DEVICE_MOVING, candidate);
                    addEvent(state, events, Event.TYPE_DEVICE_STOPPED, position);
                    state.setMotionStreak(false);
                }
                return;
            }
        }

        Position oldest = positions.peekFirst();
        long duration = position.getFixTime().getTime() - oldest.getFixTime().getTime();
        if (duration >= minDuration && state.getMotionStreak()) {
            state.setMotionStreak(false);
            addEvent(state, events, Event.TYPE_DEVICE_STOPPED, position);
        }
    }

    private static void addEvent(NewMotionState state, List<Event> events, String type, Position position) {
        Event event = new Event(type, position.getDeviceId());
        event.setPositionId(position.getId());
        event.setEventTime(position.getFixTime());
        events.add(event);
        state.setEventPosition(position);
    }

}
