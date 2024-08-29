/*
 * Copyright 2013 - 2023 Anton Tananaev (anton@traccar.org)
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
import org.traccar.config.Keys;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class TeltonikaProtocolDecoder extends BaseProtocolDecoder {

    private static final int IMAGE_PACKET_MAX = 2048;

    private static final Map<Integer, Map<Set<String>, BiConsumer<Position, ByteBuf>>> PARAMETERS = new HashMap<>();

    private final boolean connectionless;
    private boolean extended;
    private final Map<Long, ByteBuf> photos = new HashMap<>();

    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    public TeltonikaProtocolDecoder(Protocol protocol, boolean connectionless) {
        super(protocol);
        this.connectionless = connectionless;
    }

    @Override
    protected void init() {
        this.extended = getConfig().getBoolean(Keys.PROTOCOL_EXTENDED.withPrefix(getProtocolName()));
    }

    private void parseIdentification(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        int length = buf.readUnsignedShort();
        String imei = buf.toString(buf.readerIndex(), length, StandardCharsets.US_ASCII);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);

        if (channel != null) {
            ByteBuf response = Unpooled.buffer(1);
            if (deviceSession != null) {
                response.writeByte(1);
            } else {
                response.writeByte(0);
            }
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    public static final int CODEC_GH3000 = 0x07;
    public static final int CODEC_8 = 0x08;
    public static final int CODEC_8_EXT = 0x8E;
    public static final int CODEC_12 = 0x0C;
    public static final int CODEC_13 = 0x0D;
    public static final int CODEC_16 = 0x10;

    private void sendImageRequest(Channel channel, SocketAddress remoteAddress, long id, int offset, int size) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeInt(0);
            response.writeShort(0);
            response.writeShort(19); // length
            response.writeByte(CODEC_12);
            response.writeByte(1); // nod
            response.writeByte(0x0D); // camera
            response.writeInt(11); // payload length
            response.writeByte(2); // command
            response.writeInt((int) id);
            response.writeInt(offset);
            response.writeShort(size);
            response.writeByte(1); // nod
            response.writeShort(0);
            response.writeShort(Checksum.crc16(
                    Checksum.CRC16_IBM, response.nioBuffer(8, response.readableBytes() - 10)));
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private void decodeSerial(
            Channel channel, SocketAddress remoteAddress, DeviceSession deviceSession, Position position, ByteBuf buf) {

        getLastLocation(position, null);

        int type = buf.readUnsignedByte();
        if (type == 0x0D) {

            buf.readInt(); // length
            int subtype = buf.readUnsignedByte();
            if (subtype == 0x01) {

                long photoId = buf.readUnsignedInt();
                ByteBuf photo = Unpooled.buffer(buf.readInt());
                photos.put(photoId, photo);
                sendImageRequest(
                        channel, remoteAddress, photoId,
                        0, Math.min(IMAGE_PACKET_MAX, photo.capacity()));

            } else if (subtype == 0x02) {

                long photoId = buf.readUnsignedInt();
                buf.readInt(); // offset
                ByteBuf photo = photos.get(photoId);
                photo.writeBytes(buf, buf.readUnsignedShort());
                if (photo.writableBytes() > 0) {
                    sendImageRequest(
                            channel, remoteAddress, photoId,
                            photo.writerIndex(), Math.min(IMAGE_PACKET_MAX, photo.writableBytes()));
                } else {
                    photos.remove(photoId);
                    try {
                        position.set(Position.KEY_IMAGE, writeMediaFile(deviceSession.getUniqueId(), photo, "jpg"));
                    } finally {
                        photo.release();
                    }
                }

            }

        } else {

            position.set(Position.KEY_TYPE, type);

            int length = buf.readInt();
            if (BufferUtil.isPrintable(buf, length)) {
                String data = buf.readSlice(length).toString(StandardCharsets.US_ASCII).trim();
                if (data.startsWith("UUUUww") && data.endsWith("SSS")) {
                    String[] values = data.substring(6, data.length() - 4).split(";");
                    for (int i = 0; i < 8; i++) {
                        position.set("axle" + (i + 1), Double.parseDouble(values[i]));
                    }
                    position.set("loadTruck", Double.parseDouble(values[8]));
                    position.set("loadTrailer", Double.parseDouble(values[9]));
                    position.set("totalTruck", Double.parseDouble(values[10]));
                    position.set("totalTrailer", Double.parseDouble(values[11]));
                } else {
                    position.set(Position.KEY_RESULT, data);
                }
            } else {
                position.set(Position.KEY_RESULT, ByteBufUtil.hexDump(buf.readSlice(length)));
            }
        }
    }

    private long readValue(ByteBuf buf, int length) {
        return switch (length) {
            case 1 -> buf.readUnsignedByte();
            case 2 -> buf.readUnsignedShort();
            case 4 -> buf.readUnsignedInt();
            default -> buf.readLong();
        };
    }

    private static void register(int id, Set<String> models, BiConsumer<Position, ByteBuf> handler) {
        PARAMETERS.computeIfAbsent(id, key -> new HashMap<>()).put(models, handler);
    }

    static {
        var fmbXXX = Set.of(
                "FMB001", "FMB010", "FMB002", "FMB020", "FMB003", "FMB110", "FMB120", "FMB122", "FMB125", "FMB130",
                "FMB140", "FMU125", "FMB900", "FMB920", "FMB962", "FMB964", "FM3001", "FMB202", "FMB204", "FMB206",
                "FMT100", "MTB100", "FMP100", "MSP500", "FMC125", "FMM125", "FMU130", "FMC130", "FMM130", "FMB150",
                "FMC150", "FMM150", "FMC920");

        register(1, null, (p, b) -> p.set(Position.PREFIX_IN + 1, b.readUnsignedByte() > 0));
        register(2, null, (p, b) -> p.set(Position.PREFIX_IN + 2, b.readUnsignedByte() > 0));
        register(3, null, (p, b) -> p.set(Position.PREFIX_IN + 3, b.readUnsignedByte() > 0));
        register(4, null, (p, b) -> p.set(Position.PREFIX_IN + 4, b.readUnsignedByte() > 0));
        register(9, fmbXXX, (p, b) -> p.set(Position.PREFIX_ADC + 1, b.readUnsignedShort() * 0.001));
        register(10, fmbXXX, (p, b) -> p.set(Position.PREFIX_ADC + 2, b.readUnsignedShort() * 0.001));
        register(11, fmbXXX, (p, b) -> p.set(Position.KEY_ICCID, String.valueOf(b.readLong())));
        register(12, fmbXXX, (p, b) -> p.set(Position.KEY_FUEL_USED, b.readUnsignedInt() * 0.001));
        register(13, fmbXXX, (p, b) -> p.set(Position.KEY_FUEL_CONSUMPTION, b.readUnsignedShort() * 0.01));
        register(16, null, (p, b) -> p.set(Position.KEY_ODOMETER, b.readUnsignedInt()));
        register(17, null, (p, b) -> p.set("axisX", b.readShort()));
        register(18, null, (p, b) -> p.set("axisY", b.readShort()));
        register(19, null, (p, b) -> p.set("axisZ", b.readShort()));
        register(21, null, (p, b) -> p.set(Position.KEY_RSSI, b.readUnsignedByte()));
        register(24, fmbXXX, (p, b) -> p.setSpeed(UnitsConverter.knotsFromKph(b.readUnsignedShort())));
        register(25, null, (p, b) -> p.set("bleTemp1", b.readShort() * 0.01));
        register(26, null, (p, b) -> p.set("bleTemp2", b.readShort() * 0.01));
        register(27, null, (p, b) -> p.set("bleTemp3", b.readShort() * 0.01));
        register(28, null, (p, b) -> p.set("bleTemp4", b.readShort() * 0.01));
        register(30, fmbXXX, (p, b) -> p.set("faultCount", b.readUnsignedByte()));
        register(32, fmbXXX, (p, b) -> p.set(Position.KEY_COOLANT_TEMP, b.readByte()));
        register(66, null, (p, b) -> p.set(Position.KEY_POWER, b.readUnsignedShort() * 0.001));
        register(67, null, (p, b) -> p.set(Position.KEY_BATTERY, b.readUnsignedShort() * 0.001));
        register(68, fmbXXX, (p, b) -> p.set("batteryCurrent", b.readUnsignedShort() * 0.001));
        register(72, fmbXXX, (p, b) -> p.set(Position.PREFIX_TEMP + 1, b.readInt() * 0.1));
        register(73, fmbXXX, (p, b) -> p.set(Position.PREFIX_TEMP + 2, b.readInt() * 0.1));
        register(74, fmbXXX, (p, b) -> p.set(Position.PREFIX_TEMP + 3, b.readInt() * 0.1));
        register(75, fmbXXX, (p, b) -> p.set(Position.PREFIX_TEMP + 4, b.readInt() * 0.1));
        register(78, null, (p, b) -> {
            long driverUniqueId = b.readLongLE();
            if (driverUniqueId != 0) {
                p.set(Position.KEY_DRIVER_UNIQUE_ID, String.format("%016X", driverUniqueId));
            }
        });
        register(80, fmbXXX, (p, b) -> p.set("dataMode", b.readUnsignedByte()));
        register(81, fmbXXX, (p, b) -> p.set(Position.KEY_OBD_SPEED, b.readUnsignedByte()));
        register(82, fmbXXX, (p, b) -> p.set(Position.KEY_THROTTLE, b.readUnsignedByte()));
        register(83, fmbXXX, (p, b) -> p.set(Position.KEY_FUEL_USED, b.readUnsignedInt() * 0.1));
        register(84, fmbXXX, (p, b) -> p.set(Position.KEY_FUEL_LEVEL, b.readUnsignedShort() * 0.1));
        register(85, fmbXXX, (p, b) -> p.set(Position.KEY_RPM, b.readUnsignedShort()));
        register(87, fmbXXX, (p, b) -> p.set(Position.KEY_OBD_ODOMETER, b.readUnsignedInt()));
        register(89, fmbXXX, (p, b) -> p.set("fuelLevelPercentage", b.readUnsignedByte()));
        register(110, fmbXXX, (p, b) -> p.set(Position.KEY_FUEL_CONSUMPTION, b.readUnsignedShort() * 0.1));
        register(113, fmbXXX, (p, b) -> p.set(Position.KEY_BATTERY_LEVEL, b.readUnsignedByte()));
        register(115, fmbXXX, (p, b) -> p.set("engineTemp", b.readShort() * 0.1));
        register(179, null, (p, b) -> p.set(Position.PREFIX_OUT + 1, b.readUnsignedByte() > 0));
        register(180, null, (p, b) -> p.set(Position.PREFIX_OUT + 2, b.readUnsignedByte() > 0));
        register(181, null, (p, b) -> p.set(Position.KEY_PDOP, b.readUnsignedShort() * 0.1));
        register(182, null, (p, b) -> p.set(Position.KEY_HDOP, b.readUnsignedShort() * 0.1));
        register(199, null, (p, b) -> p.set(Position.KEY_ODOMETER_TRIP, b.readUnsignedInt()));
        register(200, fmbXXX, (p, b) -> p.set("sleepMode", b.readUnsignedByte()));
        register(205, fmbXXX, (p, b) -> p.set("cid2g", b.readUnsignedShort()));
        register(206, fmbXXX, (p, b) -> p.set("lac", b.readUnsignedShort()));
        register(232, fmbXXX, (p, b) -> p.set("cngStatus", b.readUnsignedByte() > 0));
        register(233, fmbXXX, (p, b) -> p.set("cngUsed", b.readUnsignedInt() * 0.1));
        register(234, fmbXXX, (p, b) -> p.set("cngLevel", b.readUnsignedShort()));
        register(235, fmbXXX, (p, b) -> p.set("oilLevel", b.readUnsignedByte()));
        register(236, null, (p, b) -> {
            p.addAlarm(b.readUnsignedByte() > 0 ? Position.ALARM_GENERAL : null);
        });
        register(239, null, (p, b) -> p.set(Position.KEY_IGNITION, b.readUnsignedByte() > 0));
        register(240, null, (p, b) -> p.set(Position.KEY_MOTION, b.readUnsignedByte() > 0));
        register(241, null, (p, b) -> p.set(Position.KEY_OPERATOR, b.readUnsignedInt()));
        register(246, fmbXXX, (p, b) -> {
            p.addAlarm(b.readUnsignedByte() > 0 ? Position.ALARM_TOW : null);
        });
        register(247, fmbXXX, (p, b) -> {
            p.addAlarm(b.readUnsignedByte() > 0 ? Position.ALARM_ACCIDENT : null);
        });
        register(249, fmbXXX, (p, b) -> {
            p.addAlarm(b.readUnsignedByte() > 0 ? Position.ALARM_JAMMING : null);
        });
        register(251, fmbXXX, (p, b) -> {
            p.addAlarm(b.readUnsignedByte() > 0 ? Position.ALARM_IDLE : null);
        });
        register(252, fmbXXX, (p, b) -> {
            p.addAlarm(b.readUnsignedByte() > 0 ? Position.ALARM_POWER_CUT : null);
        });
        register(253, null, (p, b) -> {
            switch (b.readUnsignedByte()) {
                case 1 -> p.addAlarm(Position.ALARM_ACCELERATION);
                case 2 -> p.addAlarm(Position.ALARM_BRAKING);
                case 3 -> p.addAlarm(Position.ALARM_CORNERING);
            }
        });
        register(636, fmbXXX, (p, b) -> p.set("cid4g", b.readUnsignedInt()));
        register(662, fmbXXX, (p, b) -> p.set(Position.KEY_DOOR, b.readUnsignedByte() > 0));
    }

    private void decodeGh3000Parameter(Position position, int id, ByteBuf buf, int length) {
        switch (id) {
            case 1 -> position.set(Position.KEY_BATTERY_LEVEL, readValue(buf, length));
            case 2 -> position.set("usbConnected", readValue(buf, length) == 1);
            case 5 -> position.set("uptime", readValue(buf, length));
            case 20 -> position.set(Position.KEY_HDOP, readValue(buf, length) * 0.1);
            case 21 -> position.set(Position.KEY_VDOP, readValue(buf, length) * 0.1);
            case 22 -> position.set(Position.KEY_PDOP, readValue(buf, length) * 0.1);
            case 67 -> position.set(Position.KEY_BATTERY, readValue(buf, length) * 0.001);
            case 221 -> position.set("button", readValue(buf, length));
            case 222 -> {
                if (readValue(buf, length) == 1) {
                    position.addAlarm(Position.ALARM_SOS);
                }
            }
            case 240 -> position.set(Position.KEY_MOTION, readValue(buf, length) == 1);
            case 244 -> position.set(Position.KEY_ROAMING, readValue(buf, length) == 1);
            default -> position.set(Position.PREFIX_IO + id, readValue(buf, length));
        }
    }

    private void decodeParameter(Position position, int id, ByteBuf buf, int length, int codec, String model) {
        if (codec == CODEC_GH3000) {
            decodeGh3000Parameter(position, id, buf, length);
        } else {
            int index = buf.readerIndex();
            boolean decoded = false;
            for (var entry : PARAMETERS.getOrDefault(id, new HashMap<>()).entrySet()) {
                if (entry.getKey() == null || model != null && entry.getKey().contains(model)) {
                    entry.getValue().accept(position, buf);
                    decoded = true;
                    break;
                }
            }
            if (decoded) {
                buf.readerIndex(index + length);
            } else {
                position.set(Position.PREFIX_IO + id, readValue(buf, length));
            }
        }
    }

    private void decodeCell(
            Position position, Network network, String mncKey, String lacKey, String cidKey, String rssiKey) {
        if (position.hasAttribute(mncKey) && position.hasAttribute(lacKey) && position.hasAttribute(cidKey)) {
            CellTower cellTower = CellTower.from(
                    getConfig().getInteger(Keys.GEOLOCATION_MCC),
                    ((Number) position.getAttributes().remove(mncKey)).intValue(),
                    ((Number) position.getAttributes().remove(lacKey)).intValue(),
                    ((Number) position.getAttributes().remove(cidKey)).longValue());
            cellTower.setSignalStrength(((Number) position.getAttributes().remove(rssiKey)).intValue());
            network.addCellTower(cellTower);
        }
    }

    private void decodeNetwork(Position position, String model) {
        if ("TAT100".equals(model)) {
            Network network = new Network();
            decodeCell(position, network, "io1200", "io287", "io288", "io289");
            decodeCell(position, network, "io1201", "io290", "io291", "io292");
            decodeCell(position, network, "io1202", "io293", "io294", "io295");
            decodeCell(position, network, "io1203", "io296", "io297", "io298");
            if (network.getCellTowers() != null) {
                position.setNetwork(network);
            }
        } else {
            Integer cid2g = (Integer) position.getAttributes().remove("cid2g");
            Long cid4g = (Long) position.getAttributes().remove("cid4g");
            Integer lac = (Integer) position.getAttributes().remove("lac");
            if (lac != null && (cid2g != null || cid4g != null)) {
                Network network = new Network();
                CellTower cellTower;
                if (cid2g != null) {
                    cellTower = CellTower.fromLacCid(getConfig(), lac, cid2g);
                } else {
                    cellTower = CellTower.fromLacCid(getConfig(), lac, cid4g);
                    network.setRadioType("lte");
                }
                long operator = position.getInteger(Position.KEY_OPERATOR);
                if (operator >= 1000) {
                    cellTower.setOperator(operator);
                }
                network.addCellTower(cellTower);
                position.setNetwork(new Network(cellTower));
            }
        }
    }

    private int readExtByte(ByteBuf buf, int codec, int... codecs) {
        boolean ext = false;
        for (int c : codecs) {
            if (codec == c) {
                ext = true;
                break;
            }
        }
        if (ext) {
            return buf.readUnsignedShort();
        } else {
            return buf.readUnsignedByte();
        }
    }

    private void decodeLocation(Position position, ByteBuf buf, int codec, String model) {

        int globalMask = 0x0f;

        if (codec == CODEC_GH3000) {

            long time = buf.readUnsignedInt() & 0x3fffffff;
            time += 1167609600; // 2007-01-01 00:00:00

            globalMask = buf.readUnsignedByte();
            if (BitUtil.check(globalMask, 0)) {

                position.setTime(new Date(time * 1000));

                int locationMask = buf.readUnsignedByte();

                if (BitUtil.check(locationMask, 0)) {
                    position.setLatitude(buf.readFloat());
                    position.setLongitude(buf.readFloat());
                }

                if (BitUtil.check(locationMask, 1)) {
                    position.setAltitude(buf.readUnsignedShort());
                }

                if (BitUtil.check(locationMask, 2)) {
                    position.setCourse(buf.readUnsignedByte() * 360.0 / 256);
                }

                if (BitUtil.check(locationMask, 3)) {
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
                }

                if (BitUtil.check(locationMask, 4)) {
                    position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                }

                if (BitUtil.check(locationMask, 5)) {
                    CellTower cellTower = CellTower.fromLacCid(
                            getConfig(), buf.readUnsignedShort(), buf.readUnsignedShort());

                    if (BitUtil.check(locationMask, 6)) {
                        cellTower.setSignalStrength((int) buf.readUnsignedByte());
                    }

                    if (BitUtil.check(locationMask, 7)) {
                        cellTower.setOperator(buf.readUnsignedInt());
                    }

                    position.setNetwork(new Network(cellTower));

                } else {
                    if (BitUtil.check(locationMask, 6)) {
                        position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    }
                    if (BitUtil.check(locationMask, 7)) {
                        position.set(Position.KEY_OPERATOR, buf.readUnsignedInt());
                    }
                }

            } else {

                getLastLocation(position, new Date(time * 1000));

            }

        } else {

            position.setTime(new Date(buf.readLong()));

            position.set("priority", buf.readUnsignedByte());

            position.setLongitude(buf.readInt() / 10000000.0);
            position.setLatitude(buf.readInt() / 10000000.0);
            position.setAltitude(buf.readShort());
            position.setCourse(buf.readUnsignedShort());

            int satellites = buf.readUnsignedByte();
            position.set(Position.KEY_SATELLITES, satellites);

            position.setValid(satellites != 0);

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

            position.set(Position.KEY_EVENT, readExtByte(buf, codec, CODEC_8_EXT, CODEC_16));
            if (codec == CODEC_16) {
                buf.readUnsignedByte(); // generation type
            }

            readExtByte(buf, codec, CODEC_8_EXT); // total IO data records

        }

        // Read 1 byte data
        if (BitUtil.check(globalMask, 1)) {
            int cnt = readExtByte(buf, codec, CODEC_8_EXT);
            for (int j = 0; j < cnt; j++) {
                decodeParameter(position, readExtByte(buf, codec, CODEC_8_EXT, CODEC_16), buf, 1, codec, model);
            }
        }

        // Read 2 byte data
        if (BitUtil.check(globalMask, 2)) {
            int cnt = readExtByte(buf, codec, CODEC_8_EXT);
            for (int j = 0; j < cnt; j++) {
                decodeParameter(position, readExtByte(buf, codec, CODEC_8_EXT, CODEC_16), buf, 2, codec, model);
            }
        }

        // Read 4 byte data
        if (BitUtil.check(globalMask, 3)) {
            int cnt = readExtByte(buf, codec, CODEC_8_EXT);
            for (int j = 0; j < cnt; j++) {
                decodeParameter(position, readExtByte(buf, codec, CODEC_8_EXT, CODEC_16), buf, 4, codec, model);
            }
        }

        // Read 8 byte data
        if (codec == CODEC_8 || codec == CODEC_8_EXT || codec == CODEC_16) {
            int cnt = readExtByte(buf, codec, CODEC_8_EXT);
            for (int j = 0; j < cnt; j++) {
                decodeParameter(position, readExtByte(buf, codec, CODEC_8_EXT, CODEC_16), buf, 8, codec, model);
            }
        }

        // Read 16 byte data
        if (extended) {
            int cnt = readExtByte(buf, codec, CODEC_8_EXT);
            for (int j = 0; j < cnt; j++) {
                int id = readExtByte(buf, codec, CODEC_8_EXT, CODEC_16);
                position.set(Position.PREFIX_IO + id, ByteBufUtil.hexDump(buf.readSlice(16)));
            }
        }

        // Read X byte data
        if (codec == CODEC_8_EXT) {
            int cnt = buf.readUnsignedShort();
            for (int j = 0; j < cnt; j++) {
                int id = buf.readUnsignedShort();
                int length = buf.readUnsignedShort();
                if (id == 256) {
                    position.set(Position.KEY_VIN,
                            buf.readSlice(length).toString(StandardCharsets.US_ASCII));
                } else if (id == 281) {
                    position.set(Position.KEY_DTCS,
                            buf.readSlice(length).toString(StandardCharsets.US_ASCII).replace(',', ' '));
                } else if (id == 385) {
                    ByteBuf data = buf.readSlice(length);
                    data.readUnsignedByte(); // data part
                    int index = 1;
                    while (data.isReadable()) {
                        int flags = data.readUnsignedByte();
                        if (BitUtil.from(flags, 4) > 0) {
                            position.set("beacon" + index + "Uuid", ByteBufUtil.hexDump(data.readSlice(16)));
                            position.set("beacon" + index + "Major", data.readUnsignedShort());
                            position.set("beacon" + index + "Minor", data.readUnsignedShort());
                        } else {
                            position.set("beacon" + index + "Namespace", ByteBufUtil.hexDump(data.readSlice(10)));
                            position.set("beacon" + index + "Instance", ByteBufUtil.hexDump(data.readSlice(6)));
                        }
                        position.set("beacon" + index + "Rssi", (int) data.readByte());
                        if (BitUtil.check(flags, 1)) {
                            position.set("beacon" + index + "Battery", data.readUnsignedShort() * 0.01);
                        }
                        if (BitUtil.check(flags, 2)) {
                            position.set("beacon" + index + "Temp", data.readUnsignedShort());
                        }
                        index += 1;
                    }
                } else if (id == 548 || id == 10829 || id == 10831) {
                    ByteBuf data = buf.readSlice(length);
                    data.readUnsignedByte(); // header
                    for (int i = 1; data.isReadable(); i++) {
                        ByteBuf beacon = data.readSlice(data.readUnsignedByte());
                        while (beacon.isReadable()) {
                            int parameterId = beacon.readUnsignedByte();
                            int parameterLength = beacon.readUnsignedByte();
                            switch (parameterId) {
                                case 0 -> position.set("tag" + i + "Rssi", (int) beacon.readByte());
                                case 1 -> {
                                    String beaconId = ByteBufUtil.hexDump(beacon.readSlice(parameterLength));
                                    position.set("tag" + i + "Id", beaconId);
                                }
                                case 2 -> {
                                    ByteBuf beaconData = beacon.readSlice(parameterLength);
                                    int flag = beaconData.readUnsignedByte();
                                    if (BitUtil.check(flag, 6)) {
                                        position.set("tag" + i + "LowBattery", true);
                                    }
                                    if (BitUtil.check(flag, 7)) {
                                        position.set("tag" + i + "Voltage", beaconData.readUnsignedByte() * 10 + 2000);
                                    }
                                }
                                case 13 -> position.set("tag" + i + "LowBattery", beacon.readUnsignedByte());
                                case 14 -> position.set("tag" + i + "Battery", beacon.readUnsignedShort());
                                default -> beacon.skipBytes(parameterLength);
                            }
                        }
                    }
                } else {
                    position.set(Position.PREFIX_IO + id, ByteBufUtil.hexDump(buf.readSlice(length)));
                }
            }
        }

        decodeNetwork(position, model);

        if (model != null && model.matches("FM.6..")) {
            Long driverMsb = (Long) position.getAttributes().get("io195");
            Long driverLsb = (Long) position.getAttributes().get("io196");
            if (driverMsb != null && driverLsb != null) {
                String driver = new String(ByteBuffer.allocate(16).putLong(driverMsb).putLong(driverLsb).array());
                position.set(Position.KEY_DRIVER_UNIQUE_ID, driver);
            }
        }
    }

    private List<Position> parseData(
            Channel channel, SocketAddress remoteAddress, ByteBuf buf, int locationPacketId, String... imei) {
        List<Position> positions = new LinkedList<>();

        if (!connectionless) {
            buf.readUnsignedInt(); // data length
        }

        int codec = buf.readUnsignedByte();
        int count = buf.readUnsignedByte();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        for (int i = 0; i < count; i++) {
            Position position = new Position(getProtocolName());

            position.setDeviceId(deviceSession.getDeviceId());
            position.setValid(true);

            if (codec == CODEC_13) {
                buf.readUnsignedByte(); // type
                int length = buf.readInt() - 4;
                getLastLocation(position, new Date(buf.readUnsignedInt() * 1000));
                if (BufferUtil.isPrintable(buf, length)) {
                    String data = buf.readCharSequence(length, StandardCharsets.US_ASCII).toString().trim();
                    if (data.startsWith("GTSL")) {
                        position.set(Position.KEY_DRIVER_UNIQUE_ID, data.split("\\|")[4]);
                    } else {
                        position.set(Position.KEY_RESULT, data);
                    }
                } else {
                    position.set(Position.KEY_RESULT,
                            ByteBufUtil.hexDump(buf.readSlice(length)));
                }
            } else if (codec == CODEC_12) {
                decodeSerial(channel, remoteAddress, deviceSession, position, buf);
            } else {
                decodeLocation(position, buf, codec, getDeviceModel(deviceSession));
            }

            if (!position.getOutdated() || !position.getAttributes().isEmpty()) {
                positions.add(position);
            }
        }

        if (channel != null && codec != CODEC_12 && codec != CODEC_13) {
            ByteBuf response = Unpooled.buffer();
            if (connectionless) {
                response.writeShort(5);
                response.writeShort(0);
                response.writeByte(0x01);
                response.writeByte(locationPacketId);
                response.writeByte(count);
            } else {
                response.writeInt(count);
            }
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }

        return positions.isEmpty() ? null : positions;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (connectionless) {
            return decodeUdp(channel, remoteAddress, buf);
        } else {
            return decodeTcp(channel, remoteAddress, buf);
        }
    }

    private Object decodeTcp(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        if (buf.readableBytes() == 1 && buf.readUnsignedByte() == 0xff) {
            return null;
        } else if (buf.getUnsignedShort(0) > 0) {
            parseIdentification(channel, remoteAddress, buf);
        } else {
            buf.skipBytes(4);
            return parseData(channel, remoteAddress, buf, 0);
        }

        return null;
    }

    private Object decodeUdp(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        buf.readUnsignedShort(); // length
        buf.readUnsignedShort(); // packet id
        buf.readUnsignedByte(); // packet type
        int locationPacketId = buf.readUnsignedByte();
        String imei = buf.readSlice(buf.readUnsignedShort()).toString(StandardCharsets.US_ASCII);

        return parseData(channel, remoteAddress, buf, locationPacketId, imei);

    }

}
