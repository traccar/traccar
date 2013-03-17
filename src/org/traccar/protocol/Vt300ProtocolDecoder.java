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

public class Vt300ProtocolDecoder extends BaseProtocolDecoder {

    public Vt300ProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    /**
     * Regular expressions pattern
     */
    //210000001,20070313170040,121.123456,12.654321,0,233,0,9,2,0.0, 0,0.00,0.00,0
    //YYYYMMDDhhmmss
    static private Pattern pattern = Pattern.compile(
            ".*" +                         // Header
            "(\\d+)," +                    // Device Identifier
            "(\\d{4})(\\d{2})(\\d{2})" +   // Date (YYYYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +  // Time (HHMMSS)
            "(-?\\d+\\.\\d+)," +           // Longitude
            "(-?\\d+\\.\\d+)," +           // Latitude
            "(\\d+)," +                    // Speed
            "(\\d+)," +                    // Course
            "(\\d+)," +                    // Altitude
            "(\\d+)," +                    // Satellites
            "(\\d+)," +                    // Event
            "(\\d+\\.\\d+)," +             // Milage
            "(\\d+)," +                    // Input
            "(\\d+\\.\\d+)," +             // ADC1
            "(\\d+\\.\\d+)," +             // ADC2
            "(\\d+)");                     // Output

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
        extendedInfo.append("<milage>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</milage>");
        
        // Input
        extendedInfo.append("<input>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</input>");
        
        // ADC
        extendedInfo.append("<adc1>").append(parser.group(index++)).append("</adc1>");
        extendedInfo.append("<adc2>").append(parser.group(index++)).append("</adc2>");
        
        // Output
        extendedInfo.append("<output>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</output>");

        position.setExtendedInfo(extendedInfo.toString());
        return position;
    }

}
