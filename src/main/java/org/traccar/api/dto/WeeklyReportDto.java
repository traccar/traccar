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

public class WeeklyReportDto {

    private long childId;

    public long getChildId() {
        return childId;
    }

    public void setChildId(long childId) {
        this.childId = childId;
    }

    private Double averageHeartRate;

    public Double getAverageHeartRate() {
        return averageHeartRate;
    }

    public void setAverageHeartRate(Double averageHeartRate) {
        this.averageHeartRate = averageHeartRate;
    }

    private Double averageBodyTemp;

    public Double getAverageBodyTemp() {
        return averageBodyTemp;
    }

    public void setAverageBodyTemp(Double averageBodyTemp) {
        this.averageBodyTemp = averageBodyTemp;
    }

    private Long totalSteps;

    public Long getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Long totalSteps) {
        this.totalSteps = totalSteps;
    }

    private Double sleepHours;

    public Double getSleepHours() {
        return sleepHours;
    }

    public void setSleepHours(Double sleepHours) {
        this.sleepHours = sleepHours;
    }

    private Integer alertCount;

    public Integer getAlertCount() {
        return alertCount;
    }

    public void setAlertCount(Integer alertCount) {
        this.alertCount = alertCount;
    }
}
