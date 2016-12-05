/*
 * Copyright 2014 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class Pt502FrameDecoder extends FrameDecoder {

    private static final int BINARY_HEADER = 5;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < BINARY_HEADER) {
            return null;
        }

        if (buf.getUnsignedByte(buf.readerIndex()) == 0xbf && buf.getUnsignedByte(buf.readerIndex() + 1) == 0xfb) {

            int length = buf.getShort(buf.readerIndex() + 3);
            if (buf.readableBytes() >= length) {
                buf.skipBytes(BINARY_HEADER);
                ChannelBuffer result = buf.readBytes(length - BINARY_HEADER - 2);
                buf.skipBytes(2);
                return result;
            }

        } else {

            int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '\n');
            if (index != -1) {
                ChannelBuffer result = buf.readBytes(index - 1);
                buf.skipBytes(2);
                return result;
            }

        }

        return null;
    }

}
