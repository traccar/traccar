/*
 * Copyright 2017 - 2019 Anton Tananaev (anton@traccar.org)
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

public class VtfmsProtocolDecoder extends BaseProtocolDecoder {

    private static final String[] DIRECTIONS = new String[] {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    public VtfmsProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("(")
            .number("(d{15}),")                  // imei
            .number("[0-9A-Z]{3}dd,")            // packet count
            .number("(dd),")                     // packet id
            .number("[^,]*,")                    // reserved
            .number("(d+)?,")                    // rssi
            .number("(?:d+)?,")                  // fix status
            .number("(d+)?,")                    // satellites
            .number("[^,]*,")                    // reserved
            .expression("([AV]),")               // validity
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(dd)(dd)(dd),")             // time (ddmmyy)
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+),")                // longitude
            .number("(?:(d+)|([NESW]{1,2})),")   // course
            .number("(d+),")                     // speed
            .number("(d+),")                     // hours
            .number("(d+),")                     // idle hours
            .expression("[KNT],")                // antenna status
            .number("(d+),")                     // odometer
            .expression("([01]),")               // power status
            .number("(d+.d+),")                  // power voltage
            .number("[^,]*,")                    // reserved
            .number("(d+)?,")                    // fuel level
            .number("(d+.d+)?,")                 // adc 1
            .number("[^,]*,")                    // reserved
            .number("(d+.d+)?,")                 // adc 2
            .expression("([01]),")               // di 1
            .expression("([01]),")               // di 2
            .expression("([01]),")               // di 3
            .expression("([01]),")               // di 4
            .expression("([01]),")               // do 1
            .expression("([01]),")               // do 2
            .expression("([01]),")               // do 3
            .number("[^,]*,")                    // reserved
            .number("[^,]*")                     // reserved
            .text(")")
            .number("ddd")                       // checksum
            .compile();

    private String decodeAlarm(int value) {
        switch (value) {
            case 10:
                return Position.ALARM_OVERSPEED;
            case 14:
                return Position.ALARM_POWER_CUT;
            case 15:
                return Position.ALARM_POWER_RESTORED;
            case 32:
                return Position.ALARM_BRAKING;
            case 33:
                return Position.ALARM_ACCELERATION;
            default:
                return null;
        }
    }

    private double convertToDegrees(double value) {
        double degrees = Math.floor(value / 100);
        return degrees + (value - degrees * 100) / 60;
    }

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

        position.set(Position.KEY_ALARM, decodeAlarm(parser.nextInt()));
        position.set(Position.KEY_RSSI, parser.nextInt());
        position.set(Position.KEY_SATELLITES, parser.nextInt());

        position.setValid(parser.next().equals("A"));
        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY));

        double latitude = parser.nextDouble();
        double longitude = parser.nextDouble();
        if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            position.setLatitude(convertToDegrees(latitude));
            position.setLongitude(convertToDegrees(longitude));
        } else {
            position.setLatitude(latitude);
            position.setLongitude(longitude);
        }

        position.setCourse(parser.nextDouble(0));
        if (parser.hasNext()) {
            String direction = parser.next();
            for (int i = 0; i < DIRECTIONS.length; i++) {
                if (direction.equals(DIRECTIONS[i])) {
                    position.setCourse(i * 45.0);
                    break;
                }
            }
        }

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

        position.set(Position.KEY_HOURS, UnitsConverter.msFromHours(parser.nextInt()));
        position.set("idleHours", parser.nextInt());
        position.set(Position.KEY_ODOMETER, parser.nextInt() * 100);
        position.set(Position.KEY_CHARGE, parser.next().equals("1"));
        position.set(Position.KEY_POWER, parser.nextDouble());
        position.set(Position.KEY_FUEL_LEVEL, parser.nextInt());
        position.set(Position.PREFIX_ADC + 1, parser.nextDouble());
        position.set(Position.PREFIX_ADC + 2, parser.nextDouble());
        position.set(Position.PREFIX_IN + 1, parser.nextInt());
        position.set(Position.PREFIX_IN + 2, parser.nextInt());
        position.set(Position.PREFIX_IN + 3, parser.nextInt());
        position.set(Position.PREFIX_IN + 4, parser.nextInt());
        position.set(Position.PREFIX_OUT + 1, parser.nextInt());
        position.set(Position.PREFIX_OUT + 2, parser.nextInt());
        position.set(Position.PREFIX_OUT + 3, parser.nextInt());

        return position;
    }

}
