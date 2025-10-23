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
 * - Suffix: 2 bytes - either 7E7E or 0000
 *   - Most packets: 7E7E
 *   - Some position reports: 0000
 *
 * This decoder searches for either suffix and extracts payload accordingly.
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
        int startIndex = buf.readerIndex();
        if (buf.getUnsignedByte(startIndex) == 0x7E
                && buf.getUnsignedByte(startIndex + 1) == 0x7E
                && buf.getUnsignedByte(startIndex + 2) == 0x7E
                && buf.getUnsignedByte(startIndex + 3) == 0x7E
                && buf.getUnsignedByte(startIndex + 4) == 0x00) {

            // Skip preamble (5 bytes)
            buf.skipBytes(PREAMBLE_LENGTH);

            int payloadStart = buf.readerIndex();
            int writerIndex = buf.writerIndex();

            int suffixIndex = -1;
            int suffixLength = 0;
            for (int i = payloadStart; i < writerIndex; i++) {
                int current = buf.getUnsignedByte(i);

                if (i + 1 < writerIndex) {
                    int next = buf.getUnsignedByte(i + 1);

                    if ((current == 0x7E && next == 0x7E) || (current == 0x00 && next == 0x00)) {
                        int candidateLength = 2;
                        int candidateEnd = i + candidateLength;
                        int remaining = writerIndex - candidateEnd;

                        if (remaining == 0 || hasPreamble(buf, candidateEnd)) {
                            suffixIndex = i;
                            suffixLength = candidateLength;
                            break;
                        } else if (remaining < PREAMBLE_LENGTH) {
                            buf.readerIndex(startIndex); // Wait for more data to confirm preamble
                            return null;
                        }
                    }
                }

                if (current == 0x7E) {
                    int candidateLength = 1;
                    int candidateEnd = i + candidateLength;
                    int remaining = writerIndex - candidateEnd;

                    if (remaining == 0 || hasPreamble(buf, candidateEnd)) {
                        suffixIndex = i;
                        suffixLength = candidateLength;
                        break;
                    } else if (remaining < PREAMBLE_LENGTH) {
                        buf.readerIndex(startIndex);
                        return null;
                    }
                }
            }

            if (suffixIndex == -1) {
                buf.readerIndex(startIndex); // Reset and wait for more data
                return null;
            }

            int payloadLength = suffixIndex - payloadStart;
            if (payloadLength <= 0) {
                buf.readerIndex(startIndex); // Invalid frame, reset
                return null;
            }

            ByteBuf frame = buf.readRetainedSlice(payloadLength);
            buf.skipBytes(suffixLength); // Skip suffix bytes (7E7E, 0000 or single 7E)

            return frame;
        }

        // No valid preamble found, skip one byte and try again
        buf.skipBytes(1);
        return null;
    }

    private boolean hasPreamble(ByteBuf buf, int index) {
        int remaining = buf.writerIndex() - index;
        if (remaining < PREAMBLE_LENGTH) {
            return false;
        }
        return buf.getUnsignedByte(index) == 0x7E
                && buf.getUnsignedByte(index + 1) == 0x7E
                && buf.getUnsignedByte(index + 2) == 0x7E
                && buf.getUnsignedByte(index + 3) == 0x7E
                && buf.getUnsignedByte(index + 4) == 0x00;
    }

}
