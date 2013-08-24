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

public class Tk102ProtocolDecoder extends BaseProtocolDecoder {

    private Long deviceId;

    public Tk102ProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    static private Pattern pattern = Pattern.compile(
            "\\[.\\d{10}.\\(\\p{Upper}+" +
            "(\\d{2})(\\d{2})(\\d{2})" +   // Time (HHMMSS)
            "([AV])" +                     // Validity
            "(\\d{2})(\\d{2}\\.\\d{4})" +  // Latitude (DDMM.MMMM)
            "([NS])" +
            "(\\d{3})(\\d{2}\\.\\d{4})" +  // Longitude (DDDMM.MMMM)
            "([EW])" +
            "(\\d{3}\\.\\d{3})" +          // Speed
            "(\\d{2})(\\d{2})(\\d{2})" +   // Date (DDMMYY)
            "\\d+\\)");

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Login
        if (sentence.startsWith("[!")) {
            String imei = sentence.substring(14, 14 + 15);
            try {
                deviceId = getDataManager().getDeviceByImei(imei).getId();
            } catch(Exception error) {
                Log.warning("Unknown device - " + imei);
                return null;
            }

            if (channel != null) {
                channel.write("[”0000000001" + sentence.substring(13) + "]");
            }
        }

        // Quit
        else if (sentence.startsWith("[#")) {
            // TODO: Send response
        }

        // Parse message
        else if (deviceId != null) {

            // Parse message
            Matcher parser = pattern.matcher(sentence);
            if (!parser.matches()) {
                return null;
            }

            // Create new position
            Position position = new Position();
            ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("tk102");
            position.setDeviceId(deviceId);

            Integer index = 1;

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
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

            // Speed
            position.setSpeed(Double.valueOf(parser.group(index++)));

            // Course
            position.setCourse(0.0);

            // Date
            time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
            time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
            position.setTime(time.getTime());

            // Altitude
            position.setAltitude(0.0);

            position.setExtendedInfo(extendedInfo.toString());
            return position;
        }

        return null;
    }

}
