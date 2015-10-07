/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

public class GpsMarkerProtocolDecoder extends BaseProtocolDecoder {

    public GpsMarkerProtocolDecoder(GpsMarkerProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "\\$GM" +
            "\\d" +                             // Type
            "(?:\\p{XDigit}{2})?" +             // Index
            "(\\d{15})" +                       // IMEI
            "T(\\d{2})(\\d{2})(\\d{2})" +       // Date
            "(\\d{2})(\\d{2})(\\d{2})?" +       // Time
            "([NS])" +
            "(\\d{2})(\\d{2}\\d{4})" +          // Latitude
            "([EW])" +
            "(\\d{3})(\\d{2}\\d{4})" +          // Longitude
            "(\\d{3})" +                        // Speed
            "(\\d{3})" +                        // Course
            "(\\d)" +                           // Satellites
            "(\\d{2})" +                        // Battery
            "(\\d)" +                           // Input
            "(\\d)" +                           // Output
            "(\\d{3})" +                        // Temperature
            ".*");

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Parse message
        Matcher parser = PATTERN.matcher(sentence);
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

        // Date and Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
        String seconds = parser.group(index++);
        if (seconds != null) {
            time.set(Calendar.SECOND, Integer.parseInt(seconds));
        }
        position.setTime(time.getTime());

        // Validity
        position.setValid(true);

        // Latitude
        String hemisphere = parser.group(index++);
        Double latitude = Double.parseDouble(parser.group(index++));
        latitude += Double.parseDouble(parser.group(index++)) / 600000;
        if (hemisphere.compareTo("S") == 0) {
            latitude = -latitude;
        }
        position.setLatitude(latitude);

        // Longitude
        hemisphere = parser.group(index++);
        Double longitude = Double.parseDouble(parser.group(index++));
        longitude += Double.parseDouble(parser.group(index++)) / 600000;
        if (hemisphere.compareTo("W") == 0) {
            longitude = -longitude;
        }
        position.setLongitude(longitude);

        // Speed
        position.setSpeed(Double.parseDouble(parser.group(index++)));

        // Course
        position.setCourse(Double.parseDouble(parser.group(index++)));

        // Additional data
        position.set(Event.KEY_SATELLITES, parser.group(index++));
        position.set(Event.KEY_BATTERY, parser.group(index++));
        position.set(Event.KEY_INPUT, parser.group(index++));
        position.set(Event.KEY_OUTPUT, parser.group(index++));
        position.set(Event.PREFIX_TEMP + 1, parser.group(index++));

        return position;
    }

}
