/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
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

public class V680ProtocolDecoder extends BaseProtocolDecoder {

    public V680ProtocolDecoder(String protocol) {
        super(protocol);
    }

    private static final Pattern pattern = Pattern.compile(
            "(?:#(\\d+)#" +                // IMEI
            "([^#]*)#)?" +                 // User
            "(\\d+)#" +                    // Fix
            "([^#]+)#" +                   // Password
            "([^#]+)#" +                   // Event
            "(\\d+)#" +                    // Packet number
            "([^#]+)?#?" +                 // GSM base station
            "(?:[^#]+#)?" +
            "(\\d+)?(\\d{2}\\.\\d+)," +    // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+)?(\\d{2}\\.\\d+)," +    // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d+\\.\\d+)," +             // Speed
            "(\\d+\\.?\\d*)?#" +           // Course
            "(\\d{2})(\\d{2})(\\d{2})#" +  // Date (DDMMYY)
            "(\\d{2})(\\d{2})(\\d{2})" +   // Time (HHMMSS)
            ".*");

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;
        sentence = sentence.trim();
       
        // Detect device ID
        if (sentence.length() == 16) {
            String imei = sentence.substring(1, sentence.length());
            identify(imei);
        } else {

            // Parse message
            Matcher parser = pattern.matcher(sentence);
            if (!parser.matches()) {
                return null;
            }

            // Create new position
            Position position = new Position();
            position.setProtocol(getProtocol());
            Integer index = 1;

            // Get device by IMEI
            String imei = parser.group(index++);
            if (imei != null) {
                identify(imei);
            }
            if (!hasDeviceId()) {
                return null;
            }
            position.setDeviceId(getDeviceId());

            // User
            position.set("user", parser.group(index++));

            // Validity
            position.setValid(Integer.valueOf(parser.group(index++)) > 0);

            // Password
            position.set("password", parser.group(index++));

            // Event
            position.set(Event.KEY_EVENT, parser.group(index++));

            // Packet number
            position.set("packet", parser.group(index++));

            // GSM base station
            position.set(Event.KEY_GSM, parser.group(index++));

            // Longitude
            String lon = parser.group(index++);
            Double longitude = (lon != null) ? Double.valueOf(lon) : 0.0;
            longitude += Double.valueOf(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
            position.setLongitude(longitude);

            // Latitude
            String lat = parser.group(index++);
            Double latitude = (lat != null) ? Double.valueOf(lat) : 0.0;
            latitude += Double.valueOf(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
            position.setLatitude(latitude);

            // Speed and Course
            position.setSpeed(Double.valueOf(parser.group(index++)));
            String course = parser.group(index++);
            if (course != null) {
                position.setCourse(Double.valueOf(course));
            }

            // Date
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            int day = Integer.valueOf(parser.group(index++));
            int month = Integer.valueOf(parser.group(index++));
            if (day == 0 && month == 0) {
                return null; // invalid date
            }
            time.set(Calendar.DAY_OF_MONTH, day);
            time.set(Calendar.MONTH, month - 1);
            time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));

            // Time
            time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
            position.setTime(time.getTime());
            return position;
        }
        
        return null;
    }

}
