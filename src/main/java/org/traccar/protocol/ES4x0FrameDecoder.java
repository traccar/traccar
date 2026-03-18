/*
 * Copyright 2024 - 2025 Anton Tananaev (anton@traccar.org)
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

public class ES4x0FrameDecoder extends BaseFrameDecoder {

    private static final int MESSAGE_HEADER_LENGTH = 5;
    private static final int IMEI_LENGTH = 8;
    private static final int MESSAGE_TYPE_OFFSET = MESSAGE_HEADER_LENGTH + IMEI_LENGTH;
    
    private static final int REGULAR_REPORT_MIN_LENGTH = 55;
    private static final int MAINTENANCE_REPORT_MIN_LENGTH = 95;
    private static final int OBD_REPORT_MIN_LENGTH = 46;

    private static final byte MESSAGE_TYPE_REGULAR = 0x52; // R
    private static final byte MESSAGE_TYPE_MAINTENANCE = 0x4D; // M
    private static final byte MESSAGE_TYPE_OBD = 0x4F; // O

    private static final byte[] MESSAGE_HEADER = {'E', 'T', '4', '1', '0'};

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {
        
        if (buf.readableBytes() < MESSAGE_HEADER_LENGTH + IMEI_LENGTH + 1) {
            return null;
        }

        int readerIndex = buf.readerIndex();
        
        boolean validHeader = true;
        for (int i = 0; i < MESSAGE_HEADER_LENGTH; i++) {
            if (buf.getByte(readerIndex + i) != MESSAGE_HEADER[i]) {
                validHeader = false;
                break;
            }
        }

        if (!validHeader) {
            int headerIndex = buf.indexOf(readerIndex, readerIndex + 64, (byte) 'E');
            if (headerIndex >= 0) {
                buf.readerIndex(headerIndex);
            } else {
                buf.readerIndex(buf.writerIndex());
            }
            return null;
        }

        byte messageType = buf.getByte(readerIndex + MESSAGE_TYPE_OFFSET);
        
        int messageLength;
        switch (messageType) {
            case MESSAGE_TYPE_REGULAR:
                messageLength = REGULAR_REPORT_MIN_LENGTH;
                break;
            case MESSAGE_TYPE_MAINTENANCE:
                messageLength = MAINTENANCE_REPORT_MIN_LENGTH;
                break;
            case MESSAGE_TYPE_OBD:
                messageLength = OBD_REPORT_MIN_LENGTH;
                break;
            default:
                buf.readerIndex(readerIndex + 1);
                return null;
        }

        if (buf.readableBytes() >= messageLength) {
            return buf.readRetainedSlice(messageLength);
        }

        return null;
    }

}
