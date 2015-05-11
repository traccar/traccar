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
import java.util.Properties;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import static org.jboss.netty.channel.Channels.fireMessageReceived;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import org.traccar.database.DataManager;
import org.traccar.helper.Log;
import org.traccar.model.Device;

/**
 * Base class for protocol decoders
 */
public abstract class BaseProtocolDecoder extends OneToOneDecoder {

    private final String protocol;

    public String getProtocol() {
        return protocol;
    }

    private long deviceId;

    public boolean hasDeviceId() {
        return (deviceId != 0);
    }

    public long getDeviceId() {
        return deviceId;
    }

    public boolean identify(String uniqueId, boolean logWarning) {
        try {
            Device device = Context.getDataManager().getDeviceByUniqueId(uniqueId);
            if (device != null) {
                deviceId = device.getId();
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

    public boolean identify(String uniqueId) {
        return identify(uniqueId, true);
    }

    public BaseProtocolDecoder(String protocol) {
        this.protocol = protocol;
    }
    
    @Override
    public void handleUpstream(
            ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        if (!(evt instanceof MessageEvent)) {
            ctx.sendUpstream(evt);
            return;
        }

        MessageEvent e = (MessageEvent) evt;
        Object originalMessage = e.getMessage();
        Object decodedMessage = decode(ctx, e.getChannel(), e.getRemoteAddress(), originalMessage);
        if (originalMessage == decodedMessage) {
            ctx.sendUpstream(evt);
        } else if (decodedMessage != null) {
            fireMessageReceived(ctx, decodedMessage, e.getRemoteAddress());
        }
    }
    
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        
        return decode(ctx, channel, msg);
    }
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        
        return null; // default implementation
    }

}
