/*
 * Copyright 2017 - 2023 Anton Tananaev (anton@traccar.org)
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

public class WatchFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        int brackets = 0;
        int endIndex = -1;
        for (int i = buf.readerIndex(); i < buf.writerIndex(); i++) {
            byte b = buf.getByte(i);
            switch (b) {
                case '[' -> brackets += 1;
                case ']' -> brackets -= 1;
            }
            if (brackets == 0 && i > buf.readerIndex()) {
                endIndex = i + 1;
                break;
            }
        }

        if (endIndex > 0) {
            ByteBuf frame = Unpooled.buffer();
            while (buf.readerIndex() < endIndex) {
                byte b1 = buf.readByte();
                if (b1 == '}') {
                    byte b2 = buf.readByte();
                    switch (b2) {
                        case 0x01 -> frame.writeByte('}');
                        case 0x02 -> frame.writeByte('[');
                        case 0x03 -> frame.writeByte(']');
                        case 0x04 -> frame.writeByte(',');
                        case 0x05 -> frame.writeByte('*');
                        default -> throw new IllegalArgumentException(
                                String.format("unexpected byte at %d: 0x%02x", buf.readerIndex() - 1, b2));
                    }
                } else {
                    frame.writeByte(b1);
                }
            }
            return frame;
        }

        return null;
    }

}
