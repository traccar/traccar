/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class HuabaoFrameEncoder extends MessageToByteEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {

        boolean alternative = msg.getUnsignedByte(msg.readerIndex()) == 0xe7;

        int startIndex = msg.readerIndex();
        while (msg.isReadable()) {
            int index = msg.readerIndex();
            int b = msg.readUnsignedByte();
            if (alternative && (b == 0xe6 || b == 0x3d || b == 0x3e)) {
                out.writeByte(b == 0xe6 ? 0xe6 : 0x3e);
                out.writeByte(b == 0x3d ? 0x02 : 0x01);
            } else if (alternative && b == 0xe7 && index != startIndex && msg.isReadable()) {
                out.writeByte(0xe6);
                out.writeByte(0x02);
            } else if (!alternative && b == 0x7d) {
                out.writeByte(0x7d);
                out.writeByte(0x01);
            } else if (!alternative && b == 0x7e && index != startIndex && msg.isReadable()) {
                out.writeByte(0x7d);
                out.writeByte(0x02);
            } else {
                out.writeByte(b);
            }
        }
    }
}
