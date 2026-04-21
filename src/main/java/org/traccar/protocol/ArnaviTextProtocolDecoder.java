/*
 * Copyright 2015 - 2020 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class ArnaviTextProtocolDecoder extends BaseProtocolDecoder {

    public ArnaviTextProtocolDecoder(Protocol protocol) {
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
            .groupBegin()
            .number("(d+.d+)?,")                 // altitude
            .number("(?:d+.d+)?,")               // geoid height
            .groupEnd("?")
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(dd)(dd.d+)([NS]),")        // latitude
            .number("(ddd)(dd.d+)([EW]),")       // longitude
            .number("(d+.d+),")                  // speed
            .number("(d+.d+),")                  // course
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        Parser parser = new Parser(PATTERN, buf.toString(StandardCharsets.US_ASCII));
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());

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

        position.setAltitude(parser.nextDouble(0));

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
