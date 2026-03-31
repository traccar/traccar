/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

public class Es4x0FrameDecoder extends BaseFrameDecoder {

    public static final int MSG_REGULAR = 0x52;
    public static final int MSG_MAINTENANCE = 0x4D;
    public static final int MSG_OBD = 0x4F;

    private static final byte[] HEADER = new byte[]{0x45, 0x54, 0x34, 0x31, 0x30};

    private static final int HEADER_LENGTH = 5;
    private static final int IMEI_LENGTH = 8;
    private static final int MESSAGE_TYPE_OFFSET = HEADER_LENGTH + IMEI_LENGTH;
    private static final int MASK_OFFSET = MESSAGE_TYPE_OFFSET + 6;
    private static final int MESSAGE_HEADER_LENGTH = MASK_OFFSET + 2;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        int headerIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), HEADER[0]);
        if (headerIndex < 0) {
            return null;
        }

        buf.skipBytes(headerIndex - buf.readerIndex());

        if (buf.readableBytes() < HEADER_LENGTH) {
            return null;
        }

        for (int i = 0; i < HEADER.length; i++) {
            if (buf.getByte(buf.readerIndex() + i) != HEADER[i]) {
                buf.skipBytes(1);
                return null;
            }
        }

        if (buf.readableBytes() < MESSAGE_HEADER_LENGTH) {
            return null;
        }

        int readerIndex = buf.readerIndex();
        int messageType = buf.getUnsignedByte(readerIndex + MESSAGE_TYPE_OFFSET);
        int mask = buf.getUnsignedShort(readerIndex + MASK_OFFSET);

        int dataLength = calculateDataLength(messageType, mask, buf, readerIndex);

        int totalLength = MESSAGE_HEADER_LENGTH + dataLength;

        if (buf.readableBytes() >= totalLength) {
            return buf.readRetainedSlice(totalLength);
        }

        return null;
    }

    private int calculateDataLength(int messageType, int mask, ByteBuf buf, int readerIndex) {
        switch (messageType) {
            case MSG_REGULAR:
                return calculateRegularDataLength(mask);
            case MSG_MAINTENANCE:
                return calculateMaintenanceDataLength(mask);
            case MSG_OBD:
                return calculateObdDataLength(mask, buf, readerIndex);
            default:
                return 0;
        }
    }

    private int calculateRegularDataLength(int mask) {
        int length = 0;

        if (BitUtil.check(mask, 0)) {
            length += 1;
        }
        if (BitUtil.check(mask, 1)) {
            length += 1;
        }
        if (BitUtil.check(mask, 2)) {
            length += 4;
        }
        if (BitUtil.check(mask, 3)) {
            length += 4;
        }
        if (BitUtil.check(mask, 4)) {
            length += 4;
        }
        if (BitUtil.check(mask, 5)) {
            length += 4;
        }
        if (BitUtil.check(mask, 6)) {
            length += 2;
        }
        if (BitUtil.check(mask, 7)) {
            length += 1;
        }
        if (BitUtil.check(mask, 8)) {
            length += 1;
        }
        if (BitUtil.check(mask, 9)) {
            length += 1;
        }
        if (BitUtil.check(mask, 10)) {
            length += 4;
        }
        if (BitUtil.check(mask, 11)) {
            length += 2;
        }
        if (BitUtil.check(mask, 12)) {
            length += 2;
        }
        if (BitUtil.check(mask, 13)) {
            length += 2;
        }
        if (BitUtil.check(mask, 14)) {
            length += 1;
        }

        return length;
    }

    private int calculateMaintenanceDataLength(int mask) {
        int length = 0;

        if (BitUtil.check(mask, 0)) {
            length += 1;
        }
        if (BitUtil.check(mask, 1)) {
            length += 1;
        }
        if (BitUtil.check(mask, 2)) {
            length += 1;
        }
        if (BitUtil.check(mask, 3)) {
            length += 1;
        }
        if (BitUtil.check(mask, 4)) {
            length += 1;
        }
        if (BitUtil.check(mask, 5)) {
            length += 20;
        }
        if (BitUtil.check(mask, 6)) {
            length += 35;
        }
        if (BitUtil.check(mask, 7)) {
            length += 14;
        }

        return length;
    }

    private int calculateObdDataLength(int mask, ByteBuf buf, int readerIndex) {
        int length = 0;

        int fixedLength = 0;
        if (BitUtil.check(mask, 0)) {
            fixedLength += 1;
        }
        if (BitUtil.check(mask, 1)) {
            fixedLength += 17;
        }
        if (BitUtil.check(mask, 2)) {
            fixedLength += 2;
        }
        if (BitUtil.check(mask, 3)) {
            fixedLength += 1;
        }
        if (BitUtil.check(mask, 4)) {
            fixedLength += 1;
        }
        if (BitUtil.check(mask, 5)) {
            fixedLength += 1;
        }
        if (BitUtil.check(mask, 6)) {
            fixedLength += 1;
        }

        length += fixedLength;

        if (BitUtil.check(mask, 7)) {
            int dtcCountOffset = readerIndex + MESSAGE_HEADER_LENGTH + fixedLength;
            int dtcCount = buf.getUnsignedByte(dtcCountOffset);
            length += 1 + dtcCount * 5;
        }

        return length;
    }

}
