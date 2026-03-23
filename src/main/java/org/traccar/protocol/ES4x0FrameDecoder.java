/*
 * Copyright 2024 - 2025 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;

public class ES4x0FrameDecoder extends BaseFrameDecoder {

    private static final int MESSAGE_HEADER_LENGTH = 5;
    private static final int IMEI_LENGTH = 8;
    private static final int MESSAGE_TYPE_OFFSET = MESSAGE_HEADER_LENGTH + IMEI_LENGTH;
    private static final int MIN_MESSAGE_LENGTH = MESSAGE_HEADER_LENGTH + IMEI_LENGTH + 1 + 1 + 4 + 2 + 2;

    private static final byte MESSAGE_TYPE_REGULAR = 0x52; // R
    private static final byte MESSAGE_TYPE_MAINTENANCE = 0x4D; // M
    private static final byte MESSAGE_TYPE_OBD = 0x4F; // O

    private static final byte[] MESSAGE_HEADER = {'E', 'T', '4', '1', '0'};

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < MIN_MESSAGE_LENGTH) {
            return null;
        }

        int readerIndex = buf.readerIndex();

        boolean validHeader = true;
        for (int i = 0; i < MESSAGE_HEADER_LENGTH; i++) {
            if (buf.getByte(readerIndex + i) != MESSAGE_HEADER[i]) {
                validHeader = false;
                break;
            }
        }

        if (!validHeader) {
            int headerIndex = buf.indexOf(readerIndex, readerIndex + 64, (byte) 'E');
            if (headerIndex >= 0) {
                buf.readerIndex(headerIndex);
            } else {
                buf.readerIndex(buf.writerIndex());
            }
            return null;
        }

        byte messageType = buf.getByte(readerIndex + MESSAGE_TYPE_OFFSET);

        if (messageType != MESSAGE_TYPE_REGULAR
                && messageType != MESSAGE_TYPE_MAINTENANCE
                && messageType != MESSAGE_TYPE_OBD) {
            buf.readerIndex(readerIndex + 1);
            return null;
        }

        int mask = buf.getUnsignedShort(readerIndex + MESSAGE_TYPE_OFFSET + 2);
        int dataLength = 0;

        if (messageType == MESSAGE_TYPE_REGULAR) {
            if (BitUtil.check(mask, 0)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 1)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 2)) {
                dataLength += 4;
            }
            if (BitUtil.check(mask, 3)) {
                dataLength += 4;
            }
            if (BitUtil.check(mask, 4)) {
                dataLength += 4;
            }
            if (BitUtil.check(mask, 5)) {
                dataLength += 4;
            }
            if (BitUtil.check(mask, 6)) {
                dataLength += 2;
            }
            if (BitUtil.check(mask, 7)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 8)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 9)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 10)) {
                dataLength += 4;
            }
            if (BitUtil.check(mask, 11)) {
                dataLength += 2;
            }
            if (BitUtil.check(mask, 12)) {
                dataLength += 2;
            }
            if (BitUtil.check(mask, 13)) {
                dataLength += 2;
            }
            if (BitUtil.check(mask, 14)) {
                dataLength += 1;
            }
        } else if (messageType == MESSAGE_TYPE_MAINTENANCE) {
            if (BitUtil.check(mask, 0)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 1)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 2)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 3)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 4)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 5)) {
                dataLength += 20;
            }
            if (BitUtil.check(mask, 6)) {
                dataLength += 35;
            }
            if (BitUtil.check(mask, 7)) {
                dataLength += 14;
            }
        } else if (messageType == MESSAGE_TYPE_OBD) {
            if (BitUtil.check(mask, 0)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 1)) {
                dataLength += 17;
            }
            if (BitUtil.check(mask, 2)) {
                dataLength += 2;
            }
            if (BitUtil.check(mask, 3)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 4)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 5)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 6)) {
                dataLength += 1;
            }
            if (BitUtil.check(mask, 7)) {
                int dtcCount = buf.getUnsignedByte(readerIndex + MESSAGE_TYPE_OFFSET + 4 + dataLength);
                dataLength += 1 + dtcCount * 5;
            }
        }

        int messageLength = MIN_MESSAGE_LENGTH + dataLength;

        if (buf.readableBytes() >= messageLength) {
            return buf.readRetainedSlice(messageLength);
        }

        return null;
    }

}
