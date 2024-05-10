/*
 * Copyright 2023 - 2024 Anton Tananaev (anton@traccar.org)
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

package org.traccar.reports;

import java.util.Collection;
import java.util.Date;


public class DeviceGroupQuery {
    private long userId;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    private Collection<Long> deviceIds;

    public Collection<Long> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(Collection<Long> deviceIds) {
        this.deviceIds = deviceIds;
    }

    private Collection<Long> groupIds;

    public Collection<Long> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(Collection<Long> groupIds) {
        this.groupIds = groupIds;
    }

    private Date from;

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    private Date to;

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public DeviceGroupQuery(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) {
        this.userId = userId;
        this.deviceIds = deviceIds;
        this.groupIds = groupIds;
        this.from = from;
        this.to = to;
    }
}
