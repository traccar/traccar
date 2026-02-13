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
import java.nio.charset.StandardCharsets;
import org.traccar.BaseFrameDecoder;
import org.traccar.BasePipelineFactory;

public class NavisFrameDecoder extends BaseFrameDecoder {

    private static final int NTCB_HEADER_LENGTH = 16;
    private static final int NTCB_LENGTH_OFFSET = 12;
    private static final int FLEX_HEADER_LENGTH = 2;

    private int flexDataSize;

    public void setFlexDataSize(int flexDataSize) {
        this.flexDataSize = flexDataSize;
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.getByte(buf.readerIndex()) == 0x7F) {
            return buf.readRetainedSlice(1); // keep alive
        }

        if (ctx != null && flexDataSize == 0) {
            NavisProtocolDecoder protocolDecoder =
                    BasePipelineFactory.getHandler(ctx.pipeline(), NavisProtocolDecoder.class);
            if (protocolDecoder != null) {
                flexDataSize = protocolDecoder.getFlexDataSize();
            }
        }

        if (flexDataSize > 0) {

            if (buf.readableBytes() > FLEX_HEADER_LENGTH) {
                int length = 0;
                String type = buf.toString(buf.readerIndex(), 2, StandardCharsets.US_ASCII);
                switch (type) {
                    // FLEX 1.0
                    case "~A" -> length = flexDataSize * buf.getByte(buf.readerIndex() + FLEX_HEADER_LENGTH) + 1 + 1;
                    case "~T" -> length = flexDataSize + 4 + 1;
                    case "~C" -> length = flexDataSize + 1;

                    // FLEX 2.0 (Extra packages)
                    case "~E" -> {
                        length++;
                        for (int i = 0; i < buf.getByte(buf.readerIndex() + FLEX_HEADER_LENGTH); i++) {
                            if (buf.readableBytes() > FLEX_HEADER_LENGTH + length + 1) {
                                length += buf.getUnsignedShort(length + FLEX_HEADER_LENGTH) + 2;
                            } else {
                                return null;
                            }
                        }
                        length++;
                    }
                    case "~X" -> length = buf.getUnsignedShortLE(buf.readerIndex() + FLEX_HEADER_LENGTH) + 4 + 1;
                    default -> {
                        return null;
                    }
                }

                if (buf.readableBytes() >= FLEX_HEADER_LENGTH + length) {
                    return buf.readRetainedSlice(buf.readableBytes());
                }
            }

        } else {

            if (buf.readableBytes() < NTCB_HEADER_LENGTH) {
                return null;
            }

            int length = NTCB_HEADER_LENGTH + buf.getUnsignedShortLE(buf.readerIndex() + NTCB_LENGTH_OFFSET);
            if (buf.readableBytes() >= length) {
                return buf.readRetainedSlice(length);
            }

        }

        return null;
    }

}
