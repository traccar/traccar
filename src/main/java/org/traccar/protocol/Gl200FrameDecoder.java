/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
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

import org.traccar.BaseFrameDecoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Gl200FrameDecoder extends BaseFrameDecoder {

    private static final int MINIMUM_LENGTH = 11;

    private static final Set<String> BINARY_HEADERS = new HashSet<>(
            Arrays.asList("+RSP", "+BSP", "+EVT", "+BVT", "+INF", "+BNF", "+HBD", "+CRD", "+BRD", "+LGN"));

    public static boolean isBinary(ByteBuf buf) {
        String header = buf.toString(buf.readerIndex(), 4, StandardCharsets.US_ASCII);
        if (header.equals("+ACK")) {
            return buf.getByte(buf.readerIndex() + header.length()) != (byte) ':';
        } else {
            return BINARY_HEADERS.contains(header);
        }
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < MINIMUM_LENGTH) {
            return null;
        }

        if (isBinary(buf)) {

            int length = switch (buf.toString(buf.readerIndex(), 4, StandardCharsets.US_ASCII)) {
                case "+ACK" -> buf.getUnsignedByte(buf.readerIndex() + 6);
                case "+INF", "+BNF" -> buf.getUnsignedShort(buf.readerIndex() + 7);
                case "+HBD" -> buf.getUnsignedByte(buf.readerIndex() + 5);
                case "+CRD", "+BRD", "+LGN" -> buf.getUnsignedShort(buf.readerIndex() + 6);
                default -> buf.getUnsignedShort(buf.readerIndex() + 9);
            };

            if (buf.readableBytes() >= length) {
                return buf.readRetainedSlice(length);
            }

        } else {

            int endIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '$');
            if (endIndex < 0) {
                endIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) 0);
            }
            if (endIndex > 0) {
                ByteBuf frame = buf.readRetainedSlice(endIndex - buf.readerIndex());
                buf.readByte(); // delimiter
                return frame;
            }

        }

        return null;
    }

}
