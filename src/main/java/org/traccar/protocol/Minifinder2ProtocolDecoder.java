/*
 * Copyright 2019 - 2024 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BufferUtil;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Minifinder2ProtocolDecoder extends BaseProtocolDecoder {

    public Minifinder2ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_DATA = 0x01;
    public static final int MSG_CONFIGURATION = 0x02;
    public static final int MSG_SERVICES = 0x03;
    public static final int MSG_SYSTEM_CONTROL = 0x04;
    public static final int MSG_FIRMWARE = 0x7E;
    public static final int MSG_RESPONSE = 0x7F;

    private String decodeAlarm(long code) {
        if (BitUtil.check(code, 0)) {
            return Position.ALARM_LOW_BATTERY;
        }
        if (BitUtil.check(code, 1)) {
            return Position.ALARM_OVERSPEED;
        }
        if (BitUtil.check(code, 2)) {
            return Position.ALARM_FALL_DOWN;
        }
        if (BitUtil.check(code, 8)) {
            return Position.ALARM_POWER_OFF;
        }
        if (BitUtil.check(code, 9)) {
            return Position.ALARM_POWER_ON;
        }
        if (BitUtil.check(code, 12)) {
            return Position.ALARM_SOS;
        }
        return null;
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, int index, int type, ByteBuf buf) {

        if (channel != null) {

            ByteBuf body = Unpooled.buffer();
            if (type == MSG_SERVICES) {
                while (buf.isReadable()) {
                    int endIndex = buf.readUnsignedByte() + buf.readerIndex();
                    int key = buf.readUnsignedByte();
                    switch (key) {
                        case 0x11:
                        case 0x21:
                        case 0x22:
                            body.writeByte(9 + 1); // length
                            body.writeByte(key);
                            body.writeIntLE(0); // latitude
                            body.writeIntLE(0); // longitude
                            body.writeByte(0); // address
                            break;
                        case 0x12:
                            body.writeByte(5); // length
                            body.writeByte(key);
                            body.writeIntLE((int) (System.currentTimeMillis() / 1000));
                            break;
                        default:
                            break;
                    }
                    buf.readerIndex(endIndex);
                }
            } else {
                body.writeByte(1); // key length
                body.writeByte(0); // success
            }

            ByteBuf content = Unpooled.buffer();
            content.writeByte(type == MSG_SERVICES ? type : MSG_RESPONSE);
            content.writeBytes(body);
            body.release();

            ByteBuf response = Unpooled.buffer();
            response.writeByte(0xAB); // header
            response.writeByte(0x00); // properties
            response.writeShortLE(content.readableBytes());
            response.writeShortLE(Checksum.crc16(Checksum.CRC16_XMODEM, content.nioBuffer()));
            response.writeShortLE(index);
            response.writeBytes(content);
            content.release();

            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private String readTagId(ByteBuf buf) {
        StringBuilder tagId = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            tagId.insert(0, ByteBufUtil.hexDump(buf.readSlice(1)));
        }
        return tagId.toString();
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // header
        int flags = buf.readUnsignedByte();
        buf.readUnsignedShortLE(); // length
        buf.readUnsignedShortLE(); // checksum
        int index = buf.readUnsignedShortLE();
        int type = buf.readUnsignedByte();

        if (BitUtil.check(flags, 4)) {
            sendResponse(channel, remoteAddress, index, type, buf.slice());
        }

        if (type == MSG_DATA || type == MSG_SERVICES) {

            List<Position> positions = new LinkedList<>();
            Set<Integer> keys = new HashSet<>();
            Position position = new Position(getProtocolName());

            DeviceSession deviceSession = null;

            while (buf.isReadable()) {
                int endIndex = buf.readUnsignedByte() + buf.readerIndex();
                int key = buf.readUnsignedByte();

                if (keys.contains(key)) {
                    positions.add(position);
                    keys.clear();
                    position = new Position(getProtocolName());
                }
                keys.add(key);

                switch (key) {
                    case 0x01:
                        deviceSession = getDeviceSession(
                                channel, remoteAddress, buf.readCharSequence(15, StandardCharsets.US_ASCII).toString());
                        if (deviceSession == null) {
                            return null;
                        }
                        break;
                    case 0x02:
                        long alarm = buf.readUnsignedIntLE();
                        position.set(Position.KEY_ALARM, decodeAlarm(alarm));
                        if (BitUtil.check(alarm, 31)) {
                            position.set("bark", true);
                        }
                        break;
                    case 0x14:
                        position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                        position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.001);
                        break;
                    case 0x20:
                        position.setLatitude(buf.readIntLE() * 0.0000001);
                        position.setLongitude(buf.readIntLE() * 0.0000001);
                        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE()));
                        position.setCourse(buf.readUnsignedShortLE());
                        position.setAltitude(buf.readShortLE());
                        int hdop = buf.readUnsignedShortLE();
                        position.setValid(hdop > 0);
                        position.set(Position.KEY_HDOP, hdop * 0.1);
                        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
                        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                        break;
                    case 0x21:
                        int mcc = buf.readUnsignedShortLE();
                        int mnc = buf.readUnsignedByte();
                        if (position.getNetwork() == null) {
                            position.setNetwork(new Network());
                        }
                        while (buf.readerIndex() < endIndex) {
                            int rssi = buf.readByte();
                            position.getNetwork().addCellTower(CellTower.from(
                                    mcc, mnc, buf.readUnsignedShortLE(), buf.readUnsignedShortLE(), rssi));
                        }
                        break;
                    case 0x22:
                        if (position.getNetwork() == null) {
                            position.setNetwork(new Network());
                        }
                        while (buf.readerIndex() < endIndex) {
                            int rssi = buf.readByte();
                            String mac = ByteBufUtil.hexDump(buf.readSlice(6)).replaceAll("(..)", "$1:");
                            position.getNetwork().addWifiAccessPoint(WifiAccessPoint.from(
                                    mac.substring(0, mac.length() - 1), rssi));
                        }
                        break;
                    case 0x23:
                        position.set("tagId", readTagId(buf));
                        position.setLatitude(buf.readIntLE() * 0.0000001);
                        position.setLongitude(buf.readIntLE() * 0.0000001);
                        position.setValid(true);
                        break;
                    case 0x24:
                        position.setTime(new Date(buf.readUnsignedIntLE() * 1000));
                        long status = buf.readUnsignedIntLE();
                        if (BitUtil.check(status, 4)) {
                            position.set(Position.KEY_CHARGE, true);
                        }
                        if (BitUtil.check(status, 7)) {
                            position.set(Position.KEY_ARCHIVE, true);
                        }
                        position.set(Position.KEY_MOTION, BitUtil.check(status, 9));
                        position.set(Position.KEY_RSSI, BitUtil.between(status, 19, 24));
                        position.set(Position.KEY_BATTERY_LEVEL, BitUtil.from(status, 24));
                        position.set(Position.KEY_STATUS, status);
                        break;
                    case 0x28:
                        int beaconFlags = buf.readUnsignedByte();
                        position.set("tagId", readTagId(buf));
                        position.set("tagRssi", (int) buf.readByte());
                        position.set("tag1mRssi", (int) buf.readByte());
                        if (BitUtil.check(beaconFlags, 7)) {
                            position.setLatitude(buf.readIntLE() * 0.0000001);
                            position.setLongitude(buf.readIntLE() * 0.0000001);
                            position.setValid(true);
                        }
                        if (BitUtil.check(beaconFlags, 6)) {
                            position.set("description", buf.readCharSequence(
                                    endIndex - buf.readerIndex(), StandardCharsets.US_ASCII).toString());
                        }
                        break;
                    case 0x2A:
                        buf.readUnsignedByte(); // flags
                        buf.skipBytes(6); // mac
                        buf.readUnsignedByte(); // rssi
                        position.setLatitude(buf.readIntLE() * 0.0000001);
                        position.setLongitude(buf.readIntLE() * 0.0000001);
                        position.setValid(true);
                        break;
                    case 0x30:
                        buf.readUnsignedIntLE(); // timestamp
                        position.set(Position.KEY_STEPS, buf.readUnsignedIntLE());
                        break;
                    case 0x31:
                        int i = 1;
                        while (buf.readerIndex() < endIndex) {
                            position.set("activity" + i + "Time", buf.readUnsignedIntLE());
                            position.set("activity" + i, buf.readUnsignedIntLE());
                            i += 1;
                        }
                        break;
                    case 0x37:
                        buf.readUnsignedIntLE(); // timestamp
                        long barking = buf.readUnsignedIntLE();
                        if (BitUtil.check(barking, 31)) {
                            position.set("barkStop", true);
                        }
                        position.set("barkCount", BitUtil.to(barking, 31));
                        break;
                    case 0x40:
                        buf.readUnsignedIntLE(); // timestamp
                        int heartRate = buf.readUnsignedByte();
                        if (heartRate > 1) {
                            position.set(Position.KEY_HEART_RATE, heartRate);
                        }
                        break;
                    case 0x41:
                        buf.readUnsignedIntLE(); // timestamp
                        int spO2 = buf.readUnsignedByte();
                        if (spO2 > 1) {
                            position.set("spO2", spO2);
                        }
                        break;
                    default:
                        break;
                }
                buf.readerIndex(endIndex);
            }

            positions.add(position);

            if (deviceSession != null) {
                for (Position p : positions) {
                    p.setDeviceId(deviceSession.getDeviceId());
                    if (!p.getValid() && !p.hasAttribute(Position.KEY_HDOP)) {
                        getLastLocation(p, null);
                    }
                }
            } else {
                return null;
            }

            return positions;

        } else if (type == MSG_CONFIGURATION) {

            return decodeConfiguration(channel, remoteAddress, buf);

        } else if (type == MSG_RESPONSE) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            buf.readUnsignedByte(); // length
            position.set(Position.KEY_RESULT, String.valueOf(buf.readUnsignedByte()));

            return position;

        }

        return null;
    }

    private Position decodeConfiguration(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        while (buf.isReadable()) {
            int length = buf.readUnsignedByte() - 1;
            int endIndex = buf.readerIndex() + length + 1;
            int key = buf.readUnsignedByte();

            switch (key) {
                case 0x01:
                    position.set("moduleNumber", buf.readUnsignedInt());
                    break;
                case 0x02:
                    position.set(Position.KEY_VERSION_FW, String.valueOf(buf.readUnsignedInt()));
                    break;
                case 0x03:
                    position.set("imei", buf.readCharSequence(length, StandardCharsets.US_ASCII).toString());
                    break;
                case 0x04:
                    position.set(Position.KEY_ICCID, BufferUtil.readString(buf, length));
                    break;
                case 0x05:
                    position.set("bleMac", ByteBufUtil.hexDump(buf.readSlice(length)));
                    break;
                case 0x06:
                    position.set("settingTime", buf.readUnsignedInt());
                    break;
                case 0x07:
                    position.set("runTimes", buf.readUnsignedInt());
                    break;
                case 0x0A:
                    position.set("interval", buf.readUnsignedMedium());
                    position.set("petMode", buf.readUnsignedByte());
                    break;
                case 0x0D:
                    position.set("passwordProtect", buf.readUnsignedInt());
                    break;
                case 0x0E:
                    position.set("timeZone", (int) buf.readByte());
                    break;
                case 0x0F:
                    position.set("enableControl", buf.readUnsignedInt());
                    break;
                case 0x13:
                    position.set("deviceName", BufferUtil.readString(buf, length));
                    break;
                case 0x14:
                    position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);
                    break;
                case 0x15:
                    position.set("bleLatitude", buf.readIntLE() * 0.0000001);
                    position.set("bleLongitude", buf.readIntLE() * 0.0000001);
                    position.set("bleLocation", BufferUtil.readString(buf, length - 8));
                    break;
                case 0x17:
                    position.set("gpsUrl", BufferUtil.readString(buf, length));
                    break;
                case 0x18:
                    position.set("lbsUrl", BufferUtil.readString(buf, length));
                    break;
                case 0x1A:
                    position.set("firmware", BufferUtil.readString(buf, length));
                    break;
                case 0x1B:
                    position.set("gsmModule", BufferUtil.readString(buf, length));
                    break;
                case 0x1D:
                    position.set("agpsUpdate", buf.readUnsignedByte());
                    position.set("agpsLatitude", buf.readIntLE() * 0.0000001);
                    position.set("agpsLongitude", buf.readIntLE() * 0.0000001);
                    break;
                case 0x30:
                    position.set("numberFlag", buf.readUnsignedByte());
                    position.set("number", BufferUtil.readString(buf, length - 1));
                    break;
                case 0x31:
                    position.set("prefixFlag", buf.readUnsignedByte());
                    position.set("prefix", BufferUtil.readString(buf, length - 1));
                    break;
                case 0x33:
                    position.set("phoneSwitches", buf.readUnsignedByte());
                    break;
                case 0x40:
                    position.set("apn", BufferUtil.readString(buf, length));
                    break;
                case 0x41:
                    position.set("apnUser", BufferUtil.readString(buf, length));
                    break;
                case 0x42:
                    position.set("apnPassword", BufferUtil.readString(buf, length));
                    break;
                case 0x43:
                    buf.readUnsignedByte(); // flag
                    position.set("port", buf.readUnsignedShort());
                    position.set("server", BufferUtil.readString(buf, length - 3));
                    break;
                case 0x44:
                    position.set("heartbeatInterval", buf.readUnsignedInt());
                    position.set("uploadInterval", buf.readUnsignedInt());
                    position.set("uploadLazyInterval", buf.readUnsignedInt());
                    break;
                case 0x47:
                    position.set("deviceId", BufferUtil.readString(buf, length));
                    break;
                case 0x4E:
                    position.set("gsmBand", buf.readUnsignedByte());
                    break;
                case 0x50:
                    position.set("powerAlert", buf.readUnsignedInt());
                    break;
                case 0x51:
                    position.set("geoAlert", buf.readUnsignedInt());
                    break;
                case 0x53:
                    position.set("motionAlert", buf.readUnsignedInt());
                    break;
                case 0x5C:
                    position.set("barkLevel", buf.readUnsignedByte());
                    position.set("barkInterval", buf.readUnsignedInt());
                    break;
                case 0x61:
                    position.set("msisdn", BufferUtil.readString(buf, length));
                    break;
                case 0x62:
                    position.set("wifiWhitelist", buf.readUnsignedByte());
                    position.set("wifiWhitelistMac", ByteBufUtil.hexDump(buf.readSlice(6)));
                    break;
                case 0x64:
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    position.set("networkBand", buf.readUnsignedInt());
                    position.set(Position.KEY_OPERATOR, BufferUtil.readString(buf, length - 5));
                    break;
                case 0x65:
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    position.set("networkStatus", buf.readUnsignedByte());
                    position.set("serverStatus", buf.readUnsignedByte());
                    position.set("networkPlmn", ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("homePlmn", ByteBufUtil.hexDump(buf.readSlice(6)));
                    break;
                case 0x66:
                    position.set("imsi", BufferUtil.readString(buf, length));
                    break;
                case 0x75:
                    position.set("extraEnableControl", buf.readUnsignedInt());
                    break;
                default:
                    break;
            }

            buf.readerIndex(endIndex);
        }

        return position;
    }

}
