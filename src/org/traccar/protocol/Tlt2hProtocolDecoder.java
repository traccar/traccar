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

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class Tlt2hProtocolDecoder extends BaseProtocolDecoder {

    public Tlt2hProtocolDecoder(String protocol) {
        super(protocol);
    }

    private static final Pattern patternHeader = Pattern.compile(
            "#(\\d+)#" +                   // IMEI
            "[^#]+#" +
            "\\d+#" +
            "([^#]+)#" +                   // Status
            "\\d+");                       // Number of records

    private static final Pattern patternPosition = Pattern.compile(
            "#([0-9a-f]+)?" +              // Cell info
            "\\$GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d+)," + // Time (HHMMSS.SSS)
            "([AV])," +                    // Validity
            "(\\d+)(\\d{2}\\.\\d+)," +     // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d+)(\\d{2}\\.\\d+)," +     // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+\\.\\d+)?," +            // Speed
            "(\\d+\\.\\d+)?," +            // Course
            "(\\d{2})(\\d{2})(\\d{2})" +   // Date (DDMMYY)
            ".+");                         // Other (Checksumm)

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;
        sentence = sentence.trim();

        // Decode header
        String header = sentence.substring(0, sentence.indexOf('\r'));
        Matcher parser = patternHeader.matcher(header);
        if (!parser.matches()) {
            return null;
        }

        // Get device identifier
        if (!identify(parser.group(1))) {
            return null;
        }

        // Get status
        String status = parser.group(2);
        
        String[] messages = sentence.substring(sentence.indexOf('\n') + 1).split("\r\n");
        List<Position> positions = new LinkedList<Position>();
        
        for (String message : messages) {
            parser = patternPosition.matcher(message);
            if (parser.matches()) {
                Position position = new Position();
                position.setProtocol(getProtocol());
                position.setDeviceId(getDeviceId());

                Integer index = 1;
                
                // Cell
                position.set(Event.KEY_CELL, parser.group(index++));

                // Time
                Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                time.clear();
                time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
                time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
                time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
                index += 1; // Skip milliseconds

                // Validity
                position.setValid(parser.group(index++).compareTo("A") == 0);

                // Latitude
                Double latitude = Double.valueOf(parser.group(index++));
                latitude += Double.valueOf(parser.group(index++)) / 60;
                if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
                position.setLatitude(latitude);

                // Longitude
                Double longitude = Double.valueOf(parser.group(index++));
                longitude += Double.valueOf(parser.group(index++)) / 60;
                if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
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
                
                // Status
                position.set(Event.KEY_STATUS, status);
                positions.add(position);
            }
        }

        return positions;
    }

}
