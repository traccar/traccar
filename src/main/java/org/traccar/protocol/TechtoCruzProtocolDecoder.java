/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
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

public class TechtoCruzProtocolDecoder extends BaseProtocolDecoder {

    public TechtoCruzProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$$A")
            .number("d+,")                       // length
            .number("(d+),")                     // imei
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([AV]),")               // valid
            .expression("[^,]+,")                // manufacturer
            .expression("([^,]+),")              // license plate
            .number("(d+.d+),")                  // speed
            .number("(d+),")                     // odometer
            .number("(-?d+.d+),[NS],")           // latitude
            .number("(-?d+.d+),[WE],")           // longitude
            .number("(-?d+.d+),")                // altitude
            .number("(d+.d+),")                  // course
            .number("(d+),")                     // satellites
            .number("(d+),")                     // rssi
            .number("(d+.d+),")                  // power
            .number("(d+.d+),")                  // battery
            .number("([01]),")                   // charge
            .number("[01],")                     // speed sensor
            .number("[01],")                     // gps status
            .number("([01]),")                   // ignition
            .number("([01]),")                   // overspeed
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(parser.nextDateTime());
        position.setValid(parser.next().equals("A"));

        position.set("registration", parser.next());

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

        position.set(Position.KEY_ODOMETER, parser.nextInt());

        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setAltitude(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_RSSI, parser.nextInt());
        position.set(Position.KEY_POWER, parser.nextDouble());
        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.KEY_CHARGE, parser.nextInt() > 0);
        position.set(Position.KEY_IGNITION, parser.nextInt() > 0);

        if (parser.nextInt() > 0) {
            position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
        }

        return position;
    }

}
