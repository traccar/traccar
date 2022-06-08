/*
 * Copyright 2017 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.database;

import org.traccar.BaseProtocol;
import org.traccar.ServerManager;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.ConnectionManager;
import org.traccar.session.DeviceSession;
import org.traccar.sms.SmsManager;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class CommandsManager {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<Long, Queue<Command>> deviceQueues = new ConcurrentHashMap<>();

    private final Storage storage;
    private final ServerManager serverManager;
    private final SmsManager smsManager;
    private final ConnectionManager connectionManager;

    private final boolean queueing;

    @Inject
    public CommandsManager(
            Storage storage, ServerManager serverManager, @Nullable SmsManager smsManager,
            ConnectionManager connectionManager, Config config) {
        this.storage = storage;
        this.serverManager = serverManager;
        this.smsManager = smsManager;
        this.connectionManager = connectionManager;
        queueing = config.getBoolean(Keys.COMMANDS_QUEUEING);
    }

    public boolean sendCommand(Command command) throws Exception {
        long deviceId = command.getDeviceId();
        if (command.getTextChannel()) {
            Device device = storage.getObject(Device.class, new Request(
                    new Columns.Include("positionId", "phone"), new Condition.Equals("id", "id", deviceId)));
            Position position = storage.getObject(Position.class, new Request(
                    new Columns.All(), new Condition.Equals("id", "id", device.getPositionId())));
            if (position != null) {
                BaseProtocol protocol = serverManager.getProtocol(position.getProtocol());
                protocol.sendTextCommand(device.getPhone(), command);
            } else if (command.getType().equals(Command.TYPE_CUSTOM)) {
                smsManager.sendMessage(device.getPhone(), command.getString(Command.KEY_DATA), true);
            } else {
                throw new RuntimeException("Command " + command.getType() + " is not supported");
            }
        } else {
            DeviceSession deviceSession = connectionManager.getDeviceSession(deviceId);
            if (deviceSession != null) {
                if (deviceSession.supportsLiveCommands()) {
                    deviceSession.sendCommand(command);
                } else {
                    getDeviceQueue(deviceId).add(command);
                    return false;
                }
            } else if (!queueing) {
                throw new RuntimeException("Device is not online");
            } else {
                getDeviceQueue(deviceId).add(command);
                return false;
            }
        }
        return true;
    }

    private Queue<Command> getDeviceQueue(long deviceId) {
        Queue<Command> deviceQueue;
        try {
            lock.readLock().lock();
            deviceQueue = deviceQueues.get(deviceId);
        } finally {
            lock.readLock().unlock();
        }
        if (deviceQueue != null) {
            return deviceQueue;
        } else {
            try {
                lock.writeLock().lock();
                return deviceQueues.computeIfAbsent(deviceId, key -> new ConcurrentLinkedQueue<>());
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public Collection<Command> readQueuedCommands(long deviceId) {
        return readQueuedCommands(deviceId, Integer.MAX_VALUE);
    }

    public Collection<Command> readQueuedCommands(long deviceId, int count) {
        Queue<Command> deviceQueue;
        try {
            lock.readLock().lock();
            deviceQueue = deviceQueues.get(deviceId);
        } finally {
            lock.readLock().unlock();
        }
        Collection<Command> result = new ArrayList<>();
        if (deviceQueue != null) {
            Command command = deviceQueue.poll();
            while (command != null && result.size() < count) {
                result.add(command);
                command = deviceQueue.poll();
            }
        }
        return result;
    }

}
