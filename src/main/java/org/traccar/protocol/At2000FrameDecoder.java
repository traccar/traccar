/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseFrameDecoder;
import org.traccar.NetworkMessage;

public class At2000FrameDecoder extends BaseFrameDecoder {

    private static final int BLOCK_LENGTH = 16;
    private static final int ACK_LENGTH = 496;

    private boolean firstPacket = true;

    private ByteBuf currentBuffer;
    private int acknowledgedBytes;

    private void sendResponse(Channel channel) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer(2 * BLOCK_LENGTH);
            response.writeByte(At2000ProtocolDecoder.MSG_ACKNOWLEDGEMENT);
            response.writeMedium(1);
            response.writeByte(0x00); // success
            response.writerIndex(2 * BLOCK_LENGTH);
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 5) {
            return null;
        }

        int length;
        if (firstPacket) {
            firstPacket = false;
            length = buf.getUnsignedMediumLE(buf.readerIndex() + 2);
        } else {
            length = buf.getUnsignedMediumLE(buf.readerIndex() + 1);
        }

        length += BLOCK_LENGTH;
        if (length % BLOCK_LENGTH != 0) {
            length = (length / BLOCK_LENGTH + 1) * BLOCK_LENGTH;
        }

        if ((buf.readableBytes() >= length || buf.readableBytes() % ACK_LENGTH == 0)
                && (buf != currentBuffer || buf.readableBytes() > acknowledgedBytes)) {
            sendResponse(channel);
            currentBuffer = buf;
            acknowledgedBytes = buf.readableBytes();
        }

        if (buf.readableBytes() >= length) {
            return buf.readRetainedSlice(length);
        }

        return null;
    }

}
