/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
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

import java.nio.charset.StandardCharsets;

public class TechtoCruzFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        int lengthStart = buf.readerIndex() + 3;
        int lengthEnd = buf.indexOf(lengthStart, buf.writerIndex(), (byte) ',');
        if (lengthEnd > 0) {
            int length = lengthStart
                    + Integer.parseInt(buf.toString(lengthStart, lengthEnd - lengthStart, StandardCharsets.US_ASCII));
            if (buf.readableBytes() >= length) {
                return buf.readRetainedSlice(length);
            }
        }

        return null;
    }

}
