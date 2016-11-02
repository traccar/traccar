/*
 * Copyright 2014 - 2016 Anton Tananaev (anton@traccar.org)
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

public class Gt06FrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        // Check minimum length
        if (buf.readableBytes() < 5) {
            return null;
        }

        int length = 2 + 2; // head and tail

        if (buf.getByte(buf.readerIndex()) == 0x78) {
            length += 1 + buf.getUnsignedByte(buf.readerIndex() + 2);

            int type = buf.getUnsignedByte(buf.readerIndex() + 3);
            if (type == Gt06ProtocolDecoder.MSG_STATUS && length == 13) {
                length += 2; // workaround for #1727
            }

        } else {
            length += 2 + buf.getUnsignedShort(buf.readerIndex() + 2);
        }

        // Check length and return buffer
        if (buf.readableBytes() >= length) {
            return buf.readBytes(length);
        }

        return null;
    }

}
