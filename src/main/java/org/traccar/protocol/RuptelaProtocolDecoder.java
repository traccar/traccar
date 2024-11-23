/*
 * Copyright 2013 - 2024 Anton Tananaev (anton@traccar.org)
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
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DataConverter;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class RuptelaProtocolDecoder extends BaseProtocolDecoder {

    private ByteBuf photo;

    public RuptelaProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_RECORDS = 1;
    public static final int MSG_DEVICE_CONFIGURATION = 2;
    public static final int MSG_DEVICE_VERSION = 3;
    public static final int MSG_FIRMWARE_UPDATE = 4;
    public static final int MSG_SET_CONNECTION = 5;
    public static final int MSG_SET_ODOMETER = 6;
    public static final int MSG_SMS_VIA_GPRS_RESPONSE = 7;
    public static final int MSG_SMS_VIA_GPRS = 8;
    public static final int MSG_DTCS = 9;
    public static final int MSG_IDENTIFICATION = 15;
    public static final int MSG_HEARTBEAT = 16;
    public static final int MSG_SET_IO = 17;
    public static final int MSG_FILES = 37;
    public static final int MSG_EXTENDED_RECORDS = 68;

    private Position decodeCommandResponse(DeviceSession deviceSession, int type, ByteBuf buf) {
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        position.set(Position.KEY_TYPE, type);

        return switch (type) {
            case MSG_DEVICE_CONFIGURATION, MSG_DEVICE_VERSION, MSG_FIRMWARE_UPDATE, MSG_SMS_VIA_GPRS_RESPONSE -> {
                position.set(Position.KEY_RESULT,
                        buf.toString(buf.readerIndex(), buf.readableBytes() - 2, StandardCharsets.US_ASCII).trim());
                yield position;
            }
            case MSG_SET_IO -> {
                position.set(Position.KEY_RESULT,
                        String.valueOf(buf.readUnsignedByte()));
                yield position;
            }
            default -> null;
        };
    }

    private long readValue(ByteBuf buf, int length, boolean signed) {
        return switch (length) {
            case 1 -> signed ? buf.readByte() : buf.readUnsignedByte();
            case 2 -> signed ? buf.readShort() : buf.readUnsignedShort();
            case 4 -> signed ? buf.readInt() : buf.readUnsignedInt();
            default -> buf.readLong();
        };
    }

    private void decodeDriver(Position position, String part1, String part2) {
        Long driverIdPart1 = (Long) position.getAttributes().remove(part1);
        Long driverIdPart2 = (Long) position.getAttributes().remove(part2);
        if (driverIdPart1 != null && driverIdPart2 != null) {
            ByteBuf driverId = Unpooled.copyLong(driverIdPart1, driverIdPart2);
            position.set(Position.KEY_DRIVER_UNIQUE_ID, driverId.toString(StandardCharsets.US_ASCII));
            driverId.release();
        }
    }

    private void decodeParameter(Position position, int id, ByteBuf buf, int length) {
        switch (id) {
            case 2, 3, 4, 5 -> position.set(Position.PREFIX_IN + (id - 1), readValue(buf, length, false));
            case 13, 173 -> position.set(Position.KEY_MOTION, readValue(buf, length, false) > 0);
            case 20 -> position.set(Position.PREFIX_ADC + 3, readValue(buf, length, false));
            case 21 -> position.set(Position.PREFIX_ADC + 4, readValue(buf, length, false));
            case 22 -> position.set(Position.PREFIX_ADC + 1, readValue(buf, length, false));
            case 23 -> position.set(Position.PREFIX_ADC + 2, readValue(buf, length, false));
            case 29 -> position.set(Position.KEY_POWER, readValue(buf, length, false) * 0.001);
            case 30 -> position.set(Position.KEY_BATTERY, readValue(buf, length, false) * 0.001);
            case 32 -> position.set(Position.KEY_DEVICE_TEMP, readValue(buf, length, true));
            case 39 -> position.set(Position.KEY_ENGINE_LOAD, readValue(buf, length, false));
            case 65 -> position.set(Position.KEY_ODOMETER, readValue(buf, length, false));
            case 74 -> position.set(Position.PREFIX_TEMP + 3, readValue(buf, length, true) * 0.1);
            case 78, 79, 80 -> position.set(Position.PREFIX_TEMP + (id - 78), readValue(buf, length, true) * 0.1);
            case 88 -> {
                if (readValue(buf, length, false) > 0) {
                    position.addAlarm(Position.ALARM_JAMMING);
                }
            }
            case 94 -> position.set(Position.KEY_RPM, readValue(buf, length, false) * 0.25);
            case 95 -> position.set(Position.KEY_OBD_SPEED, readValue(buf, length, false));
            case 98 -> position.set(Position.KEY_FUEL_LEVEL, readValue(buf, length, false) * 100 / 255.0);
            case 100 -> position.set(Position.KEY_FUEL_CONSUMPTION, readValue(buf, length, false) / 20.0);
            case 134 -> {
                if (readValue(buf, length, false) > 0) {
                    position.addAlarm(Position.ALARM_BRAKING);
                }
            }
            case 136 -> {
                if (readValue(buf, length, false) > 0) {
                    position.addAlarm(Position.ALARM_ACCELERATION);
                }
            }
            case 150 -> position.set(Position.KEY_OPERATOR, readValue(buf, length, false));
            case 163 -> position.set(Position.KEY_ODOMETER, readValue(buf, length, false) * 5);
            case 164 -> position.set(Position.KEY_ODOMETER_TRIP, readValue(buf, length, false) * 5);
            case 165 -> position.set(Position.KEY_OBD_SPEED, readValue(buf, length, false) / 256.0);
            case 166, 197 -> position.set(Position.KEY_RPM, readValue(buf, length, false) * 0.125);
            case 170 -> position.set(Position.KEY_CHARGE, readValue(buf, length, false) > 0);
            case 205 -> position.set(Position.KEY_FUEL_LEVEL, readValue(buf, length, false));
            case 207 -> position.set(Position.KEY_FUEL_LEVEL, readValue(buf, length, false) * 0.4);
            case 208 -> position.set(Position.KEY_FUEL_USED, readValue(buf, length, false) * 0.5);
            case 251, 409 -> position.set(Position.KEY_IGNITION, readValue(buf, length, false) > 0);
            case 410 -> {
                if (readValue(buf, length, false) > 0) {
                    position.addAlarm(Position.ALARM_TOW);
                }
            }
            case 411 -> {
                if (readValue(buf, length, false) > 0) {
                    position.addAlarm(Position.ALARM_ACCIDENT);
                }
            }
            case 415 -> {
                if (readValue(buf, length, false) == 0) {
                    position.addAlarm(Position.ALARM_GPS_ANTENNA_CUT);
                }
            }
            case 645 -> position.set(Position.KEY_OBD_ODOMETER, readValue(buf, length, false) * 1000);
            case 758 -> {
                if (readValue(buf, length, false) == 1) {
                    position.addAlarm(Position.ALARM_TAMPERING);
                }
            }
            default -> position.set(Position.PREFIX_IO + id, readValue(buf, length, false));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedShort(); // data length

        String imei = String.format("%015d", buf.readLong());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        int type = buf.readUnsignedByte();

        if (type == MSG_RECORDS || type == MSG_EXTENDED_RECORDS) {

            List<Position> positions = new LinkedList<>();

            buf.readUnsignedByte(); // records left
            int count = buf.readUnsignedByte();

            for (int i = 0; i < count; i++) {
                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                position.setTime(new Date(buf.readUnsignedInt() * 1000));
                buf.readUnsignedByte(); // timestamp extension

                if (type == MSG_EXTENDED_RECORDS) {
                    int recordExtension = buf.readUnsignedByte();
                    int mergeRecordCount = BitUtil.from(recordExtension, 4);
                    int currentRecord = BitUtil.to(recordExtension, 4);

                    if (currentRecord > 0 && currentRecord <= mergeRecordCount) {
                        if (positions.size() == 0) {
                            getLastLocation(position, null);
                        } else {
                            position = positions.remove(positions.size() - 1);
                        }
                    }
                }

                buf.readUnsignedByte(); // priority (reserved)

                int longitude = buf.readInt();
                int latitude = buf.readInt();
                if (longitude > Integer.MIN_VALUE && latitude > Integer.MIN_VALUE) {
                    position.setValid(true);
                    position.setLongitude(longitude / 10000000.0);
                    position.setLatitude(latitude / 10000000.0);
                    position.setAltitude(buf.readUnsignedShort() / 10.0);
                    position.setCourse(buf.readUnsignedShort() / 100.0);
                    position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
                    position.set(Position.KEY_HDOP, buf.readUnsignedByte() / 10.0);
                } else {
                    buf.skipBytes(8);
                    getLastLocation(position, null);
                }

                if (type == MSG_EXTENDED_RECORDS) {
                    position.set(Position.KEY_EVENT, buf.readUnsignedShort());
                } else {
                    position.set(Position.KEY_EVENT, buf.readUnsignedByte());
                }

                // Read 1 byte data
                int valueCount = buf.readUnsignedByte();
                for (int j = 0; j < valueCount; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    decodeParameter(position, id, buf, 1);
                }

                // Read 2 byte data
                valueCount = buf.readUnsignedByte();
                for (int j = 0; j < valueCount; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    decodeParameter(position, id, buf, 2);
                }

                // Read 4 byte data
                valueCount = buf.readUnsignedByte();
                for (int j = 0; j < valueCount; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    decodeParameter(position, id, buf, 4);
                }

                // Read 8 byte data
                valueCount = buf.readUnsignedByte();
                for (int j = 0; j < valueCount; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    decodeParameter(position, id, buf, 8);
                }

                decodeDriver(position, Position.PREFIX_IO + 126, Position.PREFIX_IO + 127); // can driver
                decodeDriver(position, Position.PREFIX_IO + 155, Position.PREFIX_IO + 156); // tco driver

                Long tagIdPart1 = (Long) position.getAttributes().remove(Position.PREFIX_IO + 760);
                Long tagIdPart2 = (Long) position.getAttributes().remove(Position.PREFIX_IO + 761);
                if (tagIdPart1 != null && tagIdPart2 != null) {
                    position.set("tagId", Long.toHexString(tagIdPart1) + Long.toHexString(tagIdPart2));
                }

                positions.add(position);
            }

            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(
                        Unpooled.wrappedBuffer(DataConverter.parseHex("0002640113bc")), remoteAddress));
            }

            return positions;

        } else if (type == MSG_DTCS) {

            List<Position> positions = new LinkedList<>();

            int count = buf.readUnsignedByte();

            for (int i = 0; i < count; i++) {
                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                buf.readUnsignedByte(); // reserved

                position.setTime(new Date(buf.readUnsignedInt() * 1000));

                position.setValid(true);
                position.setLongitude(buf.readInt() / 10000000.0);
                position.setLatitude(buf.readInt() / 10000000.0);

                if (buf.readUnsignedByte() == 2) {
                    position.set(Position.KEY_ARCHIVE, true);
                }

                position.set(Position.KEY_DTCS, buf.readSlice(5).toString(StandardCharsets.US_ASCII));

                positions.add(position);
            }

            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(
                        Unpooled.wrappedBuffer(DataConverter.parseHex("00026d01c4a4")), remoteAddress));
            }

            return positions;

        } else if (type == MSG_FILES) {

            int subtype = buf.readUnsignedByte();
            int source = buf.readUnsignedByte();

            if (subtype == 2) {
                ByteBuf filename = buf.readSlice(8);
                int total = buf.readUnsignedShort();
                int current = buf.readUnsignedShort();
                if (photo == null) {
                    photo = Unpooled.buffer();
                }
                photo.writeBytes(buf.readSlice(buf.readableBytes() - 2));
                if (current < total - 1) {
                    ByteBuf content = Unpooled.buffer();
                    content.writeByte(subtype);
                    content.writeByte(source);
                    content.writeBytes(filename);
                    content.writeShort(current + 1);
                    ByteBuf response = RuptelaProtocolEncoder.encodeContent(type, content);
                    content.release();
                    if (channel != null) {
                        channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
                    }
                } else {
                    Position position = new Position(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());
                    getLastLocation(position, null);
                    position.set(Position.KEY_IMAGE, writeMediaFile(imei, photo, "jpg"));
                    photo.release();
                    photo = null;
                    return position;
                }
            }

            return null;

        } else if (type == MSG_IDENTIFICATION || type == MSG_HEARTBEAT) {

            ByteBuf content = Unpooled.buffer();
            content.writeByte(1);
            ByteBuf response = RuptelaProtocolEncoder.encodeContent(type, content);
            content.release();
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }

            return null;

        } else {

            return decodeCommandResponse(deviceSession, type, buf);

        }
    }

}
