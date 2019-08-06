/*
 * Copyright 2014 - 2018 Anton Tananaev (anton@traccar.org)
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

import org.traccar.BaseFrameDecoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class UlbotechFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 2) {
            return null;
        }

        if (buf.getUnsignedByte(buf.readerIndex()) == 0xF8) {

            int index = buf.indexOf(buf.readerIndex() + 1, buf.writerIndex(), (byte) 0xF8);
            if (index != -1) {
                ByteBuf result = Unpooled.buffer(index + 1 - buf.readerIndex());

                while (buf.readerIndex() <= index) {
                    int b = buf.readUnsignedByte();
                    if (b == 0xF7) {
                        int ext = buf.readUnsignedByte();
                        if (ext == 0x00) {
                            result.writeByte(0xF7);
                        } else if (ext == 0x0F) {
                            result.writeByte(0xF8);
                        }
                    } else {
                        result.writeByte(b);
                    }
                }

                return result;
            }

        } else {

            int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '#');
            if (index != -1) {
                return buf.readRetainedSlice(index + 1 - buf.readerIndex());
            }

        }

        return null;
    }

}
