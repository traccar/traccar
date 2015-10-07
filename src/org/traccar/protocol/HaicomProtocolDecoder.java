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

public class HaicomProtocolDecoder extends BaseProtocolDecoder {

    public HaicomProtocolDecoder(HaicomProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "\\$GPRS" +
            "(\\d+)," +                   // IMEI
            "([^,]+)," +                  // Version
            "(\\d{2})(\\d{2})(\\d{2})," + // Date
            "(\\d{2})(\\d{2})(\\d{2})," + // Time
            "(\\d)" +                     // Flags
            "(\\d{2})(\\d{5})" +          // Latitude (DDMMMMM)
            "(\\d{3})(\\d{5})," +         // Longitude (DDDMMMMM)
            "(\\d+)," +                   // Speed
            "(\\d+)," +                   // Course
            "(\\d+)," +                   // Status
            "(\\d+)?," +                  // GPRS counting value
            "(\\d+)?," +                  // GPS power saving counting value
            "(\\d+)," +                   // Switch status
            "(\\d+)" +                    // Relay status
            "(?:[LH]{2})?" +              // Power status
            "#V(\\d+).*");                // Battery

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
        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Firmware version
        position.set(Event.KEY_VERSION, parser.group(index++));

        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
        position.setTime(time.getTime());

        // Validity
        int flags = Integer.parseInt(parser.group(index++));
        position.setValid((flags & 0x1) != 0);

        // Latitude
        Double latitude = Double.parseDouble(parser.group(index++));
        latitude += Double.parseDouble(parser.group(index++)) / 60000;
        if ((flags & 0x4) == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        Double longitude = Double.parseDouble(parser.group(index++));
        longitude += Double.parseDouble(parser.group(index++)) / 60000;
        if ((flags & 0x2) == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Speed
        position.setSpeed(Double.parseDouble(parser.group(index++)) / 10);

        // Course
        position.setCourse(Double.parseDouble(parser.group(index++)) / 10);

        // Additional data
        position.set(Event.KEY_STATUS, parser.group(index++));
        position.set(Event.KEY_GSM, parser.group(index++));
        position.set(Event.KEY_GPS, parser.group(index++));
        position.set(Event.KEY_INPUT, parser.group(index++));
        position.set(Event.KEY_OUTPUT, parser.group(index++));
        position.set(Event.KEY_BATTERY, Double.parseDouble(parser.group(index++)) / 10);

        return position;
    }

}
