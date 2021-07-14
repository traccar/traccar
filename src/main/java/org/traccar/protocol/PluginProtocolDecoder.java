/*
 * Copyright 2019 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class PluginProtocolDecoder extends BaseProtocolDecoder {

    public PluginProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .expression("[^0-9,]*,?")
            .number("([^,]+),")                  // device id
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(-?d+.d+),")                // longitude
            .number("(-?d+.d+),")                // latitude
            .number("(d+.?d*),")                 // speed
            .number("(d+),")                     // course
            .number("(-?d+),")                   // altitude
            .number("(-?d+),")                   // satellites
            .number("d+,")                       // type
            .number("(d+.?d*),")                 // odometer
            .number("(d+),")                     // status
            .number("(d+.?d*),")                 // fuel
            .expression("[^,]*,")
            .text("0")
            .groupBegin()
            .number(",(-?d+.?d*)")               // temperature 1
            .number(",(-?d+.?d*)")               // temperature 2
            .number(",d+")                       // oil level
            .number(",(d+)")                     // rpm
            .number(",(d+)")                     // obd speed
            .number(",d+")                       // people up
            .number(",d+")                       // people down
            .number(",d+")                       // obd status
            .number(",d+")                       // fuel intake air temperature
            .number(",(d+)")                     // throttle
            .number(",(d+)")                     // battery
            .groupEnd("?")
            .groupBegin()
            .text(",+,")
            .number("(d+),")                     // event
            .groupEnd("?")
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage("$$hb,1#", remoteAddress));
        }

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

        position.setTime(parser.nextDateTime());
        position.setLongitude(parser.nextDouble());
        position.setLatitude(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.setCourse(parser.nextInt());
        position.setAltitude(parser.nextInt());

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_ODOMETER, (long) (parser.nextDouble() * 1000));

        long status = parser.nextLong();
        position.setValid(BitUtil.check(status, 0));
        position.set(Position.KEY_IGNITION, BitUtil.check(status, 1));
        for (int i = 0; i < 4; i++) {
            position.set(Position.PREFIX_IN + (i + 1), BitUtil.check(status, 20 + i));
        }
        position.set(Position.KEY_STATUS, status);

        position.set(Position.KEY_FUEL_LEVEL, parser.nextDouble());

        if (parser.hasNext(6)) {
            position.set(Position.PREFIX_TEMP + 1, parser.nextDouble());
            position.set(Position.PREFIX_TEMP + 2, parser.nextDouble());
            position.set(Position.KEY_RPM, parser.nextInt());
            position.set(Position.KEY_OBD_SPEED, parser.nextInt());
            position.set(Position.KEY_THROTTLE, parser.nextInt() * 0.1);
            position.set(Position.KEY_POWER, parser.nextInt() * 0.1);
        }

        if (parser.hasNext()) {
            int event = parser.nextInt();
            switch (event) {
                case 11317:
                    position.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
                    break;
                case 11319:
                    position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
                    break;
                default:
                    break;

            }
            position.set(Position.KEY_EVENT, event);
        }

        return position;
    }

}
