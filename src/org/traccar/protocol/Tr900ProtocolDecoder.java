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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tr900ProtocolDecoder extends BaseProtocolDecoder {

    public Tr900ProtocolDecoder(DataManager dataManager, String protocol, Properties properties) {
        super(dataManager, protocol, properties);
    }

    private static final Pattern pattern = Pattern.compile(
            ">(\\d+)," +                   // ID
            "\\d+," +                      // Period
            "(\\d)," +                     // Fix
            "(\\d{2})(\\d{2})(\\d{2})," +  // Date (YYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +  // Time (HHMMSS)
            "([EW])" +
            "(\\d{3})(\\d{2}\\.\\d+)," +   // Longitude (DDDMM.MMMM)
            "([NS])" +
            "(\\d{2})(\\d{2}\\.\\d+)," +   // Latitude (DDMM.MMMM)
            "[^,]*," +                     // Command
            "(\\d+\\.?\\d*)," +            // Speed
            "(\\d+\\.?\\d*)," +            // Course
            "(\\d+)," +                    // GSM
            "(\\d+)," +                    // Event
            "(\\d+)-" +                    // ADC
            "(\\d+)," +                    // Battery
            "\\d+," +                      // Impulses
            "(\\d+)," +                    // Input
            "(\\d+)" +                     // Status
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
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());
        Integer index = 1;

        // Identification
        String id = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(id).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + id);
            return null;
        }

        // Validity
        position.setValid(parser.group(index++).compareTo("1") == 0);

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

        // Longitude
        String hemisphere = parser.group(index++);
        Double longitude = Double.valueOf(parser.group(index++));
        longitude += Double.valueOf(parser.group(index++)) / 60;
        if (hemisphere.compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Latitude
        hemisphere = parser.group(index++);
        Double latitude = Double.valueOf(parser.group(index++));
        latitude += Double.valueOf(parser.group(index++)) / 60;
        if (hemisphere.compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Altitude
        position.setAltitude(0.0);

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));

        // Course
        position.setCourse(Double.valueOf(parser.group(index++)));

        // Other
        extendedInfo.set("gsm", parser.group(index++));
        extendedInfo.set("event", Integer.valueOf(parser.group(index++)));
        extendedInfo.set("adc1", Integer.valueOf(parser.group(index++)));
        extendedInfo.set("battery", Integer.valueOf(parser.group(index++)));
        extendedInfo.set("input", parser.group(index++));
        extendedInfo.set("status", parser.group(index++));

        position.setExtendedInfo(extendedInfo.toString());
        return position;
    }

}
