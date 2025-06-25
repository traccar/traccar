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
import io.netty.handler.codec.TooLongFrameException;
import org.traccar.BaseFrameDecoder;

public class JmakFrameDecoder extends BaseFrameDecoder {

    private static final int DEFAULT_MAX_FRAME_LENGTH = 2000;

    private final int maxFrameLength;
    private final JsonFrameDecoder jsonDecoder;

    public JmakFrameDecoder() {
        this(DEFAULT_MAX_FRAME_LENGTH);
    }

    public JmakFrameDecoder(int maxFrameLength) {
        this.maxFrameLength = maxFrameLength;
        this.jsonDecoder = new JsonFrameDecoder();
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {
        if (buf.readableBytes() == 0) {
            return null;
        }
        buf.markReaderIndex();
        byte first = buf.readByte();
        buf.resetReaderIndex();

        if (first == '{') {
            Object frame = jsonDecoder.decode(ctx, channel, buf);
            if (frame == null) {
                if (buf.readableBytes() > maxFrameLength) {
                    buf.clear();
                    throw new TooLongFrameException(
                            "JSON object length exceeds " + maxFrameLength);
                }
                return null;
            }
            return frame;
        }

        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();
        if (first == '~' || first == '^') {
            int delimiterIndex = buf.indexOf(readerIndex, writerIndex, (byte) '$');
            if (delimiterIndex < 0) {
                if (buf.readableBytes() > maxFrameLength) {
                    buf.clear();
                    throw new TooLongFrameException(
                            "JMAK frame length exceeds " + maxFrameLength);
                }
                return null;
            }
            int frameLength = delimiterIndex - readerIndex + 1;
            if (frameLength > maxFrameLength) {
                buf.clear();
                throw new TooLongFrameException(
                        "JMAK frame length exceeds " + maxFrameLength);
            }
            return buf.readRetainedSlice(frameLength);
        }

        buf.readByte();
        return null;
    }
}
