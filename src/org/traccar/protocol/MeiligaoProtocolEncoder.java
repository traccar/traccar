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
import org.traccar.helper.Log;
import org.traccar.model.Command;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;
import java.util.Map;

public class MeiligaoProtocolEncoder extends BaseProtocolEncoder {

    public static final int MSG_TRACK_ON_DEMAND = 0x4101;
    public static final int MSG_TRACK_BY_INTERVAL = 0x4102;
    public static final int MSG_MOVEMENT_ALARM = 0x4106;
    public static final int MSG_TIME_ZONE = 0x4132;
    public static final int MSG_REBOOT_GPS = 0x4902;

    private ChannelBuffer encodeContent(long deviceId, int type, ChannelBuffer content) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        buf.writeByte('@');
        buf.writeByte('@');

        buf.writeShort(2 + 2 + 7 + 2 + content.readableBytes() + 2 + 2); // message length

        buf.writeBytes(DatatypeConverter.parseHexBinary((getUniqueId(deviceId) + "FFFFFFFFFFFFFF").substring(0, 14)));

        buf.writeShort(type);

        buf.writeBytes(content);

        buf.writeShort(Checksum.crc16(Checksum.CRC16_CCITT_FALSE, buf.toByteBuffer()));

        buf.writeByte('\r');
        buf.writeByte('\n');

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        ChannelBuffer content = ChannelBuffers.dynamicBuffer();
        Map<String, Object> attributes = command.getAttributes();

        switch (command.getType()) {
            case Command.TYPE_POSITION_SINGLE:
                return encodeContent(command.getDeviceId(), MSG_TRACK_ON_DEMAND, content);
            case Command.TYPE_POSITION_PERIODIC:
                content.writeShort(((Number) attributes.get(Command.KEY_FREQUENCY)).intValue() / 10);
                return encodeContent(command.getDeviceId(), MSG_TRACK_BY_INTERVAL, content);
            case Command.TYPE_MOVEMENT_ALARM:
                content.writeShort(((Number) attributes.get(Command.KEY_RADIUS)).intValue());
                return encodeContent(command.getDeviceId(), MSG_MOVEMENT_ALARM, content);
            case Command.TYPE_SET_TIMEZONE:
                int offset = ((Number) attributes.get(Command.KEY_TIMEZONE)).intValue() / 60;
                content.writeBytes(String.valueOf(offset).getBytes(Charset.defaultCharset()));
                return encodeContent(command.getDeviceId(), MSG_TIME_ZONE, content);
            case Command.TYPE_REBOOT_DEVICE:
                return encodeContent(command.getDeviceId(), MSG_REBOOT_GPS, content);
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
