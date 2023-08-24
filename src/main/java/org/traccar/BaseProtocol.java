/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.string.StringEncoder;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;
import org.traccar.sms.SmsManager;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import java.net.SocketAddress;
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
    private final List<TrackerConnector> connectorList = new LinkedList<>();

    private SmsManager smsManager;

    private StringProtocolEncoder textCommandEncoder = null;

    public static String nameFromClass(Class<?> clazz) {
        String className = clazz.getSimpleName();
        return className.substring(0, className.length() - 8).toLowerCase();
    }

    public BaseProtocol() {
        name = nameFromClass(getClass());
    }

    @Inject
    public void setSmsManager(@Nullable SmsManager smsManager) {
        this.smsManager = smsManager;
    }

    @Override
    public String getName() {
        return name;
    }

    protected void addServer(TrackerServer server) {
        connectorList.add(server);
    }

    protected void addClient(TrackerClient client) {
        connectorList.add(client);
    }

    @Override
    public Collection<TrackerConnector> getConnectorList() {
        return connectorList;
    }

    public void setSupportedDataCommands(String... commands) {
        supportedDataCommands.addAll(Arrays.asList(commands));
    }

    public void setSupportedTextCommands(String... commands) {
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
    public void sendDataCommand(Channel channel, SocketAddress remoteAddress, Command command) {
        if (supportedDataCommands.contains(command.getType())) {
            channel.writeAndFlush(new NetworkMessage(command, remoteAddress));
        } else if (command.getType().equals(Command.TYPE_CUSTOM)) {
            String data = command.getString(Command.KEY_DATA);
            if (BasePipelineFactory.getHandler(channel.pipeline(), StringEncoder.class) != null) {
                channel.writeAndFlush(new NetworkMessage(
                        data.replace("\\r", "\r").replace("\\n", "\n"), remoteAddress));
            } else {
                ByteBuf buf = Unpooled.wrappedBuffer(DataConverter.parseHex(data));
                channel.writeAndFlush(new NetworkMessage(buf, remoteAddress));
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
        if (smsManager != null) {
            if (command.getType().equals(Command.TYPE_CUSTOM)) {
                smsManager.sendMessage(destAddress, command.getString(Command.KEY_DATA), true);
            } else if (supportedTextCommands.contains(command.getType()) && textCommandEncoder != null) {
                String encodedCommand = (String) textCommandEncoder.encodeCommand(command);
                if (encodedCommand != null) {
                    smsManager.sendMessage(destAddress, encodedCommand, true);
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
