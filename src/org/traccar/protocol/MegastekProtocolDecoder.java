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
import org.traccar.model.Event;
import org.traccar.model.Position;

public class MegastekProtocolDecoder extends BaseProtocolDecoder {

    public MegastekProtocolDecoder(String protocol) {
        super(protocol);
    }

    private static final Pattern patternGPRMC = Pattern.compile(
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
            "[^\\*]+\\*[0-9a-fA-F]{2}");   // Checksum

    private static final Pattern patternSimple = Pattern.compile(
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

    private static final Pattern patternAlternative = Pattern.compile(
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
        Matcher parser = patternGPRMC.matcher(gprmc);
        if (!parser.matches()) {
            return false;
        }

        int index = 1;

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
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
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());        

        return true;
    }
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Detect type
        boolean simple = (sentence.charAt(3) == ',' || sentence.charAt(6) == ',');

        // Split message
        String id;
        String gprmc;
        String status;
        if (simple) {

            int beginIndex = sentence.indexOf(',') + 1;
            int endIndex = sentence.indexOf(',', beginIndex);
            id = sentence.substring(beginIndex, endIndex);

            beginIndex = endIndex + 1;
            endIndex = sentence.indexOf('*', beginIndex) + 3;
            gprmc = sentence.substring(beginIndex, endIndex);

            beginIndex = endIndex + 1;
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
        position.setProtocol(getProtocol());

        // Parse location data
        if (!parseGPRMC(gprmc, position)) {
            return null;
        }
        
        if (simple) {

            // Parse status
            Matcher parser = patternSimple.matcher(status);
            if (!parser.matches()) {
                return null;
            }
            
            int index = 1;

            // Alarm
            position.set(Event.KEY_ALARM, parser.group(index++));

            // IMEI
            if (!identify(parser.group(index++), false)) {
                if (!identify(id)) {
                    return null;
                }
            }
            position.setDeviceId(getDeviceId());

            // Satellites
            position.set(Event.KEY_SATELLITES, parser.group(index++));

            // Altitude
            String altitude = parser.group(index++);
            if (altitude != null) {
                position.setAltitude(Double.valueOf(altitude));
            }

            // Battery
            position.set(Event.KEY_POWER, Double.valueOf(parser.group(index++)));

            // Charger
            String charger = parser.group(index++);
            if (charger != null) {
                position.set("charger", Integer.valueOf(charger) == 1);
            }

            position.set(Event.KEY_MCC, parser.group(index++));
            position.set(Event.KEY_MNC, parser.group(index++));
            position.set(Event.KEY_LAC, parser.group(index++));
            
        } else {

            // Parse status
            Matcher parser = patternAlternative.matcher(status);
            if (!parser.matches()) {
                return null;
            }
            
            int index = 1;

            if (!identify(id)) {
                return null;
            }
            position.setDeviceId(getDeviceId());

            position.set(Event.KEY_MCC, parser.group(index++));
            position.set(Event.KEY_MNC, parser.group(index++));
            position.set(Event.KEY_LAC, parser.group(index++));
            position.set(Event.KEY_GSM, parser.group(index++));

            // Battery
            position.set(Event.KEY_BATTERY, Double.valueOf(parser.group(index++)));
            
            position.set(Event.KEY_FLAGS, parser.group(index++));
            position.set(Event.KEY_INPUT, parser.group(index++));
            position.set(Event.KEY_OUTPUT, parser.group(index++));
            position.set(Event.PREFIX_ADC + 1, parser.group(index++));
            position.set(Event.PREFIX_ADC + 2, parser.group(index++));
            position.set(Event.PREFIX_ADC + 3, parser.group(index++));
            position.set(Event.KEY_ALARM, parser.group(index++));
            
        }
        return position;
    }

}
