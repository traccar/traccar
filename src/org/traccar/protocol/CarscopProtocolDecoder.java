/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
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

public class CarscopProtocolDecoder extends BaseProtocolDecoder {

    public CarscopProtocolDecoder(CarscopProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "\\*.*" +
            "(\\d{2})(\\d{2})(\\d{2})" + // Time (HHMMSS)
            "([AV])" +                   // Validity
            "(\\d{2})(\\d{2}\\.\\d{4})" + // Latitude (DDMM.MMMM)
            "([NS])" +
            "(\\d{3})(\\d{2}\\.\\d{4})" + // Longitude (DDDMM.MMMM)
            "([EW])" +
            "(\\d{3}\\.\\d)" +           // Speed
            "(\\d{2})(\\d{2})(\\d{2})" + // Date (YYMMDD)
            "(\\d{3}\\.\\d{2})" +        // Course
            "(\\d{8})" +                 // State
            "L(\\d{6})");                // Odometer

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Device identification
        int index = sentence.indexOf("UB05");
        if (index != -1) {
            String imei = sentence.substring(index + 4, index + 4 + 15);
            identify(imei, channel);
        }
        if (!hasDeviceId()) {
            return null;
        }

        // Parse message
        Matcher parser = PATTERN.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setDeviceId(getDeviceId());
        position.setProtocol(getProtocolName());
        index = 1;

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));

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
        position.setSpeed(Double.parseDouble(parser.group(index++)));

        // Date
        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
        position.setTime(time.getTime());

        // Course
        position.setCourse(Double.parseDouble(parser.group(index++)));

        // State
        position.set(Event.KEY_STATUS, parser.group(index++));

        // Odometer
        position.set(Event.KEY_ODOMETER, Integer.parseInt(parser.group(index++)));
        return position;
    }

}
