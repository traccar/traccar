/*
 * Copyright 2012 Alex Wilson <alex@uq.edu.au>
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

/**
 * Maxon Datamax GPS send protocol (NMEA + GPFID)
 * As seen in the MA100-1010 router
 *
 * It sends its identity after the GPRMC sentence, and with the type
 * GPFID.
 */
public class MaxonProtocolDecoder extends BaseProtocolDecoder {

    /**
     * Device ID
     */
    private Position position = null;

    /**
     * Initialize
     */
    public MaxonProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "\\$GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d{2})," + // Time (HHMMSS.SSS)
            "([AV])," +                    // Validity
            "(\\d{2})(\\d{2}\\.\\d{5})," + // Latitude (DDMM.MMMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}\\.\\d{5})," + // Longitude (DDDMM.MMMMM)
            "([EW])," +
            "(\\d+\\.\\d{3})?," +          // Speed
            "(\\d+\\.\\d{2})?," +          // Course
            "(\\d{2})(\\d{2})(\\d{2})" +   // Date (DDMMYY)
            ".+");                         // Other (Checksumm)

    static private Pattern gpfidPattern = Pattern.compile(
            "\\$GPFID,(\\d+)$");

    /**
     * Decode message"
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Detect device ID
        // Parse message
        if (sentence.contains("$GPRMC")) {

            // Parse message
            Matcher parser = pattern.matcher(sentence);
            if (!parser.matches()) {
                return null;
            }

            // Create new position
            position = new Position();

            Integer index = 1;

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
            index += 1; // Skip milliseconds

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
            String speed = parser.group(index++);
            if (speed != null) {
                position.setSpeed(Double.valueOf(speed));
            } else {
                position.setSpeed(0.0);
            }

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

        } else if (sentence.contains("$GPFID") && position != null) {
            Matcher parser = gpfidPattern.matcher(sentence);

            if (parser.matches()) {
                String imei = parser.group(1);
                try {
                    position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
                } catch(Exception error) {
                    Log.warning("Unknown device - " + imei);
                    return null;
                }
                return position;
            }
        }

        return null;
    }

}
