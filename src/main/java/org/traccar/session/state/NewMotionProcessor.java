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
import org.traccar.model.CompactPosition;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class NewMotionProcessor {

    private NewMotionProcessor() {}

    public static void updateState(
            NewMotionState state, Position position,
            double minDistance, long minDuration, long stopGap, boolean useIgnition) {

        CompactPosition current = new CompactPosition(position);
        List<Event> events = new ArrayList<>();
        state.setEvents(events);

        if (useIgnition && position.hasAttribute(Position.KEY_IGNITION)) {
            boolean ignition = position.getBoolean(Position.KEY_IGNITION);
            if (state.getMotionStreak() != ignition) {
                state.setMotionStreak(ignition);
                addEvent(state, events, ignition ? Event.TYPE_DEVICE_MOVING : Event.TYPE_DEVICE_STOPPED, current);
            }
            return;
        }

        Deque<CompactPosition> positions = state.getPositions();
        if (positions.isEmpty()) {
            return;
        }

        double minAverageSpeed = minDistance / minDuration;

        CompactPosition last = positions.peekLast();
        if (last != null) {
            long gap = position.getFixTime().getTime() - last.fixTime().getTime();
            double gapDistance = position.getDouble(Position.KEY_DISTANCE);
            double gapAverageSpeed = gap > 0 ? gapDistance / gap : Double.NaN;
            if (gap > stopGap && gapDistance >= minDistance && gapAverageSpeed > minAverageSpeed) {
                if (state.getMotionStreak()) {
                    addEvent(state, events, Event.TYPE_DEVICE_STOPPED, last);
                }
                addEvent(state, events, Event.TYPE_DEVICE_MOVING, last);
                addEvent(state, events, Event.TYPE_DEVICE_STOPPED, current);
                state.setMotionStreak(false);
                return;
            }
        }

        if (state.getMotionStreak()) {
            for (var iterator = positions.descendingIterator(); iterator.hasNext();) {
                CompactPosition candidate = iterator.next();
                double distance = DistanceCalculator.distance(candidate, current);
                if (distance >= minDistance) {
                    return;
                }
            }

            CompactPosition oldest = positions.peekFirst();
            long duration = position.getFixTime().getTime() - oldest.fixTime().getTime();
            if (duration < minDuration) {
                return;
            }
        } else {
            double distance = DistanceCalculator.distance(
                    state.getEventLatitude(), state.getEventLongitude(),
                    position.getLatitude(), position.getLongitude());
            if (distance < minDistance) {
                return;
            }
        }

        boolean moving = !state.getMotionStreak();
        CompactPosition eventPosition = current;
        CompactPosition next = current;
        for (var iterator = positions.descendingIterator(); iterator.hasNext();) {
            CompactPosition candidate = iterator.next();
            if (!candidate.fixTime().after(state.getEventTime())) {
                break;
            }

            double legDistance = DistanceCalculator.distance(candidate, next);
            long legDuration = next.fixTime().getTime() - candidate.fixTime().getTime();
            boolean legMoving = legDuration > 0 && legDistance / legDuration > minAverageSpeed;
            boolean firstStartLeg = moving && next == current;
            if (!firstStartLeg && legMoving != moving) {
                break;
            }

            eventPosition = candidate;
            next = candidate;
        }

        state.setMotionStreak(moving);
        addEvent(state, events, moving ? Event.TYPE_DEVICE_MOVING : Event.TYPE_DEVICE_STOPPED, eventPosition);
    }

    private static void addEvent(NewMotionState state, List<Event> events, String type, CompactPosition position) {
        Event event = new Event(type, position.deviceId());
        event.setPositionId(position.id());
        event.setEventTime(position.fixTime());
        events.add(event);
        state.setEventPosition(position);
    }

}
