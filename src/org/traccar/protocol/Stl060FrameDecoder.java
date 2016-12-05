/*
 * Copyright 2014 Anton Tananaev (anton@traccar.org)
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
import org.traccar.CharacterDelimiterFrameDecoder;

public class Stl060FrameDecoder extends CharacterDelimiterFrameDecoder {

    public Stl060FrameDecoder(int maxFrameLength) {
        super(maxFrameLength, '#');
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        ChannelBuffer result = (ChannelBuffer) super.decode(ctx, channel, buf);

        if (result != null) {

            int index = result.indexOf(result.readerIndex(), result.writerIndex(), (byte) '$');
            if (index == -1) {
                return result;
            } else {
                result.skipBytes(index);
                return result.readBytes(result.readableBytes());
            }

        }

        return null;
    }

}
