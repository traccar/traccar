/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class DwayProtocolDecoder extends BaseProtocolDecoder {

    public DwayProtocolDecoder(DwayProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("AA55,")
            .number("d+,")                       // index
            .number("(d+),")                     // imei
            .number("d+,")                       // type
            .number("(dd)(dd)(dd),")             // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+),")                // longitude
            .number("(-?d+),")                   // altitude
            .number("(d+.d+),")                  // speed
            .number("(d+),")                     // course
            .number("([01]{4}),")                // input
            .number("([01]{4}),")                // output
            .number("([01])([01])([01])([01]),") // flags
            .number("(d+),")                     // battery
            .number("(d+),")                     // adc1
            .number("(d+),")                     // adc2
            .number("(d+)")                      // driver
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        if (sentence.startsWith(">H")) {
            if (channel != null) {
                channel.write(">ALIVE\r\n");
            }
            return null;
        }

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(parser.nextDateTime());
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setAltitude(parser.nextDouble(0));
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
        position.setCourse(parser.nextDouble(0));

        position.set(Position.KEY_INPUT, parser.nextBinInt());
        position.set(Position.KEY_OUTPUT, parser.nextBinInt());

        position.setValid(parser.next().equals("1"));

        position.set(Position.KEY_IGNITION, parser.next().equals("1"));
        position.set(Position.KEY_CHARGE, parser.next().equals("1"));

        if (parser.next().equals("1")) {
            position.set(Position.KEY_ALARM, Position.ALARM_SHOCK);
        }

        position.set(Position.KEY_BATTERY, parser.nextInt() * 0.001);
        position.set(Position.PREFIX_ADC + 1, parser.nextInt() * 0.001);
        position.set(Position.PREFIX_ADC + 2, parser.nextInt() * 0.001);
        position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());

        return position;
    }

}
