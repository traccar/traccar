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
 * GTR-9 devices send packets with flexible suffix handling:
 * - Standard: 7E7E7E7E00 [payload] 7E7E
 * - Alternative: 7E7E7E7E00 [payload] 0000
 * - Malformed: 7E7E7E7E00 [payload] 7E (single byte suffix)
 *
 * This decoder is more lenient than the standard XsenseFrameDecoder
 * to handle real-world packet variations from GTR-9 devices.
 */
public class XsenseGtr9FrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 5) {
            return null;
        }

        // Check for Siemens preamble: 7E 7E 7E 7E 00
        if (buf.readableBytes() >= 5
                && buf.getUnsignedByte(buf.readerIndex()) == 0x7E
                && buf.getUnsignedByte(buf.readerIndex() + 1) == 0x7E
                && buf.getUnsignedByte(buf.readerIndex() + 2) == 0x7E
                && buf.getUnsignedByte(buf.readerIndex() + 3) == 0x7E
                && buf.getUnsignedByte(buf.readerIndex() + 4) == 0x00) {

            // Skip preamble
            buf.skipBytes(5);

            // Find suffix: 7E7E, 0000, or single 7E (for malformed packets)
            int endIndex = -1;
            int suffixLength = 2; // Default: 7E7E or 0000

            // First, search for standard 2-byte suffix
            for (int i = buf.readerIndex(); i < buf.writerIndex() - 1; i++) {
                int byte1 = buf.getUnsignedByte(i);
                int byte2 = buf.getUnsignedByte(i + 1);

                // Check for standard 2-byte suffix
                if ((byte1 == 0x7E && byte2 == 0x7E) || (byte1 == 0x00 && byte2 == 0x00)) {
                    endIndex = i;
                    suffixLength = 2;
                    break;
                }
            }

            // If standard suffix not found, check for single 7E at the end (malformed packet)
            if (endIndex == -1 && buf.readableBytes() > 0) {
                int lastByte = buf.getUnsignedByte(buf.writerIndex() - 1);
                if (lastByte == 0x7E) {
                    endIndex = buf.writerIndex() - 1;
                    suffixLength = 1;
                }
            }

            if (endIndex == -1) {
                // Suffix not found, wait for more data
                buf.readerIndex(buf.readerIndex() - 5); // Reset reader index
                return null;
            }

            // Extract payload (without preamble and suffix)
            int length = endIndex - buf.readerIndex();
            ByteBuf frame = buf.readRetainedSlice(length);

            // Skip suffix (7E7E, 0000, or single 7E)
            buf.skipBytes(suffixLength);

            return frame;
        }

        // No valid preamble found, skip one byte and try again
        buf.skipBytes(1);
        return null;
    }

}
