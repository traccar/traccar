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
            NewMotionState state, Position position, double minDistance, long minDuration, long stopGap) {

        List<Event> events = new ArrayList<>();
        state.setEvents(events);

        Deque<Position> positions = state.getPositions();
        if (positions.isEmpty()) {
            return;
        }

        double minAverageSpeed = minDistance / minDuration;

        Position last = positions.peekLast();
        if (last != null) {
            long gap = position.getFixTime().getTime() - last.getFixTime().getTime();
            double gapDistance = position.getDouble(Position.KEY_DISTANCE);
            double gapAverageSpeed = gap > 0 ? gapDistance / gap : Double.NaN;
            if (gap > stopGap && gapDistance >= minDistance && gapAverageSpeed > minAverageSpeed) {
                if (state.getMotionStreak()) {
                    addEvent(state, events, Event.TYPE_DEVICE_STOPPED, last);
                }
                addEvent(state, events, Event.TYPE_DEVICE_MOVING, last);
                addEvent(state, events, Event.TYPE_DEVICE_STOPPED, position);
                state.setMotionStreak(false);
                return;
            }
        }

        if (state.getMotionStreak()) {
            for (var iterator = positions.descendingIterator(); iterator.hasNext();) {
                Position candidate = iterator.next();
                double distance = DistanceCalculator.distance(
                        candidate.getLatitude(), candidate.getLongitude(),
                        position.getLatitude(), position.getLongitude());
                if (distance >= minDistance) {
                    return;
                }
            }

            Position oldest = positions.peekFirst();
            long duration = position.getFixTime().getTime() - oldest.getFixTime().getTime();
            if (duration >= minDuration) {
                state.setMotionStreak(false);
                addEvent(state, events, Event.TYPE_DEVICE_STOPPED, position);
            }
        } else {
            double distance = DistanceCalculator.distance(
                    state.getEventLatitude(), state.getEventLongitude(),
                    position.getLatitude(), position.getLongitude());
            if (distance >= minDistance) {
                state.setMotionStreak(true);
                addEvent(state, events, Event.TYPE_DEVICE_MOVING, position);
            }
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
