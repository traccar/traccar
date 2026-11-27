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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseFrameDecoder;

public class GoSafeFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        char marker = (char) buf.getByte(buf.readerIndex());

        if (marker == '*') {
            int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '#');
            if (index != -1) {
                return buf.readRetainedSlice(index + 1 - buf.readerIndex());
            }
        } else {
            int index = buf.indexOf(buf.readerIndex() + 1, buf.writerIndex(), (byte) 0xf8);
            if (index >= 0) {
                ByteBuf result = Unpooled.buffer(index + 1 - buf.readerIndex());
                while (buf.readerIndex() <= index) {
                    int b = buf.readUnsignedByte();
                    if (b == 0x1b) {
                        int ext = buf.readUnsignedByte();
                        if (ext == 0x00) {
                            result.writeByte(0x1b);
                        } else if (ext == 0xe3) {
                            result.writeByte(0xf8);
                        }
                    } else {
                        result.writeByte(b);
                    }
                }

                return result;
            }
        }

        return null;
    }

}
