/*
 * Copyright 2017 Ivan Muratov (binakot@gmail.com)
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

public class Arnavi4FrameDecoder extends FrameDecoder {

    private static final int MIN_LENGTH = 4;
    private static final int HEADER_LENGTH = 10;
    private static final int PACKET_WRAPPER_LENGTH = 8;
    private static final int COMMAND_ANSWER_PACKET_LENGTH = 4;
    private static final int COMMAND_ANSWER_PARCEL_NUMBER = 0xfd;
    private static final byte PACKAGE_END_SIGN = 0x5d;

    private boolean firstPacket = true;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < MIN_LENGTH) {
            return null;
        }

        int length;
        if (firstPacket) {
            firstPacket = false;
            length = HEADER_LENGTH;
        } else {
            int index = buf.getUnsignedByte(1); // parcel number
            if (index == COMMAND_ANSWER_PARCEL_NUMBER) {
                length = COMMAND_ANSWER_PACKET_LENGTH;
            } else {
                int pos = 2; // start sign + parcel number
                while (pos + PACKET_WRAPPER_LENGTH < buf.readableBytes()
                        && buf.getByte(pos) != PACKAGE_END_SIGN) {

                    int dataLength = buf.getUnsignedShort(pos + 1);
                    pos += PACKET_WRAPPER_LENGTH + dataLength; // packet type + data length + unixtime + data + crc
                }

                if (buf.getByte(pos) != PACKAGE_END_SIGN) { // end sign
                    return null;
                }

                length = pos + 1;
            }
        }

        if (buf.readableBytes() >= length) {
            return buf.readBytes(length);
        }

        return null;
    }

}
