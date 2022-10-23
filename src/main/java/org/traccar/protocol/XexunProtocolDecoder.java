/*
 * Copyright 2012 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class XexunProtocolDecoder extends BaseProtocolDecoder {

    private final boolean full;

    public XexunProtocolDecoder(Protocol protocol, boolean full) {
        super(protocol);
        this.full = full;
    }

    private static final Pattern PATTERN_BASIC = new PatternBuilder()
            .expression("G[PN]RMC,")
            .number("(?:(dd)(dd)(dd))?.?d*,")    // time (hhmmss)
            .expression("([AV]),")               // validity
            .number("(d*?)(d?d.d+),([NS]),")     // latitude
            .number("(d*?)(d?d.d+),([EW])?,")    // longitude
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*)?,")                // course
            .number("(?:(dd)(dd)(dd))?,")        // date (ddmmyy)
            .expression("[^*]*").text("*")
            .number("xx")                        // checksum
            .expression("\\r\\n").optional()
            .expression(",([FL]),")              // signal
            .expression("([^,]*),").optional()   // alarm
            .any()
            .number("imei:(d+),")                // imei
            .compile();

    private static final Pattern PATTERN_FULL = new PatternBuilder()
            .any()
            .number("(d+),")                     // serial
            .expression("([^,]+)?,")             // phone number
            .expression(PATTERN_BASIC.pattern())
            .number("(d+),")                     // satellites
            .number("(-?d+.d+)?,")               // altitude
            .number("[FL]:(d+.d+)V")             // power
            .any()
            .compile();

    private String decodeStatus(Position position, String value) {
        if (value != null) {
            switch (value.toLowerCase()) {
                case "acc on":
                case "accstart":
                    position.set(Position.KEY_IGNITION, true);
                    break;
                case "acc off":
                case "accstop":
                    position.set(Position.KEY_IGNITION, false);
                    break;
                case "help me!":
                case "help me":
                    position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                    break;
                case "low battery":
                    position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                    break;
                case "move!":
                case "moved!":
                    position.set(Position.KEY_ALARM, Position.ALARM_MOVEMENT);
                    break;
                default:
                    break;
            }
        }
        return null;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Pattern pattern = PATTERN_BASIC;
        if (full) {
            pattern = PATTERN_FULL;
        }

        Parser parser = new Parser(pattern, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());

        if (full) {
            position.set("serial", parser.next());
            position.set("number", parser.next());
        }

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        position.setSpeed(convertSpeed(parser.nextDouble(0), "kn"));

        position.setCourse(parser.nextDouble(0));

        dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        position.set("signal", parser.next());

        decodeStatus(position, parser.next());

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        if (full) {
            position.set(Position.KEY_SATELLITES, parser.nextInt());

            position.setAltitude(parser.nextDouble(0));

            position.set(Position.KEY_POWER, parser.nextDouble(0));
        }

        return position;
    }

}
