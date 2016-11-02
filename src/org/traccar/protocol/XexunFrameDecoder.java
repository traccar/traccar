/*
 * Copyright 2012 - 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.StringFinder;

public class XexunFrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < 80) {
            return null;
        }

        int beginIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), new StringFinder("GPRMC"));
        if (beginIndex == -1) {
            beginIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), new StringFinder("GNRMC"));
            if (beginIndex == -1) {
                return null;
            }
        }

        int identifierIndex = buf.indexOf(beginIndex, buf.writerIndex(), new StringFinder("imei:"));
        if (identifierIndex == -1) {
            return null;
        }

        int endIndex = buf.indexOf(identifierIndex, buf.writerIndex(), (byte) ',');
        if (endIndex == -1) {
            return null;
        }

        buf.skipBytes(beginIndex - buf.readerIndex());

        return buf.readBytes(endIndex - beginIndex + 1);
    }

}
