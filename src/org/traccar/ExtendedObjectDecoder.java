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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.traccar.helper.DataConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public abstract class ExtendedObjectDecoder extends ChannelInboundHandlerAdapter {

    private void saveOriginal(Object decodedMessage, Object originalMessage) {
        if (Context.getConfig().getBoolean("database.saveOriginal") && decodedMessage instanceof Position) {
            Position position = (Position) decodedMessage;
            if (originalMessage instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) originalMessage;
                position.set(Position.KEY_ORIGINAL, ByteBufUtil.hexDump(buf));
            } else if (originalMessage instanceof String) {
                position.set(Position.KEY_ORIGINAL, DataConverter.printHex(
                                ((String) originalMessage).getBytes(StandardCharsets.US_ASCII)));
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object originalMessage) throws Exception {
        Object decodedMessage = decode(ctx.channel(), ctx.channel().remoteAddress(), originalMessage);
        onMessageEvent(ctx.channel(), ctx.channel().remoteAddress(), originalMessage, decodedMessage);
        if (originalMessage == decodedMessage) {
            ctx.fireChannelRead(originalMessage);
        } else {
            if (decodedMessage == null) {
                decodedMessage = handleEmptyMessage(ctx.channel(), ctx.channel().remoteAddress(), originalMessage);
            }
            if (decodedMessage != null) {
                if (decodedMessage instanceof Collection) {
                    for (Object o : (Collection) decodedMessage) {
                        saveOriginal(o, originalMessage);
                        ctx.fireChannelRead(o);
                    }
                } else {
                    saveOriginal(decodedMessage, originalMessage);
                    ctx.fireChannelRead(decodedMessage);
                }
            }
        }
    }

    protected void onMessageEvent(
            Channel channel, SocketAddress remoteAddress, Object originalMessage, Object decodedMessage) {
    }

    protected Object handleEmptyMessage(Channel channel, SocketAddress remoteAddress, Object msg) {
        return null;
    }

    protected abstract Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception;

}
