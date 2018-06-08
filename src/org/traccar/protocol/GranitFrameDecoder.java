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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseFrameDecoder;
import org.traccar.helper.BufferUtil;

public class GranitFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        int indexEnd = BufferUtil.indexOf("\r\n", buf);
        if (indexEnd != -1) {
            int indexTilde = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '~');
            if (indexTilde != -1 && indexTilde < indexEnd) {
                int length = buf.getUnsignedShortLE(indexTilde + 1);
                indexEnd = BufferUtil.indexOf("\r\n", buf, indexTilde + 2 + length, buf.writerIndex());
                if (indexEnd == -1) {
                    return null;
                }
            }
            ByteBuf frame = buf.readRetainedSlice(indexEnd - buf.readerIndex());
            buf.skipBytes(2);
            return frame;
        }
        return null;
    }

}
