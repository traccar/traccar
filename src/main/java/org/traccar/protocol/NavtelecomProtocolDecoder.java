/*
 * Copyright 2021 - 2024 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NavtelecomProtocolDecoder extends BaseProtocolDecoder {

    public NavtelecomProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Map<Integer, Integer> ITEM_LENGTH_MAP = new HashMap<>();

    static {
        int[] l1 = {
                4, 5, 6, 7, 8, 29, 30, 31, 32, 45, 46, 47, 48, 49, 50, 51, 52, 56, 63, 64, 65, 69, 72, 78, 79, 80, 81,
                82, 83, 98, 99, 101, 104, 118, 122, 123, 124, 125, 126, 139, 140, 144, 145, 167, 168, 169, 170, 199,
                202, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222
        };
        int[] l2 = {
                2, 14, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 35, 36, 38, 39, 40, 41, 42, 43, 44, 53, 55, 58,
                59, 60, 61, 62, 66, 68, 71, 75, 100, 106, 108, 110, 111, 112, 113, 114, 115, 116, 117, 119, 120, 121,
                133, 134, 135, 136, 137, 138, 141, 143, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159,
                160, 161, 162, 163, 164, 165, 166, 171, 175, 177, 178, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189,
                190, 191, 192, 200, 201, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237
        };
        int[] l3 = {
                84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 142, 146, 198
        };
        int[] l4 = {
                1, 3, 9, 10, 11, 12, 13, 15, 16, 33, 34, 37, 54, 57, 67, 74, 76, 102, 103, 105, 127, 128, 129, 130, 131,
                132, 172, 173, 174, 176, 179, 193, 194, 195, 196, 203, 205, 206, 238, 239, 240, 241, 242, 243, 244, 245,
                246, 247, 248, 249, 250, 251, 252
        };
        for (int i : l1) {
            ITEM_LENGTH_MAP.put(i, 1);
        }
        for (int i : l2) {
            ITEM_LENGTH_MAP.put(i, 2);
        }
        for (int i : l3) {
            ITEM_LENGTH_MAP.put(i, 3);
        }
        for (int i : l4) {
            ITEM_LENGTH_MAP.put(i, 4);
        }
        ITEM_LENGTH_MAP.put(70, 8);
        ITEM_LENGTH_MAP.put(73, 16);
        ITEM_LENGTH_MAP.put(77, 37);
        ITEM_LENGTH_MAP.put(94, 6);
        ITEM_LENGTH_MAP.put(95, 12);
        ITEM_LENGTH_MAP.put(96, 24);
        ITEM_LENGTH_MAP.put(97, 48);
        ITEM_LENGTH_MAP.put(107, 6);
        ITEM_LENGTH_MAP.put(109, 6);
        ITEM_LENGTH_MAP.put(197, 6);
        ITEM_LENGTH_MAP.put(204, 5);
        ITEM_LENGTH_MAP.put(253, 8);
        ITEM_LENGTH_MAP.put(254, 8);
        ITEM_LENGTH_MAP.put(255, 8);
    }

    private BitSet bits;

    public static int getItemLength(int id) {
        Integer length = ITEM_LENGTH_MAP.get(id);
        if (length == null) {
            throw new IllegalArgumentException(String.format("Unknown item: %d", id));
        }
        return length;
    }

    public BitSet getBits() {
        return bits;
    }

    static ByteBuf encodeContent(int receiver, int sender, ByteBuf content) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeCharSequence("@NTC", StandardCharsets.US_ASCII);
        buf.writeIntLE(receiver);
        buf.writeIntLE(sender);
        buf.writeShortLE(content.readableBytes());
        buf.writeByte(Checksum.xor(content.nioBuffer()));
        buf.writeByte(Checksum.xor(buf.nioBuffer()));
        buf.writeBytes(content);
        return buf;
    }

    private void sendResponse(
            Channel channel, SocketAddress remoteAddress, int receiver, int sender, ByteBuf content) {
        if (channel != null) {
            ByteBuf response = encodeContent(sender, receiver, content);
            content.release();
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getByte(buf.readerIndex()) == '@') {

            buf.skipBytes(4); // preamble
            int receiver = buf.readIntLE();
            int sender = buf.readIntLE();
            int length = buf.readUnsignedShortLE();
            buf.readUnsignedByte(); // data checksum
            buf.readUnsignedByte(); // header checksum

            String type = buf.toString(buf.readerIndex(), 6, StandardCharsets.US_ASCII);

            if (type.startsWith("*>S")) {

                String sentence = buf.readCharSequence(length, StandardCharsets.US_ASCII).toString();
                getDeviceSession(channel, remoteAddress, sentence.substring(4));

                ByteBuf payload = Unpooled.copiedBuffer("*<S", StandardCharsets.US_ASCII);

                sendResponse(channel, remoteAddress, receiver, sender, payload);

            } else if (type.startsWith("*>FLEX")) {

                buf.skipBytes(6);

                ByteBuf payload = Unpooled.buffer();
                payload.writeCharSequence("*<FLEX", StandardCharsets.US_ASCII);
                payload.writeByte(buf.readUnsignedByte()); // protocol
                payload.writeByte(buf.readUnsignedByte()); // protocol version
                payload.writeByte(buf.readUnsignedByte()); // struct version

                int bitCount = buf.readUnsignedByte();
                bits = new BitSet((bitCount + 7) / 8);

                int currentByte = 0;
                for (int i = 0; i < bitCount; i++) {
                    if (i % 8 == 0) {
                        currentByte = buf.readUnsignedByte();
                    }
                    bits.set(i, BitUtil.check(currentByte, 7 - i % 8));
                }

                sendResponse(channel, remoteAddress, receiver, sender, payload);

            }

        } else {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            String type = buf.readCharSequence(2, StandardCharsets.US_ASCII).toString();

            if (type.equals("~A")) {

                int count = buf.readUnsignedByte();
                List<Position> positions = new LinkedList<>();

                for (int i = 0; i < count; i++) {
                    Position position = new Position(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());

                    for (int j = 1; j <= bits.length(); j++) {
                        if (bits.get(j - 1)) {

                            int value;

                            switch (j) {
                                case 1:
                                    position.set(Position.KEY_INDEX, buf.readUnsignedIntLE());
                                    break;
                                case 2:
                                    position.set(Position.KEY_EVENT, buf.readUnsignedShortLE());
                                    break;
                                case 3:
                                    position.setDeviceTime(new Date(buf.readUnsignedIntLE() * 1000));
                                    break;
                                case 4:
                                    value = buf.readUnsignedByte();
                                    position.addAlarm(BitUtil.check(value, 2) ? Position.ALARM_GENERAL : null);
                                    int guardMode = BitUtil.between(value, 3, 4);
                                    position.set(Position.KEY_ARMED, (0 < guardMode) && (guardMode < 3));
                                    break;
                                case 5:
                                    value = buf.readUnsignedByte();
                                    position.set(Position.KEY_ROAMING, BitUtil.check(value, 6) ? true : null);
                                    break;
                                case 8:
                                    value = buf.readUnsignedByte();
                                    position.setValid(BitUtil.check(value, 1));
                                    position.set(Position.KEY_SATELLITES, BitUtil.from(value, 2));
                                    break;
                                case 9:
                                    position.setFixTime(new Date(buf.readUnsignedIntLE() * 1000));
                                    break;
                                case 10:
                                    position.setLatitude(buf.readIntLE() * 0.0001 / 60);
                                    break;
                                case 11:
                                    position.setLongitude(buf.readIntLE() * 0.0001 / 60);
                                    break;
                                case 12:
                                    position.setAltitude(buf.readIntLE() * 0.1);
                                    break;
                                case 13:
                                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readFloatLE()));
                                    break;
                                case 14:
                                    position.setCourse(buf.readUnsignedShortLE());
                                    break;
                                case 15:
                                    position.set(Position.KEY_ODOMETER, buf.readFloatLE() * 1000);
                                    break;
                                case 19:
                                    position.set(Position.KEY_POWER, buf.readShortLE() * 0.001);
                                    break;
                                case 20:
                                    position.set(Position.KEY_BATTERY, buf.readShortLE() * 0.001);
                                    break;
                                case 21:
                                case 22:
                                case 23:
                                case 24:
                                case 25:
                                case 26:
                                    position.set(Position.PREFIX_ADC + (j + 1 - 21), buf.readUnsignedShortLE() * 0.001);
                                    break;
                                case 29:
                                    value = buf.readUnsignedByte();
                                    for (int k = 0; k <= 7; k++) {
                                        position.set(Position.PREFIX_IN + (k + 1), BitUtil.check(value, k));
                                    }
                                    break;
                                case 31:
                                    value = buf.readUnsignedByte();
                                    for (int k = 0; k <= 3; k++) {
                                        position.set(Position.PREFIX_OUT + (k + 1), BitUtil.check(value, k));
                                    }
                                    break;
                                case 33:
                                case 34:
                                    position.set(Position.PREFIX_COUNT + (j + 1 - 33), buf.readUnsignedIntLE());
                                    break;
                                case 35:
                                case 36:
                                    position.set("freq" + (j + 1 - 35), buf.readUnsignedShortLE());
                                    break;
                                case 37:
                                    position.set(Position.KEY_HOURS, buf.readUnsignedIntLE() * 1000);
                                    break;
                                case 38:
                                case 39:
                                case 40:
                                case 41:
                                case 42:
                                case 43:
                                    value = buf.readUnsignedShortLE();
                                    position.set(
                                            Position.KEY_FUEL_LEVEL + (j + 1 - 38), (value < 65500) ? value : null);
                                    break;
                                case 44:
                                    value = buf.readUnsignedShortLE();
                                    position.set(Position.KEY_FUEL_LEVEL, (value < 65500) ? value : null);
                                    break;
                                case 45:
                                case 46:
                                case 47:
                                case 48:
                                case 49:
                                case 50:
                                case 51:
                                case 52:
                                    value = buf.readByte();
                                    position.set(
                                            Position.PREFIX_TEMP + (j + 1 - 45), (value != (byte) 0x80) ? value : null);
                                    break;
                                case 53:
                                    value = buf.readUnsignedShortLE();
                                    if (value != 0x7FFF) {
                                        if (BitUtil.check(value, 15)) {
                                            position.set("obdFuelLevel", BitUtil.to(value, 14));
                                        } else {
                                            position.set("obdFuel", BitUtil.to(value, 14) * 0.1);
                                        }
                                    }
                                    break;
                                case 54:
                                    double fuelUsed = buf.readFloatLE() * 0.5;
                                    position.set(Position.KEY_FUEL_USED, (fuelUsed >= 0) ? fuelUsed : null);
                                    break;
                                case 55:
                                    value = buf.readUnsignedShortLE();
                                    position.set(Position.KEY_RPM, (value != 0xFFFF) ? value : null);
                                    break;
                                case 56:
                                    value = buf.readByte();
                                    position.set(Position.KEY_COOLANT_TEMP, (value != (byte) 0x80) ? value : null);
                                    break;
                                case 57:
                                    position.set(Position.KEY_OBD_ODOMETER, buf.readFloatLE() * 1000);
                                    break;
                                case 58:
                                case 59:
                                case 60:
                                case 61:
                                case 62:
                                    value = buf.readUnsignedShortLE();
                                    position.set("axleWeight" + (j + 1 - 58), (value != 0xFFFF) ? value : null);
                                    break;
                                case 63:
                                    value = buf.readUnsignedByte();
                                    position.set("acceleratorPosition", (value != 0xFF) ? value : null);
                                    break;
                                case 64:
                                    value = buf.readUnsignedByte();
                                    position.set("brakePosition", (value != 0xFF) ? value : null);
                                    break;
                                case 65:
                                    value = buf.readUnsignedByte();
                                    position.set(Position.KEY_ENGINE_LOAD, (value != 0xFF) ? value : null);
                                    break;
                                case 66:
                                    value = buf.readUnsignedShortLE();
                                    if (value != 0x7FFF) {
                                        if (BitUtil.check(value, 15)) {
                                            position.set("obdAdBlueLevel", BitUtil.to(value, 14));
                                        } else {
                                            position.set("obdAdBlue", BitUtil.to(value, 14) * 0.1);
                                        }
                                    }
                                    break;
                                case 67:
                                    position.set("obdHours", buf.readUnsignedIntLE() * 1000);
                                    break;
                                case 68:
                                    value = buf.readUnsignedShortLE();
                                    position.set(
                                            Position.KEY_ODOMETER_SERVICE,
                                            (value != 0xFFFF) ? (value * 5000) : null);
                                    break;
                                case 69:
                                    value = buf.readUnsignedByte();
                                    position.set(Position.KEY_OBD_SPEED, (value != 0xFF) ? value : null);
                                    break;
                                case 78:
                                case 79:
                                case 80:
                                case 81:
                                case 82:
                                case 83:
                                    position.set("fuelTemp" + (j + 1 - 78), (int) buf.readByte());
                                    break;
                                case 163:
                                case 164:
                                case 165:
                                case 166:
                                    value = buf.readShortLE();
                                    position.set(
                                            Position.PREFIX_TEMP + (j + 1 + 8 - 163),
                                            (value != (short) 0x8000) ? value * 0.05 : null);
                                    break;
                                case 167:
                                case 168:
                                case 169:
                                case 170:
                                    value = buf.readUnsignedByte();
                                    position.set("humidity" + (j + 1 - 167), (value != 0xFF) ? value * 0.5 : null);
                                    break;
                                case 206:
                                    position.set("diagnostic", buf.readUnsignedIntLE());
                                    break;
                                default:
                                    if ((207 <= j) && (j <= 222)) {
                                        position.set("user1Byte" + (j + 1 - 207), buf.readUnsignedByte());
                                    } else if ((223 <= j) && (j <= 237)) {
                                        position.set("user2Byte" + (j + 1 - 223), buf.readUnsignedShortLE());
                                    } else if ((238 <= j) && (j <= 252)) {
                                        position.set("user4Byte" + (j + 1 - 238), buf.readUnsignedIntLE());
                                    } else if ((253 <= j) && (j <= 255)) {
                                        position.set("user8Byte" + (j + 1 - 253), buf.readLongLE());
                                    } else {
                                        buf.skipBytes(getItemLength(j));
                                    }
                                    break;
                            }
                        }
                    }

                    if (position.getFixTime() == null) {
                        getLastLocation(position, position.getDeviceTime());
                    }

                    positions.add(position);
                }

                if (channel != null) {
                    ByteBuf response = Unpooled.buffer();
                    response.writeCharSequence(type, StandardCharsets.US_ASCII);
                    response.writeByte(count);
                    response.writeByte(Checksum.crc8(Checksum.CRC8_EGTS, response.nioBuffer()));
                    channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
                }

                return positions;

            }

        }

        return null;
    }

}
