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

import java.net.SocketAddress;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Position;

public class FreedomProtocolDecoder extends BaseProtocolDecoder {

    public FreedomProtocolDecoder(FreedomProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "IMEI," +
            "(\\d+)," +                           // IMEI
            "(\\d{4}).(\\d{2}).(\\d{2}), " +      // Date
            "(\\d{2}):(\\d{2}):(\\d{2}), " +      // Time
            "([NS]), Lat:(\\d{2})(\\d+.\\d+), " + // Latitude
            "([EW]), Lon:(\\d{3})(\\d+.\\d+), " + // Longitude
            "Spd:(\\d+.\\d+)" +                   // Speed
            ".*");

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        // Parse message
        Matcher parser = PATTERN.matcher((String) msg);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocolName());
        Integer index = 1;

        // Identification
        if (!identify(parser.group(index++), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Validity
        position.setValid(true);

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
        Double latitude = Double.parseDouble(parser.group(index++));
        latitude += Double.parseDouble(parser.group(index++)) / 60;
        if (hemisphere.compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        hemisphere = parser.group(index++);
        Double longitude = Double.parseDouble(parser.group(index++));
        longitude += Double.parseDouble(parser.group(index++)) / 60;
        if (hemisphere.compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Speed
        position.setSpeed(Double.parseDouble(parser.group(index++)));
        return position;
    }

}
