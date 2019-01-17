/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
import io.netty.handler.codec.TooLongFrameException;
import org.traccar.BaseFrameDecoder;

public class NavisFrameDecoder extends BaseFrameDecoder {

    private static final int NTCB_HEADER_LENGHT = 16;
    private static final int NTCB_LENGHT_OFFSET = 12;
    private static final int MAX_FRAME_LENGHT = 65551;

    private static byte src8Checksum(ByteBuf buf, int length) {
        byte sum = (byte) 0xFF;
        for (int i = 0; i < length; i++) {
            sum ^= buf.getUnsignedByte(i);
            for (int j = 0; j < 8; j++) {
                sum = (sum & 0x80) != 0 ? (byte) ((sum << 1) ^ 0x31) : (byte) (sum << 1);
            }
        }
        return sum;
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        // Check minimum length
        if (buf.readableBytes() < 1) {
            return null;
        }

        if (buf.getByte(buf.readerIndex()) == 0x7F) {
            // FLEX keep alive frame
            return buf.readRetainedSlice(1);
        } else if (buf.getByte(buf.readerIndex()) == 0x7E /* "~" */) {
            // FLEX frame
            if (buf.readableBytes() > MAX_FRAME_LENGHT) {
                throw new TooLongFrameException();
            }

            if (src8Checksum(buf, buf.readableBytes() - 1) == buf.getByte(buf.readableBytes() - 1)) {
                return buf.readRetainedSlice(buf.readableBytes());
            }
        } else {
            // NTCB frame
            if (buf.readableBytes() < NTCB_HEADER_LENGHT) {
                return null;
            }

            int length = buf.getUnsignedShortLE(buf.readerIndex() + NTCB_LENGHT_OFFSET);
            if (buf.readableBytes() >= NTCB_HEADER_LENGHT + length) {
                if (buf.readableBytes() > MAX_FRAME_LENGHT) {
                    throw new TooLongFrameException();
                }
                return buf.readRetainedSlice(NTCB_HEADER_LENGHT + length);
            }
        }

        return null;
    }

}
