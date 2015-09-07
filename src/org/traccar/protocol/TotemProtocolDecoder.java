/*
 * Copyright 2013 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.model.Event;
import org.traccar.model.Position;

public class TotemProtocolDecoder extends BaseProtocolDecoder {

    public TotemProtocolDecoder(TotemProtocol protocol) {
        super(protocol);
    }

    private static final Pattern pattern1 = Pattern.compile(
            "\\$\\$" +                          // Header
            "\\p{XDigit}{2}" +                  // Length
            "(\\d+)\\|" +                       // IMEI
            "(..)" +                            // Alarm Type
            "\\$GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2})\\.\\d+," + // Time (HHMMSS.SS)
            "([AV])," +                         // Validity
            "(\\d+)(\\d{2}\\.\\d+)," +          // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d+)(\\d{2}\\.\\d+)," +          // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+\\.?\\d*)?," +                // Speed
            "(\\d+\\.?\\d*)?," +                // Course
            "(\\d{2})(\\d{2})(\\d{2})" +        // Date (DDMMYY)
            "[^\\*]+\\*\\p{XDigit}{2}\\|" +     // Checksum
            "\\d+\\.\\d+\\|" +                  // PDOP
            "(\\d+\\.\\d+)\\|" +                // HDOP
            "\\d+\\.\\d+\\|" +                  // VDOP
            "(\\d+)\\|" +                       // IO Status
            "\\d+\\|" +                         // Time
            "\\d" +                             // Charged
            "(\\d{3})" +                        // Battery
            "(\\d{4})\\|" +                     // External Power
            "(?:(\\d+)\\|)?" +                  // ADC
            "(\\p{XDigit}+)\\|" +               // Location Code
            "(\\d+)\\|" +                       // Temperature
            "(\\d+.\\d+)\\|" +                  // Odometer
            "\\d+\\|" +                         // Serial Number
            ".*\\|?" +
            "\\p{XDigit}{4}" +                  // Checksum
            "\r?\n?");

    private static final Pattern pattern2 = Pattern.compile(
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

    private static final Pattern pattern3 = Pattern.compile(
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
        Pattern pattern = pattern3;
        if (sentence.contains("$GPRMC")) {
            pattern = pattern1;
        } else {
            int index = sentence.indexOf('|');
            if (index != -1 && sentence.indexOf('|', index + 1) != -1) {
                pattern = pattern2;
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
        
        if (pattern == pattern1 || pattern == pattern2) {

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            int year = 0;
            if (pattern == pattern2) {
                time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
                time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
                year = Integer.valueOf(parser.group(index++));
                time.set(Calendar.YEAR, 2000 + year);
            }
            time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));

            // Validity
            position.setValid(parser.group(index++).compareTo("A") == 0);

            // Latitude
            Double latitude = Double.valueOf(parser.group(index++));
            latitude += Double.valueOf(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
            position.setLatitude(latitude);

            // Longitude
            Double longitude = Double.valueOf(parser.group(index++));
            longitude += Double.valueOf(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
            position.setLongitude(longitude);

            // Speed
            String speed = parser.group(index++);
            if (speed != null) {
                position.setSpeed(Double.valueOf(speed));
            }

            // Course
            String course = parser.group(index++);
            if (course != null) {
                position.setCourse(Double.valueOf(course));
            }

            // Date
            if (pattern == pattern1) {
                time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
                time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
                year = Integer.valueOf(parser.group(index++));
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
            position.set(Event.KEY_POWER, Double.valueOf(parser.group(index++)));

            // ADC
            position.set(Event.PREFIX_ADC + 1, parser.group(index++));

            // Location Code
            position.set(Event.KEY_LAC, parser.group(index++));

            // Temperature
            position.set(Event.PREFIX_TEMP + 1, parser.group(index++));

            // Odometer
            position.set(Event.KEY_ODOMETER, parser.group(index++));
        
        } else if (pattern == pattern3) {

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
            time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
            position.setTime(time.getTime());
            
            // IO Status
            position.set(Event.PREFIX_IO + 1, parser.group(index++));

            // Power
            position.set(Event.KEY_BATTERY, Double.valueOf(parser.group(index++)) / 10);
            position.set(Event.KEY_POWER, Double.valueOf(parser.group(index++)));

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
            position.setCourse(Double.valueOf(parser.group(index++)));

            // Speed
            position.setSpeed(Double.valueOf(parser.group(index++)));

            // PDOP
            position.set("pdop", parser.group(index++));

            // Odometer
            position.set(Event.KEY_ODOMETER, parser.group(index++));

            // Latitude
            Double latitude = Double.valueOf(parser.group(index++));
            latitude += Double.valueOf(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
            position.setLatitude(latitude);

            // Longitude
            Double longitude = Double.valueOf(parser.group(index++));
            longitude += Double.valueOf(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
            position.setLongitude(longitude);
        
        }
        
        if (channel != null) {
            channel.write("ACK OK\r\n");
        }

        return position;
    }

}
