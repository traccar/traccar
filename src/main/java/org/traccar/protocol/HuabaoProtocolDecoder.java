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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
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

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class HuabaoProtocolDecoder extends BaseProtocolDecoder {

    public HuabaoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_GENERAL_RESPONSE = 0x8001;
    public static final int MSG_GENERAL_RESPONSE_2 = 0x4401;
    public static final int MSG_TERMINAL_REGISTER = 0x0100;
    public static final int MSG_TERMINAL_REGISTER_RESPONSE = 0x8100;
    public static final int MSG_TERMINAL_CONTROL = 0x8105;
    public static final int MSG_TERMINAL_AUTH = 0x0102;
    public static final int MSG_LOCATION_REPORT = 0x0200;
    public static final int MSG_LOCATION_REPORT_2 = 0x5501;
    public static final int MSG_LOCATION_REPORT_BLIND = 0x5502;
    public static final int MSG_LOCATION_BATCH = 0x0704;
    public static final int MSG_OIL_CONTROL = 0XA006;

    public static final int RESULT_SUCCESS = 0;

    public static ByteBuf formatMessage(int type, ByteBuf id, boolean shortIndex, ByteBuf data) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x7e);
        buf.writeShort(type);
        buf.writeShort(data.readableBytes());
        buf.writeBytes(id);
        if (shortIndex) {
            buf.writeByte(1);
        } else {
            buf.writeShort(1);
        }
        buf.writeBytes(data);
        data.release();
        buf.writeByte(Checksum.xor(buf.nioBuffer(1, buf.readableBytes() - 1)));
        buf.writeByte(0x7e);
        return buf;
    }

    private void sendGeneralResponse(
            Channel channel, SocketAddress remoteAddress, ByteBuf id, int type, int index) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeShort(index);
            response.writeShort(type);
            response.writeByte(RESULT_SUCCESS);
            channel.writeAndFlush(new NetworkMessage(
                    formatMessage(MSG_GENERAL_RESPONSE, id, false, response), remoteAddress));
        }
    }

    private void sendGeneralResponse2(
            Channel channel, SocketAddress remoteAddress, ByteBuf id, int type) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeShort(type);
            response.writeByte(RESULT_SUCCESS);
            channel.writeAndFlush(new NetworkMessage(
                    formatMessage(MSG_GENERAL_RESPONSE_2, id, true, response), remoteAddress));
        }
    }

    private String decodeAlarm(long value) {
        if (BitUtil.check(value, 0)) {
            return Position.ALARM_SOS;
        }
        if (BitUtil.check(value, 1)) {
            return Position.ALARM_OVERSPEED;
        }
        if (BitUtil.check(value, 5)) {
            return Position.ALARM_GPS_ANTENNA_CUT;
        }
        if (BitUtil.check(value, 4) || BitUtil.check(value, 9)
                || BitUtil.check(value, 10) || BitUtil.check(value, 11)) {
            return Position.ALARM_FAULT;
        }
        if (BitUtil.check(value, 8)) {
            return Position.ALARM_POWER_OFF;
        }
        if (BitUtil.check(value, 20)) {
            return Position.ALARM_GEOFENCE;
        }
        if (BitUtil.check(value, 28)) {
            return Position.ALARM_MOVEMENT;
        }
        if (BitUtil.check(value, 29)) {
            return Position.ALARM_ACCIDENT;
        }
        return null;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getByte(buf.readerIndex()) == '(') {
            return decodeResult(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII));
        }

        buf.readUnsignedByte(); // start marker
        int type = buf.readUnsignedShort();
        int attribute = buf.readUnsignedShort();
        ByteBuf id = buf.readSlice(6); // phone number
        int index;
        if (type == MSG_LOCATION_REPORT_2 || type == MSG_LOCATION_REPORT_BLIND) {
            index = buf.readUnsignedByte();
        } else {
            index = buf.readUnsignedShort();
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, ByteBufUtil.hexDump(id));
        if (deviceSession == null) {
            return null;
        }

        if (deviceSession.getTimeZone() == null) {
            deviceSession.setTimeZone(getTimeZone(deviceSession.getDeviceId(), "GMT+8"));
        }

        if (type == MSG_TERMINAL_REGISTER) {

            if (channel != null) {
                ByteBuf response = Unpooled.buffer();
                response.writeShort(index);
                response.writeByte(RESULT_SUCCESS);
                response.writeBytes(ByteBufUtil.hexDump(id).getBytes(StandardCharsets.US_ASCII));
                channel.writeAndFlush(new NetworkMessage(
                        formatMessage(MSG_TERMINAL_REGISTER_RESPONSE, id, false, response), remoteAddress));
            }

        } else if (type == MSG_TERMINAL_AUTH) {

            sendGeneralResponse(channel, remoteAddress, id, type, index);

        } else if (type == MSG_LOCATION_REPORT) {

            sendGeneralResponse(channel, remoteAddress, id, type, index);

            return decodeLocation(deviceSession, buf);

        } else if (type == MSG_LOCATION_REPORT_2 || type == MSG_LOCATION_REPORT_BLIND) {

            if (BitUtil.check(attribute, 15)) {
                sendGeneralResponse2(channel, remoteAddress, id, type);
            }

            return decodeLocation2(deviceSession, buf, type);

        } else if (type == MSG_LOCATION_BATCH) {

            return decodeLocationBatch(deviceSession, buf);

        }

        return null;
    }

    private Position decodeResult(Channel channel, SocketAddress remoteAddress, String sentence) {
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession != null) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            getLastLocation(position, null);
            position.set(Position.KEY_RESULT, sentence);
            return position;
        }
        return null;
    }

    private Position decodeLocation(DeviceSession deviceSession, ByteBuf buf) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedInt()));

        int status = buf.readInt();

        position.set(Position.KEY_IGNITION, BitUtil.check(status, 0));
        position.set(Position.KEY_BLOCKED, BitUtil.check(status, 10));

        position.setValid(BitUtil.check(status, 1));

        double lat = buf.readUnsignedInt() * 0.000001;
        double lon = buf.readUnsignedInt() * 0.000001;

        if (BitUtil.check(status, 2)) {
            position.setLatitude(-lat);
        } else {
            position.setLatitude(lat);
        }

        if (BitUtil.check(status, 3)) {
            position.setLongitude(-lon);
        } else {
            position.setLongitude(lon);
        }

        position.setAltitude(buf.readShort());
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort() * 0.1));
        position.setCourse(buf.readUnsignedShort());

        DateBuilder dateBuilder = new DateBuilder(deviceSession.getTimeZone())
                .setYear(BcdUtil.readInteger(buf, 2))
                .setMonth(BcdUtil.readInteger(buf, 2))
                .setDay(BcdUtil.readInteger(buf, 2))
                .setHour(BcdUtil.readInteger(buf, 2))
                .setMinute(BcdUtil.readInteger(buf, 2))
                .setSecond(BcdUtil.readInteger(buf, 2));
        position.setTime(dateBuilder.getDate());

        if (buf.readableBytes() == 20) {

            buf.skipBytes(4); // remaining battery and mileage
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 1000);
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.1);
            buf.readUnsignedInt(); // area id
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());
            buf.skipBytes(3); // reserved

            return position;

        }

        while (buf.readableBytes() > 2) {

            int subtype = buf.readUnsignedByte();
            int length = buf.readUnsignedByte();
            int endIndex = buf.readerIndex() + length;
            switch (subtype) {
                case 0x01:
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 100);
                    break;
                case 0x02:
                    position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedShort() * 0.1);
                    break;
                case 0x30:
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    break;
                case 0x31:
                    position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                    break;
                case 0x33:
                    String sentence = buf.readCharSequence(length, StandardCharsets.US_ASCII).toString();
                    if (sentence.startsWith("*M00")) {
                        String lockStatus = sentence.substring(8, 8 + 7);
                        position.set(Position.KEY_BATTERY, Integer.parseInt(lockStatus.substring(2, 5)) * 0.01);
                    }
                    break;
                case 0x91:
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.1);
                    position.set(Position.KEY_RPM, buf.readUnsignedShort());
                    position.set(Position.KEY_OBD_SPEED, buf.readUnsignedByte());
                    position.set(Position.KEY_THROTTLE, buf.readUnsignedByte() * 100 / 255);
                    position.set(Position.KEY_ENGINE_LOAD, buf.readUnsignedByte() * 100 / 255);
                    position.set(Position.KEY_COOLANT_TEMP, buf.readUnsignedByte() - 40);
                    buf.readUnsignedShort();
                    position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedShort() * 0.01);
                    buf.readUnsignedShort();
                    buf.readUnsignedInt();
                    buf.readUnsignedShort();
                    position.set(Position.KEY_FUEL_USED, buf.readUnsignedShort() * 0.01);
                    break;
                case 0x94:
                    if (length > 0) {
                        position.set(
                                Position.KEY_VIN, buf.readCharSequence(length, StandardCharsets.US_ASCII).toString());
                    }
                    break;
                case 0xD0:
                    long userStatus = buf.readUnsignedInt();
                    if (BitUtil.check(userStatus, 3)) {
                        position.set(Position.KEY_ALARM, Position.ALARM_VIBRATION);
                    }
                    break;
                case 0xD3:
                    position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.1);
                    break;
                case 0xD4:
                    position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                    break;
                case 0xD5:
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.01);
                    break;
                case 0xDA:
                    buf.readUnsignedShort(); // string cut count
                    int deviceStatus = buf.readUnsignedByte();
                    position.set("string", BitUtil.check(deviceStatus, 0));
                    position.set(Position.KEY_MOTION, BitUtil.check(deviceStatus, 2));
                    position.set("cover", BitUtil.check(deviceStatus, 3));
                    break;
                case 0xEB:
                    while (buf.readerIndex() < endIndex) {
                        int extendedLength = buf.readUnsignedShort();
                        int extendedType = buf.readUnsignedShort();
                        switch (extendedType) {
                            case 0x0001:
                                position.set("fuel1", buf.readUnsignedShort() * 0.1);
                                buf.readUnsignedByte(); // unused
                                break;
                            case 0x0023:
                                position.set("fuel2", Double.parseDouble(
                                        buf.readCharSequence(6, StandardCharsets.US_ASCII).toString()));
                                break;
                            case 0x00CE:
                                position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);
                                break;
                            default:
                                buf.skipBytes(extendedLength - 2);
                                break;
                        }
                    }
                    break;
                default:
                    break;
            }
            buf.readerIndex(endIndex);
        }

        return position;
    }

    private Position decodeLocation2(DeviceSession deviceSession, ByteBuf buf, int type) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        Jt600ProtocolDecoder.decodeBinaryLocation(buf, position);
        position.setValid(type != MSG_LOCATION_REPORT_BLIND);

        position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 1000L);

        int battery = buf.readUnsignedByte();
        if (battery <= 100) {
            position.set(Position.KEY_BATTERY_LEVEL, battery);
        } else if (battery == 0xAA) {
            position.set(Position.KEY_CHARGE, true);
        }

        position.setNetwork(new Network(CellTower.fromCidLac(buf.readUnsignedInt(), buf.readUnsignedShort())));

        int product = buf.readUnsignedByte();
        int status = buf.readUnsignedShort();
        int alarm = buf.readUnsignedShort();

        if (product == 1 || product == 2) {
            if (BitUtil.check(alarm, 0)) {
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_POWER);
            }
        } else if (product == 3) {
            position.set(Position.KEY_BLOCKED, BitUtil.check(status, 5));
            if (BitUtil.check(alarm, 1)) {
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_POWER);
            }
            if (BitUtil.check(alarm, 2)) {
                position.set(Position.KEY_ALARM, Position.ALARM_VIBRATION);
            }
            if (BitUtil.check(alarm, 3)) {
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
            }
        }

        position.set(Position.KEY_STATUS, status);

        return position;
    }

    private List<Position> decodeLocationBatch(DeviceSession deviceSession, ByteBuf buf) {

        List<Position> positions = new LinkedList<>();

        int count = buf.readUnsignedShort();
        buf.readUnsignedByte(); // location type

        for (int i = 0; i < count; i++) {
            int endIndex = buf.readUnsignedShort() + buf.readerIndex();
            positions.add(decodeLocation(deviceSession, buf));
            buf.readerIndex(endIndex);
        }

        return positions;
    }

}
