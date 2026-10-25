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
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class RadshidProtocolDecoder extends BaseProtocolDecoder {

    // Radshid packet CRC: initial value 0, high-byte input, left shifts, and
    // the protocol polynomial 0xA001.
    private static final int CRC_POLYNOMIAL = 0xA001;

    // Fixed event prefix, including the extra-length byte.
    private static final int EVENT_PREFIX_LENGTH = 44;

    // Packed debug-extra sizes. Versions 11 and 13 share one layout.
    private static final int EXTRA_V7_TEMPERATURE = 27;
    private static final int EXTRA_V8_STATIC_LOCATION = 36;
    private static final int EXTRA_V10_CCID = 37;
    private static final int EXTRA_V11_CELL = 45;
    private static final int CELL_INFO_LENGTH = 28;

    public RadshidProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    static int crc16(ByteBuf buf, int index, int length) {
        int crc = 0;
        for (int n = 0; n < length; n++) {
            crc ^= buf.getUnsignedByte(index + n) << 8;
            for (int i = 0; i < 8; i++) {
                crc <<= 1;
                if ((crc & 0x10000) != 0) {
                    crc = (crc ^ CRC_POLYNOMIAL) & 0xFFFF;
                }
            }
        }
        return crc & 0xFFFF;
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, int packetTag, int requestLength) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeIntLE(0x07);
            response.writeByte(0x82);
            response.writeByte(packetTag);
            // The acknowledgement carries the request payload length in big-endian order.
            // Its own length prefix remains little-endian.
            response.writeInt(requestLength);
            response.writeByte(0x01);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    // Event codes observed in the Radshid packet format. Non-alarm events return null.
    private String decodeAlarm(int event) {
        return switch (event) {
            case 0xD6, 0xD7 -> Position.ALARM_JAMMING;
            case 0xDB -> Position.ALARM_REMOVING;
            case 0xDC -> Position.ALARM_GEOFENCE_ENTER;
            case 0xDD -> Position.ALARM_GEOFENCE_EXIT;
            case 0xE2 -> Position.ALARM_POWER_CUT;
            case 0xE3 -> Position.ALARM_POWER_RESTORED;
            case 0xE5 -> Position.ALARM_ACCELERATION;
            case 0xE6 -> Position.ALARM_CORNERING;
            case 0xEB -> Position.ALARM_SOS;
            case 0xEE -> Position.ALARM_OVERSPEED;
            case 0xF0 -> Position.ALARM_LOW_BATTERY;
            default -> null;
        };
    }

    private void decodeGpsStatus(Position position, int gpsStatus) {
        // 0x00 and 0x02 are GPS fixes; 0x03 is a valid approximate location.
        position.set(Position.KEY_GPS, gpsStatus);
        switch (gpsStatus) {
            case 0x00, 0x02 -> position.setValid(true);
            case 0x03 -> {
                position.setValid(true);
                position.set(Position.KEY_APPROXIMATE, true);
            }
            default -> position.setValid(false);
        }
    }

    private static boolean fits(int extraLength, int offset, int size) {
        return extraLength >= offset + size;
    }

    private void decodeExtra(ByteBuf buf, Position position, int extraStart, int extraLength) {
        if (extraLength < 1) {
            return;
        }
        int version = buf.getUnsignedByte(extraStart);
        // The first extra byte is the debug-extra version, not a type code.
        // Unsupported versions, including future layouts, are skipped.
        if (version == 7 || version == 8 || version == 10 || version == 11 || version == 13) {
            decodeDebugExtra(buf, position, extraStart, extraLength, version);
        }
    }

    private void decodeDebugExtra(ByteBuf buf, Position position, int start, int length, int version) {
        if (fits(length, 1, 2)) {
            position.set(Position.KEY_VERSION_FW, buf.getUnsignedShortLE(start + 1));
        }
        if (fits(length, 3, 4)) {
            position.set(Position.KEY_INDEX, buf.getUnsignedIntLE(start + 3));
        }
        if (fits(length, 7, 4)) {
            position.set("uptime", buf.getUnsignedIntLE(start + 7));
        }
        if (fits(length, 11, 2)) {
            position.set("resetCount", buf.getUnsignedShortLE(start + 11));
        }
        if (fits(length, 13, 2)) {
            position.set("storedPoints", buf.getUnsignedShortLE(start + 13));
        }
        if (fits(length, 15, 2)) {
            position.set("simCharge", (int) buf.getShortLE(start + 15));
        }
        if (fits(length, 17, 2)) {
            position.set("pauseTime", buf.getUnsignedShortLE(start + 17));
        }
        if (fits(length, 19, 2)) {
            position.set("offTime", buf.getUnsignedShortLE(start + 19));
        }
        if (fits(length, 21, 1)) {
            position.set(Position.KEY_RSSI, (int) buf.getUnsignedByte(start + 21));
        }
        if (fits(length, 22, 1)) {
            int deviceStatus = buf.getUnsignedByte(start + 22);
            position.set("deviceStatus", deviceStatus);
            if (BitUtil.check(deviceStatus, 6)) {
                position.set(Position.KEY_ARCHIVE, true);
            }
        }
        if (fits(length, 23, 1)) {
            position.set(Position.KEY_POWER, buf.getUnsignedByte(start + 23) / 10.0);
        }
        if (fits(length, 24, 1)) {
            position.set(Position.KEY_BATTERY_LEVEL, (int) buf.getUnsignedByte(start + 24));
        }
        if (fits(length, 25, 1)) {
            position.set("savedPicture", buf.getUnsignedByte(start + 25) != 0);
        }
        if (fits(length, 26, 1)) {
            position.set("resetSource", (int) buf.getUnsignedByte(start + 26));
        }
        if (fits(length, EXTRA_V7_TEMPERATURE, 4)) {
            position.set(Position.PREFIX_TEMP + 1, (double) buf.getFloatLE(start + EXTRA_V7_TEMPERATURE));
        }
        if (fits(length, 31, 4)) {
            position.set(Position.KEY_HUMIDITY, (double) buf.getFloatLE(start + 31));
        }
        if (fits(length, 35, 1)) {
            position.set("crc8", (int) buf.getUnsignedByte(start + 35));
        }
        if (version >= 8 && fits(length, EXTRA_V8_STATIC_LOCATION, 1)) {
            position.set("staticLocation", buf.getUnsignedByte(start + EXTRA_V8_STATIC_LOCATION) != 0);
        }
        if (version >= 10 && fits(length, EXTRA_V10_CCID, 8)) {
            position.set(Position.KEY_ICCID, Long.toUnsignedString(buf.getLongLE(start + EXTRA_V10_CCID)));
        }
        if (version >= 11 && fits(length, EXTRA_V11_CELL, CELL_INFO_LENGTH)) {
            decodeCell(buf, position, start + EXTRA_V11_CELL);
        }
    }

    private void decodeCell(ByteBuf buf, Position position, int offset) {
        int mcc = buf.getIntLE(offset);
        int mnc = buf.getIntLE(offset + 4);
        long lac = buf.getUnsignedIntLE(offset + 8);
        long cid = buf.getUnsignedIntLE(offset + 12);
        position.set("bcch", buf.getIntLE(offset + 16));
        position.set("bsic", buf.getIntLE(offset + 20));
        int dbm = buf.getIntLE(offset + 24);
        if (lac <= Integer.MAX_VALUE) {
            Network network = new Network();
            network.addCellTower(CellTower.from(mcc, mnc, (int) lac, cid, dbm));
            position.setNetwork(network);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        long rawLength = buf.readUnsignedIntLE();
        // Length covers the bytes after the prefix, including the CRC trailer.
        if (rawLength < 8 || rawLength > buf.readableBytes()) {
            return null;
        }
        int payloadLength = (int) rawLength;
        int payloadStart = buf.readerIndex();
        int eventLimit = payloadStart + payloadLength - 2;

        // CRC covers the payload excluding the length prefix and the two CRC bytes.
        int actualCrc = buf.getUnsignedShortLE(eventLimit);
        if (crc16(buf, payloadStart, payloadLength - 2) != actualCrc) {
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
        while (positions.size() < eventCount && eventLimit - buf.readerIndex() >= EVENT_PREFIX_LENGTH) {
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

            // This field is the geofence id in the Radshid event layout.
            position.set("geofenceId", buf.readUnsignedShort());
            // Distance is measured in meters; the high bit is a protocol flag.
            position.set(Position.KEY_DISTANCE, buf.readUnsignedInt() & 0x7FFFFFFFL);
            buf.readUnsignedInt(); // Reserved/traveled-distance field

            // IO status: bit 7 external power, bit 6 ignition, bit 5 SOS.
            int ioStatus = buf.readUnsignedByte();
            position.set("externalPower", BitUtil.check(ioStatus, 7));
            position.set(Position.KEY_IGNITION, BitUtil.check(ioStatus, 6));
            position.set("sos", BitUtil.check(ioStatus, 5));

            decodeGpsStatus(position, buf.readUnsignedByte());
            position.setLatitude(buf.readInt() / 10000000.0);
            position.setLongitude(buf.readInt() / 10000000.0);
            position.setAltitude(buf.readShort());
            position.setCourse(buf.readUnsignedShort());
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            position.set(Position.KEY_PDOP, buf.readUnsignedByte() / 10.0);

            int extraLength = buf.readUnsignedByte();
            if (extraLength > eventLimit - buf.readerIndex()) {
                break;
            }
            decodeExtra(buf, position, buf.readerIndex(), extraLength);
            buf.skipBytes(extraLength);

            positions.add(position);
        }

        return positions.isEmpty() ? null : positions;
    }

}
