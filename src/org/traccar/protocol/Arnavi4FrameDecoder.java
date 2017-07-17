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

    private static final int PACKET_MINIMUM_LENGTH = 4;
    private static final int RECORD_MINIMUM_LENGTH = 8;

    static final byte HEADER_START_SIGN = (byte) 0xff;
    static final byte HEADER_VERSION_1 = 0x22;
    static final byte HEADER_VERSION_2 = 0x23;
    private static final int HEADER_LENGTH = 10;

    private static final byte PACKAGE_START_SIGN = 0x5b;
    private static final byte PACKAGE_END_SIGN = 0x5d;
    private static final int PACKAGE_MIN_PARCEL_NUMBER = 0x01;
    private static final int PACKAGE_MAX_PARCEL_NUMBER = 0xfb;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < PACKET_MINIMUM_LENGTH) {
            return null;
        }

        if (buf.getByte(0) == HEADER_START_SIGN
                && buf.readableBytes() == HEADER_LENGTH
                && (buf.getByte(1) == HEADER_VERSION_1 || buf.getByte(1) == HEADER_VERSION_2)) {

            return buf.readBytes(HEADER_LENGTH);
        }

        int index = buf.getUnsignedByte(1); // parcel number
        if (buf.getByte(0) == PACKAGE_START_SIGN
                && index >= PACKAGE_MIN_PARCEL_NUMBER && index <= PACKAGE_MAX_PARCEL_NUMBER) {

            int bufferPosition = 2; // start sign + parcel number
            while (bufferPosition + RECORD_MINIMUM_LENGTH < buf.readableBytes()
                    && buf.getByte(bufferPosition) != PACKAGE_END_SIGN) {

                int dataLength = buf.getUnsignedShort(bufferPosition + 1);
                bufferPosition += RECORD_MINIMUM_LENGTH + dataLength; // type + data length + unixtime + data + crc
            }

            if (bufferPosition < buf.readableBytes()
                    && buf.getByte(bufferPosition) == PACKAGE_END_SIGN) {

                return buf.readBytes(bufferPosition + 1); // end sign
            }
        }

        return null;
    }

}
