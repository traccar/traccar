/*
 * Copyright 2014 - 2019 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class BoxProtocolDecoder extends BaseProtocolDecoder {

    public BoxProtocolDecoder(Protocol protocol) {
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
            .groupBegin()
            .text(";")
            .expression("(.+)")
            .groupEnd("?")
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
                channel.writeAndFlush(new NetworkMessage("A," + sentence.substring(2) + "\r", remoteAddress));
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

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setTime(parser.nextDateTime());

            position.setLatitude(parser.nextDouble());
            position.setLongitude(parser.nextDouble());
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
            position.setCourse(parser.nextDouble());

            position.set(Position.KEY_ODOMETER_TRIP, parser.nextDouble() * 1000);
            position.set(Position.KEY_EVENT, parser.next());

            int status = parser.nextInt();
            position.set(Position.KEY_IGNITION, BitUtil.check(status, 0));
            position.set(Position.KEY_MOTION, BitUtil.check(status, 1));
            position.setValid(!BitUtil.check(status, 2));
            position.set(Position.KEY_STATUS, status);

            if (parser.hasNext()) {
                String[] data = parser.next().split(";");
                for (String item : data) {
                    int valueIndex = item.indexOf(',');
                    position.set(item.substring(0, valueIndex).toLowerCase(), item.substring(valueIndex + 1));
                }
            }

            return position;
        }

        return null;
    }

}
