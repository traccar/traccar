/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

public class BceFrameDecoder extends LengthFieldBasedFrameDecoder {

    private static final int HANDSHAKE_LENGTH = 7; // "#BCE#\r\n"
    
    private boolean header = true;
    
    public BceFrameDecoder() {
        super(1024, 8, 2, 8 + 2 + 1, 0);
    }
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {
        
        if (header && buf.readableBytes() >= HANDSHAKE_LENGTH) {
            buf.skipBytes(HANDSHAKE_LENGTH);
            header = false;
        }

        return super.decode(ctx, channel, buf);
    }

}
