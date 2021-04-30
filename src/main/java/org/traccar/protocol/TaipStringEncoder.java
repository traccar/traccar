/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
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
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.traccar.Context;
import org.traccar.Protocol;
import org.traccar.config.Keys;

import java.nio.charset.StandardCharsets;
import java.util.List;

@ChannelHandler.Sharable
public class TaipStringEncoder extends MessageToMessageEncoder<CharSequence> {

    private final Protocol protocol;

    public TaipStringEncoder(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CharSequence msg, List<Object> out) throws Exception {
        if (msg.length() == 0) {
            return;
        }
        ByteBuf buf = Unpooled.buffer();
        if (Context.getConfig().getBoolean(Keys.PROTOCOL_PREFIX.withPrefix(protocol.getName()))) {
            buf.writeBytes(new byte[] {0x20, 0x20, 0x06, 0x00});
        }
        buf.writeCharSequence(msg, StandardCharsets.US_ASCII);
        out.add(buf);
    }

}
