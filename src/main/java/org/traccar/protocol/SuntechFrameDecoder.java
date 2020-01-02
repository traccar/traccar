/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseFrameDecoder;

public class SuntechFrameDecoder extends BaseFrameDecoder {

    private ByteBuf readFrame(ByteBuf buf, int delimiterIndex) {
        ByteBuf frame = buf.readRetainedSlice(delimiterIndex - buf.readerIndex());
        buf.skipBytes(1); // delimiter
        return frame;
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.getByte(buf.readerIndex() + 1) == 0) {

            int length = 1 + 2 + buf.getShort(buf.readerIndex() + 1);
            if (buf.readableBytes() >= length) {
                return buf.readRetainedSlice(length);
            }

        } else {

            int delimiterIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '\r');
            while (delimiterIndex > 0) {
                if (delimiterIndex + 1 < buf.writerIndex() && buf.getByte(delimiterIndex + 1) == '\n') {
                    delimiterIndex = buf.indexOf(delimiterIndex + 1, buf.writerIndex(), (byte) '\r');
                } else {
                    return readFrame(buf, delimiterIndex);
                }
            }

        }

        return null;
    }

}
