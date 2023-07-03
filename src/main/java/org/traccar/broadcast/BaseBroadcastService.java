/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.traccar.model.BaseModel;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Permission;
import org.traccar.model.Position;

public abstract class BaseBroadcastService implements BroadcastService {

    private final Set<BroadcastInterface> listeners = new HashSet<>();

    @Override
    public boolean singleInstance() {
        return true;
    }

    @Override
    public void registerListener(BroadcastInterface listener) {
        listeners.add(listener);
    }

    @Override
    public void updateDevice(boolean local, Device device) {
        BroadcastMessage message = new BroadcastMessage();
        message.setDevice(device);
        sendMessage(message);
    }

    @Override
    public void updatePosition(boolean local, Position position) {
        BroadcastMessage message = new BroadcastMessage();
        message.setPosition(position);
        sendMessage(message);
    }

    @Override
    public void updateEvent(boolean local, long userId, Event event) {
        BroadcastMessage message = new BroadcastMessage();
        message.setUserId(userId);
        message.setEvent(event);
        sendMessage(message);
    }

    @Override
    public void updateCommand(boolean local, long deviceId) {
        BroadcastMessage message = new BroadcastMessage();
        message.setCommandDeviceId(deviceId);
        sendMessage(message);
    }

    @Override
    public void invalidateObject(boolean local, Class<? extends BaseModel> clazz, long id) {
        BroadcastMessage message = new BroadcastMessage();
        message.setChanges(Map.of(Permission.getKey(clazz), id));
        sendMessage(message);
    }

    @Override
    public void invalidatePermission(
            boolean local,
            Class<? extends BaseModel> clazz1, long id1,
            Class<? extends BaseModel> clazz2, long id2) {
        BroadcastMessage message = new BroadcastMessage();
        message.setChanges(Map.of(Permission.getKey(clazz1), id1, Permission.getKey(clazz2), id2));
        sendMessage(message);
    }

    protected abstract void sendMessage(BroadcastMessage message);

    protected void handleMessage(BroadcastMessage message) {
        if (message.getDevice() != null) {
            listeners.forEach(listener -> listener.updateDevice(false, message.getDevice()));
        } else if (message.getPosition() != null) {
            listeners.forEach(listener -> listener.updatePosition(false, message.getPosition()));
        } else if (message.getUserId() != null && message.getEvent() != null) {
            listeners.forEach(listener -> listener.updateEvent(false, message.getUserId(), message.getEvent()));
        } else if (message.getCommandDeviceId() != null) {
            listeners.forEach(listener -> listener.updateCommand(false, message.getCommandDeviceId()));
        } else if (message.getChanges() != null) {
            var iterator = message.getChanges().entrySet().iterator();
            if (iterator.hasNext()) {
                var first = iterator.next();
                if (iterator.hasNext()) {
                    var second = iterator.next();
                    listeners.forEach(listener -> listener.invalidatePermission(
                            false,
                            Permission.getKeyClass(first.getKey()), first.getValue(),
                            Permission.getKeyClass(second.getKey()), second.getValue()));
                } else {
                    listeners.forEach(listener -> listener.invalidateObject(
                            false,
                            Permission.getKeyClass(first.getKey()), first.getValue()));
                }
            }
        }
    }

}
