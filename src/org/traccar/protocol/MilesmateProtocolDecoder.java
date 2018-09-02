/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class MilesmateProtocolDecoder extends BaseProtocolDecoder {

    public MilesmateProtocolDecoder(MilesmateProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("ApiString={")
            .number("A:(d+),")                   // imei
            .number("B:(d+.d+),")                // battery
            .number("C:(d+.d+),")                // adc
            .number("D:(dd)(dd)(dd),")           // time (hhmmss)
            .number("E:(dd)(dd.d+)([NS]),")      // latitude
            .number("F:(ddd)(dd.d+)([EW]),")     // longitude
            .number("G:(d+.d+),")                // speed
            .number("H:(dd)(dd)(dd),")           // date (ddmmyy)
            .expression("I:[GL],")               // location source
            .number("J:([01]{8}),")              // flags
            .number("K:([01]{7})")               // flags
            .expression("([AV]),")               // validity
            .number("L:d{4},")                   // pin
            .number("M:(d+.d+)")                 // course
            .text("}")
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

        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.PREFIX_ADC + 1, parser.nextDouble());

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        int flags = parser.nextBinInt();
        position.set(Position.KEY_IGNITION, BitUtil.check(flags, 8 - 1));
        position.set(Position.KEY_ALARM, BitUtil.check(flags, 8 - 2) ? Position.ALARM_SOS : null);
        position.set(Position.KEY_CHARGE, BitUtil.check(flags, 8 - 6));
        position.set(Position.KEY_ALARM, BitUtil.check(flags, 8 - 8) ? Position.ALARM_OVERSPEED : null);

        flags = parser.nextBinInt() << 1;
        position.set(Position.KEY_BLOCKED, BitUtil.check(flags, 8 - 1));
        position.set(Position.KEY_ALARM, BitUtil.check(flags, 8 - 2) ? Position.ALARM_TOW : null);

        position.setValid(parser.next().equals("A"));

        position.setCourse(parser.nextDouble());

        return position;
    }

}
