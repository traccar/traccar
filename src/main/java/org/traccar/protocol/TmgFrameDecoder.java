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

import org.traccar.BaseFrameDecoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class TmgFrameDecoder extends BaseFrameDecoder {

    private boolean isLetter(byte c) {
        return c >= 'a' && c <= 'z';
    }

    private int findHeader(ByteBuf buffer) {
        int guessedIndex = buffer.indexOf(buffer.readerIndex(), buffer.writerIndex(), (byte) '$');
        while (guessedIndex != -1 && buffer.writerIndex() - guessedIndex >= 5) {
            if (buffer.getByte(guessedIndex + 4) == ','
                        && isLetter(buffer.getByte(guessedIndex + 1))
                        && isLetter(buffer.getByte(guessedIndex + 2))
                        && isLetter(buffer.getByte(guessedIndex + 3))) {
                return guessedIndex;
            }
            guessedIndex = buffer.indexOf(guessedIndex + 1, buffer.writerIndex(), (byte) '$');
        }
        return -1;
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        int beginIndex = findHeader(buf);

        if (beginIndex >= 0) {

            buf.readerIndex(beginIndex);

            int endIndex = buf.indexOf(beginIndex, buf.writerIndex(), (byte) '\n');

            if (endIndex >= 0) {
                ByteBuf frame = buf.readRetainedSlice(endIndex - beginIndex);
                buf.readByte(); // delimiter
                return frame;
            }

        }

        return null;
    }

}
