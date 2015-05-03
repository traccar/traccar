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

public class BoxProtocolDecoder extends BaseProtocolDecoder {

    public BoxProtocolDecoder(String protocol) {
        super(protocol);
    }

    private static final Pattern pattern = Pattern.compile(
            "L," +
            "(\\d{2})(\\d{2})(\\d{2})" +  // Date
            "(\\d{2})(\\d{2})(\\d{2})," + // Time
            "G," +
            "(-?\\d+\\.\\d+)," +          // Latitude
            "(-?\\d+\\.\\d+)," +          // Longitude
            "(\\d+\\.?\\d*)," +           // Speed
            "(\\d+\\.?\\d*)," +           // Course
            "(\\d+)," +                   // Distance
            "(\\d+)," +                   // Event
            "(\\d+)" +                    // Status
            ".*");

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;
        
        if (sentence.startsWith("H,")) {
            
            int index = sentence.indexOf(',', 2) + 1;
            String id = sentence.substring(index, sentence.indexOf(',', index));
            identify(id);
        }
        
        else if (sentence.startsWith("L,")) {

            // Parse message
            Matcher parser = pattern.matcher(sentence);
            if (!parser.matches()) {
                return null;
            }

            // Create new position
            Position position = new Position();
            position.setDeviceId(getDeviceId());
            position.setProtocol(getProtocol());

            Integer index = 1;

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
            time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
            position.setTime(time.getTime());

            // Location
            position.setLatitude(Double.valueOf(parser.group(index++)));
            position.setLongitude(Double.valueOf(parser.group(index++)));
            position.setSpeed(Double.valueOf(parser.group(index++)));
            position.setCourse(Double.valueOf(parser.group(index++)));
            
            // Distance
            position.set(Event.KEY_ODOMETER, parser.group(index++));
            
            // Event
            position.set(Event.KEY_EVENT, parser.group(index++));
            
            // Status
            int status = Integer.valueOf(parser.group(index++));
            position.setValid((status & 0x04) == 0);
            position.set(Event.KEY_STATUS, status);
            return position;
        }
        
        return null;
    }

}
