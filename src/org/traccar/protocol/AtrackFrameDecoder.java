/*
 * Copyright 2013 - 2014 Anton Tananaev (anton@traccar.org)
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
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;

public class AtrackFrameDecoder extends LengthFieldBasedFrameDecoder {

    private static final int KEEPALIVE_LENGTH = 12;

    public AtrackFrameDecoder() {
        super(1024, 4, 2);
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        // Keep alive message
        if (buf.readableBytes() >= KEEPALIVE_LENGTH
                && buf.getUnsignedShort(buf.readerIndex()) == 0xfe02) {
            return buf.readBytes(KEEPALIVE_LENGTH);
        }

        return super.decode(ctx, channel, buf);
    }

}
