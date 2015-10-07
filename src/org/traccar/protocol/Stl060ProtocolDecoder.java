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

public class Stl060ProtocolDecoder extends BaseProtocolDecoder {

    public Stl060ProtocolDecoder(Stl060Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            ".*\\$1," +
            "(\\d+)," +                         // IMEI
            "D001," +                           // Type
            "[^,]*," +                          // Vehicle
            "(\\d{2})/(\\d{2})/(\\d{2})," +     // Date
            "(\\d{2}):(\\d{2}):(\\d{2})," +     // Time
            "(\\d{2})(\\d{2})\\.?(\\d+)([NS])," + // Latitude
            "(\\d{3})(\\d{2})\\.?(\\d+)([EW])," + // Longitude
            "(\\d+\\.?\\d*)," +                 // Speed
            "(\\d+\\.?\\d*)," +                 // Course

            "(?:(\\d+)," +                      // Odometer
            "(\\d+)," +                         // Ignition
            "(\\d+)," +                         // DI1
            "(\\d+)," +                         // DI2
            "(\\d+),|" +                        // Fuel

            "([01])," +                         // Charging
            "([01])," +                         // Ignition
            "0,0," +                            // Reserved
            "(\\d+)," +                         // DI
            "([^,]+)," +                        // RFID
            "(\\d+)," +                         // Odometer
            "(\\d+)," +                         // Temperature
            "(\\d+)," +                         // Fuel
            "([01])," +                         // Accelerometer
            "([01])," +                         // DO1
            "([01]),)" +                        // DO2

            "([AV])" +                          // Validity
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

        // Device identification
        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));

        // Time
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
        position.setTime(time.getTime());

        // Latitude
        Double latitude = Double.parseDouble(parser.group(index++));
        latitude += Double.parseDouble(parser.group(index++) + parser.group(index++)) / 600000;
        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        Double longitude = Double.parseDouble(parser.group(index++));
        longitude += Double.parseDouble(parser.group(index++) + parser.group(index++)) / 600000;
        if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Speed
        position.setSpeed(Double.parseDouble(parser.group(index++)));

        // Course
        position.setCourse(Double.parseDouble(parser.group(index++)));

        // Old format
        if (parser.group(index) != null) {
            position.set(Event.KEY_ODOMETER, Integer.parseInt(parser.group(index++)));
            position.set(Event.KEY_IGNITION, Integer.parseInt(parser.group(index++)));
            position.set(Event.KEY_INPUT, Integer.parseInt(parser.group(index++)) + Integer.parseInt(parser.group(index++)) << 1);
            position.set(Event.KEY_FUEL, Integer.parseInt(parser.group(index++)));
        } else {
            index += 5;
        }

        // New format
        if (parser.group(index) != null) {
            position.set(Event.KEY_CHARGE, Integer.parseInt(parser.group(index++)) == 1);
            position.set(Event.KEY_IGNITION, Integer.parseInt(parser.group(index++)));
            position.set(Event.KEY_INPUT, Integer.parseInt(parser.group(index++)));
            position.set(Event.KEY_RFID, parser.group(index++));
            position.set(Event.KEY_ODOMETER, Integer.parseInt(parser.group(index++)));
            position.set(Event.PREFIX_TEMP + 1, Integer.parseInt(parser.group(index++)));
            position.set(Event.KEY_FUEL, Integer.parseInt(parser.group(index++)));
            position.set("accel", Integer.parseInt(parser.group(index++)) == 1);
            position.set(Event.KEY_OUTPUT, Integer.parseInt(parser.group(index++)) + Integer.parseInt(parser.group(index++)) << 1);
        } else {
            index += 10;
        }

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0);

        return position;
    }

}
