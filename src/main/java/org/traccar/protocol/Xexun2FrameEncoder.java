/*
 * Copyright 2022 Stefan Clark (stefan@stefanclark.co.uk)
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

public class Xexun2FrameEncoder extends MessageToByteEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        out.writeBytes(msg.readBytes(2));

        while (msg.readableBytes() > 2) {
            int b = msg.readUnsignedByte();
            if (b == 0xfa && msg.isReadable() && msg.getUnsignedByte(msg.readerIndex()) == 0xaf) {
                msg.readUnsignedByte();
                out.writeByte(0xfb);
                out.writeByte(0xbf);
                out.writeByte(0x01);
            } else if (b == 0xfb && msg.isReadable() && msg.getUnsignedByte(msg.readerIndex()) == 0xbf) {
                msg.readUnsignedByte();
                out.writeByte(0xfb);
                out.writeByte(0xbf);
                out.writeByte(0x02);
            } else {
                out.writeByte(b);
            }
        }

        out.writeBytes(msg.readBytes(2));

    }
}
