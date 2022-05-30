/*
 * Copyright 2012 - 2021 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitBuffer;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class Jt600ProtocolDecoder extends BaseProtocolDecoder {

    public Jt600ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static double convertCoordinate(int raw) {
        int degrees = raw / 1000000;
        double minutes = (raw % 1000000) / 10000.0;
        return degrees + minutes / 60;
    }

    private void decodeStatus(Position position, ByteBuf buf) {

        int value = buf.readUnsignedByte();

        position.set(Position.KEY_IGNITION, BitUtil.check(value, 0));
        position.set(Position.KEY_DOOR, BitUtil.check(value, 6));

        value = buf.readUnsignedByte();

        position.set(Position.KEY_CHARGE, BitUtil.check(value, 0));
        position.set(Position.KEY_BLOCKED, BitUtil.check(value, 1));

        if (BitUtil.check(value, 2)) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        }
        if (BitUtil.check(value, 3) || BitUtil.check(value, 4)) {
            position.set(Position.KEY_ALARM, Position.ALARM_GPS_ANTENNA_CUT);
        }
        if (BitUtil.check(value, 4)) {
            position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
        }

        value = buf.readUnsignedByte();

        if (BitUtil.check(value, 2)) {
            position.set(Position.KEY_ALARM, Position.ALARM_FATIGUE_DRIVING);
        }
        if (BitUtil.check(value, 3)) {
            position.set(Position.KEY_ALARM, Position.ALARM_TOW);
        }

        buf.readUnsignedByte(); // reserved

    }

    static boolean isLongFormat(ByteBuf buf, int flagIndex) {
        return buf.getUnsignedByte(flagIndex) >> 4 >= 7;
    }

    static void decodeBinaryLocation(ByteBuf buf, Position position) {

        DateBuilder dateBuilder = new DateBuilder()
                .setDay(BcdUtil.readInteger(buf, 2))
                .setMonth(BcdUtil.readInteger(buf, 2))
                .setYear(BcdUtil.readInteger(buf, 2))
                .setHour(BcdUtil.readInteger(buf, 2))
                .setMinute(BcdUtil.readInteger(buf, 2))
                .setSecond(BcdUtil.readInteger(buf, 2));
        position.setTime(dateBuilder.getDate());

        double latitude = convertCoordinate(BcdUtil.readInteger(buf, 8));
        double longitude = convertCoordinate(BcdUtil.readInteger(buf, 9));

        byte flags = buf.readByte();
        position.setValid((flags & 0x1) == 0x1);
        if ((flags & 0x2) == 0) {
            latitude = -latitude;
        }
        position.setLatitude(latitude);
        if ((flags & 0x4) == 0) {
            longitude = -longitude;
        }
        position.setLongitude(longitude);

        position.setSpeed(BcdUtil.readInteger(buf, 2));
        position.setCourse(buf.readUnsignedByte() * 2.0);
    }

    private List<Position> decodeBinary(ByteBuf buf, Channel channel, SocketAddress remoteAddress) {

        List<Position> positions = new LinkedList<>();

        buf.readByte(); // header

        boolean longFormat = isLongFormat(buf, buf.readerIndex());

        String id = String.valueOf(Long.parseLong(ByteBufUtil.hexDump(buf.readSlice(5))));
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        int protocolVersion = 0;
        if (longFormat) {
            protocolVersion = buf.readUnsignedByte();
        }

        int version = BitUtil.from(buf.readUnsignedByte(), 4);
        buf.readUnsignedShort(); // length

        boolean responseRequired = false;

        while (buf.readableBytes() > 1) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            decodeBinaryLocation(buf, position);

            if (longFormat) {

                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 1000);
                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());

                buf.readUnsignedInt(); // vehicle id combined

                int status = buf.readUnsignedShort();
                position.set(Position.KEY_ALARM, BitUtil.check(status, 1) ? Position.ALARM_GEOFENCE_ENTER : null);
                position.set(Position.KEY_ALARM, BitUtil.check(status, 2) ? Position.ALARM_GEOFENCE_EXIT : null);
                position.set(Position.KEY_ALARM, BitUtil.check(status, 3) ? Position.ALARM_POWER_CUT : null);
                position.set(Position.KEY_ALARM, BitUtil.check(status, 4) ? Position.ALARM_VIBRATION : null);
                if (BitUtil.check(status, 5)) {
                    responseRequired = true;
                }
                position.set(Position.KEY_BLOCKED, BitUtil.check(status, 7));
                position.set(Position.KEY_ALARM, BitUtil.check(status, 8 + 3) ? Position.ALARM_LOW_BATTERY : null);
                position.set(Position.KEY_ALARM, BitUtil.check(status, 8 + 6) ? Position.ALARM_FAULT : null);
                position.set(Position.KEY_STATUS, status);

                int battery = buf.readUnsignedByte();
                if (battery == 0xff) {
                    position.set(Position.KEY_CHARGE, true);
                } else {
                    position.set(Position.KEY_BATTERY_LEVEL, battery);
                }

                CellTower cellTower = CellTower.fromCidLac(
                        getConfig(), buf.readUnsignedShort(), buf.readUnsignedShort());
                cellTower.setSignalStrength((int) buf.readUnsignedByte());
                position.setNetwork(new Network(cellTower));

                if (protocolVersion == 0x17 || protocolVersion == 0x19) {
                    buf.readUnsignedByte(); // geofence id
                    buf.skipBytes(3); // reserved
                    buf.skipBytes(buf.readableBytes() - 1);
                }

            } else if (version == 1) {

                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                position.set(Position.KEY_POWER, buf.readUnsignedByte());

                buf.readByte(); // other flags and sensors

                position.setAltitude(buf.readUnsignedShort());

                int cid = buf.readUnsignedShort();
                int lac = buf.readUnsignedShort();
                int rssi = buf.readUnsignedByte();

                if (cid != 0 && lac != 0) {
                    CellTower cellTower = CellTower.fromCidLac(getConfig(), cid, lac);
                    cellTower.setSignalStrength(rssi);
                    position.setNetwork(new Network(cellTower));
                } else {
                    position.set(Position.KEY_RSSI, rssi);
                }

            } else if (version == 2) {

                int fuel = buf.readUnsignedByte() << 8;

                decodeStatus(position, buf);
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 1000);

                fuel += buf.readUnsignedByte();
                position.set(Position.KEY_FUEL_LEVEL, fuel);

            } else if (version == 3) {

                BitBuffer bitBuffer = new BitBuffer(buf);

                position.set("fuel1", bitBuffer.readUnsigned(12));
                position.set("fuel2", bitBuffer.readUnsigned(12));
                position.set("fuel3", bitBuffer.readUnsigned(12));
                position.set(Position.KEY_ODOMETER, bitBuffer.readUnsigned(20) * 1000);

                int status = bitBuffer.readUnsigned(24);
                position.set(Position.KEY_IGNITION, BitUtil.check(status, 0));
                position.set(Position.KEY_STATUS, status);

            }

            positions.add(position);

        }

        int index = buf.readUnsignedByte();

        if (channel != null && responseRequired) {
            if (protocolVersion < 0x19) {
                channel.writeAndFlush(new NetworkMessage("(P35)", remoteAddress));
            } else {
                channel.writeAndFlush(new NetworkMessage("(P69,0," + index + ")", remoteAddress));
            }
        }

        return positions;
    }

    private static final Pattern PATTERN_W01 = new PatternBuilder()
            .text("(")
            .number("(d+),")                     // id
            .text("W01,")                        // type
            .number("(ddd)(dd.dddd),")           // longitude
            .expression("([EW]),")
            .number("(dd)(dd.dddd),")            // latitude
            .expression("([NS]),")
            .expression("([AV]),")               // validity
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(d+),")                     // power
            .number("(d+),")                     // gps signal
            .number("(d+),")                     // gsm signal
            .number("(d+),")                     // alert type
            .any()
            .compile();

    private Position decodeW01(String sentence, Channel channel, SocketAddress remoteAddress) {

        Parser parser = new Parser(PATTERN_W01, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setLongitude(parser.nextCoordinate());
        position.setLatitude(parser.nextCoordinate());
        position.setValid(parser.next().equals("A"));

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
        position.setCourse(parser.nextDouble(0));

        position.set(Position.KEY_POWER, parser.nextDouble(0));
        position.set(Position.KEY_GPS, parser.nextInt(0));
        position.set(Position.KEY_RSSI, parser.nextInt(0));
        position.set("alertType", parser.nextInt(0));

        return position;
    }

    private static final Pattern PATTERN_U01 = new PatternBuilder()
            .text("(")
            .number("(d+),")                     // id
            .number("(Udd),")                    // type
            .number("d+,").optional()            // alarm
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([TF]),")               // validity
            .number("(d+.d+),([NS]),")           // latitude
            .number("(d+.d+),([EW]),")           // longitude
            .number("(d+.?d*),")                 // speed
            .number("(d+),")                     // course
            .number("(d+),")                     // satellites
            .number("(d+)%,")                    // battery
            .expression("([01]+),")              // status
            .number("(d+),")                     // cid
            .number("(d+),")                     // lac
            .number("(d+),")                     // gsm signal
            .number("(d+),")                     // odometer
            .number("(d+)")                      // serial number
            .number(",(xx)").optional()          // checksum
            .any()
            .compile();

    private Position decodeU01(String sentence, Channel channel, SocketAddress remoteAddress) {

        Parser parser = new Parser(PATTERN_U01, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        String type = parser.next();

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.setValid(parser.next().equals("T"));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));

        position.setSpeed(UnitsConverter.knotsFromMph(parser.nextDouble(0)));
        position.setCourse(parser.nextDouble(0));

        position.set(Position.KEY_SATELLITES, parser.nextInt(0));
        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt(0));
        position.set(Position.KEY_STATUS, parser.nextBinInt(0));

        CellTower cellTower = CellTower.fromCidLac(getConfig(), parser.nextInt(0), parser.nextInt(0));
        cellTower.setSignalStrength(parser.nextInt(0));
        position.setNetwork(new Network(cellTower));

        position.set(Position.KEY_ODOMETER, parser.nextLong(0) * 1000);
        position.set(Position.KEY_INDEX, parser.nextInt(0));

        if (channel != null) {
            if (type.equals("U01") || type.equals("U02") || type.equals("U03")) {
                channel.writeAndFlush(new NetworkMessage("(S39)", remoteAddress));
            } else if (type.equals("U06")) {
                channel.writeAndFlush(new NetworkMessage("(S20)", remoteAddress));
            }
        }

        return position;
    }

    private static final Pattern PATTERN_P45 = new PatternBuilder()
            .text("(")
            .number("(d+),")                     // id
            .text("P45,")                        // type
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(d+.d+),([NS]),")           // latitude
            .number("(d+.d+),([EW]),")           // longitude
            .expression("([AV]),")               // validity
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("d+,")                       // event source
            .number("d+,")                       // unlock verification
            .number("(d+),")                     // rfid
            .number("d+,")                       // password verification
            .number("d+,")                       // incorrect password count
            .number("(d+),")                     // index
            .any()
            .compile();

    private Position decodeP45(String sentence, Channel channel, SocketAddress remoteAddress) {

        Parser parser = new Parser(PATTERN_P45, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setValid(parser.next().equals("A"));

        position.setSpeed(UnitsConverter.knotsFromMph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());

        String rfid = parser.next();
        if (!rfid.equals("0000000000")) {
            position.set(Position.KEY_DRIVER_UNIQUE_ID, rfid);
        }

        int index = parser.nextInt();

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage("(P69,0," + index + ")", remoteAddress));
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        char first = (char) buf.getByte(0);

        if (first == '$') {
            return decodeBinary(buf, channel, remoteAddress);
        } else if (first == '(') {
            String sentence = buf.toString(StandardCharsets.US_ASCII);
            if (sentence.contains("W01")) {
                return decodeW01(sentence, channel, remoteAddress);
            } else if (sentence.contains("P45")) {
                return decodeP45(sentence, channel, remoteAddress);
            } else {
                return decodeU01(sentence, channel, remoteAddress);
            }
        }

        return null;
    }

}
