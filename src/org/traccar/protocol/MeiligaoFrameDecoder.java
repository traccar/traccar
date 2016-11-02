/*
 * Copyright 2013 Anton Tananaev (anton@traccar.org)
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

public class MeiligaoFrameDecoder extends FrameDecoder {

    private static final int MESSAGE_HEADER = 4;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        // Strip not '$' (0x24) bytes from the beginning
        while (buf.readable() && buf.getUnsignedByte(buf.readerIndex()) != 0x24) {
            buf.readByte();
        }

        // Check length and return buffer
        if (buf.readableBytes() >= MESSAGE_HEADER) {
            int length = buf.getUnsignedShort(buf.readerIndex() + 2);
            if (buf.readableBytes() >= length) {
                return buf.readBytes(length);
            }
        }

        return null;
    }

}
