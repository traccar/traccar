/*
 * Copyright 2017 - 2020 Anton Tananaev (anton@traccar.org)
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocol;
import org.traccar.Context;
import org.traccar.model.Command;
import org.traccar.model.Typed;
import org.traccar.model.Position;

public class CommandsManager  extends ExtendedObjectManager<Command> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandsManager.class);

    private final Map<Long, Queue<Command>> deviceQueues = new ConcurrentHashMap<>();

    private final boolean queueing;

    public CommandsManager(DataManager dataManager, boolean queueing) {
        super(dataManager, Command.class);
        this.queueing = queueing;
    }

    public boolean checkDeviceCommand(long deviceId, long commandId) {
        return !getAllDeviceItems(deviceId).contains(commandId);
    }

    public boolean sendCommand(Command command) throws Exception {
        long deviceId = command.getDeviceId();
        if (command.getId() != 0) {
            command = getById(command.getId()).clone();
            command.setDeviceId(deviceId);
        }
        if (command.getTextChannel()) {
            Position lastPosition = Context.getIdentityManager().getLastPosition(deviceId);
            String phone = Context.getIdentityManager().getById(deviceId).getPhone();
            if (lastPosition != null) {
                BaseProtocol protocol = Context.getServerManager().getProtocol(lastPosition.getProtocol());
                protocol.sendTextCommand(phone, command);
            } else if (command.getType().equals(Command.TYPE_CUSTOM)) {
                if (Context.getSmsManager() != null) {
                    Context.getSmsManager().sendMessageSync(phone, command.getString(Command.KEY_DATA), true);
                } else {
                    throw new RuntimeException("SMS is not enabled");
                }
            } else {
                throw new RuntimeException("Command " + command.getType() + " is not supported");
            }
        } else {
            ActiveDevice activeDevice = Context.getConnectionManager().getActiveDevice(deviceId);
            if (activeDevice != null) {
                if (activeDevice.supportsLiveCommands()) {
                    activeDevice.sendCommand(command);
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

    public Collection<Long> getSupportedCommands(long deviceId) {
        List<Long> result = new ArrayList<>();
        Position lastPosition = Context.getIdentityManager().getLastPosition(deviceId);
        for (long commandId : getAllDeviceItems(deviceId)) {
            Command command = getById(commandId);
            if (lastPosition != null) {
                BaseProtocol protocol = Context.getServerManager().getProtocol(lastPosition.getProtocol());
                if (command.getTextChannel() && protocol.getSupportedTextCommands().contains(command.getType())
                        || !command.getTextChannel()
                        && protocol.getSupportedDataCommands().contains(command.getType())) {
                    result.add(commandId);
                }
            } else if (command.getType().equals(Command.TYPE_CUSTOM)) {
                result.add(commandId);
            }
        }
        return result;
    }

    public Collection<Typed> getCommandTypes(long deviceId, boolean textChannel) {
        Position lastPosition = Context.getIdentityManager().getLastPosition(deviceId);
        if (lastPosition != null) {
            return getCommandTypes(lastPosition.getProtocol(), textChannel);
        } else {
            return Collections.singletonList(new Typed(Command.TYPE_CUSTOM));
        }
    }

    public Collection<Typed> getCommandTypes(String protocolName, boolean textChannel) {
        List<Typed> result = new ArrayList<>();
        BaseProtocol protocol = Context.getServerManager().getProtocol(protocolName);
        Collection<String> commands;
        commands = textChannel ? protocol.getSupportedTextCommands() : protocol.getSupportedDataCommands();
        for (String commandKey : commands) {
            result.add(new Typed(commandKey));
        }
        return result;
    }

    public Collection<Typed> getAllCommandTypes() {
        List<Typed> result = new ArrayList<>();
        Field[] fields = Command.class.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && field.getName().startsWith("TYPE_")) {
                try {
                    result.add(new Typed(field.get(null).toString()));
                } catch (IllegalArgumentException | IllegalAccessException error) {
                    LOGGER.warn("Get command types error", error);
                }
            }
        }
        return result;
    }

    private Queue<Command> getDeviceQueue(long deviceId) {
        Queue<Command> deviceQueue;
        try {
            readLock();
            deviceQueue = deviceQueues.get(deviceId);
        } finally {
            readUnlock();
        }
        if (deviceQueue != null) {
            return deviceQueue;
        } else {
            try {
                writeLock();
                return deviceQueues.computeIfAbsent(deviceId, key -> new ConcurrentLinkedQueue<>());
            } finally {
                writeUnlock();
            }
        }
    }

    public Collection<Command> readQueuedCommands(long deviceId) {
        return readQueuedCommands(deviceId, Integer.MAX_VALUE);
    }

    public Collection<Command> readQueuedCommands(long deviceId, int count) {
        Queue<Command> deviceQueue;
        try {
            readLock();
            deviceQueue = deviceQueues.get(deviceId);
        } finally {
            readUnlock();
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
