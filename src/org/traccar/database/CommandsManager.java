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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.traccar.BaseProtocol;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Command;
import org.traccar.model.CommandType;
import org.traccar.model.Position;

public class CommandsManager  extends ExtendedObjectManager<Command> {

    private boolean fallbackToText;

    public CommandsManager(DataManager dataManager) {
        super(dataManager, Command.class);
        fallbackToText = Context.getConfig().getBoolean("command.fallbackToSms");
    }

    public boolean checkDeviceCommand(long deviceId, long commandId) {
        return !getAllDeviceItems(deviceId).contains(commandId);
    }

    public void sendCommand(Command command) throws Exception {
        sendCommand(command, command.getDeviceId(), fallbackToText);
    }

    public void sendCommand(long commandId, long deviceId) throws Exception {
        sendCommand(getById(commandId), deviceId, false);
    }

    public void sendCommand(Command command, long deviceId, boolean fallbackToText) throws Exception {
        if (command.getTextChannel()) {
            Position lastPosition = Context.getIdentityManager().getLastPosition(deviceId);
            String phone = Context.getIdentityManager().getById(deviceId).getPhone();
            if (lastPosition != null) {
                BaseProtocol protocol = Context.getServerManager().getProtocol(lastPosition.getProtocol());
                protocol.sendTextCommand(phone, command);
            } else if (command.getType().equals(Command.TYPE_CUSTOM)) {
                if (Context.getSmppManager() != null) {
                    Context.getSmppManager().sendMessageSync(phone, command.getString(Command.KEY_DATA), true);
                } else {
                    throw new RuntimeException("SMPP client is not enabled");
                }
            } else {
                throw new RuntimeException("Command " + command.getType() + " is not supported");
            }
        } else {
            ActiveDevice activeDevice = Context.getConnectionManager().getActiveDevice(deviceId);
            if (activeDevice != null) {
                activeDevice.sendCommand(command);
            } else {
                if (fallbackToText) {
                    command.setTextChannel(true);
                    sendCommand(command, deviceId, false);
                } else {
                    throw new RuntimeException("Device is not online");
                }
            }
        }
    }

    public Collection<Long> getSupportedCommands(long deviceId) {
        List<Long> result = new ArrayList<>();
        Position lastPosition = Context.getIdentityManager().getLastPosition(deviceId);
        boolean online = Context.getConnectionManager().getActiveDevice(deviceId) != null;
        for (long commandId : getAllDeviceItems(deviceId)) {
            Command command = getById(commandId);
            if (command.getTextChannel() || online) {
                if (lastPosition != null) {
                    BaseProtocol protocol = Context.getServerManager().getProtocol(lastPosition.getProtocol());
                    if (protocol.getSupportedTextCommands().contains(command.getType())
                            || online && protocol.getSupportedDataCommands().contains(command.getType())) {
                        result.add(commandId);
                    }
                } else if (command.getType().equals(Command.TYPE_CUSTOM)) {
                    result.add(commandId);
                }
            }
        }
        return result;
    }

    public Collection<CommandType> getCommandTypes(long deviceId, boolean textChannel) {
        List<CommandType> result = new ArrayList<>();
        Position lastPosition = Context.getIdentityManager().getLastPosition(deviceId);
        if (lastPosition != null) {
            BaseProtocol protocol = Context.getServerManager().getProtocol(lastPosition.getProtocol());
            Collection<String> commands;
            commands = textChannel ? protocol.getSupportedTextCommands() : protocol.getSupportedDataCommands();
            for (String commandKey : commands) {
                result.add(new CommandType(commandKey));
            }
        } else {
            result.add(new CommandType(Command.TYPE_CUSTOM));
        }
        return result;
    }

    public Collection<CommandType> getAllCommandTypes() {
        List<CommandType> result = new ArrayList<>();
        Field[] fields = Command.class.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && field.getName().startsWith("TYPE_")) {
                try {
                    result.add(new CommandType(field.get(null).toString()));
                } catch (IllegalArgumentException | IllegalAccessException error) {
                    Log.warning(error);
                }
            }
        }
        return result;
    }

}
