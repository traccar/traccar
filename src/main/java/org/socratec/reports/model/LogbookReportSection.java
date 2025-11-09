/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 Andrey Kunitsyn (andrey@traccar.org)
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
package org.socratec.reports.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LogbookReportSection {

    private String deviceName;
    private String groupName = "";
    private double totalDistance;
    private double totalDuration;
    private double privateDistance;
    private double businessDistance;
    private double privateDuration;
    private double businessDuration;
    private List<?> objects;


    public String getDeviceName() {
        return deviceName;
    }
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getGroupName() {
        return groupName;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Collection<?> getObjects() {
        return objects;
    }
    public void setObjects(Collection<?> objects) {
        this.objects = new ArrayList<>(objects);
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public double getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(double totalDuration) {
        this.totalDuration = totalDuration;
    }

    public double getPrivateDistance() {
        return privateDistance;
    }

    public void setPrivateDistance(double privateDistance) {
        this.privateDistance = privateDistance;
    }

    public double getBusinessDistance() {
        return businessDistance;
    }

    public void setBusinessDistance(double businessDistance) {
        this.businessDistance = businessDistance;
    }

    public double getPrivateDuration() {
        return privateDuration;
    }

    public void setPrivateDuration(double privateDuration) {
        this.privateDuration = privateDuration;
    }

    public double getBusinessDuration() {
        return businessDuration;
    }

    public void setBusinessDuration(double businessDuration) {
        this.businessDuration = businessDuration;
    }
}
