/*
 * Copyright 2026 Polimex Holding Ltd.
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
import org.traccar.BaseProtocol;

/**
 * Splits a GT02 stream into frames by header, length and tail.
 *
 * A GT02 frame is {@code 6868 <length:1> <length bytes> 0d0a}. Some GT02 devices also send
 * GT06 frames on the same connection ({@code 7878 <length:1> ... 0d0a} and
 * {@code 7979 <length:2> ... 0d0a}); a plain length-field decoder reads the first byte of the
 * two-byte length of such a frame as a GT02 length and loses the frame boundary for the rest
 * of the connection. Here GT06 frames are skipped whole, and any byte that does not start a
 * frame with a valid tail is skipped until the next frame start.
 */
public class Gt02FrameDecoder extends BaseFrameDecoder {

    private static final int HEADER_GT02 = 0x6868;
    private static final int HEADER_GT06_SHORT = 0x7878;
    private static final int HEADER_GT06_LONG = 0x7979;
    private static final int TAIL = 0x0d0a;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        while (buf.readableBytes() >= 2) {

            int header = buf.getUnsignedShort(buf.readerIndex());
            int length;
            if (header == HEADER_GT02 || header == HEADER_GT06_SHORT) {
                if (buf.readableBytes() < 3) {
                    return null;
                }
                length = 2 + 1 + buf.getUnsignedByte(buf.readerIndex() + 2) + 2;
            } else if (header == HEADER_GT06_LONG) {
                if (buf.readableBytes() < 4) {
                    return null;
                }
                length = 2 + 2 + buf.getUnsignedShort(buf.readerIndex() + 2) + 2;
            } else {
                buf.skipBytes(1);
                continue;
            }

            if (length > BaseProtocol.MAX_FRAME_LENGTH) {
                buf.skipBytes(1);
                continue;
            }
            if (buf.readableBytes() < length) {
                return null;
            }
            if (buf.getUnsignedShort(buf.readerIndex() + length - 2) != TAIL) {
                buf.skipBytes(1);
                continue;
            }

            if (header == HEADER_GT02) {
                return buf.readRetainedSlice(length);
            }
            buf.skipBytes(length);

        }

        return null;
    }

}
