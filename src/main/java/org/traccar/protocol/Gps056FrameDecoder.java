/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseFrameDecoder;

import java.nio.charset.StandardCharsets;

public class Gps056FrameDecoder extends BaseFrameDecoder {

    private static final int MESSAGE_HEADER = 4;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() >= MESSAGE_HEADER) {
            int length = Integer.parseInt(buf.toString(2, 2, StandardCharsets.US_ASCII)) + 5;
            if (buf.readableBytes() >= length) {
                ByteBuf frame = buf.readRetainedSlice(length);
                while (buf.isReadable() && buf.getUnsignedByte(buf.readerIndex()) != '$') {
                    buf.readByte();
                }
                return frame;
            }
        }

        return null;
    }

}
