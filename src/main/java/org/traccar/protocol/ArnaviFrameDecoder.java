/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Ivan Muratov (binakot@gmail.com)
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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseFrameDecoder;
import org.traccar.helper.BufferUtil;

public class ArnaviFrameDecoder extends BaseFrameDecoder {

    private static final int HEADER_LENGTH = 10;
    private static final int PACKET_WRAPPER_LENGTH = 8;
    private static final int RESULT_TYPE = 0xfd;
    private static final byte PACKAGE_END_SIGN = 0x5d;

    private boolean firstPacket = true;

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 4) {
            return null;
        }

        if (buf.getByte(buf.readerIndex()) == '$') {

            int index = BufferUtil.indexOf("\r\n", buf);
            if (index > 0) {
                ByteBuf frame = buf.readRetainedSlice(index - buf.readerIndex());
                buf.skipBytes(2);
                return frame;
            }

        } else {

            int length;
            if (firstPacket) {
                firstPacket = false;
                length = HEADER_LENGTH;
            } else {
                int type = buf.getUnsignedByte(1);
                if (type == RESULT_TYPE) {
                    length = 4;
                } else {
                    int index = 2;
                    while (index + PACKET_WRAPPER_LENGTH < buf.readableBytes()
                            && buf.getByte(index) != PACKAGE_END_SIGN) {
                        index += PACKET_WRAPPER_LENGTH + buf.getUnsignedShortLE(index + 1);
                    }
                    if (buf.getByte(index) != PACKAGE_END_SIGN) {
                        return null;
                    }
                    length = index + 1;
                }
            }

            if (buf.readableBytes() >= length) {
                return buf.readRetainedSlice(length);
            }

        }

        return null;
    }

}
