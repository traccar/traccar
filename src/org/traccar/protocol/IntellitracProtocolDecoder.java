/*
 * Copyright 2013 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class IntellitracProtocolDecoder extends BaseProtocolDecoder {

    public IntellitracProtocolDecoder(IntellitracProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .expression(".+,").optional()
            .number("(d+),")                     // identifier
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(-?d+.d+),")                // longitude
            .number("(-?d+.d+),")                // latitude
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*),")                 // course
            .number("(-?d+.?d*),")               // altitude
            .number("(d+),")                     // satellites
            .number("(d+),")                     // index
            .number("(d+),")                     // input
            .number("(d+),?")                    // output
            .number("(d+.d+)?,?")                // adc1
            .number("(d+.d+)?,?")                // adc2
            .groupBegin()
            .number("d{14},d+,")
            .number("(d+),")                     // vss
            .number("(d+),")                     // rpm
            .number("(-?d+),")                   // coolant
            .number("(d+),")                     // fuel
            .number("(d+),")                     // fuel consumption
            .number("(-?d+),")                   // fuel temperature
            .number("(d+),")                     // charger pressure
            .number("(d+),")                     // tpl
            .number("(d+),")                     // axle weight
            .number("(d+)")                      // odometer
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

        Position position = new Position(getProtocolName());

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(parser.nextDateTime());

        position.setLongitude(parser.nextDouble(0));
        position.setLatitude(parser.nextDouble(0));
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));
        position.setAltitude(parser.nextDouble(0));

        int satellites = parser.nextInt(0);
        position.setValid(satellites >= 3);
        position.set(Position.KEY_SATELLITES, satellites);

        position.set(Position.KEY_INDEX, parser.nextLong(0));
        position.set(Position.KEY_INPUT, parser.nextInt(0));
        position.set(Position.KEY_OUTPUT, parser.nextInt(0));

        position.set(Position.PREFIX_ADC + 1, parser.nextDouble(0));
        position.set(Position.PREFIX_ADC + 2, parser.nextDouble(0));

        // J1939 data
        position.set(Position.KEY_OBD_SPEED, parser.nextInt(0));
        position.set(Position.KEY_RPM, parser.nextInt(0));
        position.set("coolant", parser.nextInt(0));
        position.set(Position.KEY_FUEL_LEVEL, parser.nextInt(0));
        position.set(Position.KEY_FUEL_CONSUMPTION, parser.nextInt(0));
        position.set(Position.PREFIX_TEMP + 1, parser.nextInt(0));
        position.set("chargerPressure", parser.nextInt(0));
        position.set("tpl", parser.nextInt(0));
        position.set(Position.KEY_AXLE_WEIGHT, parser.nextInt(0));
        position.set(Position.KEY_OBD_ODOMETER, parser.nextInt(0));

        return position;
    }

}
