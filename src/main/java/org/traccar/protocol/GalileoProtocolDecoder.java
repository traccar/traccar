/*
 * Copyright 2013 - 2020 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GalileoProtocolDecoder extends BaseProtocolDecoder {

    public GalileoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf photo;

    private static final Map<Integer, Integer> TAG_LENGTH_MAP = new HashMap<>();

    static {
        int[] l1 = {
            0x01, 0x02, 0x35, 0x43, 0xc4, 0xc5, 0xc6, 0xc7,
            0xc8, 0xc9, 0xca, 0xcb, 0xcc, 0xcd, 0xce, 0xcf,
            0xd0, 0xd1, 0xd2, 0xd5, 0x88, 0x8a, 0x8b, 0x8c,
            0xa0, 0xaf, 0xa1, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6,
            0xa7, 0xa8, 0xa9, 0xaa, 0xab, 0xac, 0xad, 0xae
        };
        int[] l2 = {
            0x04, 0x10, 0x34, 0x40, 0x41, 0x42, 0x45, 0x46,
            0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x60, 0x61,
            0x62, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76,
            0x77, 0xb0, 0xb1, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6,
            0xb7, 0xb8, 0xb9, 0xd6, 0xd7, 0xd8, 0xd9, 0xda
        };
        int[] l3 = {
            0x63, 0x64, 0x6f, 0x5d, 0x65, 0x66, 0x67, 0x68,
            0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e
        };
        int[] l4 = {
            0x20, 0x33, 0x44, 0x90, 0xc0, 0xc2, 0xc3, 0xd3,
            0xd4, 0xdb, 0xdc, 0xdd, 0xde, 0xdf, 0xf0, 0xf9,
            0x5a, 0x47, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6,
            0xf7, 0xf8, 0xe2, 0xe9
        };
        for (int i : l1) {
            TAG_LENGTH_MAP.put(i, 1);
        }
        for (int i : l2) {
            TAG_LENGTH_MAP.put(i, 2);
        }
        for (int i : l3) {
            TAG_LENGTH_MAP.put(i, 3);
        }
        for (int i : l4) {
            TAG_LENGTH_MAP.put(i, 4);
        }
        TAG_LENGTH_MAP.put(0x5b, 7); // variable length
        TAG_LENGTH_MAP.put(0x5c, 68);
    }

    private static int getTagLength(int tag) {
        Integer length = TAG_LENGTH_MAP.get(tag);
        if (length == null) {
            throw new IllegalArgumentException(String.format("Unknown tag: 0x%02x", tag));
        }
        return length;
    }

    private void sendResponse(Channel channel, int header, int checksum) {
        if (channel != null) {
            ByteBuf reply = Unpooled.buffer(3);
            reply.writeByte(header);
            reply.writeShortLE((short) checksum);
            channel.writeAndFlush(new NetworkMessage(reply, channel.remoteAddress()));
        }
    }

    private void decodeTag(Position position, ByteBuf buf, int tag) {
        if (tag >= 0x50 && tag <= 0x57) {
            position.set(Position.PREFIX_ADC + (tag - 0x50), buf.readUnsignedShortLE());
        } else if (tag >= 0x60 && tag <= 0x62) {
            position.set("fuel" + (tag - 0x60), buf.readUnsignedShortLE());
        } else if (tag >= 0xa0 && tag <= 0xaf) {
            position.set("can8BitR" + (tag - 0xa0 + 15), buf.readUnsignedByte());
        } else if (tag >= 0xb0 && tag <= 0xb9) {
            position.set("can16BitR" + (tag - 0xb0 + 5), buf.readUnsignedShortLE());
        } else if (tag >= 0xc4 && tag <= 0xd2) {
            position.set("can8BitR" + (tag - 0xc4), buf.readUnsignedByte());
        } else if (tag >= 0xd6 && tag <= 0xda) {
            position.set("can16BitR" + (tag - 0xd6), buf.readUnsignedShortLE());
        } else if (tag >= 0xdb && tag <= 0xdf) {
            position.set("can32BitR" + (tag - 0xdb), buf.readUnsignedIntLE());
        } else if (tag >= 0xe2 && tag <= 0xe9) {
            position.set("userData" + (tag - 0xe2), buf.readUnsignedIntLE());
        } else if (tag >= 0xf0 && tag <= 0xf9) {
            position.set("can32BitR" + (tag - 0xf0 + 5), buf.readUnsignedIntLE());
        } else {
            decodeTagOther(position, buf, tag);
        }
    }

    private void decodeTagOther(Position position, ByteBuf buf, int tag) {
        switch (tag) {
            case 0x01:
                position.set(Position.KEY_VERSION_HW, buf.readUnsignedByte());
                break;
            case 0x02:
                position.set(Position.KEY_VERSION_FW, buf.readUnsignedByte());
                break;
            case 0x04:
                position.set("deviceId", buf.readUnsignedShortLE());
                break;
            case 0x10:
                position.set(Position.KEY_INDEX, buf.readUnsignedShortLE());
                break;
            case 0x20:
                position.setTime(new Date(buf.readUnsignedIntLE() * 1000));
                break;
            case 0x33:
                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE() * 0.1));
                position.setCourse(buf.readUnsignedShortLE() * 0.1);
                break;
            case 0x34:
                position.setAltitude(buf.readShortLE());
                break;
            case 0x35:
                position.set(Position.KEY_HDOP, buf.readUnsignedByte() * 0.1);
                break;
            case 0x40:
                position.set(Position.KEY_STATUS, buf.readUnsignedShortLE());
                break;
            case 0x41:
                position.set(Position.KEY_POWER, buf.readUnsignedShortLE() / 1000.0);
                break;
            case 0x42:
                position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() / 1000.0);
                break;
            case 0x43:
                position.set(Position.KEY_DEVICE_TEMP, buf.readByte());
                break;
            case 0x44:
                position.set(Position.KEY_ACCELERATION, buf.readUnsignedIntLE());
                break;
            case 0x45:
                position.set(Position.KEY_OUTPUT, buf.readUnsignedShortLE());
                break;
            case 0x46:
                position.set(Position.KEY_INPUT, buf.readUnsignedShortLE());
                break;
            case 0x48:
                position.set("statusExtended", buf.readUnsignedShortLE());
                break;
            case 0x58:
                position.set("rs2320", buf.readUnsignedShortLE());
                break;
            case 0x59:
                position.set("rs2321", buf.readUnsignedShortLE());
                break;
            case 0x90:
                position.set(Position.KEY_DRIVER_UNIQUE_ID, String.valueOf(buf.readUnsignedIntLE()));
                break;
            case 0xc0:
                position.set("fuelTotal", buf.readUnsignedIntLE() * 0.5);
                break;
            case 0xc1:
                position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedByte() * 0.4);
                position.set(Position.PREFIX_TEMP + 1, buf.readUnsignedByte() - 40);
                position.set(Position.KEY_RPM, buf.readUnsignedShortLE() * 0.125);
                break;
            case 0xc2:
                position.set("canB0", buf.readUnsignedIntLE());
                break;
            case 0xc3:
                position.set("canB1", buf.readUnsignedIntLE());
                break;
            case 0xd4:
                position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
                break;
            case 0xe0:
                position.set(Position.KEY_INDEX, buf.readUnsignedIntLE());
                break;
            case 0xe1:
                position.set(Position.KEY_RESULT,
                        buf.readSlice(buf.readUnsignedByte()).toString(StandardCharsets.US_ASCII));
                break;
            case 0xea:
                position.set("userDataArray", ByteBufUtil.hexDump(buf.readSlice(buf.readUnsignedByte())));
                position.set("userDataArray", ByteBufUtil.hexDump(buf.readSlice(buf.readUnsignedByte())));
                break;
            default:
                buf.skipBytes(getTagLength(tag));
                break;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int header = buf.readUnsignedByte();
        if (header == 0x01) {
            return decodePositions(channel, remoteAddress, buf);
        } else if (header == 0x07) {
            return decodePhoto(channel, remoteAddress, buf);
        }

        return null;
    }

    private Object decodePositions(
            Channel channel, SocketAddress remoteAddress, ByteBuf buf) throws Exception {

        int length = (buf.readUnsignedShortLE() & 0x7fff) + 3;

        List<Position> positions = new LinkedList<>();
        Set<Integer> tags = new HashSet<>();
        boolean hasLocation = false;

        DeviceSession deviceSession = null;
        Position position = new Position(getProtocolName());

        while (buf.readerIndex() < length) {

            int tag = buf.readUnsignedByte();
            if (tags.contains(tag)) {
                if (hasLocation && position.getFixTime() != null) {
                    positions.add(position);
                }
                tags.clear();
                hasLocation = false;
                position = new Position(getProtocolName()); // new position starts
            }
            tags.add(tag);

            if (tag == 0x03) {
                deviceSession = getDeviceSession(
                        channel, remoteAddress, buf.readSlice(15).toString(StandardCharsets.US_ASCII));
            } else if (tag == 0x30) {
                hasLocation = true;
                position.setValid((buf.readUnsignedByte() & 0xf0) == 0x00);
                position.setLatitude(buf.readIntLE() / 1000000.0);
                position.setLongitude(buf.readIntLE() / 1000000.0);
            } else {
                decodeTag(position, buf, tag);
            }

        }

        if (deviceSession == null) {
            deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }
        }

        if (hasLocation && position.getFixTime() != null) {
            positions.add(position);
        } else if (position.getAttributes().containsKey(Position.KEY_RESULT)) {
            position.setDeviceId(deviceSession.getDeviceId());
            getLastLocation(position, null);
            positions.add(position);
        }

        sendResponse(channel, 0x02, buf.readUnsignedShortLE());

        for (Position p : positions) {
            p.setDeviceId(deviceSession.getDeviceId());
        }

        return positions.isEmpty() ? null : positions;
    }

    private Object decodePhoto(
            Channel channel, SocketAddress remoteAddress, ByteBuf buf) throws Exception {

        int length = buf.readUnsignedShortLE();

        Position position = null;

        if (length > 1) {

            if (photo == null) {
                photo = Unpooled.buffer();
            }

            buf.readUnsignedByte(); // part number
            photo.writeBytes(buf, length - 1);

            sendResponse(channel, 0x07, buf.readUnsignedShortLE());

        } else if (photo != null) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            String uniqueId = Context.getIdentityManager().getById(deviceSession.getDeviceId()).getUniqueId();

            position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            position.set(Position.KEY_IMAGE, Context.getMediaManager().writeFile(uniqueId, photo, "jpg"));
            photo.release();
            photo = null;

        }

        return position;
    }

}
