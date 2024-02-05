/*
 * Copyright 2022 - 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.broadcast;

import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Position;

public class BroadcastMessage {

    private Device device;

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    private Position position;

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    private Event event;

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    private Long commandDeviceId;

    public Long getCommandDeviceId() {
        return commandDeviceId;
    }

    public void setCommandDeviceId(Long commandDeviceId) {
        this.commandDeviceId = commandDeviceId;
    }

    public static class InvalidateObject {

        private String clazz;

        public String getClazz() {
            return clazz;
        }

        public void setClazz(String clazz) {
            this.clazz = clazz;
        }

        private long id;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        private ObjectOperation operation;

        public ObjectOperation getOperation() {
            return operation;
        }

        public void setOperation(ObjectOperation operation) {
            this.operation = operation;
        }

    }

    private InvalidateObject invalidateObject;

    public InvalidateObject getInvalidateObject() {
        return invalidateObject;
    }

    public void setInvalidateObject(InvalidateObject invalidateObject) {
        this.invalidateObject = invalidateObject;
    }

    public static class InvalidatePermission {

        private String clazz1;

        public String getClazz1() {
            return clazz1;
        }

        public void setClazz1(String clazz1) {
            this.clazz1 = clazz1;
        }

        private long id1;

        public long getId1() {
            return id1;
        }

        public void setId1(long id1) {
            this.id1 = id1;
        }

        private String clazz2;

        public String getClazz2() {
            return clazz2;
        }

        public void setClazz2(String clazz2) {
            this.clazz2 = clazz2;
        }

        private long id2;

        public long getId2() {
            return id2;
        }

        public void setId2(long id2) {
            this.id2 = id2;
        }

        private boolean link;

        public boolean getLink() {
            return link;
        }

        public void setLink(boolean link) {
            this.link = link;
        }

    }

    private InvalidatePermission invalidatePermission;

    public InvalidatePermission getInvalidatePermission() {
        return invalidatePermission;
    }

    public void setInvalidatePermission(InvalidatePermission invalidatePermission) {
        this.invalidatePermission = invalidatePermission;
    }

}
