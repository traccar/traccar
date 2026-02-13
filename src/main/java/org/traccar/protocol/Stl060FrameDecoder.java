/*
 * Copyright 2014 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.CharacterDelimiterFrameDecoder;

public class Stl060FrameDecoder extends CharacterDelimiterFrameDecoder {

    public Stl060FrameDecoder(int maxFrameLength) {
        super(maxFrameLength, '#');
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {

        ByteBuf result = (ByteBuf) super.decode(ctx, buf);

        if (result != null) {

            int index = result.indexOf(result.readerIndex(), result.writerIndex(), (byte) '$');
            if (index == -1) {
                return result;
            } else {
                result.skipBytes(index);
                return result.readRetainedSlice(result.readableBytes());
            }

        }

        return null;
    }

}
