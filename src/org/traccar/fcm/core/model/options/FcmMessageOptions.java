// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.model.options;


import org.traccar.fcm.core.model.builders.FcmMessageOptionsBuilder;
import org.traccar.fcm.core.model.enums.PriorityEnum;

public class FcmMessageOptions {

    private String condition;
    private String collapseKey;
    private PriorityEnum priorityEnum;
    private Boolean contentAvailable;
    private Boolean delayWhileIdle;
    private int timeToLive;
    private String restrictedPackageName;
    private Boolean dryRun;
    private Boolean mutableContent;

    public static FcmMessageOptionsBuilder builder() {
        return new FcmMessageOptionsBuilder();
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getCollapseKey() {
        return collapseKey;
    }

    public void setCollapseKey(String collapseKey) {
        this.collapseKey = collapseKey;
    }

    public PriorityEnum getPriorityEnum() {
        return priorityEnum;
    }

    public void setPriorityEnum(PriorityEnum priorityEnum) {
        this.priorityEnum = priorityEnum;
    }

    public Boolean getContentAvailable() {
        return contentAvailable;
    }

    public void setContentAvailable(Boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
    }

    public Boolean getDelayWhileIdle() {
        return delayWhileIdle;
    }

    public void setDelayWhileIdle(Boolean delayWhileIdle) {
        this.delayWhileIdle = delayWhileIdle;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(int timeToLive) {
        this.timeToLive = timeToLive;
    }

    public String getRestrictedPackageName() {
        return restrictedPackageName;
    }

    public void setRestrictedPackageName(String restrictedPackageName) {
        this.restrictedPackageName = restrictedPackageName;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    public Boolean getMutableContent() {
        return mutableContent;
    }

    public void setMutableContent(Boolean mutableContent) {
        this.mutableContent = mutableContent;
    }
}
