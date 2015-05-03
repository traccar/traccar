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

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class Stl060ProtocolDecoder extends BaseProtocolDecoder {

    public Stl060ProtocolDecoder(String protocol) {
        super(protocol);
    }

    private static final Pattern pattern = Pattern.compile(
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
            "(\\d+)," +                         // Odometer
            "(\\d+)," +                         // Ignition
            "(\\d+)," +                         // DIP1
            "(\\d+)," +                         // DIP2
            "(\\d+)," +                         // Fuel
            "([AV])" +                          // Validity
            ".*");

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocol());

        Integer index = 1;

        // Device identification
        if (!identify(parser.group(index++))) {
            return null;
        }
        position.setDeviceId(getDeviceId());
        
        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));

        // Time
        time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Latitude
        Double latitude = Double.valueOf(parser.group(index++));
        latitude += Double.valueOf(parser.group(index++) + parser.group(index++)) / 600000;
        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        Double longitude = Double.valueOf(parser.group(index++));
        longitude += Double.valueOf(parser.group(index++) + parser.group(index++)) / 600000;
        if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));

        // Course
        position.setCourse(Double.valueOf(parser.group(index++)));

        // Other
        position.set(Event.KEY_ODOMETER, Integer.valueOf(parser.group(index++)));
        position.set(Event.KEY_IGNITION, Integer.valueOf(parser.group(index++)));
        position.set("dip1", Integer.valueOf(parser.group(index++)));
        position.set("dip2", Integer.valueOf(parser.group(index++)));
        position.set(Event.KEY_FUEL, Integer.valueOf(parser.group(index++)));

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0);

        return position;
    }

}
