/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api.dto;

import java.util.Date;

public class ChildStatusDto {

    private long childId;

    public long getChildId() {
        return childId;
    }

    public void setChildId(long childId) {
        this.childId = childId;
    }

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private Date lastPositionTime;

    public Date getLastPositionTime() {
        return lastPositionTime;
    }

    public void setLastPositionTime(Date lastPositionTime) {
        this.lastPositionTime = lastPositionTime;
    }

    private Integer lastHeartRate;

    public Integer getLastHeartRate() {
        return lastHeartRate;
    }

    public void setLastHeartRate(Integer lastHeartRate) {
        this.lastHeartRate = lastHeartRate;
    }

    private Double lastBodyTemp;

    public Double getLastBodyTemp() {
        return lastBodyTemp;
    }

    public void setLastBodyTemp(Double lastBodyTemp) {
        this.lastBodyTemp = lastBodyTemp;
    }

    private String lastEventType;

    public String getLastEventType() {
        return lastEventType;
    }

    public void setLastEventType(String lastEventType) {
        this.lastEventType = lastEventType;
    }
}
