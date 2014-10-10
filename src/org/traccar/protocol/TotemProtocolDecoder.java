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

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class TotemProtocolDecoder extends BaseProtocolDecoder {

    public TotemProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    public TotemProtocolDecoder(ServerManager serverManager, String protocol) {
        super(serverManager, protocol);
    }

    private static final Pattern patternFirst = Pattern.compile(
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
            "(\\d+)\\|" +                       // ADC
            "(\\p{XDigit}{8})\\|" +             // Location Code
            "(\\d+)\\|" +                       // Temperature
            "(\\d+.\\d+)\\|" +                  // Milage
            "\\d+\\|" +                         // Serial Number
            ".*\\|?" +
            "\\p{XDigit}{4}");                  // Checksum
            
    private static final Pattern patternSecond = Pattern.compile(
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
            "(\\d+.\\d+)\\|" +                  // Milage
            "\\d+\\|" +                         // Serial Number
            "\\p{XDigit}{4}");                  // Checksum

    private static final Pattern patternThird = Pattern.compile(
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
            "(\\d{7})" +                        // Milage
            "(\\d{2})(\\d{2}\\.\\d{4})" +       // Latitude (DDMM.MMMM)
            "([NS])" +
            "(\\d{3})(\\d{2}\\.\\d{4})" +       // Longitude (DDDMM.MMMM)
            "([EW])" +
            "\\d{4}" +                          // Serial Number
            "\\p{XDigit}{4}");                  // Checksum

    private enum MessageFormat {
        first,
        second,
        third
    }
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {
        
        String sentence = (String) msg;

        // Determine format
        MessageFormat format = MessageFormat.third;
        if (sentence.contains("$GPRMC")) {
            format = MessageFormat.first;
        } else {
            int index = sentence.indexOf('|');
            if (index != -1 && sentence.indexOf('|', index + 1) != -1) {
                format = MessageFormat.second;
            }
        }

        // Parse message
        Matcher parser = null;
        if (format == MessageFormat.first) {
            parser = patternFirst.matcher(sentence);
        } else if (format == MessageFormat.second) {
            parser = patternSecond.matcher(sentence);
        } else if (format == MessageFormat.third) {
            parser = patternThird.matcher(sentence);
        }
        if (parser == null || !parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());

        Integer index = 1;

        // Get device by IMEI
        String imei = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + imei);
            return null;
        }
        
        // Alarm type
        extendedInfo.set("alarm", parser.group(index++));
        
        if (format == MessageFormat.first || format == MessageFormat.second) {

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            int year = 0;
            if (format == MessageFormat.second) {
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
            } else {
                position.setSpeed(0.0);
            }

            // Course
            String course = parser.group(index++);
            if (course != null) {
                position.setCourse(Double.valueOf(course));
            } else {
                position.setCourse(0.0);
            }

            // Date
            if (format == MessageFormat.first) {
                time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
                time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
                year = Integer.valueOf(parser.group(index++));
                time.set(Calendar.YEAR, 2000 + year);
            }
            if (year == 0) {
                return null; // ignore invalid data
            }
            position.setTime(time.getTime());

            // Altitude
            position.setAltitude(0.0);

            // Accuracy
            extendedInfo.set("hdop", parser.group(index++));

            // IO Status
            extendedInfo.set("io", parser.group(index++));

            // Power
            extendedInfo.set("battery", parser.group(index++));
            extendedInfo.set("power", Double.valueOf(parser.group(index++)));

            // ADC
            extendedInfo.set("adc", parser.group(index++));

            // Location Code
            extendedInfo.set("lac", parser.group(index++));

            // Temperature
            extendedInfo.set("temperature", parser.group(index++));

            // Milage
            extendedInfo.set("milage", parser.group(index++));
        
        } else if (format == MessageFormat.third) {

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
            extendedInfo.set("io", parser.group(index++));

            // Power
            extendedInfo.set("battery", Double.valueOf(parser.group(index++)) / 10);
            extendedInfo.set("power", Double.valueOf(parser.group(index++)));

            // ADC
            extendedInfo.set("adc1", parser.group(index++));
            extendedInfo.set("adc2", parser.group(index++));

            // Temperature
            extendedInfo.set("temperature1", parser.group(index++));
            extendedInfo.set("temperature2", parser.group(index++));

            // Location Code
            extendedInfo.set("lac", parser.group(index++));

            // Validity
            position.setValid(parser.group(index++).compareTo("A") == 0);

            // Satellites
            extendedInfo.set("satellites", parser.group(index++));

            // Course
            position.setCourse(Double.valueOf(parser.group(index++)));

            // Speed
            position.setSpeed(Double.valueOf(parser.group(index++)));

            // Altitude
            position.setAltitude(0.0);

            // PDOP
            extendedInfo.set("pdop", parser.group(index++));

            // Milage
            extendedInfo.set("milage", parser.group(index++));

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

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

}
