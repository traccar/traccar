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
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class RadshidProtocolDecoder extends BaseProtocolDecoder {

    private static final Checksum.Algorithm CRC16 = new Checksum.Algorithm(16, 0xA001, 0x0000, false, false, 0x0000);

    public RadshidProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, int packetTag, int length) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeIntLE(0x07);
            response.writeByte(0x82);
            response.writeByte(packetTag);
            response.writeInt(length);
            response.writeByte(0x01);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private String decodeAlarm(int event) {
        return switch (event) {
            case 0xD7, 0xF9 -> Position.ALARM_JAMMING;
            case 0xE2 -> Position.ALARM_POWER_CUT;
            case 0xE3 -> Position.ALARM_POWER_RESTORED;
            case 0xE4, 0xF2 -> Position.ALARM_GPS_ANTENNA_CUT;
            case 0xE5 -> Position.ALARM_VIBRATION;
            case 0xE7 -> Position.ALARM_FATIGUE_DRIVING;
            case 0xEB -> Position.ALARM_SOS;
            case 0xEE, 0xEF -> Position.ALARM_OVERSPEED;
            case 0xF0 -> Position.ALARM_LOW_BATTERY;
            default -> null;
        };
    }

    private boolean decodeExtra(Position position, ByteBuf buf, int length) {

        int version = buf.readUnsignedByte();
        boolean supported = switch (version) {
            case 2 -> length == 29;
            case 7 -> length == 36 || length == 37;
            case 8 -> length == 37;
            case 10 -> length == 45;
            case 11, 13 -> length == 73;
            default -> false;
        };
        if (!supported) {
            return false;
        }

        position.set(Position.KEY_VERSION_FW, buf.readUnsignedShortLE());
        position.set(Position.KEY_INDEX, buf.readUnsignedIntLE());
        position.set("uptime", buf.readUnsignedIntLE());
        position.set("bootCount", buf.readUnsignedShortLE());
        position.set("flashBacklog", buf.readUnsignedShortLE());
        position.set("simCredit", (int) buf.readShortLE());
        buf.readUnsignedShortLE(); // reserved
        position.set("accOffDuration", buf.readUnsignedShortLE());
        position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        if (BitUtil.check(buf.readUnsignedByte(), 6)) {
            position.set(Position.KEY_ARCHIVE, true);
        }
        if (version == 2) {
            buf.readUnsignedByte(); // internal temperature
        }
        position.set(Position.KEY_POWER, buf.readUnsignedByte() / 10.0);
        position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
        buf.readUnsignedByte(); // reserved
        position.set("startupType", buf.readUnsignedByte());

        if (version == 2) {
            return false;
        }

        float temperature = buf.readFloatLE();
        if (temperature > -55 && temperature < 125) {
            position.set(Position.PREFIX_TEMP + 1, (double) temperature);
        }
        float humidity = buf.readFloatLE();
        if (humidity >= 0 && humidity <= 100) {
            position.set(Position.KEY_HUMIDITY, (double) humidity);
        }
        buf.readUnsignedByte(); // reserved

        boolean lastKnownPosition = false;
        if (length >= 37) {
            lastKnownPosition = buf.readUnsignedByte() != 0;
            position.set("lastKnownPosition", lastKnownPosition);
        }
        if (length >= 45) {
            position.set("partialIccid", Long.toUnsignedString(buf.readLongLE()));
        }
        if (length >= 73) {
            int mcc = buf.readIntLE();
            int mnc = buf.readIntLE();
            long lac = buf.readUnsignedIntLE();
            long cid = buf.readUnsignedIntLE();
            buf.readIntLE(); // bcch
            buf.readIntLE(); // bsic
            int rssi = buf.readIntLE();
            if (mcc > 0 && lac > 0 && lac <= 0xFFFF) {
                position.setNetwork(new Network(CellTower.from(mcc, mnc, (int) lac, cid, rssi)));
            }
        }

        return lastKnownPosition;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        int payloadLength = (int) buf.readUnsignedIntLE();

        int checksum = Checksum.crc16(CRC16, buf.nioBuffer(buf.readerIndex(), payloadLength - 2));
        if (checksum != buf.getUnsignedShortLE(buf.readerIndex() + payloadLength - 2)) {
            return null;
        }

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
            Date time = new Date(buf.readUnsignedInt() * 1000L);
            int speed = buf.readUnsignedByte();
            position.set(Position.KEY_OBD_SPEED, buf.readUnsignedByte());
            buf.readUnsignedByte(); // max gps speed
            buf.readUnsignedByte(); // max sensor speed
            position.set(Position.KEY_RPM, buf.readUnsignedShort());

            long distance = buf.readUnsignedInt();
            if (BitUtil.check(distance, 31)) {
                position.set(Position.KEY_DISTANCE, BitUtil.to(distance, 31));
            } else {
                position.set(Position.KEY_ODOMETER, distance);
            }
            buf.readUnsignedInt(); // sensor odometer

            int ioStatus = buf.readUnsignedByte();
            position.set(Position.KEY_CHARGE, BitUtil.check(ioStatus, 7));
            position.set(Position.KEY_IGNITION, BitUtil.check(ioStatus, 6));
            position.set(Position.KEY_INPUT, BitUtil.to(ioStatus, 4));

            int gpsStatus = buf.readUnsignedByte();
            double latitude = buf.readInt() / 10000000.0;
            double longitude = buf.readInt() / 10000000.0;
            int altitude = buf.readShort();
            int course = buf.readUnsignedShort();
            int satellites = buf.readUnsignedByte();
            double pdop = buf.readUnsignedByte() / 10.0;

            int extraLength = buf.readUnsignedByte();
            int extraEnd = buf.readerIndex() + extraLength;
            boolean lastKnownPosition = extraLength > 0 && decodeExtra(position, buf, extraLength);
            buf.readerIndex(extraEnd);

            if (gpsStatus == 0x00 || gpsStatus == 0x02) {
                position.setValid(!lastKnownPosition);
                position.setTime(time);
                position.setLatitude(latitude);
                position.setLongitude(longitude);
                if (gpsStatus == 0x00 && !lastKnownPosition) {
                    position.setSpeed(UnitsConverter.knotsFromKph(speed));
                    position.setCourse(course);
                    position.setAltitude(altitude);
                    position.set(Position.KEY_SATELLITES, satellites);
                    position.set(Position.KEY_PDOP, pdop);
                }
            } else {
                getLastLocation(position, time);
            }

            positions.add(position);
        }

        return positions.isEmpty() ? null : positions;
    }

}
