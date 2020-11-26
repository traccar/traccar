/*
 * Copyright 2015 - 2020 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolEncoder;
import org.traccar.Context;
import org.traccar.helper.Checksum;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;
import org.traccar.Protocol;

import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

public class MeiligaoProtocolEncoder extends BaseProtocolEncoder {

    public MeiligaoProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeContent(long deviceId, int type, ByteBuf content) {

        ByteBuf buf = Unpooled.buffer();

        buf.writeByte('@');
        buf.writeByte('@');

        buf.writeShort(2 + 2 + 7 + 2 + content.readableBytes() + 2 + 2); // message length

        buf.writeBytes(DataConverter.parseHex((getUniqueId(deviceId) + "FFFFFFFFFFFFFF").substring(0, 14)));

        buf.writeShort(type);

        buf.writeBytes(content);

        buf.writeShort(Checksum.crc16(Checksum.CRC16_CCITT_FALSE, buf.nioBuffer()));

        buf.writeByte('\r');
        buf.writeByte('\n');

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        boolean alternative = Context.getIdentityManager().lookupAttributeBoolean(
                command.getDeviceId(), getProtocolName() + ".alternative", false, false, true);

        int outputControlMessageType = alternative
                ? MeiligaoProtocolDecoder.MSG_OUTPUT_CONTROL_1
                : MeiligaoProtocolDecoder.MSG_OUTPUT_CONTROL_2;

        ByteBuf content = Unpooled.buffer();

        switch (command.getType()) {
            case Command.TYPE_POSITION_SINGLE:
                return encodeContent(command.getDeviceId(), MeiligaoProtocolDecoder.MSG_TRACK_ON_DEMAND, content);
            case Command.TYPE_POSITION_PERIODIC:
                content.writeShort(command.getInteger(Command.KEY_FREQUENCY) / 10);
                return encodeContent(command.getDeviceId(), MeiligaoProtocolDecoder.MSG_TRACK_BY_INTERVAL, content);
            case Command.TYPE_ENGINE_STOP:
                content.writeByte(0x01);
                return encodeContent(command.getDeviceId(), outputControlMessageType, content);
            case Command.TYPE_ENGINE_RESUME:
                content.writeByte(0x00);
                return encodeContent(command.getDeviceId(), outputControlMessageType, content);
            case Command.TYPE_ALARM_GEOFENCE:
                content.writeShort(command.getInteger(Command.KEY_RADIUS));
                return encodeContent(command.getDeviceId(), MeiligaoProtocolDecoder.MSG_MOVEMENT_ALARM, content);
            case Command.TYPE_SET_TIMEZONE:
                int offset = TimeZone.getTimeZone(command.getString(Command.KEY_TIMEZONE)).getRawOffset() / 60000;
                content.writeBytes(String.valueOf(offset).getBytes(StandardCharsets.US_ASCII));
                return encodeContent(command.getDeviceId(), MeiligaoProtocolDecoder.MSG_TIME_ZONE, content);
            case Command.TYPE_REQUEST_PHOTO:
                return encodeContent(command.getDeviceId(), MeiligaoProtocolDecoder.MSG_TAKE_PHOTO, content);
            case Command.TYPE_REBOOT_DEVICE:
                return encodeContent(command.getDeviceId(), MeiligaoProtocolDecoder.MSG_REBOOT_GPS, content);
            default:
                return null;
        }
    }

}
