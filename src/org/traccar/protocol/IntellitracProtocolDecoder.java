/*
 * Copyright 2013 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.SocketAddress;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class IntellitracProtocolDecoder extends BaseProtocolDecoder {

    public IntellitracProtocolDecoder(IntellitracProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .expression(".+,").optional()
            .number("(d+),")                     // identifier
            .number("(dddd)(dd)(dd)")            // date
            .number("(dd)(dd)(dd),")             // time
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

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (!identify(parser.next(), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setLongitude(parser.nextDouble());
        position.setLatitude(parser.nextDouble());
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());
        position.setAltitude(parser.nextDouble());

        int satellites = parser.nextInt();
        position.setValid(satellites >= 3);
        position.set(Event.KEY_SATELLITES, satellites);

        position.set(Event.KEY_INDEX, parser.nextLong());
        position.set(Event.KEY_INPUT, parser.next());
        position.set(Event.KEY_OUTPUT, parser.next());

        position.set(Event.PREFIX_ADC + 1, parser.next());
        position.set(Event.PREFIX_ADC + 2, parser.next());

        // J1939 data
        position.set(Event.KEY_OBD_SPEED, parser.next());
        position.set(Event.KEY_RPM, parser.next());
        position.set("coolant", parser.next());
        position.set(Event.KEY_FUEL, parser.next());
        position.set("consumption", parser.next());
        position.set(Event.PREFIX_TEMP + 1, parser.next());
        position.set(Event.KEY_CHARGE, parser.next());
        position.set("tpl", parser.next());
        position.set("axle", parser.next());
        position.set(Event.KEY_ODOMETER, parser.next());

        return position;
    }

}
