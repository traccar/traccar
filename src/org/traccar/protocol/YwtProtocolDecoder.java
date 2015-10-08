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

public class YwtProtocolDecoder extends BaseProtocolDecoder {

    public YwtProtocolDecoder(YwtProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "%(..)," +                     // Type
            "(\\d+):" +                    // Unit identifier
            "\\d+," +                      // Subtype
            "(\\d{2})(\\d{2})(\\d{2})" +   // Date (YYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +  // Time (HHMMSS)
            "([EW])" +
            "(\\d{3}\\.\\d{6})," +         // Longitude (DDDMM.MMMM)
            "([NS])" +
            "(\\d{2}\\.\\d{6})," +         // Latitude (DDMM.MMMM)
            "(\\d+)?," +                   // Altitude
            "(\\d+)," +                    // Speed
            "(\\d+)," +                    // Course
            "(\\d+)," +                    // Satellite
            "([^,]+)," +                   // Report identifier
            "([0-9a-fA-F\\-]+)" +          // Status
            ".*");

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Synchronization
        if (sentence.startsWith("%SN") && channel != null) {
            int start = sentence.indexOf(':');
            int end = start;
            for (int i = 0; i < 4; i++) {
                end = sentence.indexOf(',', end + 1);
            }
            if (end == -1) {
                end = sentence.length();
            }

            channel.write("%AT+SN=" + sentence.substring(start, end));
            return null;
        }

        // Parse message
        Matcher parser = PATTERN.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocolName());
        Integer index = 1;
        String type = parser.group(index++);

        // Device
        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

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

        // Longitude
        String hemisphere = parser.group(index++);
        Double longitude = Double.parseDouble(parser.group(index++));
        if (hemisphere.compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Latitude
        hemisphere = parser.group(index++);
        Double latitude = Double.parseDouble(parser.group(index++));
        if (hemisphere.compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Altitude
        String altitude = parser.group(index++);
        if (altitude != null) {
            position.setAltitude(Double.parseDouble(altitude));
        }

        // Speed
        position.setSpeed(Double.parseDouble(parser.group(index++)));

        // Course
        position.setCourse(Double.parseDouble(parser.group(index++)));

        // Satellites
        int satellites = Integer.parseInt(parser.group(index++));
        position.setValid(satellites >= 3);
        position.set(Event.KEY_SATELLITES, satellites);

        // Report identifier
        String reportId = parser.group(index++);

        // Status
        position.set(Event.KEY_STATUS, parser.group(index++));

        // Send response
        if ((type.equals("KP") || type.equals("EP")) && channel != null) {
            channel.write("%AT+" + type + "=" + reportId + "\r\n");
        }

        return position;
    }

}
