/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
 *                Rohit
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

public class CarTrackProtocolDecoder extends BaseProtocolDecoder {

    public CarTrackProtocolDecoder(CarTrackProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "\\$\\$" +                                      // Header
            "(\\d+)\\?*" +                                  // Device ID
            "&A" +
            "(\\d{4})" +                                    // Command - 2
            "&B" +
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d{3})," +        // HHMMSS.DDD
            "([AV])," +                                     // STATUS : A= Valid, V = Invalid
            "(\\d{2})(\\d{2}\\.\\d{4})," +                  // Lat : XXMM.DDDDD
            "([NS])," +                                     // N/S
            "(\\d{3})(\\d{2}\\.\\d{4})," +                  // Long : YYYMM.DDDD
            "([EW])," +                                     // E/W
            "(\\d+.\\d*)?," +                               // Speed in Knots
            "(\\d+.\\d*)?," +                               // Heading
            "(\\d{2})(\\d{2})(\\d{2})" +                    // DDMMYY
            ".*" +
            "&C(.*)" +                                      // IO Port Data
            "&D(.*)" +                                      // Mile Meter Data
            "&E(.*)" +                                      // Alarm Data
            "(?:&Y)?(.*)");                                 // AD Input Data

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

        // Get device by unique identifier
        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Command
        position.set("command", parser.group(index++));

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MILLISECOND, Integer.parseInt(parser.group(index++)));

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0);

        // Latitude
        double latitude = Double.parseDouble(parser.group(index++));
        latitude += Double.parseDouble(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        double longitude = Double.parseDouble(parser.group(index++));
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

        // State
        position.set(Event.PREFIX_IO + 1, parser.group(index++));

        // Odometer
        String odometer = parser.group(index++);
        odometer = odometer.replace(":", "A");
        odometer = odometer.replace(";", "B");
        odometer = odometer.replace("<", "C");
        odometer = odometer.replace("=", "D");
        odometer = odometer.replace(">", "E");
        odometer = odometer.replace("?", "F");
        position.set(Event.KEY_ODOMETER, Integer.parseInt(odometer, 16));

        position.set(Event.KEY_ALARM, parser.group(index++));
        position.set("ad", parser.group(index++));
        return position;
    }

}
