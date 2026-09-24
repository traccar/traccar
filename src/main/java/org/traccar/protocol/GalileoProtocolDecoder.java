/*
 * Copyright 2013 - 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.config.Keys;
import org.traccar.helper.BitBuffer;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class GalileoProtocolDecoder extends BaseProtocolDecoder {

    public GalileoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private boolean compressed;

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public boolean getCompressed(long deviceId) {
        Boolean value = AttributeUtil.lookup(
                getCacheManager(), Keys.PROTOCOL_EXTENDED.withPrefix(getProtocolName()), deviceId);
        return value != null ? value : compressed;
    }

    private static final Map<Integer, Integer> TAG_LENGTH_MAP = new HashMap<>();

    private static void addTagLength(int length, int... tags) {
        for (int tag : tags) {
            TAG_LENGTH_MAP.put(tag, length);
        }
    }

    static {
        addTagLength(1,
                0x01, 0x02, 0x35, 0x36, 0x43, 0xc4, 0xc5, 0xc6, 0xc7,
                0xc8, 0xc9, 0xca, 0xcb, 0xcc, 0xcd, 0xce, 0xcf,
                0xd0, 0xd1, 0xd2, 0xd5, 0x88, 0x89, 0x8a, 0x8b, 0x8c,
                0xa0, 0xaf, 0xa1, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6,
                0xa7, 0xa8, 0xa9, 0xaa, 0xab, 0xac, 0xad, 0xae);
        addTagLength(2,
                0x04, 0x10, 0x34, 0x40, 0x41, 0x42, 0x45, 0x46,
                0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x60, 0x61,
                0x62, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76,
                0x77, 0xb0, 0xb1, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6,
                0xb7, 0xb8, 0xb9, 0xd6, 0xd7, 0xd8, 0xd9, 0xda);
        addTagLength(3,
                0x63, 0x64, 0x6f, 0x5d, 0x65, 0x66, 0x67, 0x68,
                0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0xfa,
                0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87);
        addTagLength(4,
                0x20, 0x33, 0x44, 0x90, 0xc0, 0xc2, 0xc3, 0xd3,
                0xd4, 0xdb, 0xdc, 0xdd, 0xde, 0xdf, 0xf0, 0xf9,
                0x5a, 0x47, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6,
                0xf7, 0xf8, 0xe2, 0xe9);
        TAG_LENGTH_MAP.put(0xfd, 8);
    }

    private static int getTagLength(int tag) {
        Integer length = TAG_LENGTH_MAP.get(tag);
        if (length == null) {
            throw new IllegalArgumentException(String.format("Unknown tag: 0x%02x", tag));
        }
        return length;
    }

    private static final Map<Integer, Integer> EXTENDED_TAG_LENGTH_MAP = new HashMap<>();

    private static void addExtendedTagLength(int length, int... tags) {
        for (int tag : tags) {
            EXTENDED_TAG_LENGTH_MAP.put(tag, length);
        }
    }

    static {
        for (int tag = 0x0001; tag <= 0x0080; tag++) {
        EXTENDED_TAG_LENGTH_MAP.put(tag, 4);
        }
        addExtendedTagLength(2, 0x0081, 0x0082, 0x0083, 0x0084);
        addExtendedTagLength(1, 0x0085, 0x0093, 0x00a4, 0x00a5, 0x00a6, 0x00a7, 0x00a8, 0x00b0, 0x00b1, 0x00b2, 0x00fc);
        for (int tag = 0x0086; tag <= 0x0091; tag++) {
            EXTENDED_TAG_LENGTH_MAP.put(tag, 4);
        }
        EXTENDED_TAG_LENGTH_MAP.put(0x0092, 15);
        EXTENDED_TAG_LENGTH_MAP.put(0x0094, 20);
        EXTENDED_TAG_LENGTH_MAP.put(0x0095, 4);
        EXTENDED_TAG_LENGTH_MAP.put(0x00a9, 4);
        EXTENDED_TAG_LENGTH_MAP.put(0x00ab, 12);
        EXTENDED_TAG_LENGTH_MAP.put(0x00ac, 4);
        addExtendedTagLength(6, 0x00ad, 0x00ae, 0x00b4, 0x00b5, 0x00b6);
        EXTENDED_TAG_LENGTH_MAP.put(0x00af, 14);
        EXTENDED_TAG_LENGTH_MAP.put(0x00b3, 12);
        addExtendedTagLength(8, 0x00fd, 0x00fe);
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

    private String readRefrigeratorData(ByteBuf buf) {
        int start = buf.readerIndex();
        buf.skipBytes(1);
        int status = buf.readUnsignedShortLE();
        int length = 7;
        if (BitUtil.check(status, 1)) {
            length += 10;
        }
        if (BitUtil.check(status, 2)) {
            length += 10;
        }
        if (BitUtil.check(status, 3)) {
            length += 10;
        }
        if (BitUtil.check(status, 4)) {
            length += 2;
        }
        if (BitUtil.check(status, 5)) {
            length += 2;
        }
        if (BitUtil.check(status, 6)) {
            length += 2;
        }
        if (BitUtil.check(status, 7)) {
            length += 2;
        }
        if (BitUtil.check(status, 8)) {
            length += 2;
        }
        if (BitUtil.check(status, 9)) {
            length += 2;
        }
        if (BitUtil.check(status, 10)) {
            length += 32;
        }
        if (BitUtil.check(status, 11)) {
            length += 20;
        }
        if (BitUtil.check(status, 12)) {
            length += 12;
        }
        if (BitUtil.check(status, 13)) {
            length += 2;
        }
        if (BitUtil.check(status, 14)) {
            length += 2;
        }

        buf.readerIndex(start);
        String hex = ByteBufUtil.hexDump(buf.readSlice(length));
        return hex;
    }

    private String readTirePressureData(ByteBuf buf) {
        int b0 = buf.getUnsignedByte(buf.readerIndex());
        int b1 = buf.getUnsignedByte(buf.readerIndex() + 1);
        if ((b0 == 0x00 && b1 == 0xFF) || (b0 == 0xFF && b1 == 0x00)) {
            buf.skipBytes(2);
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 34; i++) {
            int pressure = buf.readUnsignedByte();
            int flags = buf.readUnsignedByte();
            boolean noLink = BitUtil.check(flags, 3);
            boolean lowBattery = BitUtil.check(flags, 4);
            if (result.length() > 0) {
                result.append(';');
            }
            result.append(i).append(':').append(pressure);
            if (noLink) {
                result.append(":noLink");
            }
            if (lowBattery) {
                result.append(":lowBattery");
            }
        }
        return result.toString();
    }

    private void decodeExtendedTags(Position position, ByteBuf buf) {
        int endIndex = buf.readUnsignedShortLE() + buf.readerIndex();
        int[] cellData = {0, 0, 0, 0, 0};
        boolean[] hasCellData = {false};
        while (buf.readerIndex() < endIndex) {
            int tag = buf.readUnsignedShortLE();
            if (!decodeExtendedTag(position, buf, tag, cellData, hasCellData)) {
            break;
            }
        }
        buf.readerIndex(endIndex);
        if (hasCellData[0]) {
        position.setNetwork(new Network(CellTower.from(
                cellData[2], cellData[3], cellData[1], cellData[0], cellData[4])));
        }
    }

    private boolean decodeExtendedTag(Position position, ByteBuf buf, int tag, int[] cellData, boolean[] hasCellData) {
        switch (tag) {
            case 0x0081 -> {
            cellData[0] = buf.readUnsignedShortLE();
            hasCellData[0] = true;
            }
            case 0x0082 -> cellData[1] = buf.readUnsignedShortLE();
            case 0x0083 -> cellData[2] = buf.readUnsignedShortLE();
            case 0x0084 -> cellData[3] = buf.readUnsignedShortLE();
            case 0x0085 -> cellData[4] = buf.readUnsignedByte();
            case 0x008e -> {
                position.set(Position.KEY_SATELLITES_VISIBLE, buf.readUnsignedByte());
                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                position.set("snrAvg", buf.readUnsignedByte());
                position.set("snrMax", buf.readUnsignedByte());
                }
            default -> {
                Integer length = EXTENDED_TAG_LENGTH_MAP.get(tag);
                if (length == null) {
                    return false;
                }
                buf.skipBytes(length);
            }
        }
        return true;
    }

    private void decodeTagOther(Position position, ByteBuf buf, int tag) {
        switch (tag) {
            case 0x01 -> position.set(Position.KEY_VERSION_HW, buf.readUnsignedByte());
            case 0x02 -> position.set(Position.KEY_VERSION_FW, buf.readUnsignedByte());
            case 0x04 -> position.set("deviceId", buf.readUnsignedShortLE());
            case 0x10 -> position.set(Position.KEY_INDEX, buf.readUnsignedShortLE());
            case 0x20 -> position.setTime(new Date(buf.readUnsignedIntLE() * 1000));
            case 0x33 -> {
                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE() / 10.0));
                position.setCourse(buf.readUnsignedShortLE() / 10.0);
            }
            case 0x34 -> position.setAltitude(buf.readShortLE());
            case 0x35 -> position.set(Position.KEY_HDOP, buf.readUnsignedByte() / 10.0);
            case 0x36 -> position.set(Position.KEY_PDOP, buf.readUnsignedByte() / 10.0);
            case 0x40 -> position.set(Position.KEY_STATUS, buf.readUnsignedShortLE());
            case 0x41 -> position.set(Position.KEY_POWER, buf.readUnsignedShortLE() / 1000.0);
            case 0x42 -> position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() / 1000.0);
            case 0x43 -> position.set(Position.KEY_DEVICE_TEMP, buf.readByte());
            case 0x44 -> position.set(Position.KEY_ACCELERATION, buf.readUnsignedIntLE());
            case 0x45 -> position.set(Position.KEY_OUTPUT, buf.readUnsignedShortLE());
            case 0x46 -> position.set(Position.KEY_INPUT, buf.readUnsignedShortLE());
            case 0x48 -> position.set("statusExtended", buf.readUnsignedShortLE());
            case 0x58 -> position.set("rs2320", buf.readUnsignedShortLE());
            case 0x59 -> position.set("rs2321", buf.readUnsignedShortLE());
            case 0x90 -> position.set(Position.KEY_DRIVER_UNIQUE_ID, String.valueOf(buf.readUnsignedIntLE()));
            case 0xc0 -> position.set("fuelTotal", buf.readUnsignedIntLE() * 0.5);
            case 0xc1 -> {
                position.set(Position.KEY_FUEL, buf.readUnsignedByte() * 0.4);
                position.set(Position.PREFIX_TEMP + 1, buf.readUnsignedByte() - 40);
                position.set(Position.KEY_RPM, buf.readUnsignedShortLE() * 0.125);
            }
            case 0xc2 -> position.set("canB0", buf.readUnsignedIntLE());
            case 0xc3 -> position.set("canB1", buf.readUnsignedIntLE());
            case 0xd4 -> position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
            case 0xe0 -> position.set(Position.KEY_INDEX, buf.readUnsignedIntLE());
            case 0xe1 -> position.set(Position.KEY_RESULT,
                    buf.readSlice(buf.readUnsignedByte()).toString(StandardCharsets.US_ASCII));
            case 0xea -> position.set("userDataArray", ByteBufUtil.hexDump(buf.readSlice(buf.readUnsignedByte())));
            case 0x5b -> position.set("refrigeratorData", readRefrigeratorData(buf));
            case 0x5c -> position.set("tirePressureData", readTirePressureData(buf));
            case 0xfe -> decodeExtendedTags(position, buf);
            default -> buf.skipBytes(getTagLength(tag));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int header = buf.readUnsignedByte();
        if (header == 0x01) {
            if (buf.getUnsignedMedium(buf.readerIndex() + 2) == 0x01001c) {
                return decodeIridiumPosition(channel, remoteAddress, buf);
            } else {
                return decodePositions(channel, remoteAddress, buf);
            }
        } else if (header == 0x07) {
            return decodePhoto(channel, remoteAddress, buf);
        } else if (header == 0x08) {
            return decodeCompressedPositions(channel, remoteAddress, buf);
        }

        return null;
    }

    private void decodeMinimalDataSet(Position position, ByteBuf buf) {
        BitBuffer bits = new BitBuffer(buf.readSlice(10));
        bits.readUnsigned(1);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
        calendar.add(Calendar.SECOND, bits.readUnsigned(25));
        position.setTime(calendar.getTime());

        position.setValid(bits.readUnsigned(1) == 0);
        position.setLongitude(360 * bits.readUnsigned(22) / 4194304.0 - 180);
        position.setLatitude(180 * bits.readUnsigned(21) / 2097152.0 - 90);
        if (bits.readUnsigned(1) > 0) {
            position.addAlarm(Position.ALARM_GENERAL);
        }
    }

    private Position decodeIridiumPosition(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        buf.readUnsignedShort(); // length

        buf.skipBytes(3); // identification header
        buf.readUnsignedInt(); // index

        DeviceSession deviceSession = getDeviceSession(
                channel, remoteAddress, buf.readSlice(15).toString(StandardCharsets.US_ASCII));
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        buf.readUnsignedByte(); // session status
        buf.skipBytes(4); // reserved
        position.setTime(new Date(buf.readUnsignedInt() * 1000));

        buf.skipBytes(3); // coordinates header
        int flags = buf.readUnsignedByte();
        double latitude = buf.readUnsignedByte() + buf.readUnsignedShort() / 60000.0;
        double longitude = buf.readUnsignedByte() + buf.readUnsignedShort() / 60000.0;
        position.setLatitude(BitUtil.check(flags, 1) ? -latitude : latitude);
        position.setLongitude(BitUtil.check(flags, 0) ? -longitude : longitude);
        buf.readUnsignedInt(); // accuracy

        buf.readUnsignedByte(); // data tag header
        ByteBuf data = buf.readSlice(buf.readUnsignedShort());
        if (getCompressed(deviceSession.getDeviceId())) {

            decodeMinimalDataSet(position, data);

            int[] tags = new int[BitUtil.to(data.readUnsignedByte(), 8)];
            for (int i = 0; i < tags.length; i++) {
                tags[i] = data.readUnsignedByte();
            }

            for (int tag : tags) {
                decodeTag(position, data, tag);
            }

        } else {

            while (data.isReadable()) {
                int tag = data.readUnsignedByte();
                if (tag == 0x30) {
                    position.setValid((data.readUnsignedByte() & 0xf0) == 0x00);
                    position.setLatitude(data.readIntLE() / 1000000.0);
                    position.setLongitude(data.readIntLE() / 1000000.0);
                } else {
                    decodeTag(position, data, tag);
                }
            }

        }

        return position;
    }

    private List<Position> decodePositions(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        int endIndex = (buf.readUnsignedShortLE() & 0x7fff) + buf.readerIndex();

        List<Position> positions = new LinkedList<>();
        Set<Integer> tags = new HashSet<>();
        boolean hasLocation = false;

        DeviceSession deviceSession = null;
        Position position = new Position(getProtocolName());

        while (buf.readerIndex() < endIndex) {

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
                int flags = buf.readUnsignedByte();
                position.setValid((flags & 0xf0) == 0x00);
                position.set(Position.KEY_SATELLITES, flags & 0x0f);
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
        } else if (position.hasAttribute(Position.KEY_RESULT)) {
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

        if (getMediaBuffer() == null) {
            newMediaBuffer();
        }

        buf.readUnsignedByte(); // part number

        if (length > 1) {

            getMediaBuffer().writeBytes(buf, length - 1);

        } else {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);

            position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            position.set(Position.KEY_IMAGE, writeMediaFile(deviceSession.getUniqueId(), "jpg"));

        }

        sendResponse(channel, 0x07, buf.readUnsignedShortLE());

        return position;
    }

    private List<Position> decodeCompressedPositions(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        buf.readUnsignedShortLE(); // length

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        List<Position> positions = new LinkedList<>();
        while (buf.readableBytes() > 2) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            decodeMinimalDataSet(position, buf);

            int[] tags = new int[BitUtil.to(buf.readUnsignedByte(), 8)];
            for (int i = 0; i < tags.length; i++) {
                tags[i] = buf.readUnsignedByte();
            }

            for (int tag : tags) {
                decodeTag(position, buf, tag);
            }

            positions.add(position);

        }

        sendResponse(channel, 0x02, buf.readUnsignedShortLE());

        return positions.isEmpty() ? null : positions;
    }

}
