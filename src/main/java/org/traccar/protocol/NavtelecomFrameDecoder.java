/*
 * Copyright 2021 - 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BasePipelineFactory;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;

public class NavtelecomFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.getByte(buf.readerIndex()) == 0x7f) {

            buf.skipBytes(1);
            return null;

        } else if (buf.getByte(buf.readerIndex()) == '@') {

            int length = buf.getUnsignedShortLE(12) + 12 + 2 + 2;
            if (buf.readableBytes() >= length) {
                return buf.readRetainedSlice(length);
            }

        } else {

            NavtelecomProtocolDecoder protocolDecoder =
                    BasePipelineFactory.getHandler(ctx.pipeline(), NavtelecomProtocolDecoder.class);
            if (protocolDecoder == null) {
                throw new RuntimeException("Decoder not found");
            }

            String type = buf.getCharSequence(buf.readerIndex(), 2, StandardCharsets.US_ASCII).toString();
            BitSet bits = protocolDecoder.getBits();

            if (type.equals("~A")) {
                int count = buf.getUnsignedByte(buf.readerIndex() + 2);
                int length = 2 + 1 + 1;

                for (int i = 0; i < count; i++) {
                    for (int j = 0; j < bits.length(); j++) {
                        if (bits.get(j)) {
                            length += NavtelecomProtocolDecoder.getItemLength(j + 1);
                        }
                    }
                }

                if (buf.readableBytes() >= length) {
                    return buf.readRetainedSlice(length);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported message type: " + type);
            }

        }

        return null;
    }

}
