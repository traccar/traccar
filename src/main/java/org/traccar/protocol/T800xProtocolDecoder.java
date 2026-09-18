/*
 * Copyright 2015 - 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class T800xProtocolDecoder extends BaseProtocolDecoder {

    private short header = DEFAULT_HEADER;

    public short getHeader() {
        return header;
    }

    public T800xProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final short DEFAULT_HEADER = 0x2323;

    public static final int MSG_LOGIN = 0x01;
    public static final int MSG_GPS = 0x02;
    public static final int MSG_HEARTBEAT = 0x03;
    public static final int MSG_ALARM = 0x04;
    public static final int MSG_NETWORK = 0x05; // 0x2727
    public static final int MSG_DRIVER_BEHAVIOR_1 = 0x05; // 0x2626
    public static final int MSG_DRIVER_BEHAVIOR_2 = 0x06; // 0x2626
    public static final int MSG_BLE = 0x10;
    public static final int MSG_NETWORK_2 = 0x11;
    public static final int MSG_GPS_2 = 0x13;
    public static final int MSG_ALARM_2 = 0x14;
    public static final int MSG_COMMAND = 0x81;

    public static final int MSG_BLE_2 = 0x12;
    public static final int MSG_LOCK = 0x17;
    public static final int MSG_GEOFENCE = 0x20;
    public static final int MSG_WIFI = 0x24;
    public static final int MSG_WIFI_ALARM = 0x25;
    public static final int MSG_TEMPERATURE = 0x26;
    public static final int MSG_SUB_LOCK = 0x27;
    public static final int MSG_ASSET_GPS = 0x62;
    public static final int MSG_ASSET_ALARM = 0x64;

    private void sendResponse(Channel channel, short header, int type, int index, ByteBuf imei, int alarm) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer(alarm > 0 ? 16 : 15);
            response.writeShort(header);
            response.writeByte(type);
            response.writeShort(response.capacity()); // length
            response.writeShort(index);
            response.writeBytes(imei);
            if (alarm > 0) {
                response.writeByte(alarm);
            }
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    private String decodeAlarm1(int value) {
        return switch (value) {
            case 1 -> Position.ALARM_POWER_CUT;
            case 2 -> Position.ALARM_LOW_BATTERY;
            case 3 -> Position.ALARM_SOS;
            case 4 -> Position.ALARM_OVERSPEED;
            case 5 -> Position.ALARM_GEOFENCE_ENTER;
            case 6 -> Position.ALARM_GEOFENCE_EXIT;
            case 7 -> Position.ALARM_TOW;
            case 8, 10 -> Position.ALARM_VIBRATION;
            case 21 -> Position.ALARM_JAMMING;
            case 23 -> Position.ALARM_POWER_RESTORED;
            case 24 -> Position.ALARM_LOW_POWER;
            default -> null;
        };
    }

    private String decodeAlarm2(int value) {
        return switch (value) {
            case 1, 4 -> Position.ALARM_REMOVING;
            case 2 -> Position.ALARM_TAMPERING;
            case 3 -> Position.ALARM_SOS;
            case 5 -> Position.ALARM_FALL_DOWN;
            case 6 -> Position.ALARM_LOW_BATTERY;
            case 8, 30, 33, 37 -> Position.ALARM_TEMPERATURE;
            case 9 -> Position.ALARM_VIBRATION;
            case 14 -> Position.ALARM_GEOFENCE_ENTER;
            case 15 -> Position.ALARM_GEOFENCE_EXIT;
            case 16 -> Position.ALARM_MOVEMENT;
            default -> null;
        };
    }

    private Date readDate(ByteBuf buf) {
        return new DateBuilder()
                .setYear(BcdUtil.readInteger(buf, 2))
                .setMonth(BcdUtil.readInteger(buf, 2))
                .setDay(BcdUtil.readInteger(buf, 2))
                .setHour(BcdUtil.readInteger(buf, 2))
                .setMinute(BcdUtil.readInteger(buf, 2))
                .setSecond(BcdUtil.readInteger(buf, 2))
                .getDate();
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.readableBytes() < 15) {
            return null;
        }

        header = buf.readShort();
        int type = buf.readUnsignedByte();
        int length = buf.readUnsignedShort();
        int index = buf.readUnsignedShort();
        ByteBuf imei = buf.readSlice(8);

        DeviceSession deviceSession = getDeviceSession(
                channel, remoteAddress, ByteBufUtil.hexDump(imei).substring(1));
        if (deviceSession == null) {
            return null;
        }

        if (header == 0x2727 && switch (type) {
            case MSG_BLE_2, MSG_LOCK, MSG_GEOFENCE, MSG_WIFI, MSG_WIFI_ALARM,
                    MSG_TEMPERATURE, MSG_SUB_LOCK, MSG_ASSET_GPS, MSG_ASSET_ALARM -> true;
            default -> false;
        }) {
            if (length != 15 + buf.readableBytes()) {
                return null;
            }
            try {
                return decodeAsset(channel, deviceSession, buf, type, index, imei);
            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                // Reject incomplete or malformed payloads without acknowledging them.
                return null;
            }
        }

        boolean positionType = type == MSG_GPS || type == MSG_GPS_2 || type == MSG_ALARM || type == MSG_ALARM_2;
        if (!positionType) {
            sendResponse(channel, header, type, header == 0x2323 ? 1 : index, imei, 0);
        }

        if (positionType) {

            return decodePosition(channel, deviceSession, buf, type, index, imei);

        } else if (type == MSG_NETWORK && header == 0x2727 || type == MSG_NETWORK_2) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, readDate(buf));

            position.set(Position.KEY_OPERATOR, buf.readCharSequence(
                    buf.readUnsignedByte(), StandardCharsets.UTF_16LE).toString());
            position.set("networkTechnology", buf.readCharSequence(
                    buf.readUnsignedByte(), StandardCharsets.US_ASCII).toString());
            position.set("networkBand", buf.readCharSequence(
                    buf.readUnsignedByte(), StandardCharsets.US_ASCII).toString());
            buf.readCharSequence(buf.readUnsignedByte(), StandardCharsets.US_ASCII); // imsi
            position.set(Position.KEY_ICCID, buf.readCharSequence(
                    buf.readUnsignedByte(), StandardCharsets.US_ASCII).toString());

            return position;

        } else if (type == MSG_DRIVER_BEHAVIOR_1 && header == 0x2626) {

            String alarm = switch (buf.readUnsignedByte()) {
                case 0, 2, 4 -> Position.ALARM_BRAKING;
                case 1, 3, 5 -> Position.ALARM_ACCELERATION;
                default -> null;
            };

            List<Position> positions = new LinkedList<>();
            while (buf.readableBytes() >= 24) {
                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                Date time = readDate(buf);

                if (buf.getInt(buf.readerIndex()) != -1) {
                    position.setValid(true);
                    position.setTime(time);
                    position.setAltitude(buf.readFloatLE());
                    position.setLongitude(buf.readFloatLE());
                    position.setLatitude(buf.readFloatLE());
                } else {
                    buf.skipBytes(12);
                    getLastLocation(position, time);
                }

                position.setSpeed(UnitsConverter.knotsFromKph(BcdUtil.readInteger(buf, 4) / 10.0));

                int course = buf.readUnsignedShort();
                if (course != 0xffff) {
                    position.setCourse(course / 10.0);
                }

                int rpm = buf.readUnsignedShort();
                if (rpm != 0xffff) {
                    position.set(Position.KEY_RPM, rpm);
                }

                positions.add(position);
            }

            positions.getLast().addAlarm(alarm);

            return positions;

        } else if (type == MSG_DRIVER_BEHAVIOR_2 && header == 0x2626) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            switch (buf.readUnsignedByte()) {
                case 0 -> position.addAlarm(Position.ALARM_BRAKING);
                case 1 -> position.addAlarm(Position.ALARM_ACCELERATION);
                case 2 -> position.addAlarm(Position.ALARM_CORNERING);
            }

            position.setTime(readDate(buf));

            int status = buf.readUnsignedByte();
            position.setValid(!BitUtil.check(status, 7));
            buf.skipBytes(5); // acceleration

            position.setAltitude(buf.readFloatLE());
            position.setLongitude(buf.readFloatLE());
            position.setLatitude(buf.readFloatLE());
            position.setSpeed(UnitsConverter.knotsFromKph(BcdUtil.readInteger(buf, 4) / 10.0));
            position.setCourse(buf.readUnsignedShort() / 10.0);

            position.set(Position.KEY_RPM, buf.readUnsignedShort());

            return position;

        } else if (type == MSG_BLE) {

            return decodeBle(channel, deviceSession, buf, type, index, imei);

        } else if (type == MSG_COMMAND) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            buf.readUnsignedByte(); // protocol number

            position.set(Position.KEY_RESULT, buf.toString(StandardCharsets.UTF_16LE));

            return position;

        }

        return null;
    }

    private void decodeAssetLockType(Position position, int value) {
        position.set("lockType", value);
        switch (value) {
            case 0x00, 0x05, 0x07,
                    0x12, 0x13, 0x14, 0x16, 0x17, 0x22, 0x23, 0x24, 0x26, 0x27,
                    0x32, 0x33, 0x34, 0x36, 0x37, 0x42, 0x43, 0x44, 0x46, 0x47,
                    0x52, 0x53, 0x54, 0x56, 0x57 ->
                    position.set(Position.KEY_LOCK, true);
            case 0x03, 0x06, 0x08, 0x11, 0x15, 0x21, 0x25, 0x31, 0x35, 0x41, 0x45, 0x51, 0x55 ->
                    position.set(Position.KEY_LOCK, false);
            default -> { }
        }
        if (value == 1) {
            position.addAlarm(Position.ALARM_TAMPERING);
        }
    }

    private void decodeAssetNetwork(Position position, ByteBuf buf, boolean lte) {
        int mcc = buf.readUnsignedShort();
        int mnc = buf.readUnsignedShort();
        if (mcc == 0xffff || mnc == 0xffff) {
            return;
        }
        Network network = position.getNetwork() != null ? position.getNetwork() : new Network();
        if (lte) {
            long cellId = buf.readUnsignedInt();
            int tac = buf.readUnsignedShort();
            network.setRadioType("lte");
            network.addCellTower(CellTower.from(mcc & 0x7fff, mnc, tac, cellId));
        } else {
            for (int i = 0; i < 3; i++) {
                int lac = buf.readUnsignedShort();
                int cellId = buf.readUnsignedShort();
                if (lac != 0xffff && cellId != 0xffff) {
                    network.addCellTower(CellTower.from(mcc, mnc, lac, cellId));
                }
            }
        }
        position.setNetwork(network);
    }

    private void decodeAssetLocation(Position position, ByteBuf buf, boolean valid) {
        if (valid && buf.getInt(buf.readerIndex()) != -1) {
            double altitude = buf.readFloatLE();
            double longitude = buf.readFloatLE();
            double latitude = buf.readFloatLE();
            if (Double.isFinite(altitude) && Double.isFinite(longitude) && Double.isFinite(latitude)
                    && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180) {
                position.setValid(true);
                position.setAltitude(altitude);
                position.setLongitude(longitude);
                position.setLatitude(latitude);
            }
            if (buf.getUnsignedShort(buf.readerIndex()) != 0xffff) {
                position.setSpeed(UnitsConverter.knotsFromKph(BcdUtil.readInteger(buf, 4) / 10.0));
            } else {
                buf.skipBytes(2);
            }
            int course = buf.readUnsignedShort();
            if (course <= 360) {
                position.setCourse(course);
            }
        } else {
            position.setValid(false);
            decodeAssetNetwork(position, buf, BitUtil.check(buf.getUnsignedByte(buf.readerIndex()), 7));
        }
    }

    private void decodeAssetWifi(Position position, ByteBuf buf) {
        position.set("wifiMac", ByteBufUtil.hexDump(buf.readSlice(6)));
        Network network = position.getNetwork() != null ? position.getNetwork() : new Network();
        for (int i = 0; i < 3; i++) {
            String mac = ByteBufUtil.hexDump(buf.readSlice(6));
            int rssi = buf.readByte();
            if (!mac.equals("ffffffffffff") && !mac.equals("000000000000")) {
                network.addWifiAccessPoint(WifiAccessPoint.from(mac, rssi));
            }
        }
        position.setNetwork(network);
    }

    private void decodeAssetValue(Position position, int id, ByteBuf buf) {
        // All-ones values indicate unavailable measurements.
        boolean unavailable = true;
        for (int i = buf.readerIndex(); i < buf.writerIndex(); i++) {
            unavailable &= buf.getUnsignedByte(i) == 0xff;
        }
        if (unavailable) {
            return;
        }
        switch (id) {
            case 0x01 -> {
                int alarm = buf.readUnsignedByte();
                position.set("alarmCode", alarm);
                position.addAlarm(decodeAlarm2(alarm));
            }
            case 0x02 -> position.set("gpsStatus", buf.readUnsignedByte());
            case 0x03 -> position.set(Position.KEY_RSSI, buf.readUnsignedByte());
            case 0x07 -> position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
            case 0x09 -> position.set("networkStatus", buf.readUnsignedByte());
            case 0x0a -> {
                int value = buf.readUnsignedByte();
                position.set(Position.KEY_DEVICE_TEMP, (value & 0x7f) * (BitUtil.check(value, 7) ? -1 : 1));
            }
            case 0x0e -> position.set("rechargeable", BitUtil.check(buf.readUnsignedByte(), 7));
            case 0x0f -> position.set("lockType", buf.readUnsignedByte());
            case 0x10 -> position.set("smartUpload", BitUtil.check(buf.readUnsignedByte(), 7));
            case 0x18 -> {
                int value = buf.readUnsignedByte();
                position.set(Position.KEY_DOOR, !BitUtil.check(value, 7));
                position.set("sensorConnected", BitUtil.check(value, 0));
            }
            case 0x60 -> position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
            case 0x61 -> {
                int value = buf.readUnsignedShort();
                position.set(Position.KEY_IGNITION, BitUtil.check(value, 15));
                position.set("usbConnected", BitUtil.check(value, 12));
                position.set("solarCharging", BitUtil.check(value, 11));
                position.set(Position.KEY_CHARGE, (value & 0x1a00) != 0);
            }
            case 0x62 -> position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
            case 0x63 -> position.set("solarPanel", buf.readUnsignedShort() / 1000.0);
            case 0x64, 0x66 -> {
                int integer = buf.readUnsignedByte();
                int fraction = buf.readUnsignedByte();
                double value = (integer + (fraction & 0x7f) / 100.0) * (BitUtil.check(fraction, 7) ? -1 : 1);
                position.set(id == 0x64 ? Position.PREFIX_TEMP + 1 : Position.KEY_DEVICE_TEMP, value);
            }
            // The workbook and manufacturer codec disagree on humidity scaling. Preserve the raw value.
            case 0x65 -> position.set("humidityRaw", buf.readUnsignedShort());
            case 0xa0 -> position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
            case 0xa1 -> position.set(Position.KEY_POWER, buf.readUnsignedInt() / 1000.0);
            case 0xa2 -> position.set("lightIntensity", buf.readUnsignedInt());
            default -> { }
        }
    }

    private void decodeAssetBlock(Position position, int id, ByteBuf buf) {
        int minimum = switch (id) {
            case 0x01 -> 17;
            case 0x02, 0x03 -> 16;
            case 0x04 -> 27;
            case 0x05 -> 6;
            default -> 0;
        };
        if (buf.readableBytes() < minimum) {
            throw new IllegalArgumentException("Incomplete asset data block");
        }
        switch (id) {
            case 0x01 -> {
                double altitude = buf.readFloatLE();
                double longitude = buf.readFloatLE();
                double latitude = buf.readFloatLE();
                int course = buf.readUnsignedShort();
                int hdop = buf.readUnsignedShort();
                int satellites = buf.readUnsignedByte();
                if (Double.isFinite(altitude) && Double.isFinite(longitude) && Double.isFinite(latitude)
                        && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180) {
                    position.setValid(true);
                    position.setAltitude(altitude);
                    position.setLongitude(longitude);
                    position.setLatitude(latitude);
                }
                if (course <= 360) {
                    position.setCourse(course);
                }
                if (hdop != 0xffff) {
                    position.set(Position.KEY_HDOP, hdop);
                }
                if (satellites != 0xff) {
                    position.set(Position.KEY_SATELLITES, satellites);
                }
            }
            case 0x02, 0x03 -> decodeAssetNetwork(position, buf, id == 0x03);
            case 0x04 -> decodeAssetWifi(position, buf);
            case 0x05 -> {
                position.set("accelerationX", buf.readShort());
                position.set("accelerationY", buf.readShort());
                position.set("accelerationZ", buf.readShort());
            }
            default -> { }
        }
    }

    private Position decodeAssetData(DeviceSession deviceSession, ByteBuf buf, int index) {
        int checksum = buf.readUnsignedByte();
        if (checksum != Checksum.crc8(Checksum.CRC8_EGTS, buf.nioBuffer())) {
            return null;
        }
        int format = buf.readUnsignedByte();
        if ((format & 0x30) != 0) {
            return null; // Encrypted asset payloads require a separate transport implementation.
        }
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        getLastLocation(position, readDate(buf));
        position.setValid(false);
        position.set(Position.KEY_INDEX, index);
        position.set(Position.KEY_ARCHIVE, BitUtil.check(format, 7));
        while (buf.isReadable()) {
            int group = buf.readUnsignedByte();
            int count = group == 3 ? buf.readUnsignedShort() : buf.readUnsignedByte();
            if (group < 1 || group > 3) {
                return null; // Unknown groups have no length with which to skip them safely.
            }
            for (int i = 0; i < count; i++) {
                int id = group == 3 ? buf.readUnsignedShort() : buf.readUnsignedByte();
                int size;
                if (group == 1) {
                    size = id < 0x80 ? 1 : id < 0xc0 ? 2 : id < 0xf0 ? 4 : 8;
                } else if (group == 2) {
                    size = id < 0x60 ? 1 : id < 0xa0 ? 2 : 4;
                } else {
                    size = buf.readUnsignedByte();
                }
                ByteBuf value = buf.readSlice(size);
                if (group == 2) {
                    decodeAssetValue(position, id, value);
                } else if (group == 3) {
                    decodeAssetBlock(position, id, value);
                }
            }
        }
        if (position.hasAttribute("gpsStatus")) {
            int gpsStatus = position.getInteger("gpsStatus");
            position.setValid(position.getValid() && BitUtil.check(gpsStatus, 2));
            position.set(Position.KEY_ARCHIVE,
                    position.getBoolean(Position.KEY_ARCHIVE) || BitUtil.check(gpsStatus, 1));
        }
        return position;
    }

    private Object decodeAsset(
            Channel channel, DeviceSession deviceSession, ByteBuf buf, int type, int index, ByteBuf imei) {
        Position position;
        boolean acknowledge = true;
        int alarm = -1;
        if (type == MSG_ASSET_GPS || type == MSG_ASSET_ALARM) {
            acknowledge = BitUtil.check(buf.getUnsignedByte(buf.readerIndex() + 1), 6);
            position = decodeAssetData(deviceSession, buf, index);
            if (position == null) {
                return null;
            }
            if (type == MSG_ASSET_ALARM) {
                if (!position.hasAttribute("alarmCode")) {
                    return null;
                }
                alarm = position.getInteger("alarmCode");
            }
        } else if (type == MSG_BLE_2) {
            position = decodeBle(null, deviceSession, buf, type, index, imei);
        } else {
            position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            position.set(Position.KEY_INDEX, index);
            if (type == MSG_WIFI || type == MSG_WIFI_ALARM) {
                int status = buf.readUnsignedByte();
                int alarmCode = buf.readUnsignedByte();
                getLastLocation(position, readDate(buf));
                position.setValid(false);
                position.set(Position.KEY_ARCHIVE, BitUtil.check(status, 7));
                position.set("alarmCode", alarmCode);
                position.addAlarm(decodeAlarm2(alarmCode));
                ByteBuf location = buf.readSlice(27);
                if (BitUtil.check(status, 5)) {
                    decodeAssetWifi(position, location);
                } else {
                    decodeAssetLocation(position, location, BitUtil.check(status, 6));
                }
                buf.skipBytes(5); // acceleration
                int battery = buf.readUnsignedByte();
                if (battery != 0xff) {
                    position.set(Position.KEY_BATTERY_LEVEL, battery == 0 ? 100 : (battery >> 4) * 10 + (battery & 15));
                }
                decodeAssetValue(position, 0x0a, buf.readSlice(1));
                for (String key : new String[] {"lightSensor", Position.KEY_BATTERY, "solarPanel"}) {
                    int value = buf.readUnsignedByte();
                    if (value != 0xff) {
                        position.set(key, ((value >> 4) * 10 + (value & 15)) / 10.0);
                    }
                }
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                int input = buf.readUnsignedShort();
                position.set(Position.KEY_IGNITION, BitUtil.check(input, 2));
                position.set(Position.KEY_RSSI, BitUtil.between(input, 4, 11));
                buf.skipBytes(13); // reporting intervals and settings
                if (buf.readableBytes() >= 3) {
                    buf.skipBytes(2); // reserved
                    decodeAssetLockType(position, buf.readUnsignedByte());
                }
                acknowledge = type == MSG_WIFI_ALARM || AttributeUtil.lookup(getCacheManager(),
                        Keys.PROTOCOL_ACK.withPrefix(getProtocolName()), deviceSession.getDeviceId());
                alarm = type == MSG_WIFI_ALARM ? alarmCode : -1;
            } else {
                getLastLocation(position, readDate(buf));
                position.setValid(false);
                switch (type) {
                    case MSG_LOCK, MSG_SUB_LOCK -> {
                        int status = buf.readUnsignedByte();
                        position.set(Position.KEY_ARCHIVE, BitUtil.check(status, 7));
                        position.set(Position.KEY_SATELLITES, status & 0x1f);
                        decodeAssetLocation(position, buf.readSlice(16), BitUtil.check(status, 6));
                        if (type == MSG_LOCK) {
                            decodeAssetLockType(position, buf.readUnsignedByte());
                            position.set("lockId", ByteBufUtil.hexDump(buf.readSlice(buf.readUnsignedByte())));
                        } else {
                            position.set("lockRssi", buf.readUnsignedByte() - 128);
                            buf.skipBytes(4); // hardware and software versions
                            position.set("lockAlarm", buf.readUnsignedByte());
                            position.set("lockType", buf.readUnsignedByte());
                            int lockStatus = buf.readUnsignedShort();
                            position.set("lockStatus", lockStatus);
                            position.set(Position.KEY_LOCK, !BitUtil.check(lockStatus, 5));
                            position.set("lockBattery", buf.readUnsignedShort() / 1000.0);
                            position.set("lockSolarPanel", buf.readUnsignedShort() / 1000.0);
                            position.set("lockTemp", buf.readByte());
                            position.set("lockId", ByteBufUtil.hexDump(buf.readSlice(6)));
                            position.set("lockDeviceId", ByteBufUtil.hexDump(buf.readSlice(4)));
                            while (buf.isReadable()) {
                                int id = buf.readUnsignedByte();
                                ByteBuf value = buf.readSlice(buf.readUnsignedByte());
                                if (id == 2 && value.readableBytes() == 1) {
                                    position.set("lockBatteryLevel", value.readUnsignedByte());
                                } else {
                                    position.set("lockData" + id, ByteBufUtil.hexDump(value));
                                }
                            }
                        }
                    }
                    case MSG_TEMPERATURE -> {
                        int sensorType = buf.readUnsignedByte();
                        if (sensorType != 1) {
                            return null;
                        }
                        position.set("sensorType", sensorType);
                        position.set("samplingInterval", buf.readUnsignedShort());
                        List<Double> samples = new LinkedList<>();
                        while (buf.isReadable()) {
                            int value = buf.readShort();
                            samples.add(value == -1 ? null : value / 100.0);
                        }
                        position.set("temperatureSamples", samples.toString());
                        if (sensorType == 1 && !samples.isEmpty() && samples.getFirst() != null) {
                            position.set(Position.PREFIX_TEMP + 1, samples.getFirst());
                        }
                    }
                    case MSG_GEOFENCE -> {
                        position.set("deviceGeofenceEnabled", buf.readUnsignedByte() != 0);
                        while (buf.isReadable()) {
                            int id = buf.readUnsignedByte();
                            ByteBuf value = buf.readSlice(buf.readUnsignedShort());
                            position.set("deviceGeofence" + id, ByteBufUtil.hexDump(value));
                        }
                    }
                    default -> { }
                }
            }
        }
        if (position.getValid()) {
            position.setTime(position.getDeviceTime());
            position.setOutdated(false);
        }
        if (acknowledge && channel != null) {
            // Alarm replies include their alarm byte even when its value is zero.
            ByteBuf response = Unpooled.buffer(alarm >= 0 ? 16 : 15);
            response.writeShort(header);
            response.writeByte(type);
            response.writeShort(response.capacity());
            response.writeShort(index);
            response.writeBytes(imei, imei.readerIndex(), imei.readableBytes());
            if (alarm >= 0) {
                response.writeByte(alarm);
            }
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
        return position;
    }

    private double decodeBleTemp(ByteBuf buf) {
        int value = buf.readUnsignedShort();
        return (BitUtil.check(value, 15) ? -BitUtil.to(value, 15) : BitUtil.to(value, 15)) / 100.0;
    }

    private Position decodeBle(
            Channel channel, DeviceSession deviceSession, ByteBuf buf, int type, int index, ByteBuf imei) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, readDate(buf));

        position.set(Position.KEY_IGNITION, buf.readUnsignedByte() > 0);

        if (type == MSG_BLE_2) {
            int status = buf.readUnsignedByte();
            position.set(Position.KEY_ARCHIVE, BitUtil.check(status, 7));
            decodeAssetLocation(position, buf.readSlice(16), BitUtil.check(status, 6));
        }

        int i = 1;
        int recordType = type == MSG_BLE_2 ? buf.readUnsignedShort() : 0;
        while (buf.isReadable()) {
            switch (recordType != 0 ? recordType : buf.readUnsignedShort()) {
                case 0x01 -> {
                    position.set("tag" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("tag" + i + "Battery", buf.readUnsignedByte() / 100.0 + 1.22);
                    position.set("tag" + i + "TirePressure", buf.readUnsignedByte() * 1.527 * 2);
                    position.set("tag" + i + "TireTemp", buf.readUnsignedByte() - 55);
                    position.set("tag" + i + "TireStatus", buf.readUnsignedByte());
                }
                case 0x02 -> {
                    position.set("tag" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("tag" + i + "Battery", BcdUtil.readInteger(buf, 2) / 10.0);
                    switch (buf.readUnsignedByte()) {
                        case 0:
                            position.addAlarm(Position.ALARM_SOS);
                            break;
                        case 1:
                            position.addAlarm(Position.ALARM_LOW_BATTERY);
                            break;
                        default:
                            break;
                    }
                    if (type != MSG_BLE_2) {
                        buf.readUnsignedByte(); // status
                        buf.skipBytes(16); // location
                    }
                }
                case 0x03 -> {
                    position.set(Position.KEY_DRIVER_UNIQUE_ID, ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("tag" + i + "Battery", BcdUtil.readInteger(buf, 2) / 10.0);
                    if (buf.readUnsignedByte() == 1) {
                        position.addAlarm(Position.ALARM_LOW_BATTERY);
                    }
                    if (type != MSG_BLE_2) {
                        buf.readUnsignedByte(); // status
                        buf.skipBytes(16); // location
                    }
                }
                case 0x04 -> {
                    position.set("tag" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("tag" + i + "Battery", buf.readUnsignedByte() / 100.0 + 2);
                    buf.readUnsignedByte(); // battery level
                    position.set("tag" + i + "Temp", decodeBleTemp(buf));
                    position.set("tag" + i + "Humidity", buf.readUnsignedShort() / 100.0);
                    position.set("tag" + i + "LightSensor", buf.readUnsignedShort());
                    position.set("tag" + i + "Rssi", buf.readUnsignedByte() - 128);
                }
                case 0x05 -> {
                    position.set("tag" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("tag" + i + "Battery", buf.readUnsignedByte() / 100.0 + 2);
                    buf.readUnsignedByte(); // battery level
                    position.set("tag" + i + "Temp", decodeBleTemp(buf));
                    position.set("tag" + i + "Door", buf.readUnsignedByte() > 0);
                    position.set("tag" + i + "Rssi", buf.readUnsignedByte() - 128);
                }
                case 0x06 -> {
                    position.set("tag" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("tag" + i + "Battery", buf.readUnsignedByte() / 100.0 + 2);
                    if (type == MSG_BLE_2) {
                        position.set("tag" + i + "BatteryLevel", buf.readUnsignedByte());
                        position.set("tag" + i + "Temp", decodeBleTemp(buf));
                    }
                    position.set("tag" + i + "Output", buf.readUnsignedByte() > 0);
                    position.set("tag" + i + "Rssi", buf.readUnsignedByte() - 128);
                }
                default -> {
                    if (type == MSG_BLE_2) {
                        throw new IllegalArgumentException("Unsupported BLE record type");
                    }
                }
            }
            i += 1;
        }

        sendResponse(channel, header, type, index, imei, 0);

        return position;
    }

    private Position decodePosition(
            Channel channel, DeviceSession deviceSession, ByteBuf buf, int type, int index, ByteBuf imei) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_INDEX, index);

        if (header != 0x2727) {

            buf.readUnsignedShort(); // acc on interval
            buf.readUnsignedShort(); // acc off interval
            buf.readUnsignedByte(); // angle compensation
            buf.readUnsignedShort(); // distance compensation

            position.set(Position.KEY_RSSI, BitUtil.to(buf.readUnsignedShort(), 7));

        }

        int status = buf.readUnsignedByte();
        position.set(Position.KEY_SATELLITES, BitUtil.to(status, 5));

        if (header != 0x2727) {

            buf.readUnsignedByte(); // gsensor manager status
            buf.readUnsignedByte(); // other flags
            buf.readUnsignedByte(); // heartbeat
            buf.readUnsignedByte(); // relay status
            buf.readUnsignedShort(); // drag alarm setting

            int io = buf.readUnsignedShort();
            position.set(Position.KEY_IGNITION, BitUtil.check(io, 14));
            position.set("ac", BitUtil.check(io, 13));
            position.set(Position.PREFIX_IN + 3, BitUtil.check(io, 12));
            position.set(Position.PREFIX_IN + 4, BitUtil.check(io, 11));

            if (type == MSG_GPS_2 || type == MSG_ALARM_2) {
                position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
                buf.readUnsignedByte(); // reserved
            } else {
                position.set(Position.PREFIX_OUT + 1, BitUtil.check(io, 7));
                position.set(Position.PREFIX_OUT + 2, BitUtil.check(io, 8));
                position.set(Position.PREFIX_OUT + 3, BitUtil.check(io, 9));
            }

            if (header != 0x2626) {
                int adcCount = type == MSG_GPS_2 || type == MSG_ALARM_2 ? 5 : 2;
                for (int i = 1; i <= adcCount; i++) {
                    String value = ByteBufUtil.hexDump(buf.readSlice(2));
                    if (!value.equals("ffff")) {
                        position.set(Position.PREFIX_ADC + i, Integer.parseInt(value, 16) / 100.0);
                    }
                }
            }

        }

        int alarm = buf.readUnsignedByte();
        position.addAlarm(header != 0x2727 ? decodeAlarm1(alarm) : decodeAlarm2(alarm));
        position.set("alarmCode", alarm);

        if (header != 0x2727) {

            buf.readUnsignedByte(); // reserved

            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

            int battery = BcdUtil.readInteger(buf, 2);
            position.set(Position.KEY_BATTERY_LEVEL, battery > 0 ? battery : 100);

        }

        if (BitUtil.check(status, 6)) {

            position.setValid(true);
            position.setTime(readDate(buf));
            position.setAltitude(buf.readFloatLE());
            position.setLongitude(buf.readFloatLE());
            position.setLatitude(buf.readFloatLE());
            if (header == 0x2626) {
                buf.readUnsignedShort(); // reserved or hdop
            } else {
                position.setSpeed(UnitsConverter.knotsFromKph(BcdUtil.readInteger(buf, 4) / 10.0));
            }
            position.setCourse(buf.readUnsignedShort());

        } else {

            getLastLocation(position, readDate(buf));

            if (header == 0x2727) {
                ByteBuf location = buf.readSlice(16);
                decodeAssetNetwork(position, location, BitUtil.check(location.getUnsignedByte(0), 7));
            } else {
                int mcc = buf.readUnsignedShortLE();
                int mnc = buf.readUnsignedShortLE();

                if (mcc != 0xffff && mnc != 0xffff) {
                    Network network = new Network();
                    for (int i = 0; i < 3; i++) {
                        network.addCellTower(CellTower.from(
                                mcc, mnc, buf.readUnsignedShortLE(), buf.readUnsignedShortLE()));
                    }
                    position.setNetwork(network);
                }
            }

        }

        if (header == 0x2727) {

            byte[] accelerationBytes = new byte[5];
            buf.readBytes(accelerationBytes);
            long acceleration = new BigInteger(accelerationBytes).longValue();
            double accelerationZ = BitUtil.between(acceleration, 8, 15) + BitUtil.between(acceleration, 4, 8) / 10.0;
            if (!BitUtil.check(acceleration, 15)) {
                accelerationZ = -accelerationZ;
            }
            double accelerationY = BitUtil.between(acceleration, 20, 27) + BitUtil.between(acceleration, 16, 20) / 10.0;
            if (!BitUtil.check(acceleration, 27)) {
                accelerationY = -accelerationY;
            }
            double accelerationX = BitUtil.between(acceleration, 28, 32) + BitUtil.between(acceleration, 32, 39) / 10.0;
            if (!BitUtil.check(acceleration, 39)) {
                accelerationX = -accelerationX;
            }
            position.set(Position.KEY_G_SENSOR, "[" + accelerationX + "," + accelerationY + "," + accelerationZ + "]");

            int battery = BcdUtil.readInteger(buf, 2);
            position.set(Position.KEY_BATTERY_LEVEL, battery > 0 ? battery : 100);
            decodeAssetValue(position, 0x0a, buf.readSlice(1));
            position.set("lightSensor", BcdUtil.readInteger(buf, 2) / 10.0);
            position.set(Position.KEY_BATTERY, BcdUtil.readInteger(buf, 2) / 10.0);
            position.set("solarPanel", BcdUtil.readInteger(buf, 2) / 10.0);
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

            int inputStatus = buf.readUnsignedShort();
            position.set(Position.KEY_IGNITION, BitUtil.check(inputStatus, 2));
            position.set(Position.KEY_RSSI, BitUtil.between(inputStatus, 4, 11));
            position.set(Position.KEY_INPUT, inputStatus);

            buf.readUnsignedShort(); // ignition on upload interval
            buf.readUnsignedInt(); // ignition off upload interval
            buf.readUnsignedByte(); // angle upload interval
            buf.readUnsignedShort(); // distance upload interval
            buf.readUnsignedByte(); // heartbeat

            position.set(Position.KEY_ARCHIVE, BitUtil.check(status, 7));
            if (buf.readableBytes() >= 6) {
                buf.skipBytes(5); // settings and reserved bytes
                decodeAssetLockType(position, buf.readUnsignedByte());
            }

        } else {

            String model = getDeviceModel(deviceSession);
            if ("TLW2-2BL".equals(model) || type == MSG_GPS_2 || type == MSG_ALARM_2) {
                position.set(Position.KEY_BATTERY, BcdUtil.readInteger(buf, 4) / 100.0);
            }
            if (buf.readableBytes() >= 2) {
                position.set(Position.KEY_POWER, BcdUtil.readInteger(buf, 4) / 100.0);
            }
            if (buf.readableBytes() >= 19) {
                position.setSpeed(UnitsConverter.knotsFromKph(BcdUtil.readInteger(buf, 4) / 10.0));
                position.set(Position.KEY_OBD_SPEED, BcdUtil.readInteger(buf, 4) / 100.0);
                position.set(Position.KEY_FUEL_USED, buf.readUnsignedInt() / 1000.0);
                position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedInt() / 1000.0);
                position.set(Position.KEY_RPM, buf.readUnsignedShort());
                int value;
                value = buf.readUnsignedByte();
                if (value != 0xff) {
                    position.set("airInput", value);
                }
                if (value != 0xff) {
                    position.set("airPressure", value);
                }
                if (value != 0xff) {
                    position.set(Position.KEY_COOLANT_TEMP, value - 40);
                }
                if (value != 0xff) {
                    position.set("airTemp", value - 40);
                }
                if (value != 0xff) {
                    position.set(Position.KEY_ENGINE_LOAD, value);
                }
                if (value != 0xff) {
                    position.set(Position.KEY_THROTTLE, value);
                }
                if (value != 0xff) {
                    position.set(Position.KEY_FUEL, value);
                }
            }
        }

        boolean acknowledgement = AttributeUtil.lookup(
                getCacheManager(), Keys.PROTOCOL_ACK.withPrefix(getProtocolName()), deviceSession.getDeviceId());
        if (acknowledgement || type == MSG_ALARM || type == MSG_ALARM_2) {
            sendResponse(channel, header, type, header == 0x2323 ? 1 : index, imei, alarm);
        }

        return position;
    }

}
