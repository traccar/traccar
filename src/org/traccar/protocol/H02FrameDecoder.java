/*
 * Copyright 2013 - 2017 Anton Tananaev (anton@traccar.org)
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

public class H02FrameDecoder extends FrameDecoder {

    private static final int MESSAGE_SHORT = 32;
    private static final int MESSAGE_LONG = 45;

    private int messageLength;

    public H02FrameDecoder(int messageLength) {
        this.messageLength = messageLength;
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        char marker = (char) buf.getByte(buf.readerIndex());

        while (marker != '*' && marker != '$' && marker != 'X' && buf.readableBytes() > 0) {
            buf.skipBytes(1);
            if (buf.readableBytes() > 0) {
                marker = (char) buf.getByte(buf.readerIndex());
            }
        }

        switch (marker) {
            case '*':

                // Return text message
                int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '#');
                if (index != -1) {
                    ChannelBuffer result = buf.readBytes(index + 1 - buf.readerIndex());
                    while (buf.readable()
                            && (buf.getByte(buf.readerIndex()) == '\r' || buf.getByte(buf.readerIndex()) == '\n')) {
                        buf.readByte(); // skip new line
                    }
                    return result;
                }

                break;

            case '$':

                if (messageLength == 0) {
                    if (buf.readableBytes() == MESSAGE_LONG) {
                        messageLength = MESSAGE_LONG;
                    } else {
                        messageLength = MESSAGE_SHORT;
                    }
                }

                if (buf.readableBytes() >= messageLength) {
                    return buf.readBytes(messageLength);
                }

                break;

            case 'X':

                if (buf.readableBytes() >= MESSAGE_SHORT) {
                    return buf.readBytes(MESSAGE_SHORT);
                }

                break;

            default:

                return null;
        }

        return null;
    }

}
