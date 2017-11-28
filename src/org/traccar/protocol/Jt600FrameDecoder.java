/*
 * Copyright 2012 - 2017 Anton Tananaev (anton@traccar.org)
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

import java.text.ParseException;

public class Jt600FrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < 10) {
            return null;
        }

        char type = (char) buf.getByte(buf.readerIndex());

        if (type == '$') {
            boolean longFormat = buf.getUnsignedByte(buf.readerIndex() + 1) == 0x75;
            int length = buf.getUnsignedShort(buf.readerIndex() + (longFormat ? 8 : 7)) + 10;
            if (length <= buf.readableBytes()) {
                return buf.readBytes(length);
            }
        } else if (type == '(') {
            int endIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ')');
            if (endIndex != -1) {
                return buf.readBytes(endIndex + 1);
            }
        } else {
            throw new ParseException(null, 0); // unknown message
        }

        return null;
    }

}
