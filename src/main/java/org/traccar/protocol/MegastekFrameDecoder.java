/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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

import java.nio.charset.StandardCharsets;

public class MegastekFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 10) {
            return null;
        }

        if (Character.isDigit(buf.getByte(buf.readerIndex()))) {
            int length = 4 + Integer.parseInt(buf.toString(buf.readerIndex(), 4, StandardCharsets.US_ASCII));
            if (buf.readableBytes() >= length) {
                return buf.readRetainedSlice(length);
            }
        } else {
            while (buf.getByte(buf.readerIndex()) == '\r' || buf.getByte(buf.readerIndex()) == '\n') {
                buf.skipBytes(1);
            }
            int delimiter = BufferUtil.indexOf("\r\n", buf);
            if (delimiter == -1) {
                delimiter = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '!');
            }
            if (delimiter == -1) {
                delimiter = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '\n');
            }
            if (delimiter != -1) {
                ByteBuf result = buf.readRetainedSlice(delimiter - buf.readerIndex());
                buf.skipBytes(1);
                return result;
            }
        }

        return null;
    }

}
