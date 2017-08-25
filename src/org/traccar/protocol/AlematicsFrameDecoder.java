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
import org.jboss.netty.handler.codec.frame.LineBasedFrameDecoder;

public class AlematicsFrameDecoder extends LineBasedFrameDecoder {

    private static final int MESSAGE_MINIMUM_LENGTH = 2;

    public AlematicsFrameDecoder(int maxFrameLength) {
        super(maxFrameLength);
    }

    // example of heartbeat: FA F8 00 07 00 03 15 AD 4E 78 3A D2

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < MESSAGE_MINIMUM_LENGTH) {
            return null;
        }

        if (buf.getUnsignedShort(buf.readerIndex()) == 0xFAF8) {
            ChannelBuffer heartbeat = buf.readBytes(12);
            if (channel != null) {
                channel.write(heartbeat);
            }
        }

        return super.decode(ctx, channel, buf);
    }

}
