/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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

import org.traccar.BaseFrameDecoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class TelicFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 4) {
            return null;
        }

        long length = buf.getUnsignedIntLE(buf.readerIndex());

        if (length < 1024) {
            if (buf.readableBytes() >= length + 4) {
                buf.readUnsignedIntLE();
                return buf.readRetainedSlice((int) length);
            }
        } else {
            int endIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) 0);
            if (endIndex >= 0) {
                ByteBuf frame = buf.readRetainedSlice(endIndex - buf.readerIndex());
                buf.readByte();
                if (frame.readableBytes() > 0) {
                    return frame;
                }
            }
        }

        return null;
    }

}
