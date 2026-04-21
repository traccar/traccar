/*
 * Copyright 2012 - 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Date;

public class NavisProtocolDecoder extends BaseProtocolDecoder {

    private static final int[] FLEX_FIELDS_SIZES = {
            4, 2, 4, 1, 1, 1, 1, 1, 4, 4, 4, 4, 4, 2, 4, 4, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 4, 4, 2, 2,
            4, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 2, 4, 2, 1, 4, 2, 2, 2, 2, 2, 1, 1, 1, 2, 4, 2, 1,
            /* FLEX 2.0 */
            8, 2, 1, 16, 4, 2, 4, 37, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 6, 12, 24, 48, 1, 1, 1, 1, 4, 4,
            1, 4, 2, 6, 2, 6, 2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 1
    };

    private String prefix;
    private long deviceUniqueId, serverId;
    private int flexDataSize;
    private int flexBitFieldSize;
    private final byte[] flexBitField = new byte[16];

    public NavisProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int F10 = 0x01;
    public static final int F20 = 0x02;
    public static final int F30 = 0x03;
    public static final int F40 = 0x04;
    public static final int F50 = 0x05;
    public static final int F51 = 0x15;
    public static final int F52 = 0x25;
    public static final int F60 = 0x06;

    public int getFlexDataSize() {
        return flexDataSize;
    }

    private static boolean isFormat(int type, int... types) {
        for (int i : types) {
            if (type == i) {
                return true;
            }
        }
        return false;
    }

    private Position parseNtcbPosition(DeviceSession deviceSession, ByteBuf buf) {
        Position position = new Position(getProtocolName());

        position.setDeviceId(deviceSession.getDeviceId());

        int format;
        if (buf.getUnsignedByte(buf.readerIndex()) == 0) {
            format = buf.readUnsignedShortLE();
        } else {
            format = buf.readUnsignedByte();
        }
        position.set("format", format);

        position.set(Position.KEY_INDEX, buf.readUnsignedIntLE());
        position.set(Position.KEY_EVENT, buf.readUnsignedShortLE());

        buf.skipBytes(6); // event time

        short armedStatus = buf.readUnsignedByte();
        if (isFormat(format, F10, F20, F30, F40, F50, F51, F52)) {
            position.set(Position.KEY_ARMED, BitUtil.to(armedStatus, 7));
            if (BitUtil.check(armedStatus, 7)) {
                position.addAlarm(Position.ALARM_GENERAL);
            }
        } else if (isFormat(format, F60)) {
            position.set(Position.KEY_ARMED, BitUtil.check(armedStatus, 0));
            if (BitUtil.check(armedStatus, 1)) {
                position.addAlarm(Position.ALARM_GENERAL);
            }
        }

        position.set(Position.KEY_STATUS, buf.readUnsignedByte());
        position.set(Position.KEY_RSSI, buf.readUnsignedByte());

        if (isFormat(format, F10, F20, F30)) {
            int output = buf.readUnsignedShortLE();
            position.set(Position.KEY_OUTPUT, output);
            for (int i = 0; i < 16; i++) {
                position.set(Position.PREFIX_OUT + (i + 1), BitUtil.check(output, i));
            }
        } else if (isFormat(format, F50, F51, F52)) {
            short extField = buf.readUnsignedByte();
            position.set(Position.KEY_OUTPUT, BitUtil.to(extField, 2));
            position.set(Position.PREFIX_OUT + 1, BitUtil.check(extField, 0));
            position.set(Position.PREFIX_OUT + 2, BitUtil.check(extField, 1));
            position.set(Position.KEY_SATELLITES, BitUtil.from(extField, 2));
        } else if (isFormat(format, F40, F60)) {
            short output = buf.readUnsignedByte();
            position.set(Position.KEY_OUTPUT, BitUtil.to(output, 4));
            for (int i = 0; i < 4; i++) {
                position.set(Position.PREFIX_OUT + (i + 1), BitUtil.check(output, i));
            }
        }

        if (isFormat(format, F10, F20, F30, F40)) {
            int input = buf.readUnsignedShortLE();
            position.set(Position.KEY_INPUT, input);
            if (!isFormat(format, F40)) {
                for (int i = 0; i < 16; i++) {
                    position.set(Position.PREFIX_IN + (i + 1), BitUtil.check(input, i));
                }
            } else {
                position.set(Position.PREFIX_IN + 1, BitUtil.check(input, 0));
                position.set(Position.PREFIX_IN + 2, BitUtil.check(input, 1));
                position.set(Position.PREFIX_IN + 3, BitUtil.check(input, 2));
                position.set(Position.PREFIX_IN + 4, BitUtil.check(input, 3));
                position.set(Position.PREFIX_IN + 5, BitUtil.between(input, 4, 7));
                position.set(Position.PREFIX_IN + 6, BitUtil.between(input, 7, 10));
                position.set(Position.PREFIX_IN + 7, BitUtil.between(input, 10, 12));
                position.set(Position.PREFIX_IN + 8, BitUtil.between(input, 12, 14));
            }
        } else if (isFormat(format, F50, F51, F52, F60)) {
            short input = buf.readUnsignedByte();
            position.set(Position.KEY_INPUT, input);
            for (int i = 0; i < 8; i++) {
                position.set(Position.PREFIX_IN + (i + 1), BitUtil.check(input, i));
            }
        }

        position.set(Position.KEY_POWER, buf.readUnsignedShortLE() * 0.001);
        position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.001);

        if (isFormat(format, F10, F20, F30)) {
            position.set(Position.PREFIX_TEMP + 1, buf.readShortLE());
        }

        if (isFormat(format, F10, F20, F50, F51, F52, F60)) {
            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShortLE());
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShortLE());
        }
        if (isFormat(format, F60)) {
            position.set(Position.PREFIX_ADC + 3, buf.readUnsignedShortLE());
        }

        // Impulse counters
        if (isFormat(format, F20, F50, F51, F52, F60)) {
            buf.readUnsignedIntLE();
            buf.readUnsignedIntLE();
        }

        if (isFormat(format, F60)) {
            // Fuel
            buf.readUnsignedShortLE();
            buf.readUnsignedShortLE();
            buf.readByte();
            buf.readShortLE();
            buf.readByte();
            buf.readUnsignedShortLE();
            buf.readByte();
            buf.readUnsignedShortLE();
            buf.readByte();
            buf.readUnsignedShortLE();
            buf.readByte();
            buf.readUnsignedShortLE();
            buf.readByte();
            buf.readUnsignedShortLE();
            buf.readByte();
            buf.readUnsignedShortLE();
            buf.readByte();
            buf.readUnsignedShortLE();

            position.set(Position.PREFIX_TEMP + 1, buf.readByte());
            position.set(Position.PREFIX_TEMP + 2, buf.readByte());
            position.set(Position.PREFIX_TEMP + 3, buf.readByte());
            position.set(Position.PREFIX_TEMP + 4, buf.readByte());
            position.set(Position.KEY_AXLE_WEIGHT, buf.readIntLE());
            position.set(Position.KEY_RPM, buf.readUnsignedShortLE());
        }

        if (isFormat(format, F20, F50, F51, F52, F60)) {
            int navSensorState = buf.readUnsignedByte();
            position.setValid(BitUtil.check(navSensorState, 1));
            if (isFormat(format, F60)) {
                position.set(Position.KEY_SATELLITES, BitUtil.from(navSensorState, 2));
            }

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setDateReverse(buf.readUnsignedByte(), buf.readUnsignedByte() + 1, buf.readUnsignedByte());
            position.setTime(dateBuilder.getDate());

            if (isFormat(format, F60)) {
                position.setLatitude(buf.readIntLE() / 600000.0);
                position.setLongitude(buf.readIntLE() / 600000.0);
                position.setAltitude(buf.readIntLE() * 0.1);
            } else {
                position.setLatitude(buf.readFloatLE() / Math.PI * 180);
                position.setLongitude(buf.readFloatLE() / Math.PI * 180);
            }

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readFloatLE()));
            position.setCourse(buf.readUnsignedShortLE());

            position.set(Position.KEY_ODOMETER, buf.readFloatLE() * 1000);
            position.set(Position.KEY_DISTANCE, buf.readFloatLE() * 1000);

            // Segment times
            buf.readUnsignedShortLE();
            buf.readUnsignedShortLE();
        }

        // Other
        if (isFormat(format, F51, F52)) {
            buf.readUnsignedShortLE();
            buf.readByte();
            buf.readUnsignedShortLE();
            buf.readUnsignedShortLE();
            buf.readByte();
            buf.readUnsignedShortLE();
            buf.readUnsignedShortLE();
            buf.readByte();
            buf.readUnsignedShortLE();
        }

        // Four temperature sensors
        if (isFormat(format, F40, F52)) {
            position.set(Position.PREFIX_TEMP + 1, buf.readByte());
            position.set(Position.PREFIX_TEMP + 2, buf.readByte());
            position.set(Position.PREFIX_TEMP + 3, buf.readByte());
            position.set(Position.PREFIX_TEMP + 4, buf.readByte());
        }

        return position;
    }

    private Object processNtcbSingle(DeviceSession deviceSession, Channel channel, ByteBuf buf) {
        Position position = parseNtcbPosition(deviceSession, buf);

        ByteBuf response = Unpooled.buffer(7);
        response.writeCharSequence("*<T", StandardCharsets.US_ASCII);
        response.writeIntLE((int) position.getLong(Position.KEY_INDEX));
        sendNtcbReply(channel, response);

        return position.getFixTime() != null ? position : null;
    }

    private Object processNtcbArray(DeviceSession deviceSession, Channel channel, ByteBuf buf) {
        List<Position> positions = new LinkedList<>();
        int count = buf.readUnsignedByte();

        for (int i = 0; i < count; i++) {
            Position position = parseNtcbPosition(deviceSession, buf);
            if (position.getFixTime() != null) {
                positions.add(position);
            }
        }

        ByteBuf response = Unpooled.buffer(7);
        response.writeCharSequence("*<A", StandardCharsets.US_ASCII);
        response.writeByte(count);
        sendNtcbReply(channel, response);

        if (positions.isEmpty()) {
            return null;
        }

        return positions;
    }

    private boolean checkFlexBitfield(int index) {
        int byteIndex = Math.floorDiv(index, 8);
        int bitIndex = Math.floorMod(index, 8);
        return BitUtil.check(flexBitField[byteIndex], 7 - bitIndex);
    }

    private Position parseFlexPosition(DeviceSession deviceSession, ByteBuf buf) {

        Position position = new Position(getProtocolName());

        position.setDeviceId(deviceSession.getDeviceId());

        int status = 0;
        short input = 0;
        short output = 0;

        for (int i = 0; i < flexBitFieldSize; i++) {
            if (!checkFlexBitfield(i)) {
                continue;
            }

            switch (i) {
                case 0 -> position.set(Position.KEY_INDEX, buf.readUnsignedIntLE());
                case 1 -> position.set(Position.KEY_EVENT, buf.readUnsignedShortLE());
                case 3 -> {
                    short armedStatus = buf.readUnsignedByte();
                    position.set(Position.KEY_ARMED, BitUtil.check(armedStatus, 0));
                    if (BitUtil.check(armedStatus, 1)) {
                        position.addAlarm(Position.ALARM_GENERAL);
                    }
                }
                case 4 -> {
                    status = buf.readUnsignedByte();
                    position.set(Position.KEY_STATUS, status);
                }
                case 5 -> {
                    int status2 = buf.readUnsignedByte();
                    position.set(Position.KEY_STATUS, (short) (BitUtil.to(status, 8) | (status2 << 8)));
                }
                case 6 -> position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                case 7 -> {
                    int navSensorState = buf.readUnsignedByte();
                    position.setValid(BitUtil.check(navSensorState, 1));
                    position.set(Position.KEY_SATELLITES, BitUtil.from(navSensorState, 2));
                }
                case 8 -> position.setTime(new DateBuilder(new Date(buf.readUnsignedIntLE() * 1000)).getDate());
                case 9 -> position.setLatitude(buf.readIntLE() / 600000.0);
                case 10 -> position.setLongitude(buf.readIntLE() / 600000.0);
                case 11 -> position.setAltitude(buf.readIntLE() * 0.1);
                case 12 -> position.setSpeed(UnitsConverter.knotsFromKph(buf.readFloatLE()));
                case 13 -> position.setCourse(buf.readUnsignedShortLE());
                case 14 -> position.set(Position.KEY_ODOMETER, buf.readFloatLE() * 1000);
                case 15 -> position.set(Position.KEY_DISTANCE, buf.readFloatLE() * 1000);
                case 18 -> position.set(Position.KEY_POWER, buf.readUnsignedShortLE() * 0.001);
                case 19 -> position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.001);
                case 20, 21, 22, 23, 24, 25, 26, 27 ->
                        position.set(Position.PREFIX_ADC + (i - 19), buf.readUnsignedShortLE());
                case 28 -> {
                    input = buf.readUnsignedByte();
                    position.set(Position.KEY_INPUT, input);
                    for (int k = 0; k < 8; k++) {
                        position.set(Position.PREFIX_IN + (k + 1), BitUtil.check(input, k));
                    }
                }
                case 29 -> {
                    short input2 = buf.readUnsignedByte();
                    position.set(Position.KEY_INPUT, (short) (BitUtil.to(input, 8) | (input2 << 8)));
                    for (int k = 0; k < 8; k++) {
                        position.set(Position.PREFIX_IN + (k + 9), BitUtil.check(input2, k));
                    }
                }
                case 30 -> {
                    output = buf.readUnsignedByte();
                    position.set(Position.KEY_OUTPUT, output);
                    for (int k = 0; k < 8; k++) {
                        position.set(Position.PREFIX_OUT + (k + 1), BitUtil.check(output, k));
                    }
                }
                case 31 -> {
                    short output2 = buf.readUnsignedByte();
                    position.set(Position.KEY_OUTPUT, (short) (BitUtil.to(output, 8) | (output2 << 8)));
                    for (int k = 0; k < 8; k++) {
                        position.set(Position.PREFIX_OUT + (k + 9), BitUtil.check(output2, k));
                    }
                }
                case 36 -> position.set(Position.KEY_HOURS, buf.readUnsignedIntLE() * 1000);
                case 44, 45, 46, 47, 48, 49, 50, 51 -> position.set(Position.PREFIX_TEMP + (i - 43), buf.readByte());
                case 68 -> position.set("can-speed", buf.readUnsignedByte());

                // FLEX 2.0
                case 69 -> {
                    int satVisible = 0;
                    for (int k = 0; k < 8; k++) {
                        satVisible += buf.readUnsignedByte();
                    }
                    position.set(Position.KEY_SATELLITES_VISIBLE, satVisible);
                }
                case 70 -> {
                    position.set(Position.KEY_HDOP, buf.readUnsignedByte() * 0.1);
                    position.set(Position.KEY_PDOP, buf.readUnsignedByte() * 0.1);
                }
                default -> {
                    if (i < FLEX_FIELDS_SIZES.length) {
                        buf.skipBytes(FLEX_FIELDS_SIZES[i]);
                    }
                }
            }
        }

        return position;
    }

    private Position parseFlex20Position(DeviceSession deviceSession, ByteBuf buf) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        int length = buf.readUnsignedShort();
        if (length <= buf.readableBytes() && buf.readUnsignedByte() == 0x0A) {

            buf.readUnsignedByte(); // length of static part

            position.set(Position.KEY_INDEX, buf.readUnsignedIntLE());

            position.set(Position.KEY_EVENT, buf.readUnsignedShortLE());
            buf.readUnsignedInt(); // event time

            int navSensorState = buf.readUnsignedByte();
            position.setValid(BitUtil.check(navSensorState, 1));
            position.set(Position.KEY_SATELLITES, BitUtil.from(navSensorState, 2));

            position.setTime(new DateBuilder(new Date(buf.readUnsignedIntLE() * 1000)).getDate());
            position.setLatitude(buf.readIntLE() / 600000.0);
            position.setLongitude(buf.readIntLE() / 600000.0);
            position.setAltitude(buf.readIntLE() * 0.1);
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readFloatLE()));
            position.setCourse(buf.readUnsignedShortLE());
            position.set(Position.KEY_ODOMETER, buf.readFloatLE() * 1000);

            buf.skipBytes(length - buf.readerIndex() - 1); // skip unused part
        }

        return position;
    }

    private interface FlexPositionParser {
        Position parsePosition(DeviceSession deviceSession, ByteBuf buf);
    }

    private Object processFlexSingle(
            FlexPositionParser parser, String flexHeader, DeviceSession deviceSession, Channel channel, ByteBuf buf) {

        if (!flexHeader.equals("~C")) {
            buf.readUnsignedInt(); // event index
        }

        Position position = parser.parsePosition(deviceSession, buf);

        ByteBuf response = Unpooled.buffer();
        response.writeCharSequence(flexHeader, StandardCharsets.US_ASCII);
        response.writeIntLE((int) position.getLong(Position.KEY_INDEX));
        sendFlexReply(channel, response);

        return position.getFixTime() != null ? position : null;
    }

    private Object processFlexArray(
            FlexPositionParser parser, String flexHeader, DeviceSession deviceSession, Channel channel, ByteBuf buf) {

        List<Position> positions = new LinkedList<>();
        int count = buf.readUnsignedByte();

        for (int i = 0; i < count; i++) {
            Position position = parser.parsePosition(deviceSession, buf);
            if (position.getFixTime() != null) {
                positions.add(position);
            }
        }

        ByteBuf response = Unpooled.buffer();
        response.writeCharSequence(flexHeader, StandardCharsets.US_ASCII);
        response.writeByte(count);
        sendFlexReply(channel, response);

        return !positions.isEmpty() ? positions : null;
    }

    private Object processFlexNegotiation(Channel channel, ByteBuf buf) {
        if ((byte) buf.readUnsignedByte() != (byte) 0xB0) {
            return null;
        }

        short flexProtocolVersion = buf.readUnsignedByte();
        short flexStructVersion = buf.readUnsignedByte();
        if ((flexProtocolVersion == 0x0A || flexProtocolVersion == 0x14)
            && (flexStructVersion == 0x0A || flexStructVersion == 0x14)) {

            flexBitFieldSize = buf.readUnsignedByte();
            if (flexBitFieldSize > 122) {
                return null;
            }

            buf.readBytes(flexBitField, 0, (int) Math.ceil((double) flexBitFieldSize / 8));

            flexDataSize = 0;
            for (int i = 0; i < flexBitFieldSize; i++) {
                if (checkFlexBitfield(i)) {
                    flexDataSize += FLEX_FIELDS_SIZES[i];
                }
            }
        } else {
            flexProtocolVersion = 0x14;
            flexStructVersion = 0x14;
        }

        ByteBuf response = Unpooled.buffer(9);
        response.writeCharSequence("*<FLEX", StandardCharsets.US_ASCII);
        response.writeByte(0xB0);
        response.writeByte(flexProtocolVersion);
        response.writeByte(flexStructVersion);
        sendNtcbReply(channel, response);

        return null;
    }

    private Object processHandshake(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {
        buf.readByte(); // colon
        if (getDeviceSession(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII)) != null) {
            sendNtcbReply(channel, Unpooled.copiedBuffer("*<S", StandardCharsets.US_ASCII));
        }
        return null;
    }

    private void sendNtcbReply(Channel channel, ByteBuf data) {
        if (channel != null) {
            ByteBuf header = Unpooled.buffer(16);
            header.writeCharSequence(prefix, StandardCharsets.US_ASCII);
            header.writeIntLE((int) deviceUniqueId);
            header.writeIntLE((int) serverId);
            header.writeShortLE(data.readableBytes());
            header.writeByte(Checksum.xor(data.nioBuffer()));
            header.writeByte(Checksum.xor(header.nioBuffer()));

            channel.writeAndFlush(new NetworkMessage(Unpooled.wrappedBuffer(header, data), channel.remoteAddress()));
        }
    }

    private void sendFlexReply(Channel channel, ByteBuf data) {
        if (channel != null) {
            data.writeByte(Checksum.crc8(Checksum.CRC8_EGTS, data.nioBuffer()));
            channel.writeAndFlush(new NetworkMessage(data, channel.remoteAddress()));
        }
    }

    private Object decodeNtcb(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        prefix = buf.toString(buf.readerIndex(), 4, StandardCharsets.US_ASCII);
        buf.skipBytes(prefix.length()); // prefix @NTC by default
        serverId = buf.readUnsignedIntLE();
        deviceUniqueId = buf.readUnsignedIntLE();
        int length = buf.readUnsignedShortLE();
        buf.skipBytes(2); // header and data XOR checksum

        if (length == 0) {
            return null; // keep alive message
        }

        String type = buf.toString(buf.readerIndex(), 3, StandardCharsets.US_ASCII);
        buf.skipBytes(type.length());

        if (type.equals("*>S")) {
            return processHandshake(channel, remoteAddress, buf);
        } else {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession != null) {
                return switch (type) {
                    case "*>A" -> processNtcbArray(deviceSession, channel, buf);
                    case "*>T" -> processNtcbSingle(deviceSession, channel, buf);
                    case "*>F" -> {
                        buf.skipBytes(3);
                        yield processFlexNegotiation(channel, buf);
                    }
                    default -> null;
                };
            }
        }

        return null;
    }

    private Object decodeFlex(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        if (buf.getByte(buf.readerIndex()) == 0x7F) {
            return null; // keep alive
        }

        String type = buf.toString(buf.readerIndex(), 2, StandardCharsets.US_ASCII);
        buf.skipBytes(type.length());

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession != null) {
            return switch (type) {
                // FLEX 1.0
                case "~A" -> processFlexArray(this::parseFlexPosition, type, deviceSession, channel, buf);
                case "~T", "~C" -> processFlexSingle(this::parseFlexPosition, type, deviceSession, channel, buf);
                // FLEX 2.0 (extra packages)
                case "~E" -> processFlexArray(this::parseFlex20Position, type, deviceSession, channel, buf);
                case "~X" -> processFlexSingle(this::parseFlex20Position, type, deviceSession, channel, buf);
                default -> null;
            };
        }
        return null;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (flexDataSize > 0) {
            return decodeFlex(channel, remoteAddress, buf);
        } else {
            return decodeNtcb(channel, remoteAddress, buf);
        }
    }

}
