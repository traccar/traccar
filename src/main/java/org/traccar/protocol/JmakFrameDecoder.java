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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;

import java.util.List;

public class JmakFrameDecoder extends ByteToMessageDecoder {

    private final int maxFrameLength;

    public JmakFrameDecoder(int maxFrameLength) {
        this.maxFrameLength = maxFrameLength;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() == 0) {
            return;
        }
        in.markReaderIndex();
        byte first = in.readByte();
        in.resetReaderIndex();

        if (first == '{') {
            int endIndex = findJsonEnd(in);
            if (endIndex < 0) {
                if (in.readableBytes() > maxFrameLength) {
                    in.clear();
                    throw new TooLongFrameException("JSON object length exceeds " + maxFrameLength);
                }
                return;
            }
            if (endIndex + 1 > maxFrameLength) {
                in.clear();
                throw new TooLongFrameException("JSON object length exceeds " + maxFrameLength);
            }
            ByteBuf frame = in.readRetainedSlice(endIndex + 1);
            out.add(frame);

        } else if (first == '~' || first == '^') { // STRING || ZIP
            int readerIndex = in.readerIndex();
            int writerIndex = in.writerIndex();
            int delimiterIndex = in.indexOf(readerIndex, writerIndex, (byte) '$');
            if (delimiterIndex < 0) {
                if (in.readableBytes() > maxFrameLength) {
                    in.clear();
                    throw new TooLongFrameException("JMAK frame length exceeds " + maxFrameLength);
                }
                return;
            }
            int frameLength = delimiterIndex - readerIndex + 1;
            if (frameLength > maxFrameLength) {
                in.clear();
                throw new TooLongFrameException("JMAK frame length exceeds " + maxFrameLength);
            }
            ByteBuf frame = in.readRetainedSlice(frameLength);
            out.add(frame);

        } else {
            // Skip unknown data
            in.readByte();
        }
    }

    private int findJsonEnd(ByteBuf buf) {
        int reader = buf.readerIndex();
        int writer = buf.writerIndex();
        int depth = 0;
        for (int i = reader; i < writer; i++) {
            byte b = buf.getByte(i);
            if (b == '{') {
                depth++;
            } else if (b == '}') {
                depth--;
                if (depth == 0) {
                    return i - reader;
                }
            }
            if (i - reader + 1 > maxFrameLength) {
                return -1;
            }
        }
        return -1;
    }
}
