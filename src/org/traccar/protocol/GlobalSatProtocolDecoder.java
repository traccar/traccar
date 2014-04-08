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
import java.util.Properties;
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

public class GlobalSatProtocolDecoder extends BaseProtocolDecoder {

    private String format0;
    private String format1;

    public GlobalSatProtocolDecoder(ServerManager serverManager) {
        super(serverManager);

        // Initialize format strings
        format0 = "TSPRXAB27GHKLMnaicz*U!";
        format1 = "SARY*U!";
        if (getServerManager() != null) {
            Properties p = getServerManager().getProperties();
            if (p.containsKey("globalsat.format0")) {
                format0 = p.getProperty("globalsat.format0");
            }
            if (p.containsKey("globalsat.format1")) {
                format1 = p.getProperty("globalsat.format1");
            }
        }
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
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("globalsat");

        for (int formatIndex = 0, valueIndex = 1; formatIndex < format.length() && valueIndex < values.length; formatIndex++) {
            String value = values[valueIndex];

            switch(format.charAt(formatIndex)) {
                case 'S':
                    try {
                        position.setDeviceId(getDataManager().getDeviceByImei(value).getId());
                    } catch(Exception error) {
                        Log.warning("Unknown device - " + value);
                        return null;
                    }
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
                    position.setSpeed(Double.valueOf(value) * 0.539957);
                    break;
                case 'J':
                    position.setSpeed(Double.valueOf(value) * 0.868976);
                    break;
                case 'K':
                    position.setCourse(Double.valueOf(value));
                    break;
                case 'N':
                    extendedInfo.set("battery", Double.valueOf(value));
                    break;
                default:
                    // Unsupported
                    break;
            }

            valueIndex += 1;
        }

        position.setExtendedInfo(extendedInfo.toString());
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
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("globalsat");
        Integer index = 1;

        // Identification
        String imei = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + imei);
            return null;
        }

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
        extendedInfo.set("satellites", Integer.valueOf(parser.group(index++)));

        // HDOP
        extendedInfo.set("hdop", parser.group(index++));

        position.setExtendedInfo(extendedInfo.toString());
        return position;
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
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
