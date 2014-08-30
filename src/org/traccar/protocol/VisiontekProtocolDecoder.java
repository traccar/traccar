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
import org.traccar.ServerManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class VisiontekProtocolDecoder extends BaseProtocolDecoder {

    public VisiontekProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    private static final Pattern pattern = Pattern.compile(
            "\\$1," +
            "([^,]+)," +                        // Identifier
            "(?:(\\d+),)?" +                    // IMEI
            "(\\d{2}),(\\d{2}),(\\d{2})," +     // Date
            "(\\d{2}),(\\d{2}),(\\d{2})," +     // Time
            "(\\d{2})(\\d{6})([NS])," +         // Latitude
            "(\\d{3})(\\d{6})([EW])," +         // Longitude
            "(\\d+\\.\\d+)," +                  // Speed
            "(\\d+)," +                         // Course
            "(?:(\\d+)," +                      // Altitude
            "(\\d+),)?" +                       // Satellites
            "(\\d+)," +                         // Milage
            "(?:(\\d)," +                       // Ignition
            "(\\d)," +                          // Input 1
            "(\\d)," +                          // Input 2
            "(\\d)," +                          // Immobilizer
            "(\\d)," +                          // External Battery Status
            "(\\d+),)?" +                       // GSM
            "([AV]),?" +                        // Validity
            "(\\d+)?" +                         // RFID
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
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("visiontek");

        Integer index = 1;

        // Device identification
        String id = parser.group(index++);
        String imei = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(id).getId());
        } catch(Exception error) {
            if (imei != null) {
                try {
                    position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
                } catch(Exception error2) {
                    Log.warning("Unknown device - " + id);
                    return null;
                }
            } else {
                Log.warning("Unknown device - " + id);
                return null;
            }
        }
        
        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Latitude
        Double latitude = Double.valueOf(parser.group(index++));
        latitude += Double.valueOf(parser.group(index++)) / 600000;
        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        Double longitude = Double.valueOf(parser.group(index++));
        longitude += Double.valueOf(parser.group(index++)) / 600000;
        if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)) * 0.539957);

        // Course
        position.setCourse(Double.valueOf(parser.group(index++)));

        // Altitude
        String altitude = parser.group(index++);
        if (altitude != null) {
            position.setAltitude(Double.valueOf(altitude));
        } else {
            position.setAltitude(0.0);
        }

        // Additional data
        extendedInfo.set("satellites", parser.group(index++));
        extendedInfo.set("milage", parser.group(index++));
        extendedInfo.set("ignition", parser.group(index++));
        extendedInfo.set("input1", parser.group(index++));
        extendedInfo.set("input2", parser.group(index++));
        extendedInfo.set("immobilizer", parser.group(index++));
        extendedInfo.set("power", parser.group(index++));
        extendedInfo.set("gsm", parser.group(index++));

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0);

        // RFID
        extendedInfo.set("rfid", parser.group(index++));

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

}
