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
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class TotemProtocolDecoder extends BaseProtocolDecoder {

    public TotemProtocolDecoder(TotemProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN1 = new PatternBuilder()
            .txt("$$")                           // header
            .num("xx")                           // length
            .num("(d+)|")                        // imei
            .xpr("(..)")                         // alarm
            .txt("$GPRMC,")
            .num("(dd)(dd)(dd).d+,")             // time
            .xpr("([AV]),")                      // validity
            .num("(d+)(dd.d+),([NS]),")          // latitude
            .num("(d+)(dd.d+),([EW]),")          // longitude
            .num("(d+.?d*)?,")                   // speed
            .num("(d+.?d*)?,")                   // course
            .num("(dd)(dd)(dd)")                 // date
            .nxt("*")
            .num("xx|")                          // checksum
            .num("d+.d+|")                       // pdop
            .num("(d+.d+)|")                     // hdop
            .num("d+.d+|")                       // vdop
            .num("(d+)|")                        // io status
            .num("d+|")                          // time
            .num("d")                            // charged
            .num("(ddd)")                        // battery
            .num("(dddd)|")                      // power
            .opn("(d+)|")                        // adc
            .num("(x+)|")                        // location code
            .num("(d+)|")                        // temperature
            .num("(d+.d+)|")                     // odometer
            .num("d+|")                          // serial number
            .any()
            .num("xxxx")                         // checksum
            .any()
            .compile();

    private static final Pattern PATTERN2 = Pattern.compile(
            "\\$\\$" +                          // Header
            "\\p{XDigit}{2}" +                  // Length
            "(\\d+)\\|" +                       // IMEI
            "(..)" +                            // Alarm Type
            "(\\d{2})(\\d{2})(\\d{2})" +        // Date (DDMMYY)
            "(\\d{2})(\\d{2})(\\d{2})\\|" +     // Time (HHMMSS)
            "([AV])\\|" +                       // Validity
            "(\\d+)(\\d{2}\\.\\d+)\\|" +        // Latitude (DDMM.MMMM)
            "([NS])\\|" +
            "(\\d+)(\\d{2}\\.\\d+)\\|" +        // Longitude (DDDMM.MMMM)
            "([EW])\\|" +
            "(\\d+\\.\\d+)?\\|" +               // Speed
            "(\\d+)?\\|" +                      // Course
            "(\\d+\\.\\d+)\\|" +                // HDOP
            "(\\d+)\\|" +                       // IO Status
            "\\d" +                             // Charged
            "(\\d{2})" +                        // Battery
            "(\\d{2})\\|" +                     // External Power
            "(\\d+)\\|" +                       // ADC
            "(\\p{XDigit}{8})\\|" +             // Location Code
            "(\\d+)\\|" +                       // Temperature
            "(\\d+.\\d+)\\|" +                  // Odometer
            "\\d+\\|" +                         // Serial Number
            "\\p{XDigit}{4}" +                  // Checksum
            "\r?\n?");

    private static final Pattern PATTERN3 = Pattern.compile(
            "\\$\\$" +                          // Header
            "\\p{XDigit}{2}" +                  // Length
            "(\\d+)\\|" +                       // IMEI
            "(..)" +                            // Alarm Type
            "(\\d{2})(\\d{2})(\\d{2})" +        // Date (YYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})" +        // Time (HHMMSS)
            "(\\p{XDigit}{4})" +                // IO Status
            "[01]" +                            // Charging
            "(\\d{2})" +                        // Battery
            "(\\d{2})" +                        // External Power
            "(\\d{4})" +                        // ADC 1
            "(\\d{4})" +                        // ADC 2
            "(\\d{3})" +                        // Temperature 1
            "(\\d{3})" +                        // Temperature 2
            "(\\p{XDigit}{8})" +                // Location Code
            "([AV])" +                          // Validity
            "(\\d{2})" +                        // Satellites
            "(\\d{3})" +                        // Course
            "(\\d{3})" +                        // Speed
            "(\\d{2}\\.\\d)" +                  // PDOP
            "(\\d{7})" +                        // Odometer
            "(\\d{2})(\\d{2}\\.\\d{4})" +       // Latitude (DDMM.MMMM)
            "([NS])" +
            "(\\d{3})(\\d{2}\\.\\d{4})" +       // Longitude (DDDMM.MMMM)
            "([EW])" +
            "\\d{4}" +                          // Serial Number
            "\\p{XDigit}{4}" +                  // Checksum
            "\r?\n?");

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Determine format
        Pattern pattern = PATTERN3;
        if (sentence.contains("$GPRMC")) {
            pattern = PATTERN1;
        } else {
            int index = sentence.indexOf('|');
            if (index != -1 && sentence.indexOf('|', index + 1) != -1) {
                pattern = PATTERN2;
            }
        }

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocolName());

        Integer index = 1;

        // Get device by IMEI
        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Alarm type
        position.set(Event.KEY_ALARM, parser.group(index++));

        if (pattern == PATTERN1 || pattern == PATTERN2) {

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            int year = 0;
            if (pattern == PATTERN2) {
                time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
                time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
                year = Integer.parseInt(parser.group(index++));
                time.set(Calendar.YEAR, 2000 + year);
            }
            time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));

            // Validity
            position.setValid(parser.group(index++).compareTo("A") == 0);

            // Latitude
            Double latitude = Double.parseDouble(parser.group(index++));
            latitude += Double.parseDouble(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
            position.setLatitude(latitude);

            // Longitude
            Double longitude = Double.parseDouble(parser.group(index++));
            longitude += Double.parseDouble(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
            position.setLongitude(longitude);

            // Speed
            String speed = parser.group(index++);
            if (speed != null) {
                position.setSpeed(Double.parseDouble(speed));
            }

            // Course
            String course = parser.group(index++);
            if (course != null) {
                position.setCourse(Double.parseDouble(course));
            }

            // Date
            if (pattern == PATTERN1) {
                time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
                time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
                year = Integer.parseInt(parser.group(index++));
                time.set(Calendar.YEAR, 2000 + year);
            }
            if (year == 0) {
                return null; // ignore invalid data
            }
            position.setTime(time.getTime());

            // Accuracy
            position.set(Event.KEY_HDOP, parser.group(index++));

            // IO Status
            position.set(Event.PREFIX_IO + 1, parser.group(index++));

            // Power
            position.set(Event.KEY_BATTERY, parser.group(index++));
            position.set(Event.KEY_POWER, Double.parseDouble(parser.group(index++)));

            // ADC
            position.set(Event.PREFIX_ADC + 1, parser.group(index++));

            // Location Code
            position.set(Event.KEY_LAC, parser.group(index++));

            // Temperature
            position.set(Event.PREFIX_TEMP + 1, parser.group(index++));

            // Odometer
            position.set(Event.KEY_ODOMETER, parser.group(index++));

        } else if (pattern == PATTERN3) {

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
            time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
            time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
            position.setTime(time.getTime());

            // IO Status
            position.set(Event.PREFIX_IO + 1, parser.group(index++));

            // Power
            position.set(Event.KEY_BATTERY, Double.parseDouble(parser.group(index++)) / 10);
            position.set(Event.KEY_POWER, Double.parseDouble(parser.group(index++)));

            // ADC
            position.set(Event.PREFIX_ADC + 1, parser.group(index++));
            position.set(Event.PREFIX_ADC + 2, parser.group(index++));

            // Temperature
            position.set(Event.PREFIX_TEMP + 1, parser.group(index++));
            position.set(Event.PREFIX_TEMP + 2, parser.group(index++));

            // Location Code
            position.set(Event.KEY_LAC, parser.group(index++));

            // Validity
            position.setValid(parser.group(index++).compareTo("A") == 0);

            // Satellites
            position.set(Event.KEY_SATELLITES, parser.group(index++));

            // Course
            position.setCourse(Double.parseDouble(parser.group(index++)));

            // Speed
            position.setSpeed(Double.parseDouble(parser.group(index++)));

            // PDOP
            position.set("pdop", parser.group(index++));

            // Odometer
            position.set(Event.KEY_ODOMETER, parser.group(index++));

            // Latitude
            Double latitude = Double.parseDouble(parser.group(index++));
            latitude += Double.parseDouble(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
            position.setLatitude(latitude);

            // Longitude
            Double longitude = Double.parseDouble(parser.group(index++));
            longitude += Double.parseDouble(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
            position.setLongitude(longitude);

        }

        if (channel != null) {
            channel.write("ACK OK\r\n");
        }

        return position;
    }

}
