/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
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

public class Ivt401ProtocolDecoder extends BaseProtocolDecoder {

    public Ivt401ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("(")
            .expression("TL[ABLN],")             // header
            .number("(d+),")                     // imei
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("([-+]d+.d+),")              // latitude
            .number("([-+]d+.d+),")              // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(-?d+.?d*),")               // altitude
            .number("d+,")                       // satellites or battery status
            .number("(d),")                      // gps status
            .number("(d+),")                     // rssi
            .number("(d+),")                     // input
            .number("(d+),")                     // output
            .number("(d+.d+),")                  // adc
            .number("(d+.d+),")                  // power
            .number("(d+.d+),")                  // battery
            .number("(-?d+.?d*),")               // pcb temp
            .expression("([^,]+),")              // temp
            .number("(d+),")                     // movement
            .number("(d+.d+),")                  // acceleration
            .number("(-?d+),")                   // tilt
            .number("(d+),")                     // trip
            .number("(d+),")                     // odometer
            .groupBegin()
            .number("([01]),")                   // overspeed
            .number("[01],")                     // input 2 misuse
            .number("[01],")                     // immobilizer
            .number("[01],")                     // temperature alert
            .number("[0-2]+,")                   // geofence
            .number("([0-3]),")                  // harsh driving
            .number("[01],")                     // reconnect
            .number("([01]),")                   // low battery
            .number("([01]),")                   // power disconnected
            .number("[01],")                     // gps failure
            .number("([01]),")                   // towing
            .number("[01],")                     // server unreachable
            .number("[128],")                    // sleep mode
            .expression("([^,]+)?,")             // driver id
            .number("d+,")                       // sms count
            .groupEnd("?")
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

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());
        position.setAltitude(parser.nextDouble());
        position.setValid(parser.nextInt() > 0);

        position.set(Position.KEY_RSSI, parser.nextInt());

        String input = parser.next();
        for (int i = 0; i < input.length(); i++) {
            int value = Character.getNumericValue(input.charAt(i));
            if (value < 2) {
                position.set(Position.PREFIX_IN + (i + 1), value > 0);
            }
        }

        String output = parser.next();
        for (int i = 0; i < output.length(); i++) {
            position.set(Position.PREFIX_OUT + (i + 1), Character.getNumericValue(output.charAt(i)) > 0);
        }

        position.set(Position.PREFIX_ADC + 1, parser.nextDouble());
        position.set(Position.KEY_POWER, parser.nextDouble());
        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.KEY_DEVICE_TEMP, parser.nextDouble());

        String temp = parser.next();
        if (temp.startsWith("M")) {
            int index = 1;
            int startIndex = 1;
            int endIndex;
            while (startIndex < temp.length()) {
                endIndex = temp.indexOf('-', startIndex + 1);
                if (endIndex < 0) {
                    endIndex = temp.indexOf('+', startIndex + 1);
                }
                if (endIndex < 0) {
                    endIndex = temp.length();
                }
                if (endIndex > 0) {
                    double value = Double.parseDouble(temp.substring(startIndex, endIndex));
                    position.set(Position.PREFIX_TEMP + index++, value);
                }
                startIndex = endIndex;
            }
        } else {
            position.set(Position.PREFIX_TEMP + 1, Double.parseDouble(temp));
        }

        position.set(Position.KEY_MOTION, parser.nextInt() > 0);
        position.set(Position.KEY_ACCELERATION, parser.nextDouble());

        parser.nextInt(); // tilt
        parser.nextInt(); // trip state

        position.set(Position.KEY_ODOMETER, parser.nextLong());

        if (parser.hasNext(6)) {
            position.addAlarm(parser.nextInt() == 1 ? Position.ALARM_OVERSPEED : null);
            switch (parser.nextInt()) {
                case 1 -> position.addAlarm(Position.ALARM_ACCELERATION);
                case 2 -> position.addAlarm(Position.ALARM_BRAKING);
                case 3 -> position.addAlarm(Position.ALARM_CORNERING);
                default -> {
                }
            }
            position.addAlarm(parser.nextInt() == 1 ? Position.ALARM_LOW_BATTERY : null);
            position.addAlarm(parser.nextInt() == 1 ? Position.ALARM_POWER_CUT : null);
            position.addAlarm(parser.nextInt() == 1 ? Position.ALARM_TOW : null);
            position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());
        }

        return position;
    }

}
