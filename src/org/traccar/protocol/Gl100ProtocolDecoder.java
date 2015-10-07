/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.model.Position;

public class Gl100ProtocolDecoder extends BaseProtocolDecoder {

    public Gl100ProtocolDecoder(Gl100Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "\\+RESP:GT...," +
            "(\\d{15})," +                      // IMEI
            "(?:(?:\\d+," +                     // Number
            "\\d," +                            // Reserved / Geofence id
            "\\d)|" +                           // Reserved / Geofence alert
            "(?:[^,]*))," +                     // Calling number
            "([01])," +                         // GPS fix
            "(\\d+.\\d)," +                     // Speed
            "(\\d+)," +                         // Course
            "(-?\\d+.\\d)," +                   // Altitude
            "\\d*," +                           // GPS accuracy
            "(-?\\d+.\\d+)," +                  // Longitude
            "(-?\\d+.\\d+)," +                  // Latitude
            "(\\d{4})(\\d{2})(\\d{2})" +        // Date (YYYYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +       // Time (HHMMSS)
            ".*");

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Send response
        if (sentence.contains("AT+GTHBD=") && channel != null) {
            String response = "+RESP:GTHBD,GPRS ACTIVE,";
            response += sentence.substring(9, sentence.lastIndexOf(','));
            response += '\0';
            channel.write(response);
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

        // Get device by IMEI
        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Validity
        position.setValid(Integer.parseInt(parser.group(index++)) == 0);

        // Position info
        position.setSpeed(Double.parseDouble(parser.group(index++)));
        position.setCourse(Double.parseDouble(parser.group(index++)));
        position.setAltitude(Double.parseDouble(parser.group(index++)));
        position.setLongitude(Double.parseDouble(parser.group(index++)));
        position.setLatitude(Double.parseDouble(parser.group(index++)));

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
        position.setTime(time.getTime());

        return position;
    }

}
