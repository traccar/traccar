/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class HomtecsProtocolDecoder extends BaseProtocolDecoder {

    public HomtecsProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .expression("([^_]+)")               // id
            .text("_R")
            .number("(x{8}),")                   // mac ending
            .number("(dd)(dd)(dd),")             // date (yymmdd)
            .number("(dd)(dd)(dd).d+,")          // time (hhmmss)
            .number("(d+),")                     // satellites
            .number("(dd)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(ddd)(dd.d+),")             // longitude
            .expression("([EW]),")
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .number("(d),")                      // fix status
            .number("(d+.?d*)?,")                // hdop
            .number("(d+.?d*)?")                 // altitude
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        String id = parser.next();
        String mac = parser.next();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id, id + "_R" + mac);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.YMD_HMS));

        position.set(Position.KEY_SATELLITES, parser.nextInt(0));

        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        position.setValid(parser.nextInt(0) > 0);

        position.set(Position.KEY_HDOP, parser.nextDouble(0));

        position.setAltitude(parser.nextDouble(0));

        return position;
    }

}
