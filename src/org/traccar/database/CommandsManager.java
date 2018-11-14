/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.hazelcast.nio.Address;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.OperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocol;
import org.traccar.Context;
import org.traccar.model.Command;
import org.traccar.model.Typed;
import org.traccar.model.Position;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;

public class CommandsManager  extends ExtendedObjectManager<Command> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandsManager.class);

    private final Cache<Long, Queue<Command>> deviceQueues;

    private boolean queueing;

    public CommandsManager(DataManager dataManager, boolean queueing) {
        super(dataManager, Command.class);
        deviceQueues = Context.getCacheManager().createCache(
                this.getClass().getSimpleName() + "DeviceQueues", new MutableConfiguration<>());
        this.queueing = queueing;
    }

    public boolean checkDeviceCommand(long deviceId, long commandId) {
        return !getAllDeviceItems(deviceId).contains(commandId);
    }

    public static class CommandOperation extends Operation {

        private boolean result;
        private Command command;

        public CommandOperation() {
        }

        public CommandOperation(Command command) {
            this.command = command;
        }

        @Override
        public Object getResponse() {
            return result;
        }

        @Override
        public void run() throws Exception {
            result = Context.getCommandsManager().sendCommand(command);
        }

        @Override
        protected void writeInternal(ObjectDataOutput out) throws IOException {
            out.writeBoolean(result);
            out.writeObject(command);
        }

        @Override
        protected void readInternal(ObjectDataInput in) throws IOException {
            result = in.readBoolean();
            command = in.readObject();
        }

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
                activeDevice.sendCommand(command);
            } else {
                Address nodeAddress = Context.getConnectionManager().getActiveDeviceNode(deviceId);
                if (nodeAddress != null) {
                    OperationService operationService =
                            Context.getHazelcastNode().getNodeEngine().getOperationService();
                    Boolean result = (Boolean) operationService.invokeOnTarget(
                            "command", new CommandOperation(command), nodeAddress).join();
                    if (result != null && result) {
                        return true;
                    } else {
                        throw new RuntimeException("Command failed on remote node");
                    }
                } else if (!queueing) {
                    throw new RuntimeException("Device is not online");
                } else {
                    Queue<Command> commandQueue = deviceQueues.get(deviceId);
                    if (commandQueue == null) {
                        commandQueue = new LinkedList<>();
                    }
                    commandQueue.add(command);
                    deviceQueues.put(deviceId, commandQueue);
                    return false;
                }
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
        List<Typed> result = new ArrayList<>();
        Position lastPosition = Context.getIdentityManager().getLastPosition(deviceId);
        if (lastPosition != null) {
            BaseProtocol protocol = Context.getServerManager().getProtocol(lastPosition.getProtocol());
            Collection<String> commands;
            commands = textChannel ? protocol.getSupportedTextCommands() : protocol.getSupportedDataCommands();
            for (String commandKey : commands) {
                result.add(new Typed(commandKey));
            }
        } else {
            result.add(new Typed(Command.TYPE_CUSTOM));
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

    public void sendQueuedCommands(ActiveDevice activeDevice) {
        Queue<Command> deviceQueue = deviceQueues.get(activeDevice.getDeviceId());
        if (deviceQueue != null) {
            Command command = deviceQueue.poll();
            while (command != null) {
                activeDevice.sendCommand(command);
                command = deviceQueue.poll();
            }
            deviceQueues.put(activeDevice.getDeviceId(), deviceQueue);
        }
    }

}
