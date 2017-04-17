/*
 * Copyright 2012 - 2016 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Jt600ProtocolDecoder extends BaseProtocolDecoder {

    public Jt600ProtocolDecoder(Jt600Protocol protocol) {
        super(protocol);
    }

    private static double convertCoordinate(int raw) {
        int degrees = raw / 1000000;
        double minutes = (raw % 1000000) / 10000.0;
        return degrees + minutes / 60;
    }

    private Position decodeBinary(ChannelBuffer buf, Channel channel, SocketAddress remoteAddress) {

        Position position = new Position();
        position.setProtocol(getProtocolName());

        buf.readByte(); // header

        boolean longFormat = buf.getUnsignedByte(buf.readerIndex()) == 0x75;

        String id = String.valueOf(Long.parseLong(ChannelBuffers.hexDump(buf.readBytes(5))));
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        if (longFormat) {
            buf.readUnsignedByte(); // protocol
        }

        int version = buf.readUnsignedByte() >> 4;
        buf.readUnsignedShort(); // length

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

        if (longFormat) {

            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 1000);
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());

            buf.readUnsignedInt(); // vehicle id combined

            position.set(Position.KEY_STATUS, buf.readUnsignedShort());

            int battery = buf.readUnsignedByte();
            if (battery == 0xff) {
                position.set(Position.KEY_CHARGE, true);
            } else {
                position.set(Position.KEY_BATTERY, battery + "%");
            }

            CellTower cellTower = CellTower.fromCidLac(buf.readUnsignedShort(), buf.readUnsignedShort());
            cellTower.setSignalStrength((int) buf.readUnsignedByte());
            position.setNetwork(new Network(cellTower));

            position.set(Position.KEY_INDEX, buf.readUnsignedByte());

        } else if (version == 1) {

            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            position.set(Position.KEY_POWER, buf.readUnsignedByte());

            buf.readByte(); // other flags and sensors

            position.setAltitude(buf.readUnsignedShort());

            int cid  = buf.readUnsignedShort();
            int lac  = buf.readUnsignedShort();
            int rssi = buf.readUnsignedByte();

            if (cid != 0 && lac != 0) {
                CellTower cellTower = CellTower.fromCidLac(cid, lac);
                cellTower.setSignalStrength(rssi);
                position.setNetwork(new Network(cellTower));
            } else {
                position.set(Position.KEY_RSSI, rssi);
            }

        } else if (version == 2) {

            int fuel = buf.readUnsignedByte() << 8;

            position.set(Position.KEY_STATUS, buf.readUnsignedInt());
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 1000);

            fuel += buf.readUnsignedByte();
            position.set(Position.KEY_FUEL_LEVEL, fuel);

        }

        return position;
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

        Position position = new Position();
        position.setProtocol(getProtocolName());
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
            .number("(d+%),")                    // battery
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

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.setValid(parser.next().equals("T"));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));

        position.setSpeed(UnitsConverter.knotsFromMph(parser.nextDouble(0)));
        position.setCourse(parser.nextDouble(0));

        position.set(Position.KEY_SATELLITES, parser.nextInt(0));
        position.set(Position.KEY_BATTERY, parser.next());
        position.set(Position.KEY_STATUS, parser.nextBinInt(0));

        CellTower cellTower = CellTower.fromCidLac(parser.nextInt(0), parser.nextInt(0));
        cellTower.setSignalStrength(parser.nextInt(0));
        position.setNetwork(new Network(cellTower));

        position.set(Position.KEY_ODOMETER, parser.nextLong(0) * 1000);
        position.set(Position.KEY_INDEX, parser.nextInt(0));

        if (channel != null) {
            if (type.equals("U01") || type.equals("U02") || type.equals("U03")) {
                channel.write("(S39)");
            } else if (type.equals("U06")) {
                channel.write("(S20)");
            }
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;
        char first = (char) buf.getByte(0);

        if (first == '$') {
            return decodeBinary(buf, channel, remoteAddress);
        } else if (first == '(') {
            String sentence = buf.toString(StandardCharsets.US_ASCII);
            if (sentence.contains("W01")) {
                return decodeW01(sentence, channel, remoteAddress);
            } else {
                return decodeU01(sentence, channel, remoteAddress);
            }
        }

        return null;
    }

}
