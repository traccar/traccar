/*
 * Copyright 2013 - 2016 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
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

    public TotemProtocolDecoder(TotemProtocol protocol) {
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
            .text("AA")                          // type
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
            .number("(dddd)")                    // temperature 2
            .groupEnd("?")
            .number("(xxxx)")                    // lac
            .number("(xxxx)")                    // cid
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

    private String decodeAlarm(Short value) {
        switch (value) {
            case 0x01:
                return Position.ALARM_SOS;
            case 0x10:
                return Position.ALARM_LOW_BATTERY;
            case 0x11:
                return Position.ALARM_OVERSPEED;
            case 0x42:
                return Position.ALARM_GEOFENCE_EXIT;
            case 0x43:
                return Position.ALARM_GEOFENCE_ENTER;
            default:
                return null;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        // Determine format
        Pattern pattern = PATTERN3;
        if (sentence.indexOf("AA") == 6) {
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

        Position position = new Position();
        position.setProtocol(getProtocolName());

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        if (pattern == PATTERN1 || pattern == PATTERN2) {
            if (parser.hasNext()) {
                position.set(Position.KEY_ALARM, decodeAlarm(Short.parseShort(parser.next(), 16)));
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
                return null; // ignore invalid data
            }
            dateBuilder.setDate(year, month, day);
            position.setTime(dateBuilder.getDate());

            if (pattern == PATTERN1) {
                position.set(Position.KEY_PDOP, parser.next());
                position.set(Position.KEY_HDOP, parser.next());
                position.set(Position.KEY_VDOP, parser.next());
            } else {
                position.set(Position.KEY_HDOP, parser.next());
            }

            position.set(Position.PREFIX_IO + 1, parser.next());
            position.set(Position.KEY_BATTERY, parser.next());
            position.set(Position.KEY_POWER, parser.nextDouble(0));
            position.set(Position.PREFIX_ADC + 1, parser.next());

            int lac = parser.nextHexInt(0);
            int cid = parser.nextHexInt(0);
            if (lac != 0 && cid != 0) {
                position.setNetwork(new Network(CellTower.fromLacCid(lac, cid)));
            }

            position.set(Position.PREFIX_TEMP + 1, parser.next());
            position.set(Position.KEY_ODOMETER, parser.nextDouble(0) * 1000);

        } else if (pattern == PATTERN3) {
            if (parser.hasNext()) {
                position.set(Position.KEY_ALARM, decodeAlarm(Short.parseShort(parser.next(), 16)));
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
            position.set(Position.KEY_SATELLITES, parser.next());
            position.setCourse(parser.nextDouble(0));
            position.setSpeed(parser.nextDouble(0));
            position.set(Position.KEY_PDOP, parser.next());
            position.set(Position.KEY_ODOMETER, parser.nextInt(0) * 1000);

            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());

        } else if (pattern == PATTERN4) {
            position.set(Position.KEY_STATUS, parser.next());

            position.setTime(parser.nextDateTime());

            position.set(Position.KEY_BATTERY, parser.nextDouble(0) * 0.1);
            position.set(Position.KEY_POWER, parser.nextDouble(0));

            position.set(Position.PREFIX_ADC + 1, parser.next());
            position.set(Position.PREFIX_ADC + 2, parser.next());
            position.set(Position.PREFIX_ADC + 3, parser.next());
            position.set(Position.PREFIX_ADC + 4, parser.next());
            position.set(Position.PREFIX_TEMP + 1, parser.next());
            position.set(Position.PREFIX_TEMP + 2, parser.next());

            CellTower cellTower = CellTower.fromLacCid(parser.nextHexInt(0), parser.nextHexInt(0));
            position.set(Position.KEY_SATELLITES, parser.nextInt(0));
            cellTower.setSignalStrength(parser.nextInt(0));
            position.setNetwork(new Network(cellTower));

            position.setCourse(parser.nextDouble(0));
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
            position.set(Position.KEY_HDOP, parser.nextDouble(0));
            position.set(Position.KEY_ODOMETER, parser.nextInt(0) * 1000);

            position.setValid(true);
            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());
        }
        if (channel != null) {
            channel.write("ACK OK\r\n");
        }
        return position;
    }

}
