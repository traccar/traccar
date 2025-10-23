/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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

/**
 * Frame decoder for GTR-9 devices using Siemens RAW format.
 *
 * GTR-9 devices send packets with fixed framing:
 * - Preamble: 7E7E7E7E00 (5 bytes)
 * - Payload: Variable length
 * - Suffix: Any 4 bytes (commonly 7E7E, 0000, 007E, etc.)
 *
 * This decoder simply strips the first 5 bytes and last 4 bytes
 * without validating the suffix content.
 */
public class XsenseGtr9FrameDecoder extends BaseFrameDecoder {

    private static final int PREAMBLE_LENGTH = 5;
    private static final int SUFFIX_LENGTH = 4;
    private static final int MIN_PACKET_LENGTH = PREAMBLE_LENGTH + SUFFIX_LENGTH; // 9 bytes

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < MIN_PACKET_LENGTH) {
            return null;
        }

        // Check for Siemens preamble: 7E 7E 7E 7E 00
        if (buf.getUnsignedByte(buf.readerIndex()) == 0x7E
                && buf.getUnsignedByte(buf.readerIndex() + 1) == 0x7E
                && buf.getUnsignedByte(buf.readerIndex() + 2) == 0x7E
                && buf.getUnsignedByte(buf.readerIndex() + 3) == 0x7E
                && buf.getUnsignedByte(buf.readerIndex() + 4) == 0x00) {

            // Skip preamble (5 bytes)
            buf.skipBytes(PREAMBLE_LENGTH);

            // Calculate payload length (total - preamble - suffix)
            int payloadLength = buf.readableBytes() - SUFFIX_LENGTH;

            if (payloadLength <= 0) {
                // Not enough data, wait for more
                buf.readerIndex(buf.readerIndex() - PREAMBLE_LENGTH); // Reset
                return null;
            }

            // Extract payload (without preamble and suffix)
            ByteBuf frame = buf.readRetainedSlice(payloadLength);

            // Skip suffix (4 bytes) - don't care what it contains
            buf.skipBytes(SUFFIX_LENGTH);

            return frame;
        }

        // No valid preamble found, skip one byte and try again
        buf.skipBytes(1);
        return null;
    }

}
