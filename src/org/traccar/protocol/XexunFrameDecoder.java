/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.helper.ChannelBufferTools;

public class XexunFrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {

        // Check minimum length
        int length = buf.readableBytes();
        if (length < 80) {
            return null;
        }

        // Find start
        Integer beginIndex = ChannelBufferTools.find(buf, buf.readerIndex(), "GPRMC");
        if (beginIndex == null) {
            beginIndex = ChannelBufferTools.find(buf, buf.readerIndex(), "GNRMC");
            if (beginIndex == null) {
                return null;
            }
        }

        // Find identifier
        Integer idIndex = ChannelBufferTools.find(buf, beginIndex, "imei:");
        if (idIndex == null) {
            return null;
        }

        // Find end
        Integer endIndex = ChannelBufferTools.find(buf, idIndex, ",");
        if (endIndex == null) {
            return null;
        }

        // Read buffer
        buf.skipBytes(beginIndex);
        ChannelBuffer frame = buf.readBytes(endIndex - beginIndex + 1);

        return frame;
    }

}
