/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocol;

public class IterFrameDecoder extends BaseFrameDecoder {

    public IterFrameDecoder() {
        super(BaseProtocol.MAX_FRAME_LENGTH_LARGE);
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 5) {
            return null;
        }

        // skip leading garbage until the 0x28 0x28 mark is found
        int index = buf.forEachByte(buf.readerIndex(), buf.readableBytes() - 1,
                value -> value != 0x28);
        if (index < 0) {
            buf.readerIndex(buf.readerIndex() + buf.readableBytes() - 1);
            return null;
        }
        if (buf.getByte(index + 1) != 0x28) {
            buf.readerIndex(index + 1);
            return null;
        }
        buf.readerIndex(index);

        int length = 2 + 1 + 2 + buf.getUnsignedShort(buf.readerIndex() + 3);
        if (buf.readableBytes() >= length) {
            return buf.readRetainedSlice(length);
        }

        return null;
    }

}
