/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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

public class SabertekFrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        int beginIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) 0x02);
        if (beginIndex >= 0) {
            int endIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) 0x03);
            if (beginIndex >= 0) {
                buf.readerIndex(beginIndex + 1);
                ChannelBuffer frame = buf.readBytes(endIndex - beginIndex - 1);
                buf.readerIndex(endIndex + 1);
                buf.skipBytes(2); // end line
                return frame;
            }
        }

        return null;
    }

}
