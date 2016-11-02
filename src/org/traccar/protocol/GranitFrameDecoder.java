/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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

public class GranitFrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        int indexEnd = buf.indexOf(buf.readerIndex(), buf.writerIndex(), new StringFinder("\r\n"));
        if (indexEnd != -1) {
            int indexTilde = buf.indexOf(buf.readerIndex(), buf.writerIndex(), new StringFinder("~"));
            if (indexTilde != -1 && indexTilde < indexEnd) {
                int length = buf.getUnsignedShort(indexTilde + 1);
                indexEnd = buf.indexOf(indexTilde + 2 + length, buf.writerIndex(), new StringFinder("\r\n"));
                if (indexEnd == -1) {
                    return null;
                }
            }
            ChannelBuffer frame = buf.readBytes(indexEnd - buf.readerIndex());
            buf.skipBytes(2);
            return frame;
        }
        return null;
    }

}
