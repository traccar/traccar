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

import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.Date;
import java.util.Deque;
import java.util.List;

public class NewMotionState {

    private boolean changed;

    public boolean isChanged() {
        return changed;
    }

    private boolean motionStreak;

    public boolean getMotionStreak() {
        return motionStreak;
    }

    public void setMotionStreak(boolean motionStreak) {
        this.motionStreak = motionStreak;
        changed = true;
    }

    private Deque<Position> positions;

    public Deque<Position> getPositions() {
        return positions;
    }

    public void setPositions(Deque<Position> positions) {
        this.positions = positions;
    }

    private List<Event> events;

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    private Date eventTime;

    public Date getEventTime() {
        return eventTime;
    }

    private double eventLatitude;

    public double getEventLatitude() {
        return eventLatitude;
    }

    private double eventLongitude;

    public double getEventLongitude() {
        return eventLongitude;
    }

    public void setEventPosition(Position position) {
        eventTime = position.getFixTime();
        eventLatitude = position.getLatitude();
        eventLongitude = position.getLongitude();
        changed = true;
    }

}
