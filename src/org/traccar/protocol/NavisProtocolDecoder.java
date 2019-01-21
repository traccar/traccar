/*
 * Copyright 2012 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Date;

public class NavisProtocolDecoder extends BaseProtocolDecoder {

    private String prefix;
    private long deviceUniqueId, serverId;
    private int flexDataSize;  // Bytes
    private int flexBitfieldDataSize;  // Bits
    private final byte[] flexBitfield;
    private byte flexProtocolVersion, flexStructVersion;
    private static final Logger LOGGER = LoggerFactory.getLogger(NavisProtocolDecoder.class);
    private static final int[] FLEX_FIELDS_SIZES = {4, 2, 4, 1, 1, 1, 1, 1, 4, 4, 4, 4, 4, 2, 4, 4, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 4, 4, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1,
        1, 2, 4, 2, 1, 4, 2, 2, 2, 2, 2, 1, 1, 1, 2, 4, 2, 1, /* FLEX 2.0 */ 8, 2, 1, 16, 4, 2, 4, 37, 1,
        1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 6, 12, 24, 48, 1, 1, 1, 1, 4, 4, 1, 4, 2, 6, 2, 6, 2,
        2, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 1};  // Bytes

    public NavisProtocolDecoder(Protocol protocol) {
        super(protocol);
        this.flexBitfield = new byte[16];
    }

    public static final int F10 = 0x01;
    public static final int F20 = 0x02;
    public static final int F30 = 0x03;
    public static final int F40 = 0x04;
    public static final int F50 = 0x05;
    public static final int F51 = 0x15;
    public static final int F52 = 0x25;
    public static final int F60 = 0x06;

    private static boolean isFormat(int type, int... types) {
        for (int i : types) {
            if (type == i) {
                return true;
            }
        }
        return false;
    }

    private static final class ParseResult {
        private final long id;
        private final Position position;

        private ParseResult(long id, Position position) {
            this.id = id;
            this.position = position;
        }

        public long getId() {
            return id;
        }

        public Position getPosition() {
            return position;
        }
    }

    private ParseResult parseNTCBPosition(DeviceSession deviceSession, ByteBuf buf) {
        Position position = new Position(getProtocolName());

        position.setDeviceId(deviceSession.getDeviceId());

        int format;
        if (buf.getUnsignedByte(buf.readerIndex()) == 0) {
            format = buf.readUnsignedShortLE();
        } else {
            format = buf.readUnsignedByte();
        }
        position.set("format", format);

        long index = buf.readUnsignedIntLE();
        position.set(Position.KEY_INDEX, index);

        position.set(Position.KEY_EVENT, buf.readUnsignedShortLE());

        buf.skipBytes(6); // Event time

        short armedStatus = buf.readUnsignedByte();
        if (isFormat(format, F10, F20, F30, F40, F50, F51, F52)) {
            position.set(Position.KEY_ARMED, armedStatus & 0x7F);
            if (BitUtil.check(armedStatus, 7)) {
                position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
            }
        } else if (isFormat(format, F60)) {
            position.set(Position.KEY_ARMED, armedStatus & 0x1);
            if (BitUtil.check(armedStatus, 1)) {
                position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
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
            position.set(Position.KEY_OUTPUT, extField & 0x3);
            position.set(Position.KEY_SATELLITES, extField >> 2);
            position.set(Position.PREFIX_OUT + 1, BitUtil.check(extField, 0));
            position.set(Position.PREFIX_OUT + 2, BitUtil.check(extField, 1));
        } else if (isFormat(format, F40, F60)) {
            short output = buf.readUnsignedByte();
            position.set(Position.KEY_OUTPUT, output & 0xF);
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
                position.set(Position.PREFIX_IN + 5, (input & 0x70) >> 4);
                position.set(Position.PREFIX_IN + 6, (input & 0x380) >> 7);
                position.set(Position.PREFIX_IN + 7, (input & 0xC00) >> 10);
                position.set(Position.PREFIX_IN + 8, (input & 0x3000) >> 12);
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
                position.set(Position.KEY_SATELLITES, navSensorState >> 2);
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

        return new ParseResult(index, position);
    }

    private Object processNTCBSingle(DeviceSession deviceSession, Channel channel, ByteBuf buf) {
        ParseResult result = parseNTCBPosition(deviceSession, buf);

        ByteBuf response = Unpooled.buffer(7);
        response.writeCharSequence("*<T", StandardCharsets.US_ASCII);
        response.writeIntLE((int) result.getId());
        sendNTCBReply(channel, response);

        if (result.getPosition().getFixTime() == null) {
            return null;
        }

        return result.getPosition();
    }

    private Object processNTCBArray(DeviceSession deviceSession, Channel channel, ByteBuf buf) {
        List<Position> positions = new LinkedList<>();
        int count = buf.readUnsignedByte();

        for (int i = 0; i < count; i++) {
            Position position = parseNTCBPosition(deviceSession, buf).getPosition();
            if (position.getFixTime() != null) {
                positions.add(position);
            }
        }

        ByteBuf response = Unpooled.buffer(7);
        response.writeCharSequence("*<A", StandardCharsets.US_ASCII);
        response.writeByte(count);
        sendNTCBReply(channel, response);

        if (positions.isEmpty()) {
            return null;
        }

        return positions;
    }

    private void skipFLEXField(int index, ByteBuf buf) {
        if (index < FLEX_FIELDS_SIZES.length) {
            buf.skipBytes(FLEX_FIELDS_SIZES[index]);
        }
    }

    private ParseResult parseFLEXPosition(DeviceSession deviceSession, ByteBuf buf) {

        Position position = new Position(getProtocolName());

        position.setDeviceId(deviceSession.getDeviceId());

        long index = 0;
        int status = 0;
        short input = 0;
        short output = 0;

        for (int i = 0; i < flexBitfieldDataSize; i++) {
            if ((flexBitfield[(int) (i / 8)] & (0x80 >> i % 8)) == 0) {
                // Skip FLEX field
                continue;
            }

            switch (i) {
                case 0:
                    index = buf.readUnsignedIntLE();
                    position.set(Position.KEY_INDEX, index);
                    break;
                case 1:
                    position.set(Position.KEY_EVENT, buf.readUnsignedShortLE());
                    break;
                case 3:
                    short armedStatus = buf.readUnsignedByte();
                    position.set(Position.KEY_ARMED, armedStatus & 0x1);
                    if (BitUtil.check(armedStatus, 1)) {
                        position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
                    }
                    break;
                case 4:
                    status = buf.readUnsignedByte();
                    position.set(Position.KEY_STATUS, status);
                    break;
                case 5:
                    int status2 = buf.readUnsignedByte();
                    position.set(Position.KEY_STATUS, (short) ((status & 0xFF) | (status2 << 8)));
                    break;
                case 6:
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    break;
                case 7:
                    int navSensorState = buf.readUnsignedByte();
                    position.setValid(BitUtil.check(navSensorState, 1));
                    position.set(Position.KEY_SATELLITES, navSensorState >> 2);
                    break;
                case 8:
                     position.setTime(new DateBuilder(new Date(buf.readUnsignedIntLE() * 1000)).getDate());
                    break;
                case 9:
                    position.setLatitude(buf.readIntLE() / 600000.0);
                    break;
                case 10:
                    position.setLongitude(buf.readIntLE() / 600000.0);
                    break;
                case 11:
                    position.setAltitude(buf.readIntLE() * 0.1);
                    break;
                case 12:
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readFloatLE()));
                    break;
                case 13:
                    position.setCourse(buf.readUnsignedShortLE());
                    break;
                case 14:
                    position.set(Position.KEY_ODOMETER, buf.readFloatLE() * 1000);
                    break;
                case 15:
                    position.set(Position.KEY_DISTANCE, buf.readFloatLE() * 1000);
                    break;
                case 18:
                    position.set(Position.KEY_POWER, buf.readUnsignedShortLE() * 0.001);
                    break;
                case 19:
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.001);
                    break;
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 26:
                case 27:
                    position.set(Position.PREFIX_ADC + (i - 19), buf.readUnsignedShortLE());
                    break;
                case 28:
                    input = buf.readUnsignedByte();
                    position.set(Position.KEY_INPUT, input);
                    for (int k = 0; k < 8; k++) {
                        position.set(Position.PREFIX_IN + (k + 1), BitUtil.check(input, k));
                    }
                    break;
                case 29:
                    short input2 = buf.readUnsignedByte();
                    position.set(Position.KEY_INPUT, (short) ((input & 0xFF) | (input2 << 8)));
                    for (int k = 0; k < 8; k++) {
                        position.set(Position.PREFIX_IN + (k + 9), BitUtil.check(input2, k));
                    }
                    break;
                case 30:
                    output = buf.readUnsignedByte();
                    position.set(Position.KEY_OUTPUT, output);
                    for (int k = 0; k < 8; k++) {
                        position.set(Position.PREFIX_OUT + (k + 1), BitUtil.check(output, k));
                    }
                    break;
                case 31:
                    short output2 = buf.readUnsignedByte();
                    position.set(Position.KEY_OUTPUT, (short) ((output & 0xFF) | (output2 << 8)));
                    for (int k = 0; k < 8; k++) {
                        position.set(Position.PREFIX_OUT + (k + 9), BitUtil.check(output2, k));
                    }
                    break;
                case 36:
                    position.set(Position.KEY_HOURS, buf.readUnsignedIntLE() * 1000);
                    break;
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                    position.set(Position.PREFIX_TEMP + (i - 43), buf.readByte());
                    break;
                case 68:  // CAN Speed
                    position.set("can-speed", buf.readUnsignedByte());
                    break;
                // FLEX 2.0
                case 69:
                    int satVisible = 0;
                    for (int k = 0; k < 8; k++) {
                        satVisible += buf.readUnsignedByte();
                    }
                    position.set(Position.KEY_SATELLITES_VISIBLE, satVisible);
                    break;
                case 70:
                    position.set(Position.KEY_HDOP, buf.readUnsignedByte() * 0.1);
                    position.set(Position.KEY_PDOP, buf.readUnsignedByte() * 0.1);
                    break;
                default:
                    skipFLEXField(i, buf);
                    break;
            }
        }

        return new ParseResult(index, position);
    }

    private ParseResult parseFLEX20Position(DeviceSession deviceSession, ByteBuf buf) {
        Position position = new Position(getProtocolName());

        position.setDeviceId(deviceSession.getDeviceId());

        long index = 0;
        int length = buf.readUnsignedShort();

        // Check buffer size and struct version
        if (length <= buf.readableBytes() && buf.readUnsignedByte() == 0x0A) {
            buf.skipBytes(1);  // Length of static part

            index = buf.readUnsignedIntLE();
            position.set(Position.KEY_INDEX, index);

            position.set(Position.KEY_EVENT, buf.readUnsignedShortLE());
            buf.skipBytes(4); // Event time

            int navSensorState = buf.readUnsignedByte();
            position.setValid(BitUtil.check(navSensorState, 1));
            position.set(Position.KEY_SATELLITES, navSensorState >> 2);

            position.setTime(new DateBuilder(new Date(buf.readUnsignedIntLE() * 1000)).getDate());
            position.setLatitude(buf.readIntLE() / 600000.0);
            position.setLongitude(buf.readIntLE() / 600000.0);
            position.setAltitude(buf.readIntLE() * 0.1);
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readFloatLE()));
            position.setCourse(buf.readUnsignedShortLE());
            position.set(Position.KEY_ODOMETER, buf.readFloatLE() * 1000);

            buf.skipBytes(length - buf.readerIndex() - 1);  // Skip unused part
        }

        return new ParseResult(index, position);
    }

    private interface FLEXPositionParser {
        ParseResult parsePosition(DeviceSession deviceSession, ByteBuf buf);
    }

    private Object processFLEXSingle(
            FLEXPositionParser parser, String flexHeader, DeviceSession deviceSession, Channel channel, ByteBuf buf) {
        buf.skipBytes(4); // Event index

        ParseResult result = parser.parsePosition(deviceSession, buf);

        ByteBuf response = Unpooled.buffer(6);
        response.writeCharSequence(flexHeader, StandardCharsets.US_ASCII);
        response.writeIntLE((int) result.getId());
        sendFLEXReply(channel, response);

        if (result.getPosition().getFixTime() == null) {
            return null;
        }

        return result.getPosition();
    }

    private Object processFLEXArray(
            FLEXPositionParser parser, String flexHeader, DeviceSession deviceSession, Channel channel, ByteBuf buf) {
        List<Position> positions = new LinkedList<>();
        int count = buf.readUnsignedByte();

        for (int i = 0; i < count; i++) {
            Position position = parser.parsePosition(deviceSession, buf).getPosition();
            if (position.getFixTime() != null) {
                positions.add(position);
            }
        }

        ByteBuf response = Unpooled.buffer(6);
        response.writeCharSequence(flexHeader, StandardCharsets.US_ASCII);
        response.writeByte(count);
        sendFLEXReply(channel, response);

        if (positions.isEmpty()) {
            return null;
        }

        return positions;
    }

    private Object processFLEXNegotiation(Channel channel, ByteBuf buf) {
        if ((byte) buf.readUnsignedByte() != (byte) 0xB0) {
            return null;
        }

        flexProtocolVersion = (byte) buf.readUnsignedByte();
        flexStructVersion = (byte) buf.readUnsignedByte();
        if (flexProtocolVersion != (byte) 0x0A && flexProtocolVersion != (byte) 0x14) {
            return null;
        }
        if (flexStructVersion != (byte) 0x0A && flexStructVersion != (byte) 0x14) {
            return null;
        }

        flexBitfieldDataSize = buf.readUnsignedByte();
        if (flexBitfieldDataSize > 122) {
            return null;
        }
        buf.readBytes(flexBitfield, 0, (int) Math.ceil((double) flexBitfieldDataSize / 8));

        flexDataSize = 0;
        for (int i = 0; i < flexBitfieldDataSize; i++) {
            if ((flexBitfield[(int) (i / 8)] & (0x80 >> i % 8)) != 0) {
                flexDataSize += FLEX_FIELDS_SIZES[i];
            }
        }

        ByteBuf response = Unpooled.buffer(9);
        response.writeCharSequence("*<FLEX", StandardCharsets.US_ASCII);
        response.writeByte(0xB0);
        response.writeByte(flexProtocolVersion);
        response.writeByte(flexStructVersion);
        sendNTCBReply(channel, response);

        return null;
    }

    private Object processHandshake(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {
        buf.skipBytes(1);  // Colon symbol
        if (getDeviceSession(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII)) != null) {
            sendNTCBReply(channel, Unpooled.copiedBuffer("*<S", StandardCharsets.US_ASCII));
        }
        return null;
    }

    private static byte xorChecksum(ByteBuf buf) {
        byte sum = 0;
        for (int i = 0; i < buf.readableBytes(); i++) {
            sum ^= buf.getUnsignedByte(i);
        }
        return sum;
    }

    private static byte src8Checksum(ByteBuf buf) {
        byte sum = (byte) 0xFF;
        for (int i = 0; i < buf.readableBytes(); i++) {
            sum ^= buf.getUnsignedByte(i);
            for (int j = 0; j < 8; j++) {
                sum = (sum & 0x80) != 0 ? (byte) ((sum << 1) ^ 0x31) : (byte) (sum << 1);
            }
        }
        return sum;
    }

    private void sendNTCBReply(Channel channel, ByteBuf data) {
        if (channel != null) {
            ByteBuf header = Unpooled.buffer(16);
            header.writeCharSequence(prefix, StandardCharsets.US_ASCII);
            header.writeIntLE((int) deviceUniqueId);
            header.writeIntLE((int) serverId);
            header.writeShortLE(data.readableBytes());
            header.writeByte(xorChecksum(data));
            header.writeByte(xorChecksum(header));

            channel.writeAndFlush(new NetworkMessage(Unpooled.wrappedBuffer(header, data), channel.remoteAddress()));
        }
    }

    private void sendFLEXReply(Channel channel, ByteBuf data) {
        if (channel != null) {
            ByteBuf cs = Unpooled.buffer(1);
            cs.writeByte(src8Checksum(data));

            channel.writeAndFlush(new NetworkMessage(Unpooled.wrappedBuffer(data, cs), channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getByte(buf.readerIndex()) == 0x7F) {
            // FLEX keep alive message
            return null;
        } else if (buf.getByte(buf.readerIndex()) == 0x7E) {  // "~"
            // FLEX message
            try {
                DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
                if (deviceSession != null) {
                    switch (buf.readShortLE()) {
                        // FLEX 1.0
                        case 0x417E:  // "~A"
                            return processFLEXArray(new FLEXPositionParser() {
                                        @Override
                                        public ParseResult parsePosition(DeviceSession deviceSession, ByteBuf buf) {
                                            return NavisProtocolDecoder.this.parseFLEXPosition(deviceSession, buf);
                                        }
                                    },
                                    "~A", deviceSession, channel, buf);
                        case 0x547E:  // "~T"
                            return processFLEXSingle(new FLEXPositionParser() {
                                        @Override
                                        public ParseResult parsePosition(DeviceSession deviceSession, ByteBuf buf) {
                                            return NavisProtocolDecoder.this.parseFLEXPosition(deviceSession, buf);
                                        }
                                    },
                                    "~T", deviceSession, channel, buf);
                        case 0x437E:  // "~C"
                            return processFLEXSingle(new FLEXPositionParser() {
                                        @Override
                                        public ParseResult parsePosition(DeviceSession deviceSession, ByteBuf buf) {
                                            return NavisProtocolDecoder.this.parseFLEXPosition(deviceSession, buf);
                                        }
                                    },
                                    "~C", deviceSession, channel, buf);
                        // FLEX 2.0 (Extra packages)
                        case 0x457E:  // "~E"
                            return processFLEXArray(new FLEXPositionParser() {
                                        @Override
                                        public ParseResult parsePosition(DeviceSession deviceSession, ByteBuf buf) {
                                            return NavisProtocolDecoder.this.parseFLEX20Position(deviceSession, buf);
                                        }
                                    },
                                    "~E", deviceSession, channel, buf);
                        case 0x587E:  // "~X"
                            return processFLEXSingle(new FLEXPositionParser() {
                                        @Override
                                        public ParseResult parsePosition(DeviceSession deviceSession, ByteBuf buf) {
                                            return NavisProtocolDecoder.this.parseFLEX20Position(deviceSession, buf);
                                        }
                                    },
                                    "~X", deviceSession, channel, buf);
                        default:
                            break;
                    }
                }
            } catch (IndexOutOfBoundsException error) {
                LOGGER.warn("Navis FLEX message parsing error", error);
            }
        } else {
            // NTCB message
            prefix = buf.toString(buf.readerIndex(), 4, StandardCharsets.US_ASCII);
            buf.skipBytes(prefix.length());  // Prefix @NTC by default
            serverId = buf.readUnsignedIntLE();
            deviceUniqueId = buf.readUnsignedIntLE();
            int length = buf.readUnsignedShortLE();
            buf.skipBytes(2);  // Header and data XOR checksum

            if (length == 0) {
                return null;  // Keep alive message
            }

            int type = buf.getIntLE(buf.readerIndex());
            buf.skipBytes(3);
            if ((type & 0xFFFFFF) == 0x533E2AL) {  // "*>S"
                return processHandshake(channel, remoteAddress, buf);
            } else {
                DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
                if (deviceSession != null) {
                    try {
                        switch (type & 0xFFFFFF) {
                            case 0x413E2A:  // "*>A"
                                return processNTCBArray(deviceSession, channel, buf);
                            case 0x543E2A:  // "*>T"
                                return processNTCBSingle(deviceSession, channel, buf);
                            case 0x463E2A:  // "*>F" (*>FLEX)
                                buf.skipBytes(3);
                                return processFLEXNegotiation(channel, buf);
                            default:
                                break;
                        }
                    } catch (IndexOutOfBoundsException error) {
                        LOGGER.warn("Navis NTCB message parsing error", error);
                    }
                }
            }
        }

        return null;
    }

    public int getFLEXDataSize() {
        return flexDataSize;
    }
}
