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
import org.traccar.broadcast.BroadcastInterface;
import org.traccar.broadcast.BroadcastService;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.model.QueuedCommand;
import org.traccar.session.ConnectionManager;
import org.traccar.session.DeviceSession;
import org.traccar.sms.SmsManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.stream.Collectors;

@Singleton
public class CommandsManager implements BroadcastInterface {

    private final Storage storage;
    private final ServerManager serverManager;
    private final SmsManager smsManager;
    private final ConnectionManager connectionManager;
    private final BroadcastService broadcastService;

    @Inject
    public CommandsManager(
            Storage storage, ServerManager serverManager, @Nullable SmsManager smsManager,
            ConnectionManager connectionManager, BroadcastService broadcastService) {
        this.storage = storage;
        this.serverManager = serverManager;
        this.smsManager = smsManager;
        this.connectionManager = connectionManager;
        this.broadcastService = broadcastService;
        broadcastService.registerListener(this);
    }

    public boolean sendCommand(Command command) throws Exception {
        long deviceId = command.getDeviceId();
        if (command.getTextChannel()) {
            if (smsManager == null) {
                throw new RuntimeException("SMS not configured");
            }
            Device device = storage.getObject(Device.class, new Request(
                    new Columns.Include("positionId", "phone"), new Condition.Equals("id", deviceId)));
            Position position = storage.getObject(Position.class, new Request(
                    new Columns.All(), new Condition.Equals("id", device.getPositionId())));
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
            if (deviceSession != null && deviceSession.supportsLiveCommands()) {
                deviceSession.sendCommand(command);
            } else {
                storage.addObject(QueuedCommand.fromCommand(command), new Request(new Columns.Exclude("id")));
                broadcastService.updateCommand(true, deviceId);
                return false;
            }
        }
        return true;
    }

    public Collection<Command> readQueuedCommands(long deviceId) {
        return readQueuedCommands(deviceId, Integer.MAX_VALUE);
    }

    public Collection<Command> readQueuedCommands(long deviceId, int count) {
        try {
            var commands = storage.getObjects(QueuedCommand.class, new Request(
                    new Columns.All(),
                    new Condition.Equals("deviceId", deviceId),
                    new Order("id", false, count)));
            for (var command : commands) {
                storage.removeObject(QueuedCommand.class, new Request(
                        new Condition.Equals("id", command.getId())));
            }
            return commands.stream().map(QueuedCommand::toCommand).collect(Collectors.toList());
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateCommand(boolean local, long deviceId) {
        if (!local) {
            DeviceSession deviceSession = connectionManager.getDeviceSession(deviceId);
            if (deviceSession != null && deviceSession.supportsLiveCommands()) {
                for (Command command : readQueuedCommands(deviceId)) {
                    deviceSession.sendCommand(command);
                }
            }
        }
    }

}
