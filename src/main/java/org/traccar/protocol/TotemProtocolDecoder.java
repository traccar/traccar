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

import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class TotemProtocolDecoder extends BaseProtocolDecoder {

    public TotemProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN1 = new PatternBuilder()
            .text("$$")                          // header
            .number("xx")                        // length
            .number("(d+)|")                     // imei
            .expression("(..)")                  // alarm
            .text("$GPRMC,")
            .number("(dd)(dd)(dd).d+,")          // time (hhmmss)
            .expression("([AV]),")               // validity
            .number("(d+)(dd.d+),([NS]),")       // latitude
            .number("(d+)(dd.d+),([EW]),")       // longitude
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .expression("[^*]*").text("*")
            .number("xx|")                       // checksum
            .number("(d+.d+)|")                  // pdop
            .number("(d+.d+)|")                  // hdop
            .number("(d+.d+)|")                  // vdop
            .number("(d+)|")                     // io status
            .number("d+|")                       // battery time
            .number("d")                         // charged
            .number("(ddd)")                     // battery
            .number("(dddd)|")                   // power
            .number("(d+)|").optional()          // adc
            .number("x*(xxxx)")                  // lac
            .number("(xxxx)|")                   // cid
            .number("(d+)|")                     // temperature
            .number("(d+.d+)|")                  // odometer
            .number("d+|")                       // serial number
            .any()
            .number("xxxx")                      // checksum
            .any()
            .compile();

    private static final Pattern PATTERN2 = new PatternBuilder()
            .text("$$")                          // header
            .number("xx")                        // length
            .number("(d+)|")                     // imei
            .expression("(..)")                  // alarm type
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .number("(dd)(dd)(dd)|")             // time (hhmmss)
            .expression("([AV])|")               // validity
            .number("(d+)(dd.d+)|")              // latitude
            .expression("([NS])|")
            .number("(d+)(dd.d+)|")              // longitude
            .expression("([EW])|")
            .number("(d+.d+)?|")                 // speed
            .number("(d+)?|")                    // course
            .number("(d+.d+)|")                  // hdop
            .number("(d+)|")                     // io status
            .number("d")                         // charged
            .number("(dd)")                      // battery
            .number("(dd)|")                     // external power
            .number("(d+)|")                     // adc
            .number("(xxxx)")                    // lac
            .number("(xxxx)|")                   // cid
            .number("(d+)|")                     // temperature
            .number("(d+.d+)|")                  // odometer
            .number("d+|")                       // serial number
            .number("xxxx")                      // checksum
            .any()
            .compile();

    private static final Pattern PATTERN3 = new PatternBuilder()
            .text("$$")                          // header
            .number("xx")                        // length
            .number("(d+)|")                     // imei
            .expression("(..)")                  // alarm type
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .number("(xxxx)")                    // io status
            .expression("[01]")                  // charging
            .number("(dd)")                      // battery
            .number("(dd)")                      // external power
            .number("(dddd)")                    // adc 1
            .number("(dddd)")                    // adc 2
            .number("(ddd)")                     // temperature 1
            .number("(ddd)")                     // temperature 2
            .number("(xxxx)")                    // lac
            .number("(xxxx)")                    // cid
            .expression("([AV])")                // validity
            .number("(dd)")                      // satellites
            .number("(ddd)")                     // course
            .number("(ddd)")                     // speed
            .number("(dd.d)")                    // pdop
            .number("(d{7})")                    // odometer
            .number("(dd)(dd.dddd)([NS])")       // latitude
            .number("(ddd)(dd.dddd)([EW])")      // longitude
            .number("dddd")                      // serial number
            .number("xxxx")                      // checksum
            .any()
            .compile();

    private static final Pattern PATTERN4 = new PatternBuilder()
            .text("$$")                          // header
            .number("dddd")                      // length
            .number("(xx)")                      // type
            .number("(d+)|")                     // imei
            .number("(x{8})")                    // status
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .number("(dd)")                      // battery
            .number("(dd)")                      // external power
            .number("(dddd)")                    // adc 1
            .groupBegin()
            .groupBegin()
            .number("(dddd)")                    // adc 2
            .number("(dddd)")                    // adc 3
            .number("(dddd)")                    // adc 4
            .groupEnd("?")
            .number("(dddd)")                    // temperature 1
            .number("(dddd)?")                   // temperature 2
            .groupEnd("?")
            .number("(xxxx)")                    // lac
            .number("(xxxx)")                    // cid
            .groupBegin()
            .number("(dd)")                      // mcc
            .number("(ddd)")                     // mnc
            .groupEnd("?")
            .number("(dd)")                      // satellites
            .number("(dd)")                      // gsm (rssi)
            .number("(ddd)")                     // course
            .number("(ddd)")                     // speed
            .number("(dd.d)")                    // hdop
            .number("(d{7})")                    // odometer
            .number("(dd)(dd.dddd)([NS])")       // latitude
            .number("(ddd)(dd.dddd)([EW])")      // longitude
            .number("dddd")                      // serial number
            .number("xx")                        // checksum
            .any()
            .compile();

    private static final Pattern PATTERN_OBD = new PatternBuilder()
            .text("$$")                          // header
            .number("dddd")                      // length
            .number("xx")                        // type
            .number("(d+)|")                     // imei
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(-?d+.d+),")                // longitude
            .number("(-?d+.d+),")                // latitude
            .expression("[^,]*,")                // obd version
            .number("(d+),")                     // odometer
            .number("(d+),")                     // fuel used
            .number("(d+),")                     // fuel consumption
            .number("(d+),")                     // power
            .number("(d+),")                     // rpm
            .number("(d+),")                     // speed
            .number("(d+),")                     // intake flow
            .number("(d+),")                     // intake pressure
            .number("(d+),")                     // coolant temperature
            .number("(d+),")                     // intake temperature
            .number("(d+),")                     // engine load
            .number("(d+),")                     // throttle
            .number("(d+),")                     // fuel
            .number("|xx")                       // checksum
            .any()
            .compile();

    private String decodeAlarm123(int value) {
        switch (value) {
            case 0x01:
                return Position.ALARM_SOS;
            case 0x10:
                return Position.ALARM_LOW_BATTERY;
            case 0x11:
                return Position.ALARM_OVERSPEED;
            case 0x30:
                return Position.ALARM_PARKING;
            case 0x42:
                return Position.ALARM_GEOFENCE_EXIT;
            case 0x43:
                return Position.ALARM_GEOFENCE_ENTER;
            default:
                return null;
        }
    }

    private String decodeAlarm4(int value) {
        switch (value) {
            case 0x01:
                return Position.ALARM_SOS;
            case 0x02:
                return Position.ALARM_OVERSPEED;
            case 0x04:
                return Position.ALARM_GEOFENCE_EXIT;
            case 0x05:
                return Position.ALARM_GEOFENCE_ENTER;
            case 0x40:
                return Position.ALARM_SHOCK;
            case 0x42:
                return Position.ALARM_ACCELERATION;
            case 0x43:
                return Position.ALARM_BRAKING;
            default:
                return null;
        }
    }

    private boolean decode12(Position position, Parser parser, Pattern pattern) {

        if (parser.hasNext()) {
            position.set(Position.KEY_ALARM, decodeAlarm123(Short.parseShort(parser.next(), 16)));
        }
        DateBuilder dateBuilder = new DateBuilder();
        int year = 0, month = 0, day = 0;
        if (pattern == PATTERN2) {
            day   = parser.nextInt(0);
            month = parser.nextInt(0);
            year  = parser.nextInt(0);
        }
        dateBuilder.setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        if (pattern == PATTERN1) {
            day   = parser.nextInt(0);
            month = parser.nextInt(0);
            year  = parser.nextInt(0);
        }
        if (year == 0) {
            return false; // ignore invalid data
        }
        dateBuilder.setDate(year, month, day);
        position.setTime(dateBuilder.getDate());

        if (pattern == PATTERN1) {
            position.set(Position.KEY_PDOP, parser.nextDouble());
            position.set(Position.KEY_HDOP, parser.nextDouble());
            position.set(Position.KEY_VDOP, parser.nextDouble());
        } else {
            position.set(Position.KEY_HDOP, parser.nextDouble());
        }

        int io = parser.nextBinInt();
        position.set(Position.KEY_STATUS, io);
        if (pattern == PATTERN1) {
            position.set(Position.KEY_ALARM, BitUtil.check(io, 0) ? Position.ALARM_SOS : null);
            position.set(Position.PREFIX_IN + 3, BitUtil.check(io, 4));
            position.set(Position.PREFIX_IN + 4, BitUtil.check(io, 5));
            position.set(Position.PREFIX_IN + 1, BitUtil.check(io, 6));
            position.set(Position.PREFIX_IN + 2, BitUtil.check(io, 7));
            position.set(Position.PREFIX_OUT + 1, BitUtil.check(io, 8));
            position.set(Position.PREFIX_OUT + 2, BitUtil.check(io, 9));
            position.set(Position.KEY_BATTERY, parser.nextDouble(0) * 0.01);
        } else {
            position.set(Position.KEY_ANTENNA, BitUtil.check(io, 0));
            position.set(Position.KEY_CHARGE, BitUtil.check(io, 1));
            for (int i = 1; i <= 6; i++) {
                position.set(Position.PREFIX_IN + i, BitUtil.check(io, 1 + i));
            }
            for (int i = 1; i <= 4; i++) {
                position.set(Position.PREFIX_OUT + i, BitUtil.check(io, 7 + i));
            }
            position.set(Position.KEY_BATTERY, parser.nextDouble(0) * 0.1);
        }

        position.set(Position.KEY_POWER, parser.nextDouble(0));
        position.set(Position.PREFIX_ADC + 1, parser.next());

        int lac = parser.nextHexInt(0);
        int cid = parser.nextHexInt(0);
        if (lac != 0 && cid != 0) {
            position.setNetwork(new Network(CellTower.fromLacCid(lac, cid)));
        }

        position.set(Position.PREFIX_TEMP + 1, parser.next());
        position.set(Position.KEY_ODOMETER, parser.nextDouble(0) * 1000);

        return true;
    }

    private boolean decode3(Position position, Parser parser) {

        if (parser.hasNext()) {
            position.set(Position.KEY_ALARM, decodeAlarm123(Short.parseShort(parser.next(), 16)));
        }

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.set(Position.PREFIX_IO + 1, parser.next());
        position.set(Position.KEY_BATTERY, parser.nextDouble(0) * 0.1);
        position.set(Position.KEY_POWER, parser.nextDouble(0));
        position.set(Position.PREFIX_ADC + 1, parser.next());
        position.set(Position.PREFIX_ADC + 2, parser.next());
        position.set(Position.PREFIX_TEMP + 1, parser.next());
        position.set(Position.PREFIX_TEMP + 2, parser.next());

        position.setNetwork(new Network(
                CellTower.fromLacCid(parser.nextHexInt(0), parser.nextHexInt(0))));

        position.setValid(parser.next().equals("A"));
        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.setCourse(parser.nextDouble(0));
        position.setSpeed(parser.nextDouble(0));
        position.set(Position.KEY_PDOP, parser.nextDouble());
        position.set(Position.KEY_ODOMETER, parser.nextInt(0) * 1000);

        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        return true;
    }

    private boolean decode4(Position position, Parser parser) {

        long status = parser.nextHexLong();

        position.set(Position.KEY_ALARM, BitUtil.check(status, 32 - 1) ? Position.ALARM_SOS : null);
        position.set(Position.KEY_IGNITION, BitUtil.check(status, 32 - 2));
        position.set(Position.KEY_ALARM, BitUtil.check(status, 32 - 3) ? Position.ALARM_OVERSPEED : null);
        position.set(Position.KEY_CHARGE, BitUtil.check(status, 32 - 4));
        position.set(Position.KEY_ALARM, BitUtil.check(status, 32 - 5) ? Position.ALARM_GEOFENCE_EXIT : null);
        position.set(Position.KEY_ALARM, BitUtil.check(status, 32 - 6) ? Position.ALARM_GEOFENCE_ENTER : null);
        position.set(Position.PREFIX_OUT + 1, BitUtil.check(status, 32 - 9));
        position.set(Position.PREFIX_OUT + 2, BitUtil.check(status, 32 - 10));
        position.set(Position.PREFIX_OUT + 3, BitUtil.check(status, 32 - 11));
        position.set(Position.PREFIX_OUT + 4, BitUtil.check(status, 32 - 12));
        position.set(Position.PREFIX_IN + 2, BitUtil.check(status, 32 - 13));
        position.set(Position.PREFIX_IN + 3, BitUtil.check(status, 32 - 14));
        position.set(Position.PREFIX_IN + 4, BitUtil.check(status, 32 - 15));
        position.set(Position.KEY_ALARM, BitUtil.check(status, 32 - 16) ? Position.ALARM_SHOCK : null);
        position.set(Position.KEY_ALARM, BitUtil.check(status, 32 - 18) ? Position.ALARM_LOW_BATTERY : null);
        position.set(Position.KEY_ALARM, BitUtil.check(status, 32 - 22) ? Position.ALARM_JAMMING : null);

        position.setTime(parser.nextDateTime());

        position.set(Position.KEY_BATTERY, parser.nextDouble() * 0.1);
        position.set(Position.KEY_POWER, parser.nextDouble());

        position.set(Position.PREFIX_ADC + 1, parser.next());
        position.set(Position.PREFIX_ADC + 2, parser.next());
        position.set(Position.PREFIX_ADC + 3, parser.next());
        position.set(Position.PREFIX_ADC + 4, parser.next());
        position.set(Position.PREFIX_TEMP + 1, parser.next());

        if (parser.hasNext()) {
            position.set(Position.PREFIX_TEMP + 2, parser.next());
            position.setValid(BitUtil.check(status, 32 - 20));
        } else {
            position.setValid(BitUtil.check(status, 32 - 18));
        }

        int lac = parser.nextHexInt();
        int cid = parser.nextHexInt();
        CellTower cellTower;
        if (parser.hasNext(2)) {
            int mnc = parser.nextInt();
            int mcc = parser.nextInt();
            cellTower = CellTower.from(mcc, mnc, lac, cid);
        } else {
            cellTower = CellTower.fromLacCid(lac, cid);
        }
        position.set(Position.KEY_SATELLITES, parser.nextInt());
        cellTower.setSignalStrength(parser.nextInt());
        position.setNetwork(new Network(cellTower));

        position.setCourse(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.set(Position.KEY_HDOP, parser.nextDouble());
        position.set(Position.KEY_ODOMETER, parser.nextInt() * 1000);

        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        return true;
    }

    private boolean decodeObd(Position position, Parser parser) {

        position.setValid(true);
        position.setTime(parser.nextDateTime());
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());

        position.set(Position.KEY_ODOMETER, parser.nextLong());
        position.set(Position.KEY_FUEL_USED, parser.nextInt());
        position.set(Position.KEY_FUEL_CONSUMPTION, parser.nextInt());
        position.set(Position.KEY_POWER, parser.nextInt() * 0.001);
        position.set(Position.KEY_RPM, parser.nextInt());
        position.set(Position.KEY_OBD_SPEED, parser.nextInt());
        parser.nextInt(); // intake flow
        parser.nextInt(); // intake pressure
        position.set(Position.KEY_COOLANT_TEMP, parser.nextInt());
        position.set("intakeTemp", parser.nextInt());
        position.set(Position.KEY_ENGINE_LOAD, parser.nextInt());
        position.set(Position.KEY_THROTTLE, parser.nextInt());
        position.set(Position.KEY_FUEL_LEVEL, parser.nextInt());

        return true;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        Pattern pattern = PATTERN3;
        if (sentence.contains("$Cloud")) {
            pattern = PATTERN_OBD;
        } else if (sentence.charAt(2) == '0') {
            pattern = PATTERN4;
        } else if (sentence.contains("$GPRMC")) {
            pattern = PATTERN1;
        } else {
            int index = sentence.indexOf('|');
            if (index != -1 && sentence.indexOf('|', index + 1) != -1) {
                pattern = PATTERN2;
            }
        }

        Parser parser = new Parser(pattern, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());

        String type = null;
        if (pattern == PATTERN4) {
            type = parser.next();
            position.set(Position.KEY_ALARM, decodeAlarm4(Integer.parseInt(type, 16)));
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        boolean result;
        if (pattern == PATTERN1 || pattern == PATTERN2) {
            result = decode12(position, parser, pattern);
        } else if (pattern == PATTERN3) {
            result = decode3(position, parser);
        } else if (pattern == PATTERN4) {
            result = decode4(position, parser);
        } else {
            result = decodeObd(position, parser);
        }

        if (channel != null) {
            if (type != null) {
                String response = "$$0014" + type + sentence.substring(sentence.length() - 6, sentence.length() - 2);
                response += String.format("%02X", Checksum.xor(response)).toUpperCase();
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            } else {
                channel.writeAndFlush(new NetworkMessage("ACK OK\r\n", remoteAddress));
            }
        }

        return result ? position : null;
    }

}
