/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class ArnaviProtocolDecoder extends BaseProtocolDecoder {

    public ArnaviProtocolDecoder(ArnaviProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$AV,")
            .number("Vd,")                       // type
            .number("(d+),")                     // device id
            .number("(d+),")                     // index
            .number("(d+),")                     // power
            .number("(d+),")                     // battery
            .number("-?d+,")
            .expression("[01],")                 // movement
            .expression("([01]),")               // ignition
            .number("(d+),")                     // input
            .number("d+,d+,")                    // input 1
            .number("d+,d+,").optional()         // input 2
            .expression("[01],")                 // fix type
            .number("(d+),")                     // satellites
            .number("(dd)(dd)(dd),")             // time
            .number("(dd)(dd.d+)([NS]),")        // latitude
            .number("(ddd)(dd.d+)([EW]),")       // longitude
            .number("(d+.d+),")                  // speed
            .number("(d+.d+),")                  // course
            .number("(dd)(dd)(dd)")              // date
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_INDEX, parser.nextInt());
        position.set(Position.KEY_POWER, parser.nextInt() * 0.01);
        position.set(Position.KEY_BATTERY, parser.nextInt() * 0.01);
        position.set(Position.KEY_IGNITION, parser.nextInt() == 1);
        position.set(Position.KEY_INPUT, parser.nextInt());
        position.set(Position.KEY_SATELLITES, parser.nextInt());

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

        position.setValid(true);
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        return position;
    }

}
