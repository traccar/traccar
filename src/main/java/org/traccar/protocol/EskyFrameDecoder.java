/*
 * Copyright 2017 - 2020 Anton Tananaev (anton@traccar.org)
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

public class EskyFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        int startIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) 'E');
        if (startIndex >= 0) {
            buf.readerIndex(startIndex);
            int endIndex = buf.indexOf(buf.readerIndex() + 1, buf.writerIndex(), (byte) 'E');
            if (endIndex > 0) {
                return buf.readRetainedSlice(endIndex - buf.readerIndex());
            } else {
                return buf.readRetainedSlice(buf.readableBytes()); // assume full frame
            }
        }

        return null;
    }

}
