/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class TmgFrameDecoder extends FrameDecoder {

    private boolean isLetter(byte c) {
        return c >= 'a' && c <= 'z';
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        int beginIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), new ChannelBufferIndexFinder() {
            @Override
            public boolean find(ChannelBuffer buffer, int guessedIndex) {
                if (buffer.getByte(guessedIndex) != (byte) '$' || buffer.writerIndex() - guessedIndex < 5) {
                    return false;
                }
                if (buffer.getByte(guessedIndex + 4) == ','
                        && isLetter(buffer.getByte(guessedIndex + 1))
                        && isLetter(buffer.getByte(guessedIndex + 2))
                        && isLetter(buffer.getByte(guessedIndex + 3))) {
                    return true;
                }
                return false;
            }
        });

        if (beginIndex >= 0) {

            buf.readerIndex(beginIndex);

            int endIndex = buf.indexOf(beginIndex, buf.writerIndex(), (byte) '\n');

            if (endIndex >= 0) {
                ChannelBuffer frame = buf.readBytes(endIndex - beginIndex);
                buf.readByte(); // delimiter
                return frame;
            }

        }

        return null;
    }

}
