/*
 * Copyright 2013 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.SocketAddress;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

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
            .number("(dd)(dd)(dd).d+,")          // time
            .expression("([AV]),")               // validity
            .number("(d+)(dd.d+),([NS]),")       // latitude
            .number("(d+)(dd.d+),([EW]),")       // longitude
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .number("(dd)(dd)(dd)")              // date
            .expression("[^*]*").text("*")
            .number("xx|")                       // checksum
            .number("d+.d+|")                    // pdop
            .number("(d+.d+)|")                  // hdop
            .number("d+.d+|")                    // vdop
            .number("(d+)|")                     // io status
            .number("d+|")                       // time
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
            .number("(dd)(dd)(dd)|")             // time
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
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd)")              // time
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
            .number("(dd)(dd)(dd)")              // time
            .number("(dd)")                      // battery
            .number("(dd)")                      // external power
            .number("(dddd)")                    // adc 1
            .number("(xxxx)")                    // lac
            .number("(xxxx)")                    // cid
            .number("(dd)")                      // satellites
            .number("(dd)")                      // gsm
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

        if (!identify(parser.next(), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        if (pattern == PATTERN1 || pattern == PATTERN2) {

            position.set(Event.KEY_ALARM, parser.next());

            DateBuilder dateBuilder = new DateBuilder();
            int year = 0;
            if (pattern == PATTERN2) {
                dateBuilder.setDay(parser.nextInt()).setMonth(parser.nextInt());
                year = parser.nextInt();
                dateBuilder.setYear(year);
            }
            dateBuilder.setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

            position.setValid(parser.next().equals("A"));
            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());
            position.setSpeed(parser.nextDouble());
            position.setCourse(parser.nextDouble());

            if (pattern == PATTERN1) {
                dateBuilder.setDay(parser.nextInt()).setMonth(parser.nextInt());
                year = parser.nextInt();
                dateBuilder.setYear(year);
            }
            if (year == 0) {
                return null; // ignore invalid data
            }
            position.setTime(dateBuilder.getDate());

            position.set(Event.KEY_HDOP, parser.next());
            position.set(Event.PREFIX_IO + 1, parser.next());
            position.set(Event.KEY_BATTERY, parser.next());
            position.set(Event.KEY_POWER, parser.nextDouble());
            position.set(Event.PREFIX_ADC + 1, parser.next());

            int lac = parser.nextInt(16);
            int cid = parser.nextInt(16);
            if (lac != 0 && cid != 0) {
                position.set(Event.KEY_LAC, lac);
                position.set(Event.KEY_CID, cid);
            }

            position.set(Event.PREFIX_TEMP + 1, parser.next());
            position.set(Event.KEY_ODOMETER, parser.next());

        } else if (pattern == PATTERN3) {

            position.set(Event.KEY_ALARM, parser.next());

            DateBuilder dateBuilder = new DateBuilder()
                    .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt())
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
            position.setTime(dateBuilder.getDate());

            position.set(Event.PREFIX_IO + 1, parser.next());
            position.set(Event.KEY_BATTERY, parser.nextDouble() / 10);
            position.set(Event.KEY_POWER, parser.nextDouble());
            position.set(Event.PREFIX_ADC + 1, parser.next());
            position.set(Event.PREFIX_ADC + 2, parser.next());
            position.set(Event.PREFIX_TEMP + 1, parser.next());
            position.set(Event.PREFIX_TEMP + 2, parser.next());
            position.set(Event.KEY_LAC, parser.nextInt(16));
            position.set(Event.KEY_CID, parser.nextInt(16));

            position.setValid(parser.next().equals("A"));
            position.set(Event.KEY_SATELLITES, parser.next());

            position.setCourse(parser.nextDouble());
            position.setSpeed(parser.nextDouble());

            position.set("pdop", parser.next());

            position.set(Event.KEY_ODOMETER, parser.next());

            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());

        } else if (pattern == PATTERN4) {

            position.set(Event.KEY_STATUS, parser.next());

            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
            position.setTime(dateBuilder.getDate());

            position.set(Event.KEY_BATTERY, parser.nextDouble() / 10);
            position.set(Event.KEY_POWER, parser.nextDouble());
            position.set(Event.PREFIX_ADC + 1, parser.next());
            position.set(Event.KEY_LAC, parser.nextInt(16));
            position.set(Event.KEY_CID, parser.nextInt(16));
            position.set(Event.KEY_SATELLITES, parser.nextInt());
            position.set(Event.KEY_GSM, parser.nextInt());

            position.setCourse(parser.nextDouble());
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

            position.set(Event.KEY_HDOP, parser.nextDouble());
            position.set(Event.KEY_ODOMETER, parser.nextInt());

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
