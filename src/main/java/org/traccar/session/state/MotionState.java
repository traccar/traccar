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

import org.traccar.model.Device;
import org.traccar.model.Event;

import java.util.Date;

public class MotionState {

    public static MotionState fromDevice(Device device) {
        MotionState state = new MotionState();
        state.motionStreak = device.getMotionStreak();
        state.motionState = device.getMotionState();
        state.motionTime = device.getMotionTime();
        state.motionDistance = device.getMotionDistance();
        return state;
    }

    public void toDevice(Device device) {
        device.setMotionStreak(motionStreak);
        device.setMotionState(motionState);
        device.setMotionTime(motionTime);
        device.setMotionDistance(motionDistance);
    }

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

    private boolean motionState;

    public boolean getMotionState() {
        return motionState;
    }

    public void setMotionState(boolean motionState) {
        this.motionState = motionState;
        changed = true;
    }

    private Date motionTime;

    public Date getMotionTime() {
        return motionTime;
    }

    public void setMotionTime(Date motionTime) {
        this.motionTime = motionTime;
        changed = true;
    }

    private double motionDistance;

    public double getMotionDistance() {
        return motionDistance;
    }

    public void setMotionDistance(double motionDistance) {
        this.motionDistance = motionDistance;
        changed = true;
    }

    private Event event;

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

}
