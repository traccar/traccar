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
import org.traccar.helper.BufferUtil;

public class ItsFrameDecoder extends BaseFrameDecoder {

    private static final int MINIMUM_LENGTH = 20;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        ByteBuf frame;
        int delimiterIndex = BufferUtil.indexOf("\r\n", buf);
        if (delimiterIndex > MINIMUM_LENGTH) {
            frame = buf.readRetainedSlice(delimiterIndex - buf.readerIndex());
            buf.skipBytes(2);
            return frame;
        } else {
            delimiterIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '*');
            if (delimiterIndex > MINIMUM_LENGTH) {
                if (buf.writerIndex() > delimiterIndex + 1 && buf.getByte(delimiterIndex + 1) == '*') {
                    delimiterIndex += 1;
                }
                if (buf.getByte(delimiterIndex - 2) == ',') {
                    frame = buf.readRetainedSlice(delimiterIndex - 1 - buf.readerIndex());
                    buf.skipBytes(1); // binary checksum
                } else {
                    frame = buf.readRetainedSlice(delimiterIndex - buf.readerIndex());
                }
                buf.skipBytes(1); // delimiter
                return frame;
            }
        }

        return null;
    }

}
