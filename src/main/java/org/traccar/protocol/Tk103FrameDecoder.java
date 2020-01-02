/*
 * Copyright 2017 Valerii Vyshniak (val@val.one)
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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseFrameDecoder;

public class Tk103FrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 2) {
            return null;
        }

        int frameStartIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '(');
        if (frameStartIndex == -1) {
            buf.clear();
            return null;
        }

        int frameEndIndex, freeTextSymbolCounter;
        for (frameEndIndex = frameStartIndex, freeTextSymbolCounter = 0;; frameEndIndex++) {
            int freeTextIndex = frameEndIndex;
            frameEndIndex = buf.indexOf(frameEndIndex, buf.writerIndex(), (byte) ')');
            if (frameEndIndex == -1) {
                break;
            }
            for (;; freeTextIndex++, freeTextSymbolCounter++) {
                freeTextIndex = buf.indexOf(freeTextIndex, frameEndIndex, (byte) '$');
                if (freeTextIndex == -1 || freeTextIndex >= frameEndIndex) {
                    break;
                }
            }
            if (freeTextSymbolCounter % 2 == 0) {
                break;
            }
        }

        if (frameEndIndex == -1) {
            while (buf.readableBytes() > 1024) {
                int discardUntilIndex = buf.indexOf(buf.readerIndex() + 1, buf.writerIndex(), (byte) '(');
                if (discardUntilIndex == -1) {
                    buf.clear();
                } else {
                    buf.readerIndex(discardUntilIndex);
                }
            }
            return null;
        }

        buf.readerIndex(frameStartIndex);

        return buf.readRetainedSlice(frameEndIndex + 1 - frameStartIndex);
    }

}
