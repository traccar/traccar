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
import org.traccar.helper.Checksum;
import org.traccar.model.Position;

public class LaipacProtocolDecoder extends BaseProtocolDecoder {

    public LaipacProtocolDecoder(LaipacProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "\\$AVRMC," +
            "([^,]+)," +                   // Identifier
            "(\\d{2})(\\d{2})(\\d{2})," +  // Time (HHMMSS)
            "([AVRavr])," +                // Validity
            "(\\d{2})(\\d{2}\\.\\d+)," +   // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}\\.\\d+)," +   // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+\\.\\d+)," +             // Speed
            "(\\d+\\.\\d+)," +             // Course
            "(\\d{2})(\\d{2})(\\d{2})," +  // Date (DDMMYY)
            "(.)," +                       // Type
            "[^\\*]+\\*" +
            "(\\p{XDigit}{2})");           // Checksum

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Heartbeat
        if (sentence.startsWith("$ECHK") && channel != null) {
            channel.write(sentence + "\r\n");
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

        // Identification
        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));

        // Validity
        String status = parser.group(index++);
        position.setValid(status.compareToIgnoreCase("A") == 0);

        // Latitude
        Double latitude = Double.parseDouble(parser.group(index++));
        latitude += Double.parseDouble(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        Double longitude = Double.parseDouble(parser.group(index++));
        longitude += Double.parseDouble(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Speed
        position.setSpeed(Double.parseDouble(parser.group(index++)));

        // Course
        position.setCourse(Double.parseDouble(parser.group(index++)));

        // Date
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
        position.setTime(time.getTime());

        // Response
        String type = parser.group(index++);
        String checksum = parser.group(index++);
        String response = null;

        if (type.equals("0") && Character.isLowerCase(status.charAt(0))) {
            response = "$EAVACK,0," + checksum;
            response += Checksum.nmea(response);
        } else if (type.equals("S") || type.equals("T")) {
            response = "$AVCFG,00000000,t*21";
        } else if (type.equals("3")) {
            response = "$AVCFG,00000000,d*31";
        } else if (type.equals("X") || type.equals("4")) {
            response = "$AVCFG,00000000,x*2D";
        }

        if (response != null && channel != null) {
            channel.write(response + "\r\n");
        }
        return position;
    }

}
