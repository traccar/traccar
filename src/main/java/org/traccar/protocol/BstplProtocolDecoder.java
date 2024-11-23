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
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class BstplProtocolDecoder extends BaseProtocolDecoder {

    public BstplProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("BSTPL$")                      // header
            .number("(d),")                      // type
            .expression("([^,]+),")              // device id
            .expression("([AV]),")               // validity
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(d+.d+),([0NS]),")          // latitude
            .number("(d+.d+),([0EW]),")          // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // odometer
            .number("(d+),")                     // course
            .number("(d+),")                     // satellites
            .number("([01]),")                   // box open
            .number("(d+),")                     // rssi
            .number("([01]),")                   // charge
            .number("([01]),")                   // ignition
            .number("([01]),")                   // engine
            .number("([01]),")                   // locked
            .number("(d+.d+),")                  // adc
            .number("d+,")                       // reserved
            .number("(d+.d+),")                  // battery
            .expression("([^,]+),")              // firmware
            .number("([^,]+),")                  // iccid
            .number("(d+.d+)")                   // power
            .compile();

    private String decodeAlarm(int value) {
        return switch (value) {
            case 4 -> Position.ALARM_LOW_BATTERY;
            case 5 -> Position.ALARM_ACCELERATION;
            case 6 -> Position.ALARM_BRAKING;
            case 7 -> Position.ALARM_OVERSPEED;
            case 9 -> Position.ALARM_SOS;
            default -> null;
        };
    }
    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        int type = parser.nextInt();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.addAlarm(decodeAlarm(type));

        position.setValid(parser.next().equals("A"));
        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));

        position.set(Position.KEY_ODOMETER, parser.nextInt() * 1000L);

        position.setCourse(parser.nextInt());

        position.set(Position.KEY_SATELLITES, parser.nextInt());

        boolean boxOpen = parser.nextInt() > 0;
        if (type == 8 && boxOpen) {
            position.addAlarm(Position.ALARM_TAMPERING);
        }
        position.set("boxOpen", boxOpen);

        position.set(Position.KEY_RSSI, parser.nextInt());

        boolean charge = parser.nextInt() > 0;
        if (type == 3) {
            position.addAlarm(charge ? Position.ALARM_POWER_RESTORED : Position.ALARM_POWER_CUT);
        }
        position.set(Position.KEY_CHARGE, charge);

        position.set(Position.KEY_IGNITION, parser.nextInt() > 0);
        position.set("engine", parser.nextInt() > 0);
        position.set(Position.KEY_BLOCKED, parser.nextInt() > 0);
        position.set(Position.PREFIX_ADC + 1, parser.nextDouble());
        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.KEY_ICCID, parser.next());
        position.set(Position.KEY_POWER, parser.nextDouble());

        return position;
    }

}
