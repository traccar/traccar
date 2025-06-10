/*
 * Copyright 2015 - 2024 Anton Tananaev (anton@traccar.org)
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

public class HuabaoFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 2) {
            return null;
        }

        if (buf.getByte(buf.readerIndex()) == '(') {

            int index = buf.indexOf(buf.readerIndex() + 1, buf.writerIndex(), (byte) ')');
            if (index >= 0) {
                return buf.readRetainedSlice(index + 1);
            }

        } else {

            int delimiter = buf.getUnsignedByte(buf.readerIndex());
            boolean alternative = delimiter == 0xe7;

            int index = buf.indexOf(buf.readerIndex() + 1, buf.writerIndex(), (byte) delimiter);
            if (index >= 0) {
                ByteBuf result = Unpooled.buffer(index + 1 - buf.readerIndex());

                while (buf.readerIndex() <= index) {
                    int b = buf.readUnsignedByte();
                    if (alternative && (b == 0xe6 || b == 0x3e)) {
                        int ext = buf.readUnsignedByte();
                        if (b == 0xe6 && ext == 0x01) {
                            result.writeByte(0xe6);
                        } else if (b == 0xe6 && ext == 0x02) {
                            result.writeByte(0xe7);
                        } else if (b == 0x3e && ext == 0x01) {
                            result.writeByte(0x3e);
                        } else if (b == 0x3e && ext == 0x02) {
                            result.writeByte(0x3d);
                        }
                    } else if (!alternative && b == 0x7d) {
                        int ext = buf.readUnsignedByte();
                        if (ext == 0x01) {
                            result.writeByte(0x7d);
                        } else if (ext == 0x02) {
                            result.writeByte(0x7e);
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
