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

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class MtxProtocolDecoder extends BaseProtocolDecoder {

    public MtxProtocolDecoder(MtxProtocol protocol) {
        super(protocol);
    }

    private static final Pattern pattern = Pattern.compile(
            "#MTX," +
            "(\\d+)," +                         // IMEI
            "(\\d{4})(\\d{2})(\\d{2})," +       // Date
            "(\\d{2})(\\d{2})(\\d{2})," +       // Time
            "(-?\\d+\\.\\d+)," +                // Latitude
            "(-?\\d+\\.\\d+)," +                // Longitude
            "(\\d+\\.?\\d*)," +                 // Speed
            "(\\d+)," +                         // Course
            "(\\d+\\.?\\d*)," +                 // Odometer
            "(?:\\d+|X)," +
            "(?:[01]|X)," +
            "([01]+)," +                        // Input
            "([01]+)," +                        // Output
            "(\\d+)," +                         // ADC1
            "(\\d+)" +                          // ADC2
            ".*");

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Send response
        if (channel != null) {
            channel.write("#ACK");
        }

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
        
        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Location
        position.setValid(true);
        position.setLatitude(Double.valueOf(parser.group(index++)));
        position.setLongitude(Double.valueOf(parser.group(index++)));
        position.setSpeed(Double.valueOf(parser.group(index++)));
        position.setCourse(Double.valueOf(parser.group(index++)));

        // Other
        position.set(Event.KEY_ODOMETER, Double.valueOf(parser.group(index++)));
        position.set(Event.KEY_INPUT, parser.group(index++));
        position.set(Event.KEY_OUTPUT, parser.group(index++));
        position.set(Event.PREFIX_ADC + 1, parser.group(index++));
        position.set(Event.PREFIX_ADC + 2, parser.group(index++));

        return position;
    }

}
