/*
 * Copyright 2025 - 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;

public class JmakFrameDecoder extends JsonFrameDecoder {

    private static final int[] FIELD_LENGTHS = {
            11, 8, 4, 10, 8, 4, 4, 3, 1, 3, 1, 1, 2, 3, 1, 1, 5, 5, 1, 1,
            8, 2, 2, 4, 1, 8, 1, 1, 8, 1, 0, 8, 8, 8, 2, 8, 8, 4};

    private static final int[] CAN_LENGTHS = {
            5, 5, 3, 3, 2, 0, 1, 1, 1, 0, 3, 1, 2, 5, 1, 2, 1, 1, 2, 1,
            1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 2, 2, 1, 2};

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {
        if (buf.readableBytes() == 0) {
            return null;
        }
        byte first = buf.getByte(buf.readerIndex());

        if (first == '{') {
            return super.decode(ctx, channel, buf);
        }

        if (first == '^') {
            return decodeBinary(buf);
        }

        if (first == '~') {
            int delimiterIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '$');
            if (delimiterIndex > 0) {
                return buf.readRetainedSlice(delimiterIndex - buf.readerIndex() + 1);
            }
        }

        return null;
    }

    private ByteBuf decodeBinary(ByteBuf buf) {

        int index = buf.readerIndex() + 1;
        if (buf.writerIndex() < index + 8) {
            return null;
        }
        long mask = buf.getLong(index);
        index += 8;

        int length = 0;
        for (int i = 0; i < FIELD_LENGTHS.length; i++) {
            if (BitUtil.check(mask, i)) {
                if (i == 29) {
                    if (index >= buf.writerIndex()) {
                        return null;
                    }
                    length = buf.getUnsignedByte(index);
                }
                index += i == 30 ? length : FIELD_LENGTHS[i];
            }
        }

        if (index >= buf.writerIndex()) {
            return null;
        }
        if (buf.getByte(index) != '$') {
            if (buf.writerIndex() < index + 8) {
                return null;
            }
            long canMask = buf.getLong(index);
            index += 8;
            for (int i = 0; i < CAN_LENGTHS.length; i++) {
                if (BitUtil.check(canMask, i)) {
                    index += CAN_LENGTHS[i];
                }
            }
            if (index >= buf.writerIndex()) {
                return null;
            }
        }

        if (buf.getByte(index) == '$') {
            return buf.readRetainedSlice(index + 1 - buf.readerIndex());
        }

        buf.skipBytes(1);
        return null;
    }
}
