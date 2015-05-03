/*
 * Copyright 2013 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseEvent {

    private long id;
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    private String protocol;
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    private long deviceId;
    public long getDeviceId() { return deviceId; }
    public void setDeviceId(long deviceId) { this.deviceId = deviceId; }

    private Date serverTime;
    public Date getServerTime() { return serverTime; }
    public void setServerTime(Date serverTime) { this.serverTime = serverTime; }

    private Date deviceTime;
    public Date getDeviceTime() { return deviceTime; }
    public void setDeviceTime(Date deviceTime) { this.deviceTime = deviceTime; }

    private final Map<String, Object> other = new LinkedHashMap<String, Object>();
    public void set(String key, Object value) {
        if (value != null && (!(value instanceof String) || !((String) value).isEmpty())) {
            other.put(key, value);
        }
    }
    public String getOther() {
        return MiscFormatter.toXmlString(other);
    }

}
