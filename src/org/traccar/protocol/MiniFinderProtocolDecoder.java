/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
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

public class MiniFinderProtocolDecoder extends BaseProtocolDecoder {

    public MiniFinderProtocolDecoder(MiniFinderProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "!D," +
            "(\\d+)/(\\d+)/(\\d+)," +      // Date
            "(\\d+):(\\d+):(\\d+)," +      // Time
            "(-?\\d+\\.\\d+)," +           // Latitude
            "(-?\\d+\\.\\d+)," +           // Longitude
            "(\\d+\\.?\\d*)," +            // Speed
            "(\\d+\\.?\\d*)," +            // Course
            "(\\p{XDigit}+)," +            // Flags
            "(-?\\d+\\.\\d+)," +           // Altitude
            "(\\d+)," +                    // Battery
            "(\\d+)," +                    // Satellites in use
            "(\\d+)," +                    // Satellites in view
            "0");

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("!1")) {

            // Identification
            identify(sentence.substring(3, sentence.length()), channel);

        } else if (sentence.startsWith("!D") && hasDeviceId()) {

            // Location
            Matcher parser = PATTERN.matcher(sentence);
            if (!parser.matches()) {
                return null;
            }

            // Create new position
            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());

            Integer index = 1;

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
            time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
            time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
            position.setTime(time.getTime());

            // Location
            position.setLatitude(Double.parseDouble(parser.group(index++)));
            position.setLongitude(Double.parseDouble(parser.group(index++)));
            position.setSpeed(Double.parseDouble(parser.group(index++)));
            position.setCourse(Double.parseDouble(parser.group(index++)));

            // Flags
            String flags = parser.group(index++);
            position.set(Event.KEY_FLAGS, flags);
            position.setValid((Integer.parseInt(flags, 16) & 0x01) != 0);

            // Altitude
            position.setAltitude(Double.parseDouble(parser.group(index++)));

            // Battery
            position.set(Event.KEY_BATTERY, parser.group(index++));

            // Satellites
            position.set(Event.KEY_SATELLITES, parser.group(index++));

            return position;
        }

        return null;
    }

}
