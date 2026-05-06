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
    private static final int MESSAGE_HEADER_LENGTH = 21;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {


        if (buf.readableBytes() < 5) {
            return null;
        }

        if (buf.getByte(buf.readerIndex()) != 0x45
        || buf.getByte(buf.readerIndex() + 1) != 0x54
        || buf.getByte(buf.readerIndex() + 2) != 0x34
        || buf.getByte(buf.readerIndex() + 3) != 0x31
        || buf.getByte(buf.readerIndex() + 4) != 0x30) {
    buf.skipBytes(1);
    return null;
}

        if (buf.readableBytes() < MESSAGE_HEADER_LENGTH) {
            return null;
        }

        int messageType = buf.getUnsignedByte(buf.readerIndex() + 13);
        int mask = buf.getUnsignedShort(buf.readerIndex() + 19);

        int dataLength = calculateDataLength(messageType, mask, buf, buf.readerIndex());

        int totalLength = MESSAGE_HEADER_LENGTH + dataLength;

        if (buf.readableBytes() >= totalLength) {
            return buf.readRetainedSlice(totalLength);
        }

        return null;
    }

    private int calculateDataLength(int messageType, int mask, ByteBuf buf, int readerIndex) {
        switch (messageType) {
            case 0x52:
                return calculateRegularDataLength(mask);
            case 0x4D:
                return calculateMaintenanceDataLength(mask);
            case 0x4F:
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

    if (BitUtil.check(mask, 0)) {
        length += 1;
    }
    if (BitUtil.check(mask, 1)) {
        length += 17;
    }
    if (BitUtil.check(mask, 2)) {
        length += 2;
    }
    if (BitUtil.check(mask, 3)) {
        length += 1;
    }
    if (BitUtil.check(mask, 4)) {
        length += 1;
    }
    if (BitUtil.check(mask, 5)) {
        length += 1;
    }
    if (BitUtil.check(mask, 6)) {
        length += 1;
    }

    if (BitUtil.check(mask, 7)) {
        int dtcCountOffset = readerIndex + MESSAGE_HEADER_LENGTH + length;
        int dtcCount = buf.getUnsignedByte(dtcCountOffset);
        length += 1 + dtcCount * 5;
    }
    return length;
}
}
