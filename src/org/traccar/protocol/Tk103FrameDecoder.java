/*
 * Copyright 2017 Valerii Vyshniak (val@val.one)
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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import java.nio.ByteOrder;

public class Tk103FrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < 2) {
            return null;
        }

        int index;
        for (index = buf.readerIndex(); true; index++) {
            index = buf.indexOf(index, buf.writerIndex(), (byte) ')');
            if (index == -1) {
                break;
            }
            int cnt = 0;
            for (int i = buf.readerIndex(); i < index; i++) {
                if (buf.getByte(i) == (byte) '$') {
                    cnt++;
                }
            }
            if (cnt % 2 == 0) {
                break;
            }
        }

        if (index != -1) {
            ChannelBuffer result = ChannelBuffers.buffer(ByteOrder.LITTLE_ENDIAN, index + 1 - buf.readerIndex());
            buf.readBytes(result, index + 1 - buf.readerIndex());
            result.writerIndex(result.writerIndex() - 1);
            return result;
        }

        return null;
    }

}
