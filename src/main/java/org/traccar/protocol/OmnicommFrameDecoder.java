/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseFrameDecoder;

public class OmnicommFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 6) {
            return null;
        }

        int endIndex = buf.getUnsignedShortLE(buf.readerIndex() + 2) + buf.readerIndex() + 6;
        if (buf.writerIndex() < endIndex) {
            return null;
        }

        ByteBuf result = Unpooled.buffer();
        result.writeByte(buf.readUnsignedByte());
        while (buf.readerIndex() < endIndex) {
            int b = buf.readUnsignedByte();
            if (b == 0xDB) {
                int ext = buf.readUnsignedByte();
                if (ext == 0xDC) {
                    result.writeByte(0xC0);
                } else if (ext == 0xDD) {
                    result.writeByte(0xDB);
                }
                endIndex += 1;
            } else {
                result.writeByte(b);
            }
        }
        return result;
    }

}
