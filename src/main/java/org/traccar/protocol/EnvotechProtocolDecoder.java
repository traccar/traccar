/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class EnvotechProtocolDecoder extends BaseProtocolDecoder {

    public EnvotechProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$")
            .number("dd")                        // mode
            .expression("...,")                  // hardware
            .number("(x+),")                     // event
            .number("x+,")                       // group
            .number("(x+),")                     // device id
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("xx")                        // connection status
            .number("(dd)")                      // rssi
            .number("d{5},")                     // mcc
            .number("(ddd)")                     // power
            .number("(ddd),")                    // battery
            .number("(xx)")                      // inputs
            .number("(xx),")                     // outputs
            .number("(xxx)?,")                   // fuel
            .number("(x{8}),")                   // status
            .expression("[^']*'")
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .number("(d)")                       // fix
            .number("(d+)(d{5})([NS])")          // latitude
            .number("(d+)(d{5})([EW])")          // longitude
            .number("(ddd)")                     // speed
            .number("(ddd)")                     // course
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());

        int event = parser.nextHexInt();
        switch (event) {
            case 0x60:
                position.set(Position.KEY_ALARM, Position.ALARM_LOCK);
                break;
            case 0x61:
                position.set(Position.KEY_ALARM, Position.ALARM_UNLOCK);
                break;
            default:
                break;
        }
        position.set(Position.KEY_EVENT, event);

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        position.setDeviceTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.set(Position.KEY_RSSI, parser.nextInt());
        position.set(Position.KEY_POWER, parser.nextInt() * 0.01);
        position.set(Position.KEY_BATTERY, parser.nextInt() * 0.01);
        position.set(Position.KEY_INPUT, parser.nextHexInt());
        position.set(Position.PREFIX_OUT, parser.nextHexInt());
        position.set(Position.KEY_FUEL_LEVEL, parser.nextHexInt());
        position.set(Position.KEY_STATUS, parser.nextHexLong());

        position.setFixTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setValid(parser.nextInt() > 0);
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_DEG_HEM));
        position.setSpeed(parser.nextInt());
        position.setCourse(parser.nextInt());

        return position;
    }

}
