/*
 * Copyright 2014 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class Pt502FrameDecoder extends FrameDecoder {

    private static final int BINARY_HEADER = 5;

    private void skipFrameEnd(ChannelBuffer buf) {
        while (buf.readable()) {
            short currentByte = buf.getUnsignedByte(buf.readerIndex());

            if (currentByte != (byte) '\r' && currentByte != (byte) '#' && currentByte != (byte) '\n') {
                break;
            }

            buf.skipBytes(1);
        }
    }


    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < BINARY_HEADER) {
            return null;
        }

        if (buf.getUnsignedByte(buf.readerIndex()) == 0xbf) {
            buf.skipBytes(BINARY_HEADER);
        }

        if (buf.getUnsignedByte(buf.readerIndex()) == (byte) '$') {
            int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '\r');

            if (index < 0) {
                index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '#');
            }
            if (index < 0) {
                index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '\n');
            }

            if (index > 0) {
                ChannelBuffer result = buf.readBytes(index - buf.readerIndex());

                skipFrameEnd(buf);

                return result;
            }
        } else if (buf.getUnsignedByte(buf.readerIndex()) == (byte) '@') {
            int toRead = 5;

            while (toRead <= buf.readableBytes()) {
                toRead += buf.getUnsignedByte(buf.readerIndex() + toRead - 1);

                if (toRead == buf.readableBytes() || buf.getUnsignedByte(buf.readerIndex() + toRead) > 31) {
                    break;
                } else {
                    toRead += 3;
                }
            }

            ChannelBuffer result = buf.readBytes(toRead);

            return result;
        }

        return null;
    }

}
