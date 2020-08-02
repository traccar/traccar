/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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

public class PstFrameEncoder extends MessageToByteEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {

        out.writeByte('(');
        while (msg.isReadable()) {
            int b = msg.readUnsignedByte();
            if (b == 0x27 || b == 0x28 || b == 0x29) {
                out.writeByte(0x27);
                out.writeByte(b ^ 0x40);
            } else {
                out.writeByte(b);
            }
        }
        out.writeByte(')');
    }
}
