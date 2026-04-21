/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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

public class SnapperFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        byte header = buf.getByte(buf.readerIndex());
        if (header == 'P') {
            if (buf.readableBytes() >= 2) {
                return buf.readRetainedSlice(2);
            }
        } else if (buf.readableBytes() >= 16) {
            int length = buf.getIntLE(buf.readerIndex() + 12) + 12 + 4 + 9;
            if (buf.readableBytes() >= length) {
                return buf.readRetainedSlice(length);
            }
        }

        return null;
    }

}
