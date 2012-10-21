/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.text.ParseException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.traccar.helper.ChannelBufferTools;

public class Jt600FrameDecoder extends FrameDecoder {

    private static int MESSAGE_LENGTH = 37;

    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {

        // Message identifier
        char first = (char) buf.getByte(0);
        int length = buf.readableBytes();
        
        if (first == '$') {
            // Check length
            if (length >= MESSAGE_LENGTH) {
                return buf.readBytes(MESSAGE_LENGTH);
            }
        } else if (first == '(') {
            // Find ending
            Integer endIndex = ChannelBufferTools.find(buf, 0, length, ")");
            if (endIndex != null) {
                return buf.readBytes(endIndex + 1);
            }
        } else {
            // Unknown message
            throw new ParseException(null, 0);
        }

        return null;
    }

}
