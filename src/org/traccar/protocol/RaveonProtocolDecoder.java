/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;
import java.util.regex.Pattern;

public class RaveonProtocolDecoder extends BaseProtocolDecoder {

    public RaveonProtocolDecoder(RaveonProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$PRAVE,")
            .number("(d+),")                     // id
            .number("d+,")
            .number("(-?)(d+)(dd.d+),")          // latitude
            .number("(-?)(d+)(dd.d+),")          // longitude
            .number("(dd)(dd)(dd),")             // time
            .number("(d),")                      // validity
            .number("(d+),")                     // satellites
            .number("(-?d+),")                   // altitude
            .number("(-?d+),")                   // temperature
            .number("(d+.d+),")                  // power
            .number("(d+),")                     // inputs
            .number("(-?d+),")                   // gsm
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .expression("([PMACIVSX])?,")        // status
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        Parser parser = new Parser(PATTERN, sentence);
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

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));

        DateBuilder dateBuilder = new DateBuilder(new Date())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setValid(parser.nextInt() != 0);

        position.set(Position.KEY_SATELLITES, parser.nextInt());

        position.setAltitude(parser.nextInt());

        position.set(Position.PREFIX_TEMP + 1, parser.nextInt());
        position.set(Position.KEY_POWER, parser.nextDouble());
        position.set(Position.KEY_INPUT, parser.nextInt());
        position.set(Position.KEY_RSSI, parser.nextInt());

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());

        position.set(Position.KEY_ALARM, parser.next());

        return position;
    }

}
