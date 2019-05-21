/*
 * Copyright 2014 - 2018 Anton Tananaev (anton@traccar.org)
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

import java.nio.charset.StandardCharsets;

public class Pt502FrameDecoder extends BaseFrameDecoder {

    private static final int BINARY_HEADER = 5;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 10) {
            return null;
        }

        if (buf.getUnsignedByte(buf.readerIndex()) == 0xbf
                && buf.toString(buf.readerIndex() + BINARY_HEADER, 4, StandardCharsets.US_ASCII).equals("$PHD")) {

            int length = buf.getUnsignedShortLE(buf.readerIndex() + 3);
            if (buf.readableBytes() >= length) {
                buf.skipBytes(BINARY_HEADER);
                ByteBuf result = buf.readRetainedSlice(length - BINARY_HEADER - 2);
                buf.skipBytes(2); // line break
                return result;
            }

        } else {

            if (buf.getUnsignedByte(buf.readerIndex()) == 0xbf) {
                buf.skipBytes(BINARY_HEADER);
            }

            int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '\r');
            if (index < 0) {
                index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '\n');
            }

            if (index > 0) {
                ByteBuf result = buf.readRetainedSlice(index - buf.readerIndex());
                while (buf.isReadable()
                        && (buf.getByte(buf.readerIndex()) == '\r' || buf.getByte(buf.readerIndex()) == '\n')) {
                    buf.skipBytes(1);
                }
                return result;
            }

        }

        return null;
    }

}
