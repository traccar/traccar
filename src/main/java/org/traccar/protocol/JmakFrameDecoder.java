/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class JmakFrameDecoder extends JsonFrameDecoder {

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {
        if (buf.readableBytes() == 0) {
            return null;
        }
        byte first = buf.getByte(buf.readerIndex());

        if (first == '{') {
            return super.decode(ctx, channel, buf);
        }

        if (first == '~' || first == '^') {
            int delimiterIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '$');
            if (delimiterIndex > 0) {
                return buf.readRetainedSlice(delimiterIndex - buf.readerIndex() + 1);
            }
        }

        return null;
    }
}
