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
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class HaicomProtocolDecoder extends BaseProtocolDecoder {

    public HaicomProtocolDecoder(String protocol) {
        super(protocol);
    }

    private static final Pattern pattern = Pattern.compile(
            "\\$GPRS" +
            "(\\d+)," +                   // IMEI
            "([^,]+)," +                  // Version
            "(\\d{2})(\\d{2})(\\d{2})," + // Date
            "(\\d{2})(\\d{2})(\\d{2})," + // Time
            "(\\d)" +                     // Flags
            "(\\d{2})(\\d{5})" +          // Latitude (DDMMMMM)
            "(\\d{3})(\\d{5})," +         // Longitude (DDDMMMMM)
            "(\\d+)," +                   // Speed
            "(\\d+)," +                   // Course
            "(\\d+)," +                   // Status
            "(\\d+)?," +                  // GPRS counting value
            "(\\d+)?," +                  // GPS power saving counting value
            "(\\d+)," +                   // Switch status
            "(\\d+)" +                    // Relay status
            "(?:[LH]{2})?" +              // Power status
            "\\#V(\\d+).*");              // Battery

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

        // Get device by IMEI
        if (!identify(parser.group(index++))) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Firmware version
        extendedInfo.set("version", parser.group(index++));
        
        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Validity
        int flags = Integer.valueOf(parser.group(index++));
        position.setValid((flags & 0x1) != 0);

        // Latitude
        Double latitude = Double.valueOf(parser.group(index++));
        latitude += Double.valueOf(parser.group(index++)) / 60000;
        if ((flags & 0x2) == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        Double longitude = Double.valueOf(parser.group(index++));
        longitude += Double.valueOf(parser.group(index++)) / 60000;
        if ((flags & 0x4) == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)) / 10);

        // Course
        position.setCourse(Double.valueOf(parser.group(index++)) / 10);

        // Altitude
        position.setAltitude(0.0);
        
        // Additional data
        extendedInfo.set("status", parser.group(index++));
        extendedInfo.set("gprs", parser.group(index++));
        extendedInfo.set("gps", parser.group(index++));
        extendedInfo.set("input", parser.group(index++));
        extendedInfo.set("output", parser.group(index++));
        extendedInfo.set("battery", Double.valueOf(parser.group(index++)) / 10);

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

}
