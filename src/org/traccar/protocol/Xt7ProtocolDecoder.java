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

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class Xt7ProtocolDecoder extends BaseProtocolDecoder {

    public Xt7ProtocolDecoder(Xt7Protocol protocol) {
        super(protocol);
    }

    private static final Pattern pattern = Pattern.compile(
            "\\$GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d+)," + // Time (HHMMSS.SSS)
            "([AV])," +                         // Validity
            "(\\d{2})(\\d{2}\\.\\d+)," +        // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}\\.\\d+)," +        // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+.\\d+)," +                    // Speed
            "(\\d+\\.?\\d*)?," +                // Course
            "(\\d{2})(\\d{2})(\\d{2})" +        // Date (DDMMYY)
            "[^\\*]+\\*[0-9a-fA-F]{2}," +
            "(\\d+,\\d+)," +                    // IMSI
            "([0-9a-fA-F]+,[0-9a-fA-F]+)," +    // Cell
            "(\\d+)," +                         // Signal quality
            "(\\d+)," +                         // Battery
            "([01]{4})," +                      // Flags
            "([01]{4})," +                      // Sensors
            "(\\d+)," +                         // Fuel
            "(.+)?");                           // Alarm

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {
        
        ChannelBuffer buf = (ChannelBuffer) msg;
        
        buf.skipBytes(3); // STX

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocolName());
        
        // Get device by id
        String id = buf.readBytes(16).toString(Charset.defaultCharset()).trim();
        if (!identify(id, channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        buf.readUnsignedByte(); // command
        int length = buf.readUnsignedByte();
        
        // Parse message
        String sentence = buf.readBytes(length).toString(Charset.defaultCharset());
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }
        
        Integer index = 1;

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
        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        Double longitude = Double.valueOf(parser.group(index++));
        longitude += Double.valueOf(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));

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

        // IMSI
        position.set("imsi", parser.group(index++));

        // Cell
        position.set(Event.KEY_CELL, parser.group(index++));

        // GSM signal quality
        position.set(Event.KEY_GSM, parser.group(index++));
        
        // Battery
        position.set(Event.KEY_POWER, Double.valueOf(parser.group(index++)));
        
        // Flags
        position.set(Event.KEY_FLAGS, parser.group(index++));

        // Sensors
        position.set(Event.KEY_INPUT, parser.group(index++));

        // Fuel
        position.set(Event.KEY_FUEL, parser.group(index++));

        // Alarm
        position.set(Event.KEY_ALARM, parser.group(index++));

        return position;
    }

}
