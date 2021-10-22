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

public class JsonFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        int startIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '{');
        if (startIndex >= 0) {

            buf.readerIndex(startIndex);

            int currentIndex = startIndex + 1;
            int nesting = 1;
            while (currentIndex < buf.writerIndex() && nesting > 0) {
                byte currentByte = buf.getByte(currentIndex);
                if (currentByte == '{') {
                    nesting += 1;
                } else if (currentByte == '}') {
                    nesting -= 1;
                }
                currentIndex += 1;
            }

            if (nesting == 0) {
                return buf.readRetainedSlice(currentIndex - startIndex);
            }

        }

        return null;
    }

}
