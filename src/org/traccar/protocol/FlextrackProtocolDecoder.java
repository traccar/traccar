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

import java.net.SocketAddress;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class FlextrackProtocolDecoder extends BaseProtocolDecoder {

    public FlextrackProtocolDecoder(FlextrackProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_LOGON = Pattern.compile(
            "(-?\\d+)," +                  // Index
            "LOGON," +
            "(\\d+)," +                    // Node ID
            "(\\d+)");                     // ICCID

    private static final Pattern PATTERN = Pattern.compile(
            "(-?\\d+)," +                  // Index
            "UNITSTAT," +
            "(\\d{4})(\\d{2})(\\d{2})," +  // Date (YYYYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +  // Time (HHMMSS)
            "\\d+," +                      // Node ID
            "([NS])(\\d+)\\.(\\d+\\.\\d+)," + // Longitude
            "([EW])(\\d+)\\.(\\d+\\.\\d+)," + // Latitude
            "(\\d+)," +                    // Speed
            "(\\d+)," +                    // Course
            "(\\d+)," +                    // Satellites
            "(\\d+)," +                    // Battery
            "(-?\\d+)," +                  // GSM
            "(\\p{XDigit}+)," +            // State
            "(\\d{3})" +                   // MCC
            "(\\d{2})," +                  // MNC
            "(-?\\d+)," +                  // Altitude
            "(\\d+)," +                    // HDOP
            "(\\p{XDigit}+)," +            // Cell
            "\\d+," +                      // GPS fix time
            "(\\p{XDigit}+)," +            // LAC
            "(\\d+)");                     // Odometer

    private void sendAcknowledgement(Channel channel, String index) {
        if (channel != null) {
            channel.write(index + ",ACK\r");
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        if (sentence.contains("LOGON")) {

            Matcher parser = PATTERN_LOGON.matcher(sentence);
            if (!parser.matches()) {
                return null;
            }

            int index = 1;

            sendAcknowledgement(channel, parser.group(index++));

            String id = parser.group(index++);
            String iccid = parser.group(index++);

            if (!identify(iccid, channel, null, false) && !identify(id, channel)) {
                return null;
            }

        } else if (sentence.contains("UNITSTAT") && hasDeviceId()) {

            Matcher parser = PATTERN.matcher(sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());

            int index = 1;

            sendAcknowledgement(channel, parser.group(index++));

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.YEAR, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
            time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
            position.setTime(time.getTime());

            // Latitude
            String hemisphere = parser.group(index++);
            double lat = Integer.parseInt(parser.group(index++));
            lat += Double.parseDouble(parser.group(index++)) / 60;
            if (hemisphere.equals("S")) {
                lat = -lat;
            }
            position.setLatitude(lat);

            // Longitude
            hemisphere = parser.group(index++);
            double lon = Integer.parseInt(parser.group(index++));
            lon += Double.parseDouble(parser.group(index++)) / 60;
            if (hemisphere.equals("W")) {
                lon = -lon;
            }
            position.setLongitude(lon);

            position.setValid(true);
            position.setSpeed(UnitsConverter.knotsFromKph(Integer.parseInt(parser.group(index++))));
            position.setCourse(Integer.parseInt(parser.group(index++)));

            position.set(Event.KEY_SATELLITES, Integer.parseInt(parser.group(index++)));
            position.set(Event.KEY_BATTERY, Integer.parseInt(parser.group(index++)));
            position.set(Event.KEY_GSM, Integer.parseInt(parser.group(index++)));
            position.set(Event.KEY_STATUS, Integer.parseInt(parser.group(index++), 16));
            position.set(Event.KEY_MCC, Integer.parseInt(parser.group(index++)));
            position.set(Event.KEY_MNC, Integer.parseInt(parser.group(index++)));

            position.setAltitude(Integer.parseInt(parser.group(index++)));

            position.set(Event.KEY_HDOP, Integer.parseInt(parser.group(index++)) / 10.0);
            position.set(Event.KEY_CELL, parser.group(index++));
            position.set(Event.KEY_LAC, parser.group(index++));
            position.set(Event.KEY_ODOMETER, Integer.parseInt(parser.group(index++)));

            return position;
        }

        return null;
    }

}
