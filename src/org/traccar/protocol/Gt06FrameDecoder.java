/*
 * Copyright 2014 - 2017 Anton Tananaev (anton@traccar.org)
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

        if (buf.readableBytes() < 5) {
            return null;
        }

        int length = 2 + 2; // head and tail

        if (buf.getByte(buf.readerIndex()) == 0x78) {
            length += 1 + buf.getUnsignedByte(buf.readerIndex() + 2);
        } else {
            length += 2 + buf.getUnsignedShort(buf.readerIndex() + 2);
        }

        if (buf.readableBytes() >= length && buf.getUnsignedShort(buf.readerIndex() + length - 2) == 0x0d0a) {
            return buf.readBytes(length);
        }

        int endIndex = buf.readerIndex() - 1;
        do {
            endIndex = buf.indexOf(endIndex + 1, buf.writerIndex(), (byte) 0x0d);
            if (endIndex > 0 && buf.writerIndex() > endIndex + 1 && buf.getByte(endIndex + 1) == 0x0a) {
                return buf.readBytes(endIndex + 2 - buf.readerIndex());
            }
        } while (endIndex > 0);

        return null;
    }

}
