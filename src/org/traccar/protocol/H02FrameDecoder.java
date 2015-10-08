/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.nio.charset.Charset;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.traccar.helper.ChannelBufferTools;

public class H02FrameDecoder extends FrameDecoder {

    private static final int MESSAGE_LENGTH = 32;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {

        String marker = buf.toString(buf.readerIndex(), 1, Charset.defaultCharset());

        while (!marker.equals("*") && !marker.equals("$") && buf.readableBytes() > 0) {
            buf.skipBytes(1);
            if (buf.readableBytes() > 0) {
                marker = buf.toString(buf.readerIndex(), 1, Charset.defaultCharset());
            }
        }

        if (marker.equals("*")) {

            // Return text message
            Integer index = ChannelBufferTools.find(buf, buf.readerIndex(), buf.readableBytes(), "#");
            if (index != null) {
                return buf.readBytes(index + 1 - buf.readerIndex());
            }

        } else if (marker.equals("$") && buf.readableBytes() >= MESSAGE_LENGTH) {

            // Return binary message
            return buf.readBytes(MESSAGE_LENGTH);

        }

        return null;
    }

}
