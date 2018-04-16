/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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

public class RoboTrackFrameDecoder extends FrameDecoder {

    private int messageLength(ChannelBuffer buf) {
        switch ((int) buf.getByte(buf.readerIndex())) {
            case RoboTrackProtocolDecoder.MSG_ID:
                return 69;
            case RoboTrackProtocolDecoder.MSG_ACK:
                return 3;
            case RoboTrackProtocolDecoder.MSG_GPS:
            case RoboTrackProtocolDecoder.MSG_GSM:
            case RoboTrackProtocolDecoder.MSG_IMAGE_START:
                return 24;
            case RoboTrackProtocolDecoder.MSG_IMAGE_DATA:
                return 8 + buf.getUnsignedShort(buf.readerIndex() + 1);
            case RoboTrackProtocolDecoder.MSG_IMAGE_END:
                return 6;
            default:
                return Integer.MAX_VALUE;
        }
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        int length = messageLength(buf);

        if (buf.readableBytes() >= length) {
            return buf.readBytes(length);
        }

        return null;
    }

}
