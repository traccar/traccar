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
        int index = 0;
        int end = msg.readableBytes();

        while (msg.isReadable()) {
            int b = msg.readUnsignedByte();
            if (++index > 1 && index < end) {
                if (b == 0x7E) {
                    out.writeBytes(new byte[]{0x7D, 0x02});
                } else if (b == 0x7D) {
                    out.writeBytes(new byte[]{0x7D, 0x01});
                } else {
                    out.writeByte(b);
                }
            } else {
                out.writeByte(b);
            }
        }
    }
}

