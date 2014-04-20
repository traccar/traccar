/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
 *                Rohit
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
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class CarTrackProtocolDecoder extends BaseProtocolDecoder {

    public CarTrackProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    private static final Pattern pattern = Pattern.compile(
            "\\$\\$" +                                      // Header
            "(\\d+)\\?*" +                                  // Device ID
            "\\&A" +
            "(\\d{4})" +                                    // Command - 2
            "\\&B" +
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d{3})," +        // HHMMSS.DDD
            "([AV])," +                                     // STATUS : A= Valid, V = Invalid
            "(\\d{2})(\\d{2}\\.\\d{4})," +                  // Lat : XXMM.DDDDD
            "([NS])," +                                     // N/S
            "(\\d{3})(\\d{2}\\.\\d{4})," +                  // Long : YYYMM.DDDD
            "([EW])," +                                     // E/W
            "(\\d+.\\d*)," +                                // Speed in Knots
            "(\\d+.\\d*)?," +                               // Heading
            "(\\d{2})(\\d{2})(\\d{2})" +                    // DDMMYY
            ".*" +
            "\\&C(.*)" +                                    // IO Port Data
            "\\&D(.*)" +                                    // Mile Meter Data
            "\\&E(.*)" +                                    // Alarm Data
            "\\&Y(.*)");                                    // AD Input Data

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
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("cartrack");
        Integer index = 1;

        // Get device by unique identifier
        String id = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(id).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + id);
            return null;
        }

        // Command
        extendedInfo.set("command", parser.group(index++));

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
        } else {
            position.setCourse(0.0);
        }

        // Date
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Altitude
        position.setAltitude(0.0);
        
        // State
        extendedInfo.set("io", parser.group(index++));
        extendedInfo.set("milage", parser.group(index++));
        extendedInfo.set("alarm", parser.group(index++));
        extendedInfo.set("ad", parser.group(index++));

        position.setExtendedInfo(extendedInfo.toString());
        return position;
    }

}
