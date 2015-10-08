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

public class MegastekProtocolDecoder extends BaseProtocolDecoder {

    public MegastekProtocolDecoder(MegastekProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_GPRMC = Pattern.compile(
            "\\$GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2})\\.\\d+," + // Time (HHMMSS.SSS)
            "([AV])," +                    // Validity
            "(\\d+)(\\d{2}\\.\\d+)," +     // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d+)(\\d{2}\\.\\d+)," +     // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+\\.\\d+)?," +            // Speed
            "(\\d+\\.\\d+)?," +            // Course
            "(\\d{2})(\\d{2})(\\d{2})" +   // Date (DDMMYY)
            ".*");                         // Checksum

    private static final Pattern PATTERN_SIMPLE = Pattern.compile(
            "[FL]," +                      // Flag
            "([^,]*)," +                   // Alarm
            "imei:(\\d+)," +               // IMEI
            "(\\d+/?\\d*)?," +             // Satellites
            "(\\d+\\.\\d+)?," +            // Altitude
            "Battery=(\\d+)%,," +          // Battery
            "(\\d)?," +                    // Charger
            "(\\d+)?," +                   // MCC
            "(\\d+)?," +                   // MNC
            "(\\p{XDigit}{4},\\p{XDigit}{4});" + // Location code
            ".+");                         // Checksum

    private static final Pattern PATTERN_ALTERNATIVE = Pattern.compile(
            "(\\d+)," +                    // MCC
            "(\\d+)," +                    // MNC
            "(\\p{XDigit}{4},\\p{XDigit}{4})," + // Location code
            "(\\d+)," +                    // GSM signal
            "(\\d+)," +                    // Battery
            "(\\d+)," +                    // Flags
            "(\\d+)," +                    // Inputs
            "(?:(\\d+),)?" +               // Outputs
            "(\\d\\.?\\d*)," +             // ADC 1
            "(?:(\\d\\.\\d{2})," +         // ADC 2
            "(\\d\\.\\d{2}),)?" +          // ADC 3
            "([^;]+);" +                   // Alarm
            ".*");                         // Checksum

    private boolean parseGPRMC(String gprmc, Position position) {

        // Parse message
        Matcher parser = PATTERN_GPRMC.matcher(gprmc);
        if (!parser.matches()) {
            return false;
        }

        int index = 1;

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
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
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
        position.setTime(time.getTime());

        return true;
    }

    private Position decodeOld(Channel channel, String sentence) {

        // Detect type
        boolean simple = sentence.charAt(3) == ',' || sentence.charAt(6) == ',';

        // Split message
        String id;
        String gprmc;
        String status;
        if (simple) {

            int beginIndex = sentence.indexOf(',') + 1;
            int endIndex = sentence.indexOf(',', beginIndex);
            id = sentence.substring(beginIndex, endIndex);

            beginIndex = endIndex + 1;
            endIndex = sentence.indexOf('*', beginIndex);
            if (endIndex != -1) {
                endIndex += 3;
            } else {
                endIndex = sentence.length();
            }
            gprmc = sentence.substring(beginIndex, endIndex);

            beginIndex = endIndex + 1;
            if (beginIndex > sentence.length()) {
                beginIndex = endIndex;
            }
            status = sentence.substring(beginIndex);

        } else {

            int beginIndex = 3;
            int endIndex = beginIndex + 16;
            id = sentence.substring(beginIndex, endIndex).trim();

            beginIndex = endIndex + 2;
            endIndex = sentence.indexOf('*', beginIndex) + 3;
            gprmc = sentence.substring(beginIndex, endIndex);

            beginIndex = endIndex + 1;
            status = sentence.substring(beginIndex);

        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocolName());

        // Parse location data
        if (!parseGPRMC(gprmc, position)) {
            return null;
        }

        if (simple) {

            Matcher parser = PATTERN_SIMPLE.matcher(status);
            if (parser.matches()) {

                int index = 1;

                position.set(Event.KEY_ALARM, parser.group(index++));

                // IMEI
                if (!identify(parser.group(index++), channel, null, false) && !identify(id, channel)) {
                    return null;
                }
                position.setDeviceId(getDeviceId());

                position.set(Event.KEY_SATELLITES, parser.group(index++));

                String altitude = parser.group(index++);
                if (altitude != null) {
                    position.setAltitude(Double.parseDouble(altitude));
                }

                position.set(Event.KEY_POWER, Double.parseDouble(parser.group(index++)));

                String charger = parser.group(index++);
                if (charger != null) {
                    position.set(Event.KEY_CHARGE, Integer.parseInt(charger) == 1);
                }

                position.set(Event.KEY_MCC, parser.group(index++));
                position.set(Event.KEY_MNC, parser.group(index++));
                position.set(Event.KEY_LAC, parser.group(index++));

            } else {

                if (!identify(id, channel)) {
                    return null;
                }
                position.setDeviceId(getDeviceId());

            }

        } else {

            Matcher parser = PATTERN_ALTERNATIVE.matcher(status);
            if (!parser.matches()) {

                int index = 1;

                if (!identify(id, channel)) {
                    return null;
                }
                position.setDeviceId(getDeviceId());

                position.set(Event.KEY_MCC, parser.group(index++));
                position.set(Event.KEY_MNC, parser.group(index++));
                position.set(Event.KEY_LAC, parser.group(index++));
                position.set(Event.KEY_GSM, parser.group(index++));

                position.set(Event.KEY_BATTERY, Double.parseDouble(parser.group(index++)));

                position.set(Event.KEY_FLAGS, parser.group(index++));
                position.set(Event.KEY_INPUT, parser.group(index++));
                position.set(Event.KEY_OUTPUT, parser.group(index++));
                position.set(Event.PREFIX_ADC + 1, parser.group(index++));
                position.set(Event.PREFIX_ADC + 2, parser.group(index++));
                position.set(Event.PREFIX_ADC + 3, parser.group(index++));
                position.set(Event.KEY_ALARM, parser.group(index++));
            }
        }

        return position;
    }

    private static final Pattern PATTERN_NEW = Pattern.compile(
            "\\$MGV\\d{3}," +
            "(\\d+)," +                    // IMEI
            "[^,]*," +                     // Name
            "([RS])," +
            "(\\d{2})(\\d{2})(\\d{2})," +  // Date (DDMMYY)
            "(\\d{2})(\\d{2})(\\d{2})," +  // Time (HHMMSS)
            "([AV])," +                    // Validity
            "(\\d+)(\\d{2}\\.\\d+)," +     // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d+)(\\d{2}\\.\\d+)," +     // Longitude (DDDMM.MMMM)
            "([EW])," +
            "\\d{2}," +
            "(\\d{2})," +                  // Satellites
            "\\d{2}," +
            "(\\d+\\.\\d+)," +             // HDOP
            "(\\d+\\.\\d+)," +             // Speed
            "(\\d+\\.\\d+)," +             // Course
            "(\\d+\\.\\d+)," +             // Altitude
            "(\\d+\\.\\d+)," +             // Odometer
            "(\\d+)," +                    // MCC
            "(\\d+)," +                    // MNC
            "(\\p{XDigit}{4},\\p{XDigit}{4})," + // Cell
            "(\\d+)?," +                   // GSM
            "([01]+)," +                   // Input
            "([01]+)," +                   // Output
            "(\\d+)," +                    // ADC1
            "(\\d+)," +                    // ADC2
            "(\\d+)," +                    // ADC3
            "(?:(-?\\d+\\.?\\d*)| )," +    // Temperature 1
            "(?:(-?\\d+\\.?\\d*)| )," +    // Temperature 2
            "(\\d+)?,," +                  // RFID
            "(\\d+)?," +                   // Battery
            "([^,]*);" +                   // Alert
            ".*");

    private Position decodeNew(Channel channel, String sentence) {

        Matcher parser = PATTERN_NEW.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }
        int index = 1;

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        if (parser.group(index++).equals("S")) {
            position.set(Event.KEY_ARCHIVE, true);
        }

        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
        position.setTime(time.getTime());

        position.setValid(parser.group(index++).equals("A"));

        // Latitude
        double latitude = Double.parseDouble(parser.group(index++));
        latitude += Double.parseDouble(parser.group(index++)) / 60;
        if (parser.group(index++).equals("S")) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        double longitude = Double.parseDouble(parser.group(index++));
        longitude += Double.parseDouble(parser.group(index++)) / 60;
        if (parser.group(index++).equals("W")) longitude = -longitude;
        position.setLongitude(longitude);

        position.set(Event.KEY_SATELLITES, Integer.parseInt(parser.group(index++)));
        position.set(Event.KEY_HDOP, Double.parseDouble(parser.group(index++)));

        position.setSpeed(Double.parseDouble(parser.group(index++)));
        position.setCourse(Double.parseDouble(parser.group(index++)));
        position.setAltitude(Double.parseDouble(parser.group(index++)));

        position.set(Event.KEY_ODOMETER, Double.parseDouble(parser.group(index++)));
        position.set(Event.KEY_MCC, Integer.parseInt(parser.group(index++)));
        position.set(Event.KEY_MNC, Integer.parseInt(parser.group(index++)));
        position.set(Event.KEY_CELL, parser.group(index++));

        String gsm = parser.group(index++);
        if (gsm != null) {
            position.set(Event.KEY_GSM, Integer.parseInt(gsm));
        }

        position.set(Event.KEY_INPUT, Integer.parseInt(parser.group(index++), 2));
        position.set(Event.KEY_OUTPUT, Integer.parseInt(parser.group(index++), 2));

        for (int i = 1; i <= 3; i++) {
            position.set(Event.PREFIX_ADC + i, Integer.parseInt(parser.group(index++)));
        }

        for (int i = 1; i <= 2; i++) {
            String adc = parser.group(index++);
            if (adc != null) {
                position.set(Event.PREFIX_TEMP + i, Double.parseDouble(adc));
            }
        }

        position.set(Event.KEY_RFID, parser.group(index++));

        String battery = parser.group(index++);
        if (battery != null) {
            position.set(Event.KEY_BATTERY, Integer.parseInt(battery));
        }

        position.set(Event.KEY_ALARM, parser.group(index++));

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("$MG")) {
            return decodeNew(channel, sentence);
        } else {
            return decodeOld(channel, sentence);
        }
    }

}
