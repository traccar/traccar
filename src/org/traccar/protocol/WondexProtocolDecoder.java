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
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class WondexProtocolDecoder extends BaseProtocolDecoder {

    public WondexProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "[^\\d]*" +                    // Header
            "(\\d+)," +                    // Device Identifier
            "(\\d{4})(\\d{2})(\\d{2})" +   // Date (YYYYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +  // Time (HHMMSS)
            "(-?\\d+\\.\\d+)," +           // Longitude
            "(-?\\d+\\.\\d+)," +           // Latitude
            "(\\d+)," +                    // Speed
            "(\\d+)," +                    // Course
            "(\\d+)," +                    // Altitude
            "(\\d+)," +                    // Satellites
            "(\\d+),?" +                   // Event
            "(\\d+\\.\\d+)?,?" +           // Milage
            "(\\d+)?,?" +                  // Input
            "(\\d+\\.\\d+)?,?" +           // ADC1
            "(\\d+\\.\\d+)?,?" +           // ADC2
            "(\\d+)?");                    // Output

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        // Parse message
        Matcher parser = pattern.matcher((String) msg);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        StringBuilder extendedInfo = new StringBuilder("<protocol>vt300</protocol>");
        int index = 1;

        // Device identifier
        String id = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(id).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + id);
            return null;
        }

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Position data
        position.setLongitude(Double.valueOf(parser.group(index++)));
        position.setLatitude(Double.valueOf(parser.group(index++)));
        position.setSpeed(Double.valueOf(parser.group(index++)));
        position.setCourse(Double.valueOf(parser.group(index++)));
        position.setAltitude(Double.valueOf(parser.group(index++)));

        // Satellites
        int satellites = Integer.valueOf(parser.group(index++));
        position.setValid(satellites >= 3);
        extendedInfo.append("<satellites>").append(satellites).append("</satellites>");
        
        // Event
        extendedInfo.append("<event>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</event>");
        
        // Milage
        String milage = parser.group(index++);
        if (milage != null) {
            extendedInfo.append("<milage>").append(milage).append("</milage>");
        }
        
        // Input
        String input = parser.group(index++);
        if (input != null) {
            extendedInfo.append("<input>").append(input).append("</input>");
        }
        
        // ADC
        String adc1 = parser.group(index++);
        if (adc1 != null) {
            extendedInfo.append("<adc1>").append(adc1).append("</adc1>");
        }
        String adc2 = parser.group(index++);
        if (adc2 != null) {
            extendedInfo.append("<adc2>").append(adc2).append("</adc2>");
        }
        
        // Output
        String output = parser.group(index++);
        if (output != null) {
            extendedInfo.append("<output>").append(output).append("</output>");
        }

        position.setExtendedInfo(extendedInfo.toString());
        return position;
    }

}
