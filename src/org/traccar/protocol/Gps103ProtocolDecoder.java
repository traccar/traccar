/*
 * Copyright 2012 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.SocketAddress;
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

public class Gps103ProtocolDecoder extends BaseProtocolDecoder {

    public Gps103ProtocolDecoder(DataManager dataManager, String protocol, Properties properties) {
        super(dataManager, protocol, properties);
    }

    private static final Pattern pattern = Pattern.compile(
            "imei:" +
            "(\\d+)," +                         // IMEI
            "([^,]+)," +                        // Alarm
            "(\\d{2})/?(\\d{2})/?(\\d{2})\\s?" + // Local Date
            "(\\d{2}):?(\\d{2})(?:\\d{2})?," +  // Local Time
            "[^,]*," +
            "[FL]," +                           // F - full / L - low
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d+)," + // Time UTC (HHMMSS.SSS)
            "([AV])," +                         // Validity
            "(\\d+)(\\d{2}\\.\\d+)," +          // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d+)(\\d{2}\\.\\d+)," +          // Longitude (DDDMM.MMMM)
            "([EW])?," +
            "(\\d+\\.?\\d*)," +                 // Speed
            "(\\d+\\.?\\d*)?,?" +               // Course
            "(\\d+\\.?\\d*)?,?" +               // Altitude
            "([^,]+)?,?" +
            "([^,]+)?,?" +
            "([^,]+)?,?" +
            "([^,]+)?,?" +
            ".*");

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Send response #1
        if (sentence.contains("##")) {
            if (channel != null) {
                channel.write("LOAD", remoteAddress);
            }
            return null;
        }

        // Send response #2
        if (sentence.length() == 15 && Character.isDigit(sentence.charAt(0))) {
            if (channel != null) {
                channel.write("ON", remoteAddress);
            }
            return null;
        }

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
        String imei = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + imei);
            return null;
        }

        // Alarm message
        extendedInfo.set("alarm", parser.group(index++));
        
        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        
        int localHours = Integer.valueOf(parser.group(index++));
        int localMinutes = Integer.valueOf(parser.group(index++));
        
        int utcHours = Integer.valueOf(parser.group(index++));
        int utcMinutes = Integer.valueOf(parser.group(index++));

        // Time
        time.set(Calendar.HOUR_OF_DAY, localHours);
        time.set(Calendar.MINUTE, localMinutes);
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MILLISECOND, Integer.valueOf(parser.group(index++)));
        
        // Timezone calculation
        int deltaMinutes = (localHours - utcHours) * 60 + localMinutes - utcMinutes;
        if (deltaMinutes <= -12 * 60) {
            deltaMinutes += 24 * 60;
        } else if (deltaMinutes > 12 * 60) {
            deltaMinutes -= 24 * 60;
        }
        time.add(Calendar.MINUTE, -deltaMinutes);
        position.setTime(time.getTime());

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
        String hemisphere = parser.group(index++);
        if (hemisphere != null) {
            if (hemisphere.compareTo("W") == 0) longitude = -longitude;
        }
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

        // Altitude
        String altitude = parser.group(index++);
        if (altitude != null) {
            position.setAltitude(Double.valueOf(altitude));
        } else {
            position.setAltitude(0.0);
        }

        // Additional data
        extendedInfo.set("io1", parser.group(index++));
        extendedInfo.set("io2", parser.group(index++));
        extendedInfo.set("io3", parser.group(index++));
        extendedInfo.set("io4", parser.group(index++));

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }

}
