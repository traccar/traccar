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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class RadshidProtocolDecoder extends BaseProtocolDecoder {

    public RadshidProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, int packetTag, int length) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeIntLE(0x07);
            response.writeByte(0x82);
            response.writeByte(packetTag);
            response.writeIntLE(length);
            response.writeByte(0x01);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private String decodeAlarm(int event) {
        return switch (event) {
            case 0xE2 -> Position.ALARM_POWER_CUT;
            case 0xE3 -> Position.ALARM_POWER_RESTORED;
            case 0xE4, 0xF1, 0xF2 -> Position.ALARM_GPS_ANTENNA_CUT;
            case 0xE5 -> Position.ALARM_VIBRATION;
            case 0xE7, 0xF7 -> Position.ALARM_FATIGUE_DRIVING;
            case 0xEE, 0xEF -> Position.ALARM_OVERSPEED;
            case 0xF0 -> Position.ALARM_LOW_BATTERY;
            case 0xF9 -> Position.ALARM_JAMMING;
            default -> null;
        };
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        int payloadLength = (int) buf.readUnsignedIntLE();

        int packetTag = buf.readUnsignedByte();
        long serialNumber = buf.readUnsignedInt();
        int eventCount = buf.readUnsignedByte();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(serialNumber));
        if (deviceSession == null) {
            return null;
        }

        sendResponse(channel, remoteAddress, packetTag, payloadLength);

        List<Position> positions = new LinkedList<>();
        for (int i = 0; i < eventCount; i++) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            buf.readUnsignedByte(); // event version

            int eventCode = buf.readUnsignedByte();
            position.set(Position.KEY_EVENT, eventCode);
            position.addAlarm(decodeAlarm(eventCode));

            long driverId = buf.readUnsignedInt() << 8 | buf.readUnsignedByte();
            if (driverId != 0) {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, String.valueOf(driverId));
            }

            position.set(Position.KEY_HOURS, buf.readUnsignedShort() * 60_000L);
            position.setTime(new Date(buf.readUnsignedInt() * 1000L));
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
            position.set(Position.KEY_OBD_SPEED, buf.readUnsignedByte());
            buf.readUnsignedByte(); // max gps speed
            buf.readUnsignedByte(); // max sensor speed
            position.set(Position.KEY_RPM, buf.readUnsignedShort());
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
            buf.readUnsignedInt(); // sensor odometer

            int ioStatus = buf.readUnsignedByte();
            position.set(Position.KEY_CHARGE, BitUtil.check(ioStatus, 7));
            position.set(Position.KEY_IGNITION, BitUtil.check(ioStatus, 6));
            position.set(Position.KEY_INPUT, BitUtil.to(ioStatus, 4));

            position.setValid(buf.readUnsignedByte() != 0x01);
            position.setLatitude(buf.readInt() / 10000000.0);
            position.setLongitude(buf.readInt() / 10000000.0);
            position.setAltitude(buf.readShort());
            position.setCourse(buf.readUnsignedShort());
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            position.set(Position.KEY_PDOP, buf.readUnsignedByte() / 10.0);

            int extraLength = buf.readUnsignedByte();
            int extraEnd = buf.readerIndex() + extraLength;
            if (extraLength > 0 && buf.readUnsignedByte() == 0x07) {
                position.set(Position.KEY_VERSION_FW, buf.readUnsignedShortLE());
                buf.readUnsignedIntLE(); // point counter
                buf.readUnsignedIntLE(); // system live time
                buf.readUnsignedShortLE(); // reset number
                buf.readUnsignedShortLE(); // flash stored points
                buf.readUnsignedShortLE(); // sim charge
                buf.readUnsignedShortLE(); // pause time
                buf.readUnsignedShortLE(); // off time
                position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                buf.readUnsignedByte(); // device status
                position.set(Position.KEY_POWER, buf.readUnsignedByte() / 10.0);
                position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                buf.readUnsignedByte(); // saved pictures
                buf.readUnsignedByte(); // reset source
                position.set(Position.PREFIX_TEMP + 1, buf.readIntLE() / 10.0);
                position.set(Position.KEY_HUMIDITY, buf.readIntLE() / 10.0);
            }
            buf.readerIndex(extraEnd);

            positions.add(position);
        }

        return positions.isEmpty() ? null : positions;
    }

}
