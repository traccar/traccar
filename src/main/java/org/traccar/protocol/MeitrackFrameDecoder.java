/*
 * Copyright 2014 - 2018 Anton Tananaev (anton@traccar.org)
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

public class MeitrackFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 10) {
            return null;
        }

        int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ',');
        if (index != -1) {
            int length = index - buf.readerIndex() + Integer.parseInt(
                    buf.toString(buf.readerIndex() + 3, index - buf.readerIndex() - 3, StandardCharsets.US_ASCII));
            
            // For CCE protocol messages, ensure we have enough buffer space
            // The protocol decoder needs extra bytes for parsing multiple position records
            int requiredBytes = Math.max(length, buf.readableBytes());
            
            if (buf.readableBytes() >= requiredBytes) {
                return buf.readRetainedSlice(requiredBytes);
            }
        }

        return null;
    }

}
