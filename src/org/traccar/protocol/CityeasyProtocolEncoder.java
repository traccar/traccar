/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.helper.Checksum;
import org.traccar.model.Command;

public class CityeasyProtocolEncoder extends BaseProtocolEncoder {

    private ChannelBuffer encodeContent(int type, ChannelBuffer content) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        buf.writeByte('S');
        buf.writeByte('S');
        buf.writeShort(2 + 2 + 2 + content.readableBytes() + 4 + 2 + 2);
        buf.writeShort(type);
        buf.writeBytes(content);
        buf.writeInt(0x0B);
        buf.writeShort(Checksum.crc16Ccitt(buf.toByteBuffer(), 0, 0));
        buf.writeByte('\r');
        buf.writeByte('\n');

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        ChannelBuffer content = ChannelBuffers.dynamicBuffer();

        switch (command.getType()) {
            case Command.TYPE_POSITION_SINGLE:
                return encodeContent(CityeasyProtocolDecoder.MSG_LOCATION_REQUEST, content);
            case Command.TYPE_POSITION_PERIODIC:
            case Command.TYPE_POSITION_STOP:
                content.writeShort(((Number) command.getAttributes().getOrDefault(Command.KEY_FREQUENCY, 0)).intValue());
                return encodeContent(CityeasyProtocolDecoder.MSG_LOCATION_INTERVAL, content);
            case Command.TYPE_SET_TIMEZONE:
                int timezone = ((Number) command.getAttributes().getOrDefault(Command.KEY_TIMEZONE, 0)).intValue();
                content.writeByte(timezone < 0 ? 1 : 0);
                content.writeShort(Math.abs(timezone) / 60);
                return encodeContent(CityeasyProtocolDecoder.MSG_TIMEZONE, content);
        }

        return null;
    }

}
