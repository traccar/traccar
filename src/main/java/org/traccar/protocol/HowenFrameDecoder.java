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
import org.traccar.BaseFrameDecoder;

public class HowenFrameDecoder extends BaseFrameDecoder {

    private static final int HEADER_LENGTH = 8;
    private static final int HEADER_FLAG = 0x48;

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        while (buf.readableBytes() > 0 && buf.getUnsignedByte(buf.readerIndex()) != HEADER_FLAG) {
            buf.readUnsignedByte();
        }

        if (buf.readableBytes() < HEADER_LENGTH) {
            return null;
        }

        int length = buf.getIntLE(buf.readerIndex() + 4);
        int frameLength = HEADER_LENGTH + length;

        if (buf.readableBytes() >= frameLength) {
            return buf.readRetainedSlice(frameLength);
        }

        return null;
    }
}
