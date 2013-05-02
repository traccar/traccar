/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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

public class MeitrackProtocolDecoder extends BaseProtocolDecoder {

    public MeitrackProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    static private Pattern pattern = Pattern.compile(
            "\\$\\$." +                         // Flag
            "\\d+," +                           // Length
            "(\\d+)," +                         // IMEI
            "[0-9a-fA-F]{3}," +                 // Command
            "(\\d+)," +                         // Event
            "(-?\\d+\\.\\d+)," +                // Latitude
            "(-?\\d+\\.\\d+)," +                // Longitude
            "(\\d{2})(\\d{2})(\\d{2})" +        // Date (YYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +       // Time (HHMMSS)
            "([AV])," +                         // Validity
            "(\\d+)," +                         // Satellites
            "(\\d+)," +                         // GSM Signal
            "(\\d+)," +                         // Speed
            "(\\d+)," +                         // Course
            "(\\d+)," +                         // HDOP
            "(\\d+)," +                         // Altitude
            "(\\d+)," +                         // Milage
            ".*"); // TODO: parse other stuff

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        // Parse message
        String sentence = (String) msg;
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("meitrack");

        Integer index = 1;

        // Get device by IMEI
        String imei = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + imei);
            return null;
        }

        // Event
        extendedInfo.set("event", parser.group(index++));

        // Coordinates
        position.setLatitude(Double.valueOf(parser.group(index++)));
        position.setLongitude(Double.valueOf(parser.group(index++)));

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0 ? true : false);

        // Satellites
        extendedInfo.set("satellites", parser.group(index++));

        // GSM Signal
        extendedInfo.set("gsm", parser.group(index++));

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));

        // Course
        position.setCourse(Double.valueOf(parser.group(index++)));

        // HDOP
        extendedInfo.set("hdop", parser.group(index++));

        // Altitude
        position.setAltitude(Double.valueOf(parser.group(index++)));

        // Milage
        extendedInfo.set("milage", parser.group(index++));

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

}
