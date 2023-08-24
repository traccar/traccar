/*
 * Copyright 2016 - 2023 Anton Tananaev (anton@traccar.org)
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

import java.util.HashSet;
import java.util.Set;

import org.traccar.storage.QueryIgnore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.traccar.storage.StorageName;

@StorageName("tc_notifications")
public class Notification extends ExtendedModel implements Schedulable {

    private long calendarId;

    @Override
    public long getCalendarId() {
        return calendarId;
    }

    @Override
    public void setCalendarId(long calendarId) {
        this.calendarId = calendarId;
    }

    private boolean always;

    public boolean getAlways() {
        return always;
    }

    public void setAlways(boolean always) {
        this.always = always;
    }

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private long commandId;

    public long getCommandId() {
        return commandId;
    }

    public void setCommandId(long commandId) {
        this.commandId = commandId;
    }

    private String notificators;

    public String getNotificators() {
        return notificators;
    }

    public void setNotificators(String transports) {
        this.notificators = transports;
    }

    @JsonIgnore
    @QueryIgnore
    public Set<String> getNotificatorsTypes() {
        final Set<String> result = new HashSet<>();
        if (notificators != null) {
            final String[] transportsList = notificators.split(",");
            for (String transport : transportsList) {
                result.add(transport.trim());
            }
        }
        return result;
    }

}
