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

public class TrackboxProtocolDecoder extends BaseProtocolDecoder {
    
    public TrackboxProtocolDecoder(String protocol) {
        super(protocol);
    }

    private static final Pattern pattern = Pattern.compile(
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d{3})," + // Time
            "(\\d{2})(\\d{2}\\.\\d{4})([NS])," + // Latitude (DDMM.MMMM)
            "(\\d{3})(\\d{2}\\.\\d{4})([EW])," + // Longitude (DDDMM.MMMM)
            "(\\d+\\.\\d)," +                    // HDOP
            "(-?\\d+\\.?\\d*)," +                // Altitude
            "(\\d)," +                           // Fix Type
            "(\\d+\\.\\d+)," +                   // Course
            "(\\d+\\.\\d+)," +                   // Speed (kph)
            "(\\d+\\.\\d+)," +                   // Speed (knots)
            "(\\d{2})(\\d{2})(\\d{2})," +        // Date
            "(\\d+)");                           // Satellites

    private void sendResponse(Channel channel) {
        if (channel != null) {
            channel.write("=OK=\r\n");
        }
    }
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("a=connect")) {
            String id = sentence.substring(sentence.indexOf("i=") + 2);
            if (identify(id)) {
                sendResponse(channel);
            }
        }
        
        else {
            // Parse message
            Matcher parser = pattern.matcher(sentence);
            if (!parser.matches()) {
                return null;
            }
            sendResponse(channel);

            // Create new position
            Position position = new Position();
            position.setDeviceId(getDeviceId());
            position.setProtocol(getProtocol());

            Integer index = 1;

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MILLISECOND, Integer.valueOf(parser.group(index++)));

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
            
            // HDOP
            position.set(Event.KEY_HDOP, parser.group(index++));

            // Altitude
            position.setAltitude(Double.valueOf(parser.group(index++)));
            
            // Validity
            int fix = Integer.valueOf(parser.group(index++));
            position.set(Event.KEY_GPS, fix);
            position.setValid(fix > 0);

            // Course
            position.setCourse(Double.valueOf(parser.group(index++)));

            // Speed
            index += 1; // speed in kph
            position.setSpeed(Double.valueOf(parser.group(index++)));

            // Date
            time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
            time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
            position.setTime(time.getTime());

            // Satellites
            position.set(Event.KEY_SATELLITES, parser.group(index++));

            return position;
        }
        
        return null;
    }

}
