/*
 * Copyright 2013 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class MictrackLowAltitudeFlightProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MictrackLowAltitudeFlightProtocol.class);

    public MictrackLowAltitudeFlightProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_HEADER = new PatternBuilder()
            .number("(d+)")                     // imei
            .compile();

    private static final Pattern PATTERN_POSITION = new PatternBuilder()
            .number("(dd)(dd)(dd).d+,")          // time (hhmmss.sss)
            .expression("([AV]),")               // validity
            .number("(d+)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d+)(dd.d+),")              // longitude
            .expression("([EW]),")
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .number("(d+.?d*)?,")                // altitude
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        sentence = sentence.trim();

        String header = sentence.substring(0, sentence.indexOf('$'));
        Parser parser = new Parser(PATTERN_HEADER, header);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        String[] messages = sentence.substring(sentence.indexOf('$') + 1).split("\\$");
        List<Position> positions = new LinkedList<>();

        for (String message : messages) {
            parser = new Parser(PATTERN_POSITION, message);
            if (parser.matches()) {

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                DateBuilder dateBuilder = new DateBuilder()
                        .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

                position.setValid(parser.next().equals("A"));
                position.setLatitude(parser.nextCoordinate());
                position.setLongitude(parser.nextCoordinate());

                position.setSpeed(parser.nextDouble(0));
                position.setCourse(parser.nextDouble(0));
                position.setAltitude(parser.nextDouble(0));

                dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
                position.setTime(dateBuilder.getDate());

                positions.add(position);
            }
        }

        return positions;
    }

}
