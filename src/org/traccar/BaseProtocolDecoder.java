/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.SocketAddress;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import static org.jboss.netty.channel.Channels.fireMessageReceived;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import org.traccar.helper.Log;
import org.traccar.model.Device;

/**
 * Base class for protocol decoders
 */
public abstract class BaseProtocolDecoder extends ExtendedObjectDecoder {

    private final Protocol protocol;

    public String getProtocolName() {
        return protocol.getName();
    }

    private long deviceId;

    public boolean hasDeviceId() {
        return (deviceId != 0);
    }

    public long getDeviceId() {
        return deviceId;
    }

    public boolean identify(String uniqueId, Channel channel, SocketAddress remoteAddress, boolean logWarning) {
        try {
            Device device = Context.getDataManager().getDeviceByUniqueId(uniqueId);
            if (device != null) {
                deviceId = device.getId();
                Context.getConnectionManager().setActiveDevice(device.getUniqueId(), protocol, channel, remoteAddress);
                return true;
            } else {
                deviceId = 0;
                if (logWarning) {
                    Log.warning("Unknown device - " + uniqueId);
                }
                return false;
            }
        } catch (Exception error) {
            deviceId = 0;
            Log.warning(error);
            return false;
        }
    }

    public boolean identify(String uniqueId, Channel channel, SocketAddress remoteAddress) {
        return identify(uniqueId, channel, remoteAddress, true);
    }

    public boolean identify(String uniqueId, Channel channel) {
        return identify(uniqueId, channel, null, true);
    }

    public BaseProtocolDecoder(Protocol protocol) {
        this.protocol = protocol;
    }

}
