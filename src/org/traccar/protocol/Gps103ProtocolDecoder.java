/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

public class Gps103ProtocolDecoder extends BaseProtocolDecoder {

    public Gps103ProtocolDecoder(Gps103Protocol protocol) {
        super(protocol);
    }

    private static final Pattern pattern = Pattern.compile(
            "imei:" +
            "(\\d+)," +                         // IMEI
            "([^,]+)," +                        // Alarm
            "(\\d{2})/?(\\d{2})/?(\\d{2})\\s?" + // Local Date
            "(\\d{2}):?(\\d{2})(?:\\d{2})?," +  // Local Time
            "[^,]*," +
            "[FL]," +                           // F - full / L - low
            "(?:(\\d{2})(\\d{2})(\\d{2})\\.(\\d+)|" + // Time UTC (HHMMSS.SSS)
            "(?:\\d{1,5}\\.\\d+))," +
            "([AV])," +                         // Validity
            "(?:([NS]),)?" +
            "(\\d+)(\\d{2}\\.\\d+)," +          // Latitude (DDMM.MMMM)
            "(?:([NS]),)?" +
            "(?:([EW]),)?" +
            "(\\d+)(\\d{2}\\.\\d+)," +          // Longitude (DDDMM.MMMM)
            "(?:([EW])?,)?" +
            "(\\d+\\.?\\d*)?,?" +               // Speed
            "(\\d+\\.?\\d*)?,?" +               // Course
            "(\\d+\\.?\\d*)?,?" +               // Altitude
            "([^,;]+)?,?" +
            "([^,;]+)?,?" +
            "([^,;]+)?,?" +
            "([^,;]+)?,?" +
            "([^,;]+)?,?" +
            ".*");

    private static final Pattern handshakePattern = Pattern.compile("##,imei:(\\d+),A");

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Send response #1
        if (sentence.contains("##")) {
            if (channel != null) {
                channel.write("LOAD", remoteAddress);
                Matcher handshakeMatcher = handshakePattern.matcher(sentence);
                if (handshakeMatcher.matches()) {
                    identify(handshakeMatcher.group(1), channel);
                }
            }
            return null;
        }

        // Send response #2
        if (sentence.length() == 15 && Character.isDigit(sentence.charAt(0))) {
            if (channel != null) {
                channel.write("ON", remoteAddress);
            }
            return null;
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
        String imei = parser.group(index++);
        if (!identify(imei, channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Alarm message
        String alarm = parser.group(index++);
        position.set(Event.KEY_ALARM, alarm);
        if (channel != null && alarm.equals("help me")) {
            channel.write("**,imei:" + imei + ",E;", remoteAddress);
        }

        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        
        int localHours = Integer.valueOf(parser.group(index++));
        int localMinutes = Integer.valueOf(parser.group(index++));
        
        String utcHours = parser.group(index++);
        String utcMinutes = parser.group(index++);

        // Time
        time.set(Calendar.HOUR_OF_DAY, localHours);
        time.set(Calendar.MINUTE, localMinutes);
        String seconds = parser.group(index++);
        if (seconds != null) {
            time.set(Calendar.SECOND, Integer.valueOf(seconds));
        }
        String milliseconds = parser.group(index++);
        if (milliseconds != null) {
            time.set(Calendar.MILLISECOND, Integer.valueOf(milliseconds));
        }
        
        // Timezone calculation
        if (utcHours != null && utcMinutes != null) {
            int deltaMinutes = (localHours - Integer.valueOf(utcHours)) * 60;
            deltaMinutes += localMinutes - Integer.valueOf(utcMinutes);
            if (deltaMinutes <= -12 * 60) {
                deltaMinutes += 24 * 60;
            } else if (deltaMinutes > 12 * 60) {
                deltaMinutes -= 24 * 60;
            }
            time.add(Calendar.MINUTE, -deltaMinutes);
        }
        position.setTime(time.getTime());

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0);

        // Latitude
        String hemisphere = parser.group(index++);
        Double latitude = Double.valueOf(parser.group(index++));
        latitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index) != null) {
            hemisphere = parser.group(index);
        }
        index++;
        if (hemisphere.compareTo("S") == 0) {
            latitude = -latitude;
        }
        position.setLatitude(latitude);

        // Longitude
        hemisphere = parser.group(index++);
        Double longitude = Double.valueOf(parser.group(index++));
        longitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index) != null) {
            hemisphere = parser.group(index);
        }
        index++;
        if (hemisphere != null && hemisphere.compareTo("W") == 0) {
            longitude = -longitude;
        }
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

        // Altitude
        String altitude = parser.group(index++);
        if (altitude != null) {
            position.setAltitude(Double.valueOf(altitude));
        }

        // Additional data
        for (int i = 1; i <= 5; i++) {
            position.set(Event.PREFIX_IO + 1, parser.group(index++));
        }

        return position;
    }

}
