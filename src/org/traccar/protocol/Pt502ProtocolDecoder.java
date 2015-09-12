/*
 * Copyright 2012 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
 *                       Luis Parada (luis.parada@gmail.com)
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

public class Pt502ProtocolDecoder extends BaseProtocolDecoder {

    public Pt502ProtocolDecoder(Pt502Protocol protocol) {
        super(protocol);
    }

    private static final Pattern pattern = Pattern.compile(
            ".*" +
            "\\$[A-Z]{3}\\d?," +                // Type
            "(\\d+)," +                         // Id
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d{3})," + // Time (HHMMSS.SSS)
            "([AV])," +                         // Validity
            "(\\d{2})(\\d{2}\\.\\d{4})," +      // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}\\.\\d{4})," +      // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+\\.\\d+)?," +                 // Speed
            "(\\d+\\.\\d+)?," +                 // Course
            "(\\d{2})(\\d{2})(\\d{2}),,," +     // Date
            "./" +
            "([01])+," +                        // Input
            "([01])+/" +                        // Output
            "([^/]+)?/" +                       // ADC
            "(\\d+)" +                          // Odometer
            "(?:/([^/]+)?/" +                   // RFID
            "(\\p{XDigit}{3}))?" +              // State
            ".*");

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

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
        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MILLISECOND, Integer.valueOf(parser.group(index++)));

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0);

        // Latitude
        Double latitude = Double.valueOf(parser.group(index++));
        latitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("S") == 0) {
            latitude = -latitude;
        }
        position.setLatitude(latitude);

        // Longitude
        Double longitude = Double.valueOf(parser.group(index++));
        longitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("W") == 0) {
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

        // Date
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // IO
        position.set(Event.KEY_INPUT, parser.group(index++));
        position.set(Event.KEY_OUTPUT, parser.group(index++));

        // ADC
        String adc = parser.group(index++);
        if (adc != null) {
            String[] values = adc.split(",");
            for (int i = 0; i < values.length; i++) {
                position.set(Event.PREFIX_ADC + (i + 1), Integer.parseInt(values[i], 16));
            }
        }

        position.set(Event.KEY_ODOMETER, parser.group(index++));

        // Driver
        position.set(Event.KEY_RFID, parser.group(index++));

        // Other
        String status = parser.group(index++);
        if (status != null) {
            int value = Integer.parseInt(status, 16);
            position.set(Event.KEY_BATTERY, value >> 8);
            position.set(Event.KEY_GSM, (value >> 4) & 0xf);
            position.set(Event.KEY_SATELLITES, value & 0xf);
        }

        return position;
    }
}
