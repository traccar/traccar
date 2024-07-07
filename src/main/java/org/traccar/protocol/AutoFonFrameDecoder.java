/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
 * Copyright 2015 Vitaly Litvak (vitavaque@gmail.com)
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

public class AutoFonFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        // Check minimum length
        if (buf.readableBytes() < 12) {
            return null;
        }

        int length = switch (buf.getUnsignedByte(buf.readerIndex())) {
            case AutoFonProtocolDecoder.MSG_LOGIN -> 12;
            case AutoFonProtocolDecoder.MSG_LOCATION -> 78;
            case AutoFonProtocolDecoder.MSG_HISTORY -> 257;
            case AutoFonProtocolDecoder.MSG_45_LOGIN -> 19;
            case AutoFonProtocolDecoder.MSG_45_LOCATION -> 34;
            default -> 0;
        };

        // Check length and return buffer
        if (length != 0 && buf.readableBytes() >= length) {
            return buf.readRetainedSlice(length);
        }

        return null;
    }

}
