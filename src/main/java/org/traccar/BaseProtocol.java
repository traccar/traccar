/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.string.StringEncoder;
import org.traccar.database.ActiveDevice;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class BaseProtocol implements Protocol {

    private final String name;
    private final Set<String> supportedDataCommands = new HashSet<>();
    private final Set<String> supportedTextCommands = new HashSet<>();
    private final List<TrackerServer> serverList = new LinkedList<>();

    private StringProtocolEncoder textCommandEncoder = null;

    public static String nameFromClass(Class<?> clazz) {
        String className = clazz.getSimpleName();
        return className.substring(0, className.length() - 8).toLowerCase();
    }

    public BaseProtocol() {
        name = nameFromClass(getClass());
    }

    @Override
    public String getName() {
        return name;
    }

    protected void addServer(TrackerServer server) {
        serverList.add(server);
    }

    @Override
    public Collection<TrackerServer> getServerList() {
        return serverList;
    }

    public void setSupportedDataCommands(String... commands) {
        supportedDataCommands.addAll(Arrays.asList(commands));
    }

    public void setSupportedTextCommands(String... commands) {
        supportedTextCommands.addAll(Arrays.asList(commands));
    }

    public void setSupportedCommands(String... commands) {
        supportedDataCommands.addAll(Arrays.asList(commands));
        supportedTextCommands.addAll(Arrays.asList(commands));
    }

    @Override
    public Collection<String> getSupportedDataCommands() {
        Set<String> commands = new HashSet<>(supportedDataCommands);
        commands.add(Command.TYPE_CUSTOM);
        return commands;
    }

    @Override
    public Collection<String> getSupportedTextCommands() {
        Set<String> commands = new HashSet<>(supportedTextCommands);
        commands.add(Command.TYPE_CUSTOM);
        return commands;
    }

    @Override
    public void sendDataCommand(ActiveDevice activeDevice, Command command) {
        if (supportedDataCommands.contains(command.getType())) {
            activeDevice.write(command);
        } else if (command.getType().equals(Command.TYPE_CUSTOM)) {
            String data = command.getString(Command.KEY_DATA);
            if (BasePipelineFactory.getHandler(activeDevice.getChannel().pipeline(), StringEncoder.class) != null) {
                activeDevice.write(data);
            } else {
                activeDevice.write(Unpooled.wrappedBuffer(DataConverter.parseHex(data)));
            }
        } else {
            throw new RuntimeException("Command " + command.getType() + " is not supported in protocol " + getName());
        }
    }

    public void setTextCommandEncoder(StringProtocolEncoder textCommandEncoder) {
        this.textCommandEncoder = textCommandEncoder;
    }

    @Override
    public void sendTextCommand(String destAddress, Command command) throws Exception {
        if (Context.getSmsManager() != null) {
            if (command.getType().equals(Command.TYPE_CUSTOM)) {
                Context.getSmsManager().sendMessageSync(destAddress, command.getString(Command.KEY_DATA), true);
            } else if (supportedTextCommands.contains(command.getType()) && textCommandEncoder != null) {
                String encodedCommand = (String) textCommandEncoder.encodeCommand(command);
                if (encodedCommand != null) {
                    Context.getSmsManager().sendMessageSync(destAddress, encodedCommand, true);
                } else {
                    throw new RuntimeException("Failed to encode command");
                }
            } else {
                throw new RuntimeException(
                        "Command " + command.getType() + " is not supported in protocol " + getName());
            }
        } else {
            throw new RuntimeException("SMS is not enabled");
        }
    }

}
