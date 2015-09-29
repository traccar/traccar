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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class AquilaProtocolDecoder extends BaseProtocolDecoder {

    public AquilaProtocolDecoder(AquilaProtocol protocol) {
        super(protocol);
    }

    private static final Pattern pattern = Pattern.compile(
            "\\$\\$" +
            "[^,]+," +                          // Client
            "(\\d+)," +                         // Device serial number
            "(\\d+)," +                         // Event
            "(-?\\d+\\.\\d+)," +                // Latitude
            "(-?\\d+\\.\\d+)," +                // Longitude
            "(\\d{2})(\\d{2})(\\d{2})" +        // Date (YYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +       // Time (HHMMSS)
            "([AV])," +                         // Validity
            "(\\d+)," +                         // GSM
            "(\\d+)," +                         // Speed
            "(\\d+)," +                         // Distance
            "\\d+," +                           // Driver code
            "(\\d+)," +                         // Fuel
            "([01])," +                         // IO 1
            "[01]," +                           // Case open switch
            "[01]," +                           // Over speed start
            "[01]," +                           // Over speed end
            "(?:\\d+,){3}" +                    // Reserved
            "([01])," +                         // Power status
            "([01])," +                         // IO 2
            "\\d+," +                           // Reserved
            "([01])," +                         // Ignition
            "[01]," +                           // Ignition off event
            "(?:\\d+,){7}" +                    // Reserved
            "[01]," +                           // Corner packet
            "(?:\\d+,){8}" +                    // Reserved
            "([01])," +                         // Course bit 0
            "([01])," +                         // Course bit 1
            "([01])," +                         // Course bit 2
            "([01])," +                         // Course bit 3
            "\\*(\\p{XDigit}{2})");             // Checksum

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        Integer index = 1;

        String id = parser.group(index++);
        if (!identify(id, channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        position.set(Event.KEY_EVENT, Integer.parseInt(parser.group(index++)));

        position.setLatitude(Double.parseDouble(parser.group(index++)));
        position.setLongitude(Double.parseDouble(parser.group(index++)));

        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
        position.setTime(time.getTime());

        position.setValid(parser.group(index++).equals("A"));

        position.set(Event.KEY_GSM, Integer.parseInt(parser.group(index++)));

        position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(parser.group(index++))));

        position.set(Event.KEY_ODOMETER, parser.group(index++));
        position.set(Event.KEY_FUEL, parser.group(index++));
        position.set(Event.PREFIX_IO + 1, parser.group(index++));
        position.set(Event.KEY_CHARGE, parser.group(index++));
        position.set(Event.PREFIX_IO + 2, parser.group(index++));

        position.set(Event.KEY_IGNITION, parser.group(index++).equals("1"));

        int course =
                (Integer.parseInt(parser.group(index++)) << 3) +
                (Integer.parseInt(parser.group(index++)) << 2) +
                (Integer.parseInt(parser.group(index++)) << 1) +
                (Integer.parseInt(parser.group(index++)));

        if (course > 0 && course <= 8) {
            position.setCourse((course - 1) * 45);
        }

        return position;
    }

}
