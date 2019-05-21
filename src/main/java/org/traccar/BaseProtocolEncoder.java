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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.Command;
import org.traccar.model.Device;

public abstract class BaseProtocolEncoder extends ChannelOutboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseProtocolEncoder.class);

    protected String getUniqueId(long deviceId) {
        return Context.getIdentityManager().getById(deviceId).getUniqueId();
    }

    protected void initDevicePassword(Command command, String defaultPassword) {
        if (!command.getAttributes().containsKey(Command.KEY_DEVICE_PASSWORD)) {
            Device device = Context.getIdentityManager().getById(command.getDeviceId());
            String password = device.getString(Command.KEY_DEVICE_PASSWORD);
            if (password != null) {
                command.set(Command.KEY_DEVICE_PASSWORD, password);
            } else {
                command.set(Command.KEY_DEVICE_PASSWORD, defaultPassword);
            }
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        NetworkMessage networkMessage = (NetworkMessage) msg;

        if (networkMessage.getMessage() instanceof Command) {

            Command command = (Command) networkMessage.getMessage();
            Object encodedCommand = encodeCommand(ctx.channel(), command);

            StringBuilder s = new StringBuilder();
            s.append("[").append(ctx.channel().id().asShortText()).append("] ");
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
