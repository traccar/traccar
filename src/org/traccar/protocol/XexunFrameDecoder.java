/*
 * Copyright 2012 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BufferUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class XexunFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 80) {
            return null;
        }

        int beginIndex = BufferUtil.indexOf("GPRMC", buf);
        if (beginIndex == -1) {
            beginIndex = BufferUtil.indexOf("GNRMC", buf);
            if (beginIndex == -1) {
                return null;
            }
        }

        int identifierIndex = BufferUtil.indexOf("imei:", buf, beginIndex, buf.writerIndex());
        if (identifierIndex == -1) {
            return null;
        }

        int endIndex = buf.indexOf(identifierIndex, buf.writerIndex(), (byte) ',');
        if (endIndex == -1) {
            return null;
        }

        buf.skipBytes(beginIndex - buf.readerIndex());

        return buf.readRetainedSlice(endIndex - beginIndex + 1);
    }

}
