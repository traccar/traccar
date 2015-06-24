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

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class IntellitracProtocolDecoder extends BaseProtocolDecoder {

    public IntellitracProtocolDecoder(IntellitracProtocol protocol) {
        super(protocol);
    }

    private static final Pattern pattern = Pattern.compile(
            "(?:.+,)?(\\d+)," +            // Device Identifier
            "(\\d{4})(\\d{2})(\\d{2})" +   // Date (YYYYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +  // Time (HHMMSS)
            "(-?\\d+\\.\\d+)," +           // Longitude
            "(-?\\d+\\.\\d+)," +           // Latitude
            "(\\d+\\.?\\d*)," +            // Speed
            "(\\d+\\.?\\d*)," +            // Course
            "(-?\\d+\\.?\\d*)," +          // Altitude
            "(\\d+)," +                    // Satellites
            "(\\d+)," +                    // Report Identifier
            "(\\d+)," +                    // Input
            "(\\d+),?" +                   // Output
            "(\\d+\\.\\d+)?,?" +           // ADC1
            "(\\d+\\.\\d+)?,?" +           // ADC2
            "(?:\\d{14},\\d+," +
            "(\\d+)," +                    // VSS
            "(\\d+)," +                    // RPM
            "(-?\\d+)," +                  // Coolant
            "(\\d+)," +                    // Fuel
            "(\\d+)," +                    // Fuel Consumption
            "(-?\\d+)," +                  // Fuel Temperature
            "(\\d+)," +                    // Charger Pressure
            "(\\d+)," +                    // TPL
            "(\\d+)," +                    // Axle Weight
            "(\\d+))?" +                   // Odometer
            ".*");

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
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

        // Detect device
        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());
        
        // Date and time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());
        
        // Location data
        position.setLongitude(Double.valueOf(parser.group(index++)));
        position.setLatitude(Double.valueOf(parser.group(index++)));
        position.setSpeed(Double.valueOf(parser.group(index++)));
        position.setCourse(Double.valueOf(parser.group(index++)));
        position.setAltitude(Double.valueOf(parser.group(index++)));
        
        // Satellites
        int satellites = Integer.valueOf(parser.group(index++));
        position.setValid(satellites >= 3);
        position.set(Event.KEY_SATELLITES, satellites);
        
        // Report identifier
        position.set(Event.KEY_INDEX, Long.valueOf(parser.group(index++)));

        // Input
        position.set(Event.KEY_INPUT, parser.group(index++));

        // Output
        position.set(Event.KEY_OUTPUT, parser.group(index++));

        // ADC
        position.set(Event.PREFIX_ADC + 1, parser.group(index++));
        position.set(Event.PREFIX_ADC + 2, parser.group(index++));

        // J1939 data
        position.set("vss", parser.group(index++));
        position.set("rpm", parser.group(index++));
        position.set("coolant", parser.group(index++));
        position.set(Event.KEY_FUEL, parser.group(index++));
        position.set("consumption", parser.group(index++));
        position.set(Event.PREFIX_TEMP + 1, parser.group(index++));
        position.set(Event.KEY_CHARGE, parser.group(index++));
        position.set("tpl", parser.group(index++));
        position.set("axle", parser.group(index++));
        position.set(Event.KEY_ODOMETER, parser.group(index++));
        return position;
    }

}
