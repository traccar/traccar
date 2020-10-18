/*
 * Copyright 2016 - 2019 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.ObdDecoder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class L100ProtocolDecoder extends BaseProtocolDecoder {

    public L100ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("ATL")
            .expression(",[^,]+,").optional()
            .number("(d{15}),")                  // imei
            .text("$GPRMC,")
            .number("(dd)(dd)(dd)")              // time (hhmmss.sss)
            .number(".(ddd)").optional()
            .expression(",([AV]),")              // validity
            .number("(d+)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d+)(dd.d+),")              // longitude
            .expression("([EW]),")
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .any()
            .text("#")
            .number("([01]+),")                  // io status
            .number("(d+.?d*|N.C),")             // adc
            .expression("[^,]*,")                // reserved
            .expression("[^,]*,")                // reserved
            .number("(d+.?d*),")                 // odometer
            .number("(d+.?d*),")                 // temperature
            .number("(d+.?d*),")                 // battery
            .number("(d+),")                     // rssi
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(x+),")                     // lac
            .number("(x+)")                      // cid
            .any()
            .text("ATL")
            .compile();

    private static final Pattern PATTERN_OBD_LOCATION = new PatternBuilder()
            .expression("[LH],")                 // archive
            .text("ATL,")
            .number("(d{15}),")                  // imei
            .number("(d+),")                     // type
            .number("(d+),")                     // index
            .groupBegin()
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .expression("([AV]),")               // validity
            .number("(d+.d+);([NS]),")           // latitude
            .number("(d+.d+);([EW]),")           // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(d+.d+),")                  // odometer
            .number("(d+.d+),")                  // battery
            .number("(d+),")                     // rssi
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(d+),")                     // lac
            .number("(x+),")                     // cid
            .number("#(d)(d)(d)(d),")            // status
            .number("(d),")                      // overspeed
            .text("ATL,")
            .groupEnd("?")
            .compile();

    private static final Pattern PATTERN_OBD_DATA = new PatternBuilder()
            .expression("[LH],")                 // archive
            .text("ATLOBD,")
            .number("(d{15}),")                  // imei
            .number("d+,")                       // type
            .number("d+,")                       // index
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .expression("[^,]+,")                // obd protocol
            .expression("(.+)")                  // data
            .compile();

    private static final Pattern PATTERN_NEW = new PatternBuilder()
            .groupBegin()
            .text("ATL,")
            .expression("[LH],")                 // archive
            .number("(d{15}),")                  // imei
            .groupEnd("?")
            .expression("([NPT]),")              // alarm
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([AV]),")               // validity
            .number("(d+.d+),([NS]),")           // latitude
            .number("(d+.d+),([EW]),")           // longitude
            .number("(d+.?d*),")                 // speed
            .expression("(?:GPS|GSM|INV),")
            .number("(d+),")                     // battery
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(d+),")                     // lac
            .number("(d+)")                      // cid
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("L") || sentence.startsWith("H")) {
            if (sentence.startsWith("ATLOBD", 2)) {
                return decodeObdData(channel, remoteAddress, sentence);
            } else {
                return decodeObdLocation(channel, remoteAddress, sentence);
            }
        } else if (!sentence.contains("$GPRMC")) {
            return decodeNew(channel, remoteAddress, sentence);
        } else {
            return decodeNormal(channel, remoteAddress, sentence);
        }
    }

    private Object decodeNormal(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt(), parser.nextInt(0));

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.set(Position.KEY_STATUS, parser.next());
        position.set(Position.PREFIX_ADC + 1, parser.next());
        position.set(Position.KEY_ODOMETER, parser.nextDouble());
        position.set(Position.PREFIX_TEMP + 1, parser.nextDouble());
        position.set(Position.KEY_BATTERY, parser.nextDouble());

        int rssi = parser.nextInt();
        if (rssi > 0) {
            position.setNetwork(new Network(CellTower.from(
                    parser.nextInt(), parser.nextInt(), parser.nextHexInt(), parser.nextHexInt(), rssi)));
        }

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(String.valueOf((char) 0x01), remoteAddress));
        }

        return position;
    }

    private Object decodeObdLocation(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_OBD_LOCATION, sentence);
        if (!parser.matches()) {
            return null;
        }

        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        int type = parser.nextInt();
        int index = parser.nextInt();

        if (type == 1) {
            if (channel != null) {
                String response = "@" + imei + ",00," + index + ",";
                response += "*" + (char) Checksum.xor(response);
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY));
        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setSpeed(parser.nextInt());
        position.setCourse(parser.nextInt());

        position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);
        position.set(Position.KEY_BATTERY, parser.nextDouble());

        int rssi = parser.nextInt();
        position.setNetwork(new Network(CellTower.from(
                parser.nextInt(), parser.nextInt(), parser.nextInt(), parser.nextHexInt(), rssi)));

        position.set(Position.KEY_IGNITION, parser.nextInt() == 1);
        parser.next(); // reserved

        switch (parser.nextInt()) {
            case 0:
                position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
                break;
            case 2:
                position.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
                break;
            case 1:
                position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
                break;
            default:
                break;
        }

        position.set(Position.KEY_CHARGE, parser.nextInt() == 1);

        if (parser.nextInt() == 1) {
            position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
        }

        return position;
    }

    private Object decodeObdData(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_OBD_DATA, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY));

        for (String entry : parser.next().split(",")) {
            String[] values = entry.split(":");
            if (values.length == 2 && values[1].charAt(0) != 'X') {
                position.add(ObdDecoder.decodeData(
                        Integer.parseInt(values[0].substring(2), 16), Integer.parseInt(values[1], 16), true));
            }
        }

        return position;
    }

    private Object decodeNew(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_NEW, sentence);
        if (!parser.matches()) {
            return null;
        }

        String imei = parser.next();
        DeviceSession deviceSession;
        if (imei != null) {
            deviceSession = getDeviceSession(channel, remoteAddress, imei);
        } else {
            deviceSession = getDeviceSession(channel, remoteAddress);
        }
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        switch (parser.next()) {
            case "P":
                position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                break;
            case "T":
                position.set(Position.KEY_ALARM, Position.ALARM_TAMPERING);
                break;
            default:
                break;
        }

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setSpeed(parser.nextDouble());

        position.set(Position.KEY_BATTERY, parser.nextInt() * 0.001);

        position.setNetwork(new Network(CellTower.from(
                parser.nextInt(), parser.nextInt(), parser.nextInt(), parser.nextHexInt())));

        return position;
    }

}
