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
import org.traccar.helper.BitUtil;

public class Jt1078FrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 30) {
            return null;
        }

        int startIndex = buf.readerIndex();
        int index = startIndex + 15; // skip header

        int type = BitUtil.from(buf.getUnsignedByte(index), 4);
        index += 1 + 8; // data type + timestamp

        if (type <= 2) {
            index += 4; // i-frame interval + frame interval
        }

        if (buf.readableBytes() < index - startIndex + 2) {
            return null;
        }

        int bodyLength = buf.getUnsignedShort(index);
        index += 2;

        int length = index - startIndex + bodyLength;
        if (buf.readableBytes() < length) {
            return null;
        }

        return buf.readRetainedSlice(length);
    }

}
