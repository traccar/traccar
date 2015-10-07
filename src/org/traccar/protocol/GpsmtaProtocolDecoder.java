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
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class GpsmtaProtocolDecoder extends BaseProtocolDecoder {

    public GpsmtaProtocolDecoder(GpsmtaProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "(\\d+) " +                         // UID
            "(\\d+) " +                         // Time
            "(\\d+\\.\\d+) " +                  // Latitude
            "(\\d+\\.\\d+) " +                  // Longitude
            "(\\d+) " +                         // Speed
            "(\\d+) " +                         // Course
            "(\\d+) " +                         // Accuracy
            "(\\d+) " +                         // Altitude
            "(\\d+) " +                         // Flags
            "(\\d+) " +                         // Battery
            "(\\d+) " +                         // Temperature
            "(\\d).*");                         // Changing status

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        Matcher parser = PATTERN.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        Integer index = 1;

        if (!identify(parser.group(index++), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        String time = parser.group(index++);
        position.setTime(new Date(Long.parseLong(time) * 1000));

        position.setLatitude(Double.parseDouble(parser.group(index++)));
        position.setLongitude(Double.parseDouble(parser.group(index++)));
        position.setSpeed(Integer.parseInt(parser.group(index++)));
        position.setCourse(Integer.parseInt(parser.group(index++)));
        index++; // accuracy
        position.setAltitude(Integer.parseInt(parser.group(index++)));

        position.set(Event.KEY_STATUS, Integer.parseInt(parser.group(index++)));
        position.set(Event.KEY_BATTERY, Integer.parseInt(parser.group(index++)));
        position.set(Event.PREFIX_TEMP + 1, Integer.parseInt(parser.group(index++)));
        position.set(Event.KEY_CHARGE, Integer.parseInt(parser.group(index++)) == 1);

        if (channel != null) {
            channel.write(time, remoteAddress);
        }

        return position;
    }

}
