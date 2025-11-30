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
package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_children_health")
public class ChildHealthRecord extends BaseModel {

    private long childId;

    public long getChildId() {
        return childId;
    }

    public void setChildId(long childId) {
        this.childId = childId;
    }

    private long deviceId;

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    private Date serverTime;

    public Date getServerTime() {
        return serverTime;
    }

    public void setServerTime(Date serverTime) {
        this.serverTime = serverTime;
    }

    private Integer heartRate;

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    private Double bodyTemp;

    public Double getBodyTemp() {
        return bodyTemp;
    }

    public void setBodyTemp(Double bodyTemp) {
        this.bodyTemp = bodyTemp;
    }

    private Integer steps;

    public Integer getSteps() {
        return steps;
    }

    public void setSteps(Integer steps) {
        this.steps = steps;
    }

    private String sleepStatus;

    public String getSleepStatus() {
        return sleepStatus;
    }

    public void setSleepStatus(String sleepStatus) {
        this.sleepStatus = sleepStatus;
    }

    private Long rawPositionId;

    public Long getRawPositionId() {
        return rawPositionId;
    }

    public void setRawPositionId(Long rawPositionId) {
        this.rawPositionId = rawPositionId;
    }
}
