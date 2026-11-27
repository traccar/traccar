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

public class TranSyncFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 5) {
            return null;
        }

        int index = 3 + buf.getUnsignedByte(buf.readerIndex() + 2) + buf.readerIndex();
        while (buf.writerIndex() - index >= 5) {
            int tagIndex = buf.getUnsignedByte(index);
            if (tagIndex == 0x23) {
                break;
            }
            index += 1;
            int tagLength = buf.getUnsignedByte(index++);
            index += tagLength;
        }

        if (buf.getUnsignedShort(index) == 0x2323) {
            index += 2;
            return buf.readRetainedSlice(index - buf.readerIndex());
        }

        return null;
    }

}
