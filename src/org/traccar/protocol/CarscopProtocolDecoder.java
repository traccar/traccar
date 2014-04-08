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
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class CarscopProtocolDecoder extends BaseProtocolDecoder {

    private Long deviceId;

    public CarscopProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    // Very similar to TK103 protocol
    static private Pattern pattern = Pattern.compile(
            "\\*.*" +
            "(\\d{2})(\\d{2})(\\d{2})" + // Time (HHMMSS)
            "([AV])" +                   // Validity
            "(\\d{2})(\\d{2}\\.\\d{4})" + // Latitude (DDMM.MMMM)
            "([NS])" +
            "(\\d{3})(\\d{2}\\.\\d{4})" + // Longitude (DDDMM.MMMM)
            "([EW])" +
            "(\\d{3}\\.\\d)" +           // Speed
            "(\\d{2})(\\d{2})(\\d{2})" + // Date (YYMMDD)
            "(\\d{3}\\.\\d{2})" +        // Course
            "(\\d{8})" +                 // State
            "L(\\d{6})");                // Milage

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Device identification
        int index = sentence.indexOf("UB05");
        if (index != -1) {
            String imei = sentence.substring(index + 4, index + 4 + 15);
            try {
                deviceId = getDataManager().getDeviceByImei(imei).getId();
            } catch(Exception error) {
                Log.warning("Unknown device - " + imei);
            }
        }
        if (deviceId == null) {
            return null;
        }

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setDeviceId(deviceId);
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("carscop");
        index = 1;

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0 ? true : false);

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

        // Altitude
        position.setAltitude(0.0);

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));

        // Date
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Course
        position.setCourse(Double.valueOf(parser.group(index++)));
        
        // State
        extendedInfo.set("state", parser.group(index++));

        // Milage
        extendedInfo.set("milage", Integer.valueOf(parser.group(index++)));

        position.setExtendedInfo(extendedInfo.toString());
        return position;
    }

}
