/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseFrameDecoder;

import java.nio.charset.StandardCharsets;

public class AtrackFrameDecoder extends BaseFrameDecoder {

    private static final int KEEPALIVE_LENGTH = 12;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() >= 2) {

            if (buf.getUnsignedShort(buf.readerIndex()) == 0xfe02) {

                if (buf.readableBytes() >= KEEPALIVE_LENGTH) {
                    return buf.readBytes(KEEPALIVE_LENGTH);
                }

            } else if (buf.getUnsignedShort(buf.readerIndex()) == 0x4050 && buf.getByte(buf.readerIndex() + 2) != ',') {

                if (buf.readableBytes() > 6) {
                    int length = buf.getUnsignedShort(buf.readerIndex() + 4) + 4 + 2;
                    if (buf.readableBytes() >= length) {
                        return buf.readBytes(length);
                    }
                }

            } else {

                ByteBuf delimiter = Unpooled.wrappedBuffer("\r\n".getBytes(StandardCharsets.US_ASCII));
                int endIndex = ByteBufUtil.indexOf(delimiter, buf);
                if (endIndex > 0) {
                    return buf.readBytes(endIndex - buf.readerIndex() + 2);
                }

            }

        }

        return null;
    }

}
