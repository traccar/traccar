/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class KenjiProtocolDecoder extends BaseProtocolDecoder {

    public KenjiProtocolDecoder(KenjiProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text(">")
            .number("C(d{6}),")                  // device id
            .number("M(x{6}),")                  // alarm
            .number("O(x{4}),")                  // output
            .number("I(x{4}),")                  // input
            .number("D(dd)(dd)(dd),")            // time
            .expression("([AV]),")               // valid
            .number("([NS])(dd)(dd.d+),")        // latitude
            .number("([EW])(ddd)(dd.d+),")       // longitude
            .number("T(d+.d+),")                 // speed
            .number("H(d+.d+),")                 // course
            .number("Y(dd)(dd)(dd),")            // date
            .number("G(d+)")                     // satellites
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

        if (!identify(parser.next(), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        position.set(Event.KEY_ALARM, parser.nextInt(16));
        position.set(Event.KEY_OUTPUT, parser.nextInt(16));
        position.set(Event.KEY_INPUT, parser.nextInt(16));

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

        position.setValid(parser.next().equals("A"));

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.set(Event.KEY_SATELLITES, parser.nextInt());

        return position;
    }

}
