/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.jboss.netty.channel.Channel;
import org.traccar.Protocol;
import org.traccar.command.GpsCommand;

import java.net.SocketAddress;

public class ActiveDevice {

    private String uniqueId;
    private Protocol protocol;
    private Channel channel;
    private SocketAddress remoteAddress;

    public ActiveDevice(String uniqueId, Protocol protocol, Channel channel, SocketAddress remoteAddress) {
        this.uniqueId = uniqueId;
        this.protocol = protocol;
        this.channel = channel;
        this.remoteAddress = remoteAddress;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void sendCommand(GpsCommand command) {
        protocol.sendCommand(this, command);
    }

    public void write(Object message) {
        getChannel().write(message, remoteAddress);
    }

}
