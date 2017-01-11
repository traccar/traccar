/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import java.nio.ByteOrder;

public class At2000FrameDecoder extends FrameDecoder {

    private static final int BLOCK_LENGTH = 16;
    private static final int ACK_LENGTH = 496;

    private boolean firstPacket = true;

    private ChannelBuffer currentBuffer;
    private int acknowledgedBytes;

    private void sendResponse(Channel channel) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 2 * BLOCK_LENGTH);
            response.writeByte(At2000ProtocolDecoder.MSG_ACKNOWLEDGEMENT);
            response.writeMedium(ChannelBuffers.swapMedium(1));
            response.writeByte(0x00); // success
            response.writerIndex(2 * BLOCK_LENGTH);
            channel.write(response);
        }
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < 5) {
            return null;
        }

        int length;
        if (firstPacket) {
            firstPacket = false;
            length = buf.getUnsignedMedium(buf.readerIndex() + 2);
        } else {
            length = buf.getUnsignedMedium(buf.readerIndex() + 1);
        }

        length += BLOCK_LENGTH;
        if (length % BLOCK_LENGTH != 0) {
            length = (length / BLOCK_LENGTH + 1) * BLOCK_LENGTH;
        }

        if (buf.readableBytes() >= length || buf.readableBytes() % ACK_LENGTH == 0) {
            if (buf != currentBuffer || buf.readableBytes() > acknowledgedBytes) {
                sendResponse(channel);
                currentBuffer = buf;
                acknowledgedBytes = buf.readableBytes();
            }
        }

        if (buf.readableBytes() >= length) {
            return buf.readBytes(length);
        }

        return null;
    }

}
