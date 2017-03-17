/*
 * Copyright 2014 - 2015 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class BoxProtocolDecoder extends BaseProtocolDecoder {

    public BoxProtocolDecoder(BoxProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("L,")
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .text("G,")
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+),")                // longitude
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*),")                 // course
            .number("(d+.?d*),")                 // distance
            .number("(d+),")                     // event
            .number("(d+)")                      // status
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("H,")) {

            int index = sentence.indexOf(',', 2) + 1;
            String id = sentence.substring(index, sentence.indexOf(',', index));
            getDeviceSession(channel, remoteAddress, id);

        } else if (sentence.startsWith("E,")) {

            if (channel != null) {
                channel.write("A," + sentence.substring(2) + "\r");
            }

        } else if (sentence.startsWith("L,")) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Parser parser = new Parser(PATTERN, sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position();
            position.setDeviceId(deviceSession.getDeviceId());
            position.setProtocol(getProtocolName());

            position.setTime(parser.nextDateTime());

            position.setLatitude(parser.nextDouble());
            position.setLongitude(parser.nextDouble());
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
            position.setCourse(parser.nextDouble());

            position.set(Position.KEY_ODOMETER_TRIP, parser.nextDouble() * 1000);
            position.set(Position.KEY_EVENT, parser.next());

            int status = parser.nextInt();
            position.setValid((status & 0x04) == 0);
            position.set(Position.KEY_STATUS, status);

            return position;
        }

        return null;
    }

}
