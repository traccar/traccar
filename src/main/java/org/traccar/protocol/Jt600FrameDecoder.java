/*
 * Copyright 2012 - 2025 Anton Tananaev (anton@traccar.org)
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

import java.text.ParseException;

public class Jt600FrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 10) {
            return null;
        }

        char type = (char) buf.getByte(buf.readerIndex());

        if (type == '$') {
            boolean longFormat = Jt600ProtocolDecoder.isLongFormat(buf);
            int length = buf.getUnsignedShort(buf.readerIndex() + (longFormat ? 8 : 7)) + 10;
            if (length <= buf.readableBytes()) {
                return buf.readRetainedSlice(length);
            }
        } else if (type == '(') {
            int endIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ')');
            if (endIndex >= 0) {
                ByteBuf result = Unpooled.buffer(endIndex + 1 - buf.readerIndex());

                while (buf.readerIndex() <= endIndex) {
                    int b = buf.readUnsignedByte();
                    if (b == 0x3d) {
                        int ext = buf.readUnsignedByte();
                        if (ext == 0x15) {
                            result.writeByte(0x28);
                        } else if (ext == 0x14) {
                            result.writeByte(0x29);
                        } else if (ext == 0x11) {
                            result.writeByte(0x2c);
                        } else if (ext == 0x00) {
                            result.writeByte(0x3d);
                        }
                    } else {
                        result.writeByte(b);
                    }
                }

                return result;
            }
        } else {
            throw new ParseException(null, 0); // unknown message
        }

        return null;
    }

}
