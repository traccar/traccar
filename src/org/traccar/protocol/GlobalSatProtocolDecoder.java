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
import org.traccar.Context;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class GlobalSatProtocolDecoder extends BaseProtocolDecoder {

    private String format0;
    private String format1;

    public GlobalSatProtocolDecoder(GlobalSatProtocol protocol) {
        super(protocol);
        
        format0 = Context.getConfig().getString(getProtocolName() + ".format0", "TSPRXAB27GHKLMnaicz*U!");
        format1 = Context.getConfig().getString(getProtocolName() + ".format1", "SARY*U!");
    }

    public void setFormat0(String format) {
        format0 = format;
    }

    public void setFormat1(String format) {
        format1 = format;
    }

    private Position decodeOriginal(Channel channel, String sentence) {

        // Send acknowledgement
        if (channel != null) {
            channel.write("ACK\r");
        }

        // Message type
        String format;
        if (sentence.startsWith("GSr")) {
            format = format0;
        } else if (sentence.startsWith("GSh")) {
            format = format1;
        } else {
            return null;
        }

        // Check that message contains required parameters
        if (!format.contains("B") || !format.contains("S") ||
            !(format.contains("1") || format.contains("2") || format.contains("3")) ||
            !(format.contains("6") || format.contains("7") || format.contains("8"))) {
            return null;
        }

        // Tokenise
        if (format.contains("*")) {
            format = format.substring(0, format.indexOf('*'));
            sentence = sentence.substring(0, sentence.indexOf('*'));
        }
        String[] values = sentence.split(",");

        // Parse data
        Position position = new Position();
        position.setProtocol(getProtocolName());

        for (int formatIndex = 0, valueIndex = 1; formatIndex < format.length() && valueIndex < values.length; formatIndex++) {
            String value = values[valueIndex];

            switch(format.charAt(formatIndex)) {
                case 'S':
                    if (!identify(value, channel)) {
                        return null;
                    }
                    position.setDeviceId(getDeviceId());
                    break;
                case 'A':
                    if (value.isEmpty()) {
                        position.setValid(false);
                    } else {
                        position.setValid(Integer.valueOf(value) != 1);
                    }
                    break;
                case 'B':
                    Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    time.clear();
                    time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(value.substring(0, 2)));
                    time.set(Calendar.MONTH, Integer.valueOf(value.substring(2, 4)) - 1);
                    time.set(Calendar.YEAR, 2000 + Integer.valueOf(value.substring(4)));
                    value = values[++valueIndex];
                    time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(value.substring(0, 2)));
                    time.set(Calendar.MINUTE, Integer.valueOf(value.substring(2, 4)));
                    time.set(Calendar.SECOND, Integer.valueOf(value.substring(4)));
                    position.setTime(time.getTime());
                    break;
                case 'C':
                    valueIndex += 1;
                    break;
                case '1':
                    double longitude = Double.valueOf(value.substring(1));
                    if (value.charAt(0) == 'W') longitude = -longitude;
                    position.setLongitude(longitude);
                    break;
                case '2':
                    longitude = Double.valueOf(value.substring(4)) / 60;
                    longitude += Integer.valueOf(value.substring(1, 4));
                    if (value.charAt(0) == 'W') longitude = -longitude;
                    position.setLongitude(longitude);
                    break;
                case '3':
                    position.setLongitude(Double.valueOf(value) * 0.000001);
                    break;
                case '6':
                    double latitude = Double.valueOf(value.substring(1));
                    if (value.charAt(0) == 'S') latitude = -latitude;
                    position.setLatitude(latitude);
                    break;
                case '7':
                    latitude = Double.valueOf(value.substring(3)) / 60;
                    latitude += Integer.valueOf(value.substring(1, 3));
                    if (value.charAt(0) == 'S') latitude = -latitude;
                    position.setLatitude(latitude);
                    break;
                case '8':
                    position.setLatitude(Double.valueOf(value) * 0.000001);
                    break;
                case 'G':
                    position.setAltitude(Double.valueOf(value));
                    break;
                case 'H':
                    position.setSpeed(Double.valueOf(value));
                    break;
                case 'I':
                    position.setSpeed(UnitsConverter.knotsFromKph(Double.valueOf(value)));
                    break;
                case 'J':
                    position.setSpeed(UnitsConverter.knotsFromMph(Double.valueOf(value)));
                    break;
                case 'K':
                    position.setCourse(Double.valueOf(value));
                    break;
                case 'N':
                    position.set(Event.KEY_BATTERY, value);
                    break;
                default:
                    // Unsupported
                    break;
            }

            valueIndex += 1;
        }
        return position;
    }
    
    private static final Pattern pattern = Pattern.compile(
            "\\$" +
            "(\\d+)," +                    // IMEI
            "\\d+," +                      // mode
            "(\\d)," +                     // Fix
            "(\\d{2})(\\d{2})(\\d{2})," +  // Date (DDMMYY)
            "(\\d{2})(\\d{2})(\\d{2})," +  // Time (HHMMSS)
            "([EW])" +
            "(\\d{3})(\\d{2}\\.\\d+)," +   // Longitude (DDDMM.MMMM)
            "([NS])" +
            "(\\d{2})(\\d{2}\\.\\d+)," +   // Latitude (DDMM.MMMM)
            "(\\d+\\.?\\d*)," +            // Altitude
            "(\\d+\\.?\\d*)," +            // Speed
            "(\\d+\\.?\\d*)," +            // Course
            "(\\d+)," +                    // Satellites
            "(\\d+\\.?\\d*)");             // HDOP
    
    private Position decodeAlternative(Channel channel, String sentence) {

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocolName());
        Integer index = 1;

        // Identification
        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Validity
        position.setValid(parser.group(index++).compareTo("1") != 0);
        
        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Longitude
        String hemisphere = parser.group(index++);
        Double longitude = Double.valueOf(parser.group(index++));
        longitude += Double.valueOf(parser.group(index++)) / 60;
        if (hemisphere.compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Latitude
        hemisphere = parser.group(index++);
        Double latitude = Double.valueOf(parser.group(index++));
        latitude += Double.valueOf(parser.group(index++)) / 60;
        if (hemisphere.compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Altitude
        position.setAltitude(Double.valueOf(parser.group(index++)));

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));

        // Course
        position.setCourse(Double.valueOf(parser.group(index++)));

        // Satellites
        position.set(Event.KEY_SATELLITES, Integer.valueOf(parser.group(index++)));

        // HDOP
        position.set(Event.KEY_HDOP, parser.group(index++));
        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;
        
        if (sentence.startsWith("GS")) {
            return decodeOriginal(channel, sentence);
        } else if (sentence.startsWith("$")) {
            return decodeAlternative(channel, sentence);
        }
        
        return null;
    }

}
