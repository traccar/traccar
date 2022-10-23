/*
 * Copyright 2020 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class PortmanProtocolDecoder extends BaseProtocolDecoder {

    public PortmanProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_STANDARD = new PatternBuilder()
            .text("$PTMLA,")                     // header
            .expression("([^,]+),")              // id
            .expression("([ABCL]),")             // validity
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([NS])")
            .number("(dd)(dd.d+)")               // latitude
            .expression("([EW])")
            .number("(d{2,3})(dd.d+),")          // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(?:NA|C(-?d+)),")           // temperature
            .number("(x{8}),")                   // status
            .number("(?:NA|(d+)),")              // card id
            .number("(d+),")                     // event
            .number("(d+),")                     // satellites
            .number("(d+.d+),")                  // odometer
            .number("(d+),")                     // rssi
            .number("(?:G(d+)|[^,]*)")           // fuel
            .compile();

    private Object decodeStandard(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_STANDARD, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setValid(!parser.next().equals("L"));
        position.setTime(parser.nextDateTime());
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());

        position.set(Position.PREFIX_TEMP + 1, parser.next());
        position.set(Position.KEY_STATUS, parser.nextHexLong());
        position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());

        int event = parser.nextInt();
        position.set(Position.KEY_EVENT, event);
        if (event == 253) {
            position.set(Position.KEY_IGNITION, true);
        } else if (event == 254) {
            position.set(Position.KEY_IGNITION, false);
        }

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);
        position.set(Position.KEY_RSSI, parser.nextInt());
        position.set(Position.KEY_FUEL_LEVEL, parser.nextInt());

        return position;
    }

    private static final Pattern PATTERN_EXTENDED = new PatternBuilder()
            .text("$EXT,")                        // header
            .expression("([^,]+),")              // id
            .expression("([ABCL]),")             // validity
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([NS])")
            .number("(dd)(dd.d+)")               // latitude
            .expression("([EW])")
            .number("(d{2,3})(dd.d+),")          // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(?:NA|C(-?d+)),")           // temperature
            .number("(?:NA|F(d+)),")             // fuel
            .number("(d+),")                     // satellites
            .number("(d+),")                     // rssi
            .number("(d+.d+),")                  // odometer
            .number("(?:NA|(d+)),")              // card id
            .number("(x{8}),")                   // status
            .number("(d+)")                      // event
            .any()
            .compile();

    private Object decodeExtended(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_EXTENDED, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setValid(!parser.next().equals("L"));
        position.setTime(parser.nextDateTime());
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());

        position.set(Position.PREFIX_TEMP + 1, parser.next());
        position.set(Position.KEY_FUEL_LEVEL, parser.nextInt());
        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_RSSI, parser.nextInt());
        position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);
        position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());
        position.set(Position.KEY_STATUS, parser.nextHexLong());
        position.set(Position.KEY_EVENT, parser.nextInt());

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        if (sentence.startsWith("$PTMLA")) {
            return decodeStandard(channel, remoteAddress, sentence);
        } else if (sentence.startsWith("$EXT")) {
            return decodeExtended(channel, remoteAddress, sentence);
        } else {
            return null;
        }
    }

}
