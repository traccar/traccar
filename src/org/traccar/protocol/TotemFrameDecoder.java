/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.nio.charset.Charset;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class TotemFrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {
        
        // Check minimum length
        if (buf.readableBytes() < 10) {
            return null;
        }
        
        // Trim end line
        if (buf.getUnsignedShort(buf.readerIndex()) == 0x0d0a) {
            buf.skipBytes(2);
        }

        // Read message
        int length = Integer.parseInt(buf.toString(buf.readerIndex() + 2, 2, Charset.defaultCharset()), 16);
        if (length <= buf.readableBytes()) {
            return buf.readBytes(length);
        }

        return null;
    }

}
