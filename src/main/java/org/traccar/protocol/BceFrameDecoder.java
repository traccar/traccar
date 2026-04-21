/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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

public class BceFrameDecoder extends BaseFrameDecoder {

    private static final int HANDSHAKE_LENGTH = 7; // "#BCE#\r\n"

    private boolean header = true;

    private static byte checksum(ByteBuf buf, int end) {
        byte result = 0;
        for (int i = 0; i < end; i++) {
            result += buf.getByte(buf.readerIndex() + i);
        }
        return result;
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (header && buf.readableBytes() >= HANDSHAKE_LENGTH) {
            buf.skipBytes(HANDSHAKE_LENGTH);
            header = false;
        }

        int end = 8; // IMEI

        while (buf.readableBytes() >= end + 2 + 1 + 1 + 1) {
            end += buf.getUnsignedShortLE(buf.readerIndex() + end) + 2;

            if (buf.readableBytes() > end && checksum(buf, end) == buf.getByte(buf.readerIndex() + end)) {
                return buf.readRetainedSlice(end + 1);
            }
        }

        return null;
    }

}
