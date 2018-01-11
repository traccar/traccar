/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import java.nio.charset.StandardCharsets;

public class T57FrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < 10) {
            return null;
        }

        String type = buf.toString(buf.readerIndex() + 5, 2, StandardCharsets.US_ASCII);
        int count = type.equals("F3") ? 12 : 14;

        int index = 0;
        while (index >= 0 && count > 0) {
            index = buf.indexOf(index + 1, buf.writerIndex(), (byte) '#');
            if (index > 0) {
                count -= 1;
            }
        }

        return index > 0 ? buf.readBytes(index + 1 - buf.readerIndex()) : null;
    }

}
