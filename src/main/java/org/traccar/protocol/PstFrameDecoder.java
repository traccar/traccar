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

public class PstFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        while (buf.isReadable() && buf.getByte(buf.readerIndex()) == 0x28) {
            buf.skipBytes(1);
        }

        int endIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) 0x29);
        if (endIndex > 0) {
            ByteBuf result = Unpooled.buffer(endIndex - buf.readerIndex());
            while (buf.readerIndex() < endIndex) {
                int b = buf.readUnsignedByte();
                if (b == 0x27) {
                    b = buf.readUnsignedByte() ^ 0x40;
                }
                result.writeByte(b);
            }
            buf.skipBytes(1);
            return result;
        }

        return null;
    }

}
