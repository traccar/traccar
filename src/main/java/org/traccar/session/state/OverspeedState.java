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

public class OverspeedState {

    public static OverspeedState fromDevice(Device device) {
        OverspeedState state = new OverspeedState();
        state.overspeedState = device.getOverspeedState();
        state.overspeedTime = device.getOverspeedTime();
        state.overspeedGeofenceId = device.getOverspeedGeofenceId();
        return state;
    }

    public void toDevice(Device device) {
        device.setOverspeedState(overspeedState);
        device.setOverspeedTime(overspeedTime);
        device.setOverspeedGeofenceId(overspeedGeofenceId);
    }

    private boolean changed;

    public boolean isChanged() {
        return changed;
    }

    private boolean overspeedState;

    public boolean getOverspeedState() {
        return overspeedState;
    }

    public void setOverspeedState(boolean overspeedState) {
        this.overspeedState = overspeedState;
        changed = true;
    }

    private Date overspeedTime;

    public Date getOverspeedTime() {
        return overspeedTime;
    }

    public void setOverspeedTime(Date overspeedTime) {
        this.overspeedTime = overspeedTime;
        changed = true;
    }

    private long overspeedGeofenceId;

    public long getOverspeedGeofenceId() {
        return overspeedGeofenceId;
    }

    public void setOverspeedGeofenceId(long overspeedGeofenceId) {
        this.overspeedGeofenceId = overspeedGeofenceId;
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
