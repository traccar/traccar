/*
 * Copyright 2013 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class WialonProtocolDecoder extends BaseProtocolDecoder {

    public WialonProtocolDecoder(WialonProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "(\\d{2})(\\d{2})(\\d{2});" +  // Date (DDMMYY)
            "(\\d{2})(\\d{2})(\\d{2});" +  // Time (HHMMSS)
            "(\\d{2})(\\d{2}\\.\\d+);" +   // Latitude (DDMM.MMMM)
            "([NS]);" +
            "(\\d{3})(\\d{2}\\.\\d+);" +   // Longitude (DDDMM.MMMM)
            "([EW]);" +
            "(\\d+\\.?\\d*)?;" +           // Speed
            "(\\d+\\.?\\d*)?;" +           // Course
            "(?:NA|(\\d+\\.?\\d*));" +     // Altitude
            "(?:NA|(\\d+))" +              // Satellites
            "(?:;" +
            "(?:NA|(\\d+\\.?\\d*));" +     // hdop
            "(?:NA|(\\d+));" +             // inputs
            "(?:NA|(\\d+));" +             // outputs
            "(?:NA|([^;]*));" +            // adc
            "(?:NA|([^;]*));" +            // ibutton
            "(?:NA|(.*))" +                // params
            ")?");

    private void sendResponse(Channel channel, String prefix, Integer number) {
        if (channel != null) {
            StringBuilder response = new StringBuilder(prefix);
            if (number != null) {
                response.append(number);
            }
            response.append("\r\n");
            channel.write(response.toString());
        }
    }

    private Position decodePosition(String substring) {

        // Parse message
        Matcher parser = PATTERN.matcher(substring);
        if (!hasDeviceId() || !parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(getDeviceId());

        Integer index = 1;

        // Date and Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
        position.setTime(time.getTime());

        // Latitude
        Double latitude = Double.parseDouble(parser.group(index++));
        latitude += Double.parseDouble(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        Double longitude = Double.parseDouble(parser.group(index++));
        longitude += Double.parseDouble(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Speed
        String speed = parser.group(index++);
        if (speed != null) {
            position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(speed)));
        }

        // Course
        String course = parser.group(index++);
        if (course != null) {
            position.setCourse(Double.parseDouble(course));
        }

        // Altitude
        String altitude = parser.group(index++);
        if (altitude != null) {
            position.setAltitude(Double.parseDouble(altitude));
        }

        // Satellites
        String satellites = parser.group(index++);
        if (satellites != null) {
            position.setValid(Integer.parseInt(satellites) >= 3);
            position.set(Event.KEY_SATELLITES, satellites);
        } else {
            position.setValid(false);
        }

        // Other
        position.set(Event.KEY_HDOP, parser.group(index++));
        position.set(Event.KEY_INPUT, parser.group(index++));
        position.set(Event.KEY_OUTPUT, parser.group(index++));

        // ADC
        String adc = parser.group(index++);
        if (adc != null) {
            String[] values = adc.split(",");
            for (int i = 0; i < values.length; i++) {
                position.set(Event.PREFIX_ADC + (i + 1), values[i]);
            }
        }

        // iButton
        position.set(Event.KEY_RFID, parser.group(index++));

        // Params
        String params = parser.group(index);
        if (params != null) {
            String[] values = params.split(",");
            for (String param : values) {
                Matcher paramParser = Pattern.compile("(.*):[1-3]:(.*)").matcher(param);
                if (paramParser.matches()) {
                    position.set(paramParser.group(1).toLowerCase(), paramParser.group(2));
                }
            }
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("#L#")) {

            // Detect device ID
            String imei = sentence.substring(3, sentence.indexOf(';'));
            if (identify(imei, channel)) {
                sendResponse(channel, "#AL#", 1);
            }

        } else if (sentence.startsWith("#P#")) {

            // Heartbeat
            sendResponse(channel, "#AP#", null);

        } else if (sentence.startsWith("#SD#") || sentence.startsWith("#D#")) {

            Position position = decodePosition(
                    sentence.substring(sentence.indexOf('#', 1) + 1));

            if (position != null) {
                sendResponse(channel, "#AD#", 1);
                return position;
            }

        } else if (sentence.startsWith("#B#")) {

            String[] messages = sentence.substring(sentence.indexOf('#', 1) + 1).split("\\|");
            List<Position> positions = new LinkedList<>();

            for (String message : messages) {
                Position position = decodePosition(message);
                if (position != null) {
                    positions.add(position);
                }
            }

            sendResponse(channel, "#AB#", messages.length);
            if (!positions.isEmpty()) {
                return positions;
            }
        }

        return null;
    }

}
