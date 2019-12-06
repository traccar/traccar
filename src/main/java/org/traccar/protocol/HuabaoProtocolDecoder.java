/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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
    public static final int MSG_TERMINAL_REGISTER = 0x0100;
    public static final int MSG_TERMINAL_REGISTER_RESPONSE = 0x8100;
    public static final int MSG_TERMINAL_CONTROL = 0x8105;
    public static final int MSG_TERMINAL_AUTH = 0x0102;
    public static final int MSG_LOCATION_REPORT = 0x0200;
    public static final int MSG_LOCATION_BATCH = 0x0704;
    public static final int MSG_OIL_CONTROL = 0XA006;

    public static final int RESULT_SUCCESS = 0;

    public static ByteBuf formatMessage(int type, ByteBuf id, ByteBuf data) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x7e);
        buf.writeShort(type);
        buf.writeShort(data.readableBytes());
        buf.writeBytes(id);
        buf.writeShort(0); // index
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
                    formatMessage(MSG_GENERAL_RESPONSE, id, response), remoteAddress));
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

        buf.readUnsignedByte(); // start marker
        int type = buf.readUnsignedShort();
        buf.readUnsignedShort(); // body length
        ByteBuf id = buf.readSlice(6); // phone number
        int index = buf.readUnsignedShort();

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
                        formatMessage(MSG_TERMINAL_REGISTER_RESPONSE, id, response), remoteAddress));
            }

        } else if (type == MSG_TERMINAL_AUTH) {

            sendGeneralResponse(channel, remoteAddress, id, type, index);

        } else if (type == MSG_LOCATION_REPORT) {

            return decodeLocation(deviceSession, buf);

        } else if (type == MSG_LOCATION_BATCH) {

            return decodeLocationBatch(deviceSession, buf);

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
                default:
                    break;
            }
            buf.readerIndex(endIndex);
        }

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
