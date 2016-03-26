/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;

import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class NoranProtocolEncoder extends BaseProtocolEncoder {

    private ChannelBuffer encodeContent(String content) {

        ChannelBuffer buf = ChannelBuffers.buffer(ByteOrder.LITTLE_ENDIAN, 12 + 56);

        buf.writeBytes(
                ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, "\r\n*KW", Charset.defaultCharset()));
        buf.writeByte(0);
        buf.writeShort(buf.capacity());
        buf.writeShort(NoranProtocolDecoder.MSG_CONTROL);
        buf.writeInt(0); // gis ip
        buf.writeShort(0); // gis port
        buf.writeBytes(content.getBytes(Charset.defaultCharset()));
        buf.writerIndex(buf.writerIndex() + 50 - content.length());
        buf.writeBytes(
                ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, "\r\n", Charset.defaultCharset()));

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_POSITION_SINGLE:
                return encodeContent("*KW,000,000,000000#");
            case Command.TYPE_POSITION_PERIODIC:
                int interval = ((Number) command.getAttributes().get(Command.KEY_FREQUENCY)).intValue();
                return encodeContent("*KW,000,002,000000," + interval + "#");
            case Command.TYPE_POSITION_STOP:
                return encodeContent("*KW,000,002,000000,0#");
            case Command.TYPE_ENGINE_STOP:
                return encodeContent("*KW,000,007,000000,0#");
            case Command.TYPE_ENGINE_RESUME:
                return encodeContent("*KW,000,007,000000,1#");
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
