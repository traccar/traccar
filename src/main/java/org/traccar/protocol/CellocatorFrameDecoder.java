/*
 * Copyright 2013 - 2019 Anton Tananaev (anton@traccar.org)
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

public class CellocatorFrameDecoder extends BaseFrameDecoder {

    private static final int MESSAGE_MINIMUM_LENGTH = 15;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < MESSAGE_MINIMUM_LENGTH) {
            return null;
        }

        int length = 0;
        int type = buf.getUnsignedByte(4);
        switch (type) {
            case CellocatorProtocolDecoder.MSG_CLIENT_STATUS -> length = 70;
            case CellocatorProtocolDecoder.MSG_CLIENT_PROGRAMMING -> length = 31;
            case CellocatorProtocolDecoder.MSG_CLIENT_SERIAL_LOG -> length = 70;
            case CellocatorProtocolDecoder.MSG_CLIENT_SERIAL -> {
                if (buf.readableBytes() >= 19) {
                    length = 19 + buf.getUnsignedShortLE(buf.readerIndex() + 16);
                }
            }
            case CellocatorProtocolDecoder.MSG_CLIENT_MODULAR -> {
                length = 15 + buf.getUnsignedByte(buf.readerIndex() + 13);
            }
            case CellocatorProtocolDecoder.MSG_CLIENT_MODULAR_EXT -> {
                length = 16 + buf.getUnsignedShortLE(buf.readerIndex() + 13);
            }
        }

        if (length > 0 && buf.readableBytes() >= length) {
            return buf.readRetainedSlice(length);
        }

        return null;
    }

}
