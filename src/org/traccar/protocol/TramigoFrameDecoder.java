/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;

import java.nio.ByteOrder;

public class TramigoFrameDecoder extends LengthFieldBasedFrameDecoder {

    public TramigoFrameDecoder() {
        super(1024, 6, 2, -8, 0);
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < 20) {
            return null;
        }

        // Swap byte order for legacy protocol
        if (buf.getUnsignedByte(buf.readerIndex()) == 0x80) {
            int length = buf.readableBytes();
            byte[] bytes = new byte[length];
            buf.getBytes(buf.readerIndex(), bytes);

            ChannelBuffer result = (ChannelBuffer) super.decode(
                    ctx, channel, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, bytes));
            if (result != null) {
                buf.skipBytes(result.readableBytes());
            }
            return result;
        }

        return super.decode(ctx, channel, buf);
    }

}
