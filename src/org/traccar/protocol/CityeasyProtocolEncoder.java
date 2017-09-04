/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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

import java.util.TimeZone;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.Checksum;
import org.traccar.helper.Log;
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
        buf.writeShort(Checksum.crc16(Checksum.CRC16_KERMIT, buf.toByteBuffer()));
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
                content.writeShort(command.getInteger(Command.KEY_FREQUENCY));
                return encodeContent(CityeasyProtocolDecoder.MSG_LOCATION_INTERVAL, content);
            case Command.TYPE_POSITION_STOP:
                content.writeShort(0);
                return encodeContent(CityeasyProtocolDecoder.MSG_LOCATION_INTERVAL, content);
            case Command.TYPE_SET_TIMEZONE:
                int timezone = TimeZone.getTimeZone(command.getString(Command.KEY_TIMEZONE)).getRawOffset() / 60000;
                if (timezone < 0) {
                    content.writeByte(1);
                } else {
                    content.writeByte(0);
                }
                content.writeShort(Math.abs(timezone));
                return encodeContent(CityeasyProtocolDecoder.MSG_TIMEZONE, content);
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
