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

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class GnxProtocolDecoder extends BaseProtocolDecoder {

    public GnxProtocolDecoder(GnxProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$GNX_")
            .expression("...,")                  // type
            .number("(d+),")                     // imei
            .number("d+,")                       // length
            .expression("([01]),")               // history
            .number("(dd)(dd)(dd),")             // device time
            .number("(dd)(dd)(dd),")             // device date
            .number("(dd)(dd)(dd),")             // fix time
            .number("(dd)(dd)(dd),")             // fix date
            .number("(d),")                      // valid
            .number("(dd.d+),")                  // latitude
            .expression("([NS]),")
            .number("(ddd.d+),")                 // longitude
            .expression("([EW]),")
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*),")                 // course
            .number("(d+),")                     // satellites
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

        if (parser.nextInt() == 1) {
            position.set(Event.KEY_ARCHIVE, true);
        }

        DateBuilder dateBuilder;

        dateBuilder = new DateBuilder(TimeZone.getTimeZone("GMT+5:30"))
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setDeviceTime(dateBuilder.getDate());

        dateBuilder = new DateBuilder(TimeZone.getTimeZone("GMT+5:30"))
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setFixTime(dateBuilder.getDate());

        position.setValid(parser.nextInt() != 0);

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());

        position.set(Event.KEY_SATELLITES, parser.nextInt());

        return position;
    }

}
