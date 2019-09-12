/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class RstProtocolDecoder extends BaseProtocolDecoder {

    public RstProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("RST;")
            .expression("[AL];")
            .expression("[^,]+;")                // model
            .expression(".{5};")                 // firmware
            .number("(d{9});")                   // serial number
            .number("d+;")                       // index
            .number("d+;")                       // type
            .number("(dd)-(dd)-(dddd) ")         // event date
            .number("(dd):(dd):(dd);")           // event time
            .number("(dd)-(dd)-(dddd) ")         // fix date
            .number("(dd):(dd):(dd);")           // fix time
            .number("(-?d+.d+);")                // longitude
            .number("(-?d+.d+);")                // latitude
            .number("(d+);")                     // speed
            .number("(d+);")                     // course
            .number("(-?d+);")                   // altitude
            .number("([01]);")                   // valid
            .number("(d+);")                     // satellites
            .number("(d+);")                     // hdop
            .number("(xx);")                     // inputs 1
            .number("(xx);")                     // inputs 2
            .number("(xx);")                     // inputs 3
            .number("(xx);")                     // outputs 1
            .number("(xx);")                     // outputs 2
            .number("(d+.d+);")                  // power
            .number("(d+.d+);")                  // battery
            .number("(d+);")                     // odometer
            .number("(d+);")                     // rssi
            .number("(xx);")                     // temperature
            .number("x{4};")                     // sensors
            .number("(xx);")                     // status 1
            .number("(xx);")                     // status 2
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

        position.setDeviceTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setFixTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setLongitude(parser.nextDouble());
        position.setLatitude(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());
        position.setAltitude(parser.nextInt());
        position.setValid(parser.nextInt() > 0);

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_HDOP, parser.nextInt());
        position.set(Position.PREFIX_IN + 1, parser.nextHexInt());
        position.set(Position.PREFIX_IN + 2, parser.nextHexInt());
        position.set(Position.PREFIX_IN + 3, parser.nextHexInt());
        position.set(Position.PREFIX_OUT + 1, parser.nextHexInt());
        position.set(Position.PREFIX_OUT + 2, parser.nextHexInt());
        position.set(Position.KEY_POWER, parser.nextDouble());
        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.KEY_ODOMETER, parser.nextInt());
        position.set(Position.KEY_RSSI, parser.nextInt());
        position.set(Position.PREFIX_TEMP + 1, (int) parser.nextHexInt().byteValue());
        position.set(Position.KEY_STATUS, parser.nextHexInt() << 8 + parser.nextHexInt());

        return position;
    }

}
