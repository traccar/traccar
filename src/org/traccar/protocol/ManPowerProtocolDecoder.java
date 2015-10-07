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

import java.net.SocketAddress;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class ManPowerProtocolDecoder extends BaseProtocolDecoder {

    public ManPowerProtocolDecoder(ManPowerProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "simei:" +
            "(\\d+)," +                         // IMEI
            "[^,]*,[^,]*," +
            "([^,]*)," +                        // Status
            "\\d+,\\d+,\\d+\\.?\\d*," +
            "(\\d{2})(\\d{2})(\\d{2})" +        // Date
            "(\\d{2})(\\d{2})(\\d{2})," +       // Time
            "([AV])," +                         // Validity
            "(\\d{2})(\\d{2}\\.\\d{4})," +      // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}\\.\\d{4})," +      // Longitude (DDDMM.MMMM)
            "([EW])?," +
            "(\\d+\\.?\\d*)," +                 // Speed
            ".*");

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Parse message
        Matcher parser = PATTERN.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocolName());

        Integer index = 1;

        // Get device by IMEI
        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Alarm message
        position.set(Event.KEY_STATUS, parser.group(index++));

        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
        position.setTime(time.getTime());

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0);

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
        position.setSpeed(Double.parseDouble(parser.group(index++)));

        return position;
    }

}
