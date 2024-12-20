/*
 * Copyright 2012 - 2023 Anton Tananaev (anton@traccar.org)
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
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class MeitrackProtocolDecoder extends BaseProtocolDecoder {

    private ByteBuf photo;

    public MeitrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$$").expression(".")          // flag
            .number("d+,")                       // length
            .number("(d+),")                     // imei
            .number("xxx,")                      // command
            .number("d+,").optional()
            .number("(d+),")                     // event
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+),")                // longitude
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("([AV]),")                   // validity
            .number("(d+),")                     // satellites
            .number("(d+),")                     // rssi
            .number("(d+.?d*),")                 // speed
            .number("(d+),")                     // course
            .number("(d+.?d*),")                 // hdop
            .number("(-?d+),")                   // altitude
            .number("(d+),")                     // odometer
            .number("(d+),")                     // runtime
            .number("(d+)|")                     // mcc
            .number("(d+)|")                     // mnc
            .number("(x+)?|")                    // lac
            .number("(x+)?,")                    // cid
            .number("(xx)")                      // input
            .number("(xx),")                     // output
            .groupBegin()
            .number("(d+.d+)|")                  // battery
            .number("(d+.d+)|")                  // power
            .number("d+.d+|")                    // rtc voltage
            .number("d+.d+|")                    // mcu voltage
            .number("d+.d+,")                    // gps voltage
            .or()
            .number("(x+)?|")                    // adc1
            .number("(x+)?|")                    // adc2
            .number("(x+)?|")                    // adc3
            .number("(x+)|")                     // battery
            .number("(x+)?,")                    // power
            .groupEnd()
            .groupBegin()
            .expression("([^,]+)?,").optional()  // event specific
            .expression("[^,]*,")                // reserved
            .number("(d+)?,")                    // protocol
            .number("(x{4})?")                   // fuel
            .groupBegin()
            .number(",(x{6}(?:|x{6})*)?")        // temperature
            .groupBegin()
            .number(",(d+)")                     // data count
            .expression(",([^*]*)")              // data
            .groupEnd("?")
            .groupEnd("?")
            .or()
            .any()
            .groupEnd()
            .text("*")
            .number("xx")
            .text("\r\n").optional()
            .compile();

    private String decodeAlarm(int event) {
        return switch (event) {
            case 1 -> Position.ALARM_SOS;
            case 17 -> Position.ALARM_LOW_BATTERY;
            case 18 -> Position.ALARM_LOW_POWER;
            case 19 -> Position.ALARM_OVERSPEED;
            case 20 -> Position.ALARM_GEOFENCE_ENTER;
            case 21 -> Position.ALARM_GEOFENCE_EXIT;
            case 22 -> Position.ALARM_POWER_RESTORED;
            case 23 -> Position.ALARM_POWER_CUT;
            case 36 -> Position.ALARM_TOW;
            case 44 -> Position.ALARM_JAMMING;
            case 78 -> Position.ALARM_ACCIDENT;
            case 90, 91 -> Position.ALARM_CORNERING;
            case 129 -> Position.ALARM_BRAKING;
            case 130 -> Position.ALARM_ACCELERATION;
            case 135 -> Position.ALARM_FATIGUE_DRIVING;
            default -> null;
        };
    }

    private Position decodeRegular(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        Parser parser = new Parser(PATTERN, buf.toString(StandardCharsets.US_ASCII));
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        int event = parser.nextInt();
        position.set(Position.KEY_EVENT, event);
        position.addAlarm(decodeAlarm(event));

        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());

        position.setTime(parser.nextDateTime());

        position.setValid(parser.next().equals("A"));

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        int rssi = parser.nextInt();

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());

        position.set(Position.KEY_HDOP, parser.nextDouble());

        position.setAltitude(parser.nextDouble());

        position.set(Position.KEY_ODOMETER, parser.nextInt());
        position.set("runtime", parser.next());

        int mcc = parser.nextInt();
        int mnc = parser.nextInt();
        int lac = parser.nextHexInt(0);
        int cid = parser.nextHexInt(0);
        if (mcc != 0 && mnc != 0) {
            position.setNetwork(new Network(CellTower.from(mcc, mnc, lac, cid, rssi)));
        }

        position.set(Position.KEY_INPUT, parser.nextHexInt());
        position.set(Position.KEY_OUTPUT, parser.nextHexInt());

        if (parser.hasNext(2)) {

            position.set(Position.KEY_BATTERY, parser.nextDouble());
            position.set(Position.KEY_POWER, parser.nextDouble());

        } else {

            for (int i = 1; i <= 3; i++) {
                position.set(Position.PREFIX_ADC + i, parser.nextHexInt());
            }

            switch (Objects.requireNonNullElse(getDeviceModel(deviceSession), "").toUpperCase()) {
                case "MVT340", "MVT380" -> {
                    position.set(Position.KEY_BATTERY, parser.nextHexInt() * 3.0 * 2.0 / 1024.0);
                    position.set(Position.KEY_POWER, parser.nextHexInt(0) * 3.0 * 16.0 / 1024.0);
                }
                case "MT90" -> {
                    position.set(Position.KEY_BATTERY, parser.nextHexInt() * 3.3 * 2.0 / 4096.0);
                    position.set(Position.KEY_POWER, parser.nextHexInt(0));
                }
                case "T1", "T3", "MVT100", "MVT600", "MVT800", "TC68", "TC68S" -> {
                    position.set(Position.KEY_BATTERY, parser.nextHexInt() * 3.3 * 2.0 / 4096.0);
                    position.set(Position.KEY_POWER, parser.nextHexInt(0) * 3.3 * 16.0 / 4096.0);
                }
                default -> {
                    position.set(Position.KEY_BATTERY, parser.nextHexInt() / 100.0);
                    position.set(Position.KEY_POWER, parser.nextHexInt(0) / 100.0);
                }
            }

        }

        String eventData = parser.next();
        if (eventData != null && !eventData.isEmpty()) {
            switch (event) {
                case 37 -> position.set(Position.KEY_DRIVER_UNIQUE_ID, eventData);
                default -> position.set("eventData", eventData);
            }
        }

        int protocol = parser.nextInt(0);

        if (parser.hasNext()) {
            String fuel = parser.next();
            position.set(Position.KEY_FUEL_LEVEL,
                    Integer.parseInt(fuel.substring(0, 2), 16) + Integer.parseInt(fuel.substring(2), 16) * 0.01);
        }

        if (parser.hasNext()) {
            for (String temp : parser.next().split("\\|")) {
                int index = Integer.parseInt(temp.substring(0, 2), 16);
                if (protocol >= 3) {
                    double value = (short) Integer.parseInt(temp.substring(2), 16);
                    position.set(Position.PREFIX_TEMP + index, value * 0.01);
                } else {
                    double value = Byte.parseByte(temp.substring(2, 4), 16);
                    value += (value < 0 ? -0.01 : 0.01) * Integer.parseInt(temp.substring(4), 16);
                    position.set(Position.PREFIX_TEMP + index, value);
                }
            }
        }

        if (parser.hasNext(2)) {
            parser.nextInt(); // count
            decodeDataFields(position, parser.next().split(","));
        }

        return position;
    }

    private void decodeDataFields(Position position, String[] values) {

        if (values.length > 1 && !values[1].isEmpty()) {
            position.set("tempData", values[1]);
        }

        if (values.length > 5 && !values[5].isEmpty()) {
            String[] data = values[5].split("\\|");
            boolean started = data[0].charAt(1) == '0';
            position.set("taximeterOn", started);
            position.set("taximeterStart", data[1]);
            if (data.length > 2) {
                position.set("taximeterEnd", data[2]);
                position.set("taximeterDistance", Integer.parseInt(data[3]));
                position.set("taximeterFare", Integer.parseInt(data[4]));
                position.set("taximeterTrip", data[5]);
                position.set("taximeterWait", data[6]);
            }
        }

    }

    private List<Position> decodeBinaryC(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {
        List<Position> positions = new LinkedList<>();

        String flag = buf.toString(2, 1, StandardCharsets.US_ASCII);
        int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ',');

        String imei = buf.toString(index + 1, 15, StandardCharsets.US_ASCII);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        buf.skipBytes(index + 1 + 15 + 1 + 3 + 1 + 2 + 2 + 4);

        while (buf.readableBytes() >= 0x34) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.set(Position.KEY_EVENT, buf.readUnsignedByte());

            position.setLatitude(buf.readIntLE() * 0.000001);
            position.setLongitude(buf.readIntLE() * 0.000001);

            position.setTime(new Date((946684800 + buf.readUnsignedIntLE()) * 1000)); // 946684800 = 2000-01-01

            position.setValid(buf.readUnsignedByte() == 1);

            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            int rssi = buf.readUnsignedByte();

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE()));
            position.setCourse(buf.readUnsignedShortLE());

            position.set(Position.KEY_HDOP, buf.readUnsignedShortLE() * 0.1);

            position.setAltitude(buf.readUnsignedShortLE());

            position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
            position.set("runtime", buf.readUnsignedIntLE());

            position.setNetwork(new Network(CellTower.from(
                    buf.readUnsignedShortLE(), buf.readUnsignedShortLE(),
                    buf.readUnsignedShortLE(), buf.readUnsignedShortLE(),
                    rssi)));

            position.set(Position.KEY_STATUS, buf.readUnsignedShortLE());

            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShortLE());
            position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.01);
            position.set(Position.KEY_POWER, buf.readUnsignedShortLE());

            buf.readUnsignedIntLE(); // geo-fence

            positions.add(position);
        }

        if (channel != null) {
            StringBuilder command = new StringBuilder("@@");
            command.append(flag).append(27 + positions.size() / 10).append(",");
            command.append(imei).append(",CCC,").append(positions.size()).append("*");
            command.append(Checksum.sum(command.toString()));
            command.append("\r\n");
            channel.writeAndFlush(new NetworkMessage(command.toString(), remoteAddress));
        }

        return positions;
    }

    private List<Position> decodeBinaryE(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {
        List<Position> positions = new LinkedList<>();

        buf.readerIndex(buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ',') + 1);
        String imei = buf.readSlice(15).toString(StandardCharsets.US_ASCII);
        buf.skipBytes(1 + 3 + 1);

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        buf.readUnsignedIntLE(); // remaining cache
        int count = buf.readUnsignedShortLE();

        for (int i = 0; i < count; i++) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            Network network = new Network();

            buf.readUnsignedShortLE(); // length
            buf.readUnsignedShortLE(); // index

            int paramCount = buf.readUnsignedByte();
            for (int j = 0; j < paramCount; j++) {
                boolean extension = buf.getUnsignedByte(buf.readerIndex()) == 0xFE;
                int id = extension ? buf.readUnsignedShort() : buf.readUnsignedByte();
                switch (id) {
                    case 0x01 -> position.set(Position.KEY_EVENT, buf.readUnsignedByte());
                    case 0x05 -> position.setValid(buf.readUnsignedByte() > 0);
                    case 0x06 -> position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                    case 0x07 -> position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    case 0x14 -> position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
                    case 0x15 -> position.set(Position.KEY_INPUT, buf.readUnsignedByte());
                    case 0x47 -> {
                        int lockState = buf.readUnsignedByte();
                        if (lockState > 0) {
                            position.set(Position.KEY_LOCK, lockState == 2);
                        }
                    }
                    case 0x97 -> position.set(Position.KEY_THROTTLE, buf.readUnsignedByte());
                    case 0x9D -> position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedByte());
                    case 0xFE69 -> position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                    default -> buf.readUnsignedByte();
                }
            }

            paramCount = buf.readUnsignedByte();
            for (int j = 0; j < paramCount; j++) {
                boolean extension = buf.getUnsignedByte(buf.readerIndex()) == 0xFE;
                int id = extension ? buf.readUnsignedShort() : buf.readUnsignedByte();
                switch (id) {
                    case 0x08 -> position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE()));
                    case 0x09 -> position.setCourse(buf.readUnsignedShortLE());
                    case 0x0A -> position.set(Position.KEY_HDOP, buf.readUnsignedShortLE());
                    case 0x0B -> position.setAltitude(buf.readShortLE());
                    case 0x16 -> position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShortLE() * 0.01);
                    case 0x17 -> position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShortLE() * 0.01);
                    case 0x19 -> position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.01);
                    case 0x1A -> position.set(Position.KEY_POWER, buf.readUnsignedShortLE() * 0.01);
                    case 0x29 -> position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedShortLE() * 0.01);
                    case 0x40 -> position.set(Position.KEY_EVENT, buf.readUnsignedShortLE());
                    case 0x91, 0x92 -> position.set(Position.KEY_OBD_SPEED, buf.readUnsignedShortLE());
                    case 0x98 -> position.set(Position.KEY_FUEL_USED, buf.readUnsignedShortLE());
                    case 0x99 -> position.set(Position.KEY_RPM, buf.readUnsignedShortLE());
                    case 0x9C -> position.set(Position.KEY_COOLANT_TEMP, buf.readUnsignedShortLE());
                    case 0x9F -> position.set(Position.PREFIX_TEMP + 1, buf.readUnsignedShortLE());
                    case 0xC9 -> position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedShortLE());
                    default -> buf.readUnsignedShortLE();
                }
            }

            paramCount = buf.readUnsignedByte();
            for (int j = 0; j < paramCount; j++) {
                boolean extension = buf.getUnsignedByte(buf.readerIndex()) == 0xFE;
                int id = extension ? buf.readUnsignedShort() : buf.readUnsignedByte();
                switch (id) {
                    case 0x02 -> position.setLatitude(buf.readIntLE() * 0.000001);
                    case 0x03 -> position.setLongitude(buf.readIntLE() * 0.000001);
                    case 0x04 -> position.setTime(new Date((946684800 + buf.readUnsignedIntLE()) * 1000)); // 2000-01-01
                    case 0x0C -> position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
                    case 0x0D -> position.set("runtime", buf.readUnsignedIntLE());
                    case 0x25 -> position.set(Position.KEY_DRIVER_UNIQUE_ID, String.valueOf(buf.readUnsignedIntLE()));
                    case 0x9B -> position.set(Position.KEY_OBD_ODOMETER, buf.readUnsignedIntLE());
                    case 0xA0 -> position.set(Position.KEY_FUEL_USED, buf.readUnsignedIntLE() * 0.001);
                    case 0xA2 -> position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedIntLE() * 0.01);
                    case 0xFEF4 -> position.set(Position.KEY_HOURS, buf.readUnsignedIntLE() * 60000);
                    default -> buf.readUnsignedIntLE();
                }
            }

            paramCount = buf.readUnsignedByte();
            for (int j = 0; j < paramCount; j++) {
                boolean extension = buf.getUnsignedByte(buf.readerIndex()) == 0xFE;
                int id = extension ? buf.readUnsignedShort() : buf.readUnsignedByte();
                int length = buf.readUnsignedByte();
                switch (id) {
                    case 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25 -> {
                        String wifiMac = ByteBufUtil.hexDump(buf.readSlice(6)).replaceAll("(..)", "$1:");
                        network.addWifiAccessPoint(WifiAccessPoint.from(
                                wifiMac.substring(0, wifiMac.length() - 1), buf.readShortLE()));
                    }
                    case 0x0E, 0x0F, 0x10, 0x12, 0x13 -> {
                        network.addCellTower(CellTower.from(
                                buf.readUnsignedShortLE(), buf.readUnsignedShortLE(),
                                buf.readUnsignedShortLE(), buf.readUnsignedIntLE(), buf.readShortLE()));
                    }
                    case 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31 -> {
                        buf.readUnsignedByte(); // label
                        position.set(Position.PREFIX_TEMP + (id - 0x2A), buf.readShortLE() * 0.01);
                    }
                    case 0x4B -> buf.skipBytes(length); // network information
                    case 0xFE31 -> {
                        buf.readUnsignedByte(); // alarm protocol
                        buf.readUnsignedByte(); // alarm type
                        buf.skipBytes(length - 2);
                    }
                    case 0xFE73 -> {
                        buf.readUnsignedByte(); // version
                        position.set(
                                "tagName",
                                buf.readCharSequence(buf.readUnsignedByte(), StandardCharsets.US_ASCII).toString());
                        buf.skipBytes(6); // mac
                        position.set("tagBattery", buf.readUnsignedByte());
                        position.set("tagTemp", buf.readShortLE() / 256.0);
                        position.set("tagHumidity", buf.readShortLE() / 256.0);
                        buf.readUnsignedShortLE(); // high temperature threshold
                        buf.readUnsignedShortLE(); // low temperature threshold
                        buf.readUnsignedShortLE(); // high humidity threshold
                        buf.readUnsignedShortLE(); // low humidity threshold
                    }
                    case 0xFEA8 -> {
                        for (int k = 1; k <= 3; k++) {
                            if (buf.readUnsignedByte() > 0) {
                                String key = k == 1 ? Position.KEY_BATTERY_LEVEL : "battery" + k + "Level";
                                position.set(key, buf.readUnsignedByte());
                            } else {
                                buf.readUnsignedByte();
                            }
                        }
                        buf.readUnsignedByte(); // battery alert
                    }
                    default -> buf.skipBytes(length);
                }
            }

            if (network.getCellTowers() != null || network.getWifiAccessPoints() != null) {
                position.setNetwork(network);
            }
            positions.add(position);
        }

        return positions;
    }

    private void requestPhotoPacket(Channel channel, SocketAddress remoteAddress, String imei, String file, int index) {
        if (channel != null) {
            String content = "D00," + file + "," + index;
            int length = 1 + imei.length() + 1 + content.length() + 5;
            String response = String.format("@@O%02d,%s,%s*", length, imei, content);
            response += Checksum.sum(response) + "\r\n";
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ',');
        String imei = buf.toString(index + 1, 15, StandardCharsets.US_ASCII);
        index = buf.indexOf(index + 1, buf.writerIndex(), (byte) ',');
        String type = buf.toString(index + 1, 3, StandardCharsets.US_ASCII);

        return switch (type) {
            case "AAC" -> {
                if (channel != null) {
                    String response = String.format("@@z27,%s,AAC,1*", imei);
                    response += Checksum.sum(response) + "\r\n";
                    channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
                }
                yield null;
            }
            case "D00" -> {
                if (photo == null) {
                    photo = Unpooled.buffer();
                }

                index = index + 1 + type.length() + 1;
                int endIndex = buf.indexOf(index, buf.writerIndex(), (byte) ',');
                String file = buf.toString(index, endIndex - index, StandardCharsets.US_ASCII);
                index = endIndex + 1;
                endIndex = buf.indexOf(index, buf.writerIndex(), (byte) ',');
                int total = Integer.parseInt(buf.toString(index, endIndex - index, StandardCharsets.US_ASCII));
                index = endIndex + 1;
                endIndex = buf.indexOf(index, buf.writerIndex(), (byte) ',');
                int current = Integer.parseInt(buf.toString(index, endIndex - index, StandardCharsets.US_ASCII));

                buf.readerIndex(endIndex + 1);
                photo.writeBytes(buf.readSlice(buf.readableBytes() - 1 - 2 - 2));

                if (current == total - 1) {
                    Position position = new Position(getProtocolName());
                    position.setDeviceId(getDeviceSession(channel, remoteAddress, imei).getDeviceId());

                    getLastLocation(position, null);

                    position.set(Position.KEY_IMAGE, writeMediaFile(imei, photo, "jpg"));
                    photo.release();
                    photo = null;

                    yield position;
                } else {
                    if ((current + 1) % 8 == 0) {
                        requestPhotoPacket(channel, remoteAddress, imei, file, current + 1);
                    }
                    yield null;
                }
            }
            case "D03" -> {
                photo = Unpooled.buffer();
                requestPhotoPacket(channel, remoteAddress, imei, "camera_picture.jpg", 0);
                yield null;
            }
            case "D82" -> {
                Position position = new Position(getProtocolName());
                position.setDeviceId(getDeviceSession(channel, remoteAddress, imei).getDeviceId());
                getLastLocation(position, null);
                String result = buf.toString(index + 1, buf.writerIndex() - index - 4, StandardCharsets.US_ASCII);
                position.set(Position.KEY_RESULT, result);
                yield position;
            }
            case "CCC" -> decodeBinaryC(channel, remoteAddress, buf);
            case "CCE" -> decodeBinaryE(channel, remoteAddress, buf);
            default -> decodeRegular(channel, remoteAddress, buf);
        };
    }

}
