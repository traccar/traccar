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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.helper.NetworkUtil;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.session.cache.CacheManager;

import javax.inject.Inject;

public abstract class BaseProtocolEncoder extends ChannelOutboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseProtocolEncoder.class);

    private static final String PROTOCOL_UNKNOWN = "unknown";

    private final Protocol protocol;

    private CacheManager cacheManager;

    public BaseProtocolEncoder(Protocol protocol) {
        this.protocol = protocol;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    @Inject
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public String getProtocolName() {
        return protocol != null ? protocol.getName() : PROTOCOL_UNKNOWN;
    }

    protected String getUniqueId(long deviceId) {
        return cacheManager.getObject(Device.class, deviceId).getUniqueId();
    }

    protected void initDevicePassword(Command command, String defaultPassword) {
        if (!command.getAttributes().containsKey(Command.KEY_DEVICE_PASSWORD)) {
            String password = AttributeUtil.getDevicePassword(
                    cacheManager, command.getDeviceId(), getProtocolName(), defaultPassword);
            command.set(Command.KEY_DEVICE_PASSWORD, password);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        NetworkMessage networkMessage = (NetworkMessage) msg;

        if (networkMessage.getMessage() instanceof Command) {

            Command command = (Command) networkMessage.getMessage();
            Object encodedCommand = encodeCommand(ctx.channel(), command);

            StringBuilder s = new StringBuilder();
            s.append("[").append(NetworkUtil.session(ctx.channel())).append("] ");
            s.append("id: ").append(getUniqueId(command.getDeviceId())).append(", ");
            s.append("command type: ").append(command.getType()).append(" ");
            if (encodedCommand != null) {
                s.append("sent");
            } else {
                s.append("not sent");
            }
            LOGGER.info(s.toString());

            ctx.write(new NetworkMessage(encodedCommand, networkMessage.getRemoteAddress()), promise);

        } else {

            super.write(ctx, msg, promise);

        }
    }

    protected Object encodeCommand(Channel channel, Command command) {
        return encodeCommand(command);
    }

    protected Object encodeCommand(Command command) {
        return null;
    }

}
