/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.nio.charset.StandardCharsets;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.traccar.helper.StringFinder;

public class GranitFrameDecoder extends FrameDecoder {

    private static final int HEADER_LENGTH = 6;
    private static final int CHECKSUM_LENGTH = 3;
    private static final int SHORT_LENGTH = 2;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() > HEADER_LENGTH + SHORT_LENGTH) {
            ChannelBuffer frame = ChannelBuffers.dynamicBuffer();
            buf.getBytes(buf.readerIndex(), frame, HEADER_LENGTH);
            String headerString = frame.toString(StandardCharsets.US_ASCII);
            if (headerString.equals("+RRCB~") || headerString.equals("+DDAT~")) {
                int length = buf.getUnsignedShort(buf.readerIndex() + HEADER_LENGTH);
                if (buf.readableBytes() >= HEADER_LENGTH + SHORT_LENGTH + length + CHECKSUM_LENGTH) {
                    frame = buf.readBytes(HEADER_LENGTH + SHORT_LENGTH + length + CHECKSUM_LENGTH);
                    if (buf.readableBytes() > 2) {
                        buf.skipBytes(2); //skip \r\n
                    }
                    return frame;
                } else {
                    return null; //damaged packet
                }
            }
        }

        int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), new StringFinder("\r\n"));
        if (index != -1) {
            ChannelBuffer frame = buf.readBytes(index - buf.readerIndex());
            buf.skipBytes(2);
            return frame;
        }

        return null;
    }

}
