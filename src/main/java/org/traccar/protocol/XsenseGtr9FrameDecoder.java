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
 * GTR-9 devices send packets with variable suffix:
 * - Preamble: 7E7E7E7E00 (5 bytes, fixed)
 * - Payload: Variable length
 * - Suffix: 2 or 4 bytes ending with 7E7E
 *   - Position packets: 4 bytes (e.g., 00007E7E, XXXX7E7E)
 *   - Ping/other packets: 2 bytes (7E7E)
 *
 * This decoder searches for 7E7E suffix and extracts payload accordingly.
 */
public class XsenseGtr9FrameDecoder extends BaseFrameDecoder {

    private static final int PREAMBLE_LENGTH = 5;
    private static final int MIN_PACKET_LENGTH = PREAMBLE_LENGTH + 2; // 5 + at least 7E7E

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

            // Search for 7E7E suffix from the end
            // Check last 2 bytes for 7E7E
            int totalBytes = buf.readableBytes();
            if (totalBytes < 2) {
                buf.readerIndex(buf.readerIndex() - PREAMBLE_LENGTH); // Reset
                return null;
            }

            // Check if packet ends with 7E7E
            int lastByte = buf.getUnsignedByte(buf.writerIndex() - 1);
            int secondLastByte = buf.getUnsignedByte(buf.writerIndex() - 2);
            
            if (lastByte == 0x7E && secondLastByte == 0x7E) {
                // Found 7E7E suffix - extract payload
                int payloadLength = totalBytes - 2; // Remove 7E7E

                ByteBuf frame = buf.readRetainedSlice(payloadLength);
                buf.skipBytes(2); // Skip 7E7E

                return frame;
            } else {
                // 7E7E not found, wait for more data
                buf.readerIndex(buf.readerIndex() - PREAMBLE_LENGTH); // Reset
                return null;
            }
        }

        // No valid preamble found, skip one byte and try again
        buf.skipBytes(1);
        return null;
    }

}
