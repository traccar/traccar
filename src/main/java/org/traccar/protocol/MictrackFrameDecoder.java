/*
 * Copyright 2026 Drew Taylor (Drew.Taylor@fognetx.com)
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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MictrackFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        if (!buf.isReadable()) {
            return;
        }
        byte first = buf.getByte(buf.readerIndex());
        if (first == '*') {
            // *HQ,...# format
            int end = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '#');
            if (end < 0) {
                return;
            }
            out.add(buf.readRetainedSlice(end - buf.readerIndex()));
            buf.skipBytes(1);
        } else if (first == '#') {
            // #IMEI#MT700#...## format
            for (int i = buf.readerIndex(); i < buf.writerIndex() - 1; i++) {
                if (buf.getByte(i) == '#' && buf.getByte(i + 1) == '#') {
                    out.add(buf.readRetainedSlice(i - buf.readerIndex()));
                    buf.skipBytes(2);
                    while (buf.isReadable() && (buf.getByte(buf.readerIndex()) == '\r'
                            || buf.getByte(buf.readerIndex()) == '\n')) {
                        buf.skipBytes(1);
                    }
                    return;
                }
            }
        } else {
            // MT;... or IMEI$... format: newline-terminated
            int end = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '\n');
            if (end < 0) {
                return;
            }
            int length = end - buf.readerIndex();
            if (length > 0 && buf.getByte(buf.readerIndex() + length - 1) == '\r') {
                out.add(buf.readRetainedSlice(length - 1));
                buf.skipBytes(2);
            } else {
                out.add(buf.readRetainedSlice(length));
                buf.skipBytes(1);
            }
        }
    }

}
