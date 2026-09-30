/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import java.nio.charset.StandardCharsets;

public class AccentFrameDecoder extends BaseFrameDecoder {

    private String buffer = "";

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {
        buffer += buf.readCharSequence(buf.readableBytes(), StandardCharsets.US_ASCII)
                .toString()
                .replace("\r", "")
                .replace("\n", "");
        if (buffer.isEmpty()) {
            return null;
        }
        if (buffer.endsWith("AAAA") && !buffer.startsWith("AA1") && !buffer.startsWith("AA2")) {
            buffer = buffer.substring(0, buffer.length() - 4);
            return "AAAA";
        }
        if (buffer.startsWith("AA3") && buffer.length() < 70) {
            buffer = "";
            return null;
        }
        if (buffer.contains("AA01MF2")) {
            String message = buffer;
            buffer = "";
            return message;
        }
        if ((buffer.startsWith("AA1") || buffer.startsWith("AA2")) && !buffer.startsWith("AA01")) {
            int length = frameLength(buffer.length());
            if (length == 0) {
                return null;
            }
            String frame = buffer.substring(0, length);
            buffer = buffer.substring(length);
            return frame;
        }
        if (buffer.contains("AA00") || buffer.contains("AA1") || buffer.contains("AA2")) {
            String message = buffer;
            buffer = "";
            return message;
        }
        return null;
    }

    private static int frameLength(int available) {
        if (available == 74 || available == 104 || available == 106) {
            return available;
        }
        return 0;
    }

}
