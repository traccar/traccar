/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.session;

import org.traccar.model.Position;

public class DeviceState {

    private Boolean motionState;

    public void setMotionState(boolean motionState) {
        this.motionState = motionState;
    }

    public Boolean getMotionState() {
        return motionState;
    }

    private Position motionPosition;

    public void setMotionPosition(Position motionPosition) {
        this.motionPosition = motionPosition;
    }

    public Position getMotionPosition() {
        return motionPosition;
    }

    private Boolean overspeedState;

    public void setOverspeedState(boolean overspeedState) {
        this.overspeedState = overspeedState;
    }

    public Boolean getOverspeedState() {
        return overspeedState;
    }

    private Position overspeedPosition;

    public void setOverspeedPosition(Position overspeedPosition) {
        this.overspeedPosition = overspeedPosition;
    }

    public Position getOverspeedPosition() {
        return overspeedPosition;
    }

    private long overspeedGeofenceId;

    public void setOverspeedGeofenceId(long overspeedGeofenceId) {
        this.overspeedGeofenceId = overspeedGeofenceId;
    }

    public long getOverspeedGeofenceId() {
        return overspeedGeofenceId;
    }

}
