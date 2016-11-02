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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class HuaShengFrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < 2) {
            return null;
        }

        int index = buf.indexOf(buf.readerIndex() + 1, buf.writerIndex(), (byte) 0xC0);
        if (index != -1) {
            ChannelBuffer result = ChannelBuffers.buffer(index + 1 - buf.readerIndex());

            while (buf.readerIndex() <= index) {
                int b = buf.readUnsignedByte();
                if (b == 0xDB) {
                    int ext = buf.readUnsignedByte();
                    if (ext == 0xDC) {
                        result.writeByte(0xC0);
                    } else if (ext == 0xDD) {
                        result.writeByte(0xDB);
                    }
                } else {
                    result.writeByte(b);
                }
            }

            return result;
        }

        return null;
    }

}
