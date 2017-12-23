// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.model.builders;

import org.joda.time.Duration;
import org.traccar.fcm.core.model.enums.PriorityEnum;
import org.traccar.fcm.core.model.options.FcmMessageOptions;

public class FcmMessageOptionsBuilder {

    private String condition = null;
    private String collapseKey = null;
    private PriorityEnum priorityEnum = null;
    private Boolean contentAvailable = null;
    private Boolean delayWhileIdle = null;
    private int timeToLive = 60;
    private String restrictedPackageName = null;
    private Boolean dryRun = null;
    private Boolean mutableContent = null;

    public FcmMessageOptionsBuilder setCondition(String condition) {
        this.condition = condition;

        return this;
    }

    public FcmMessageOptionsBuilder setCollapseKey(String collapseKey) {
        this.collapseKey = collapseKey;

        return this;
    }

    public FcmMessageOptionsBuilder setPriorityEnum(PriorityEnum priorityEnum) {
        this.priorityEnum = priorityEnum;

        return this;
    }

    public FcmMessageOptionsBuilder setContentAvailable(Boolean contentAvailable) {
        this.contentAvailable = contentAvailable;

        return this;
    }

    public FcmMessageOptionsBuilder setDelayWhileIdle(Boolean delayWhileIdle) {
        this.delayWhileIdle = delayWhileIdle;

        return this;
    }

    public FcmMessageOptionsBuilder setTimeToLive(Duration timeToLive) {
        this.timeToLive = (int) timeToLive.getStandardSeconds();
        return this;
    }

    public FcmMessageOptionsBuilder setRestrictedPackageName(String restrictedPackageName) {
        this.restrictedPackageName = restrictedPackageName;

        return this;
    }

    public FcmMessageOptionsBuilder setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;

        return this;
    }

    public FcmMessageOptionsBuilder setMutableContent(Boolean mutableContent) {
        this.mutableContent = mutableContent;

        return this;
    }

    public FcmMessageOptions build() {
        FcmMessageOptions fcmMessageOptions = new FcmMessageOptions();
        fcmMessageOptions.setCondition(condition);
        fcmMessageOptions.setCollapseKey(collapseKey);
        fcmMessageOptions.setPriorityEnum(priorityEnum);
        fcmMessageOptions.setContentAvailable(contentAvailable);
        fcmMessageOptions.setDelayWhileIdle(delayWhileIdle);
        fcmMessageOptions.setTimeToLive(timeToLive);
        fcmMessageOptions.setRestrictedPackageName(restrictedPackageName);
        fcmMessageOptions.setDryRun(dryRun);
        fcmMessageOptions.setMutableContent(mutableContent);
        return fcmMessageOptions;
    }
}
