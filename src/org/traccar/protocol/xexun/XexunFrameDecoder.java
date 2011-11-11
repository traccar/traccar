/*
 * Copyright 2010 Anton Tananaev (anton@tananaev.com)
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
package net.sourceforge.opentracking.protocol.xexun;

import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.buffer.ChannelBuffer;

public class XexunFrameDecoder extends FrameDecoder {

    /**
     * Find string in network buffer
     */
    private static Integer find(
            ChannelBuffer buf,
            Integer start,
            Integer length,
            String subString) {

        int index = start;
        boolean match;

        for (; index < length; index++) {
            match = true;

            for (int i = 0; i < subString.length(); i++) {
                char c = (char) buf.getByte(index + i);
                if (c != subString.charAt(i)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return index;
            }
        }

        return null;
    }

    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {

        // Check minimum length
        int length = buf.readableBytes();
        if (length < 100) {
            return null;
        }

        // Find start
        Integer beginIndex = find(buf, 0, length, "GPRMC");
        if (beginIndex == null) {
            return null;
        }

        // Find identifier
        Integer idIndex = find(buf, beginIndex, length, "imei:");
        if (idIndex == null) {
            return null;
        }

        // Find end
        Integer endIndex = find(buf, idIndex, length, ",");
        if (endIndex == null) {
            return null;
        }

        // Read buffer
        buf.skipBytes(beginIndex);
        ChannelBuffer frame = buf.readBytes(endIndex - beginIndex + 1);

        return frame;
    }

}
