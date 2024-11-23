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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.PatternBuilder;

import java.net.SocketAddress;
import java.util.regex.Pattern;
import org.traccar.session.DeviceSession;
import org.traccar.helper.Parser;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

public class SviasProtocolDecoder extends BaseProtocolDecoder {

    public SviasProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("[")                           // delimiter
            .number("d{4},")                     // hardware version
            .number("d{4},")                     // software version
            .number("d+,")                       // index
            .number("(d+),")                     // imei
            .number("d+,")                       // hour meter
            .number("(d+)(dd)(dd),")             // date (dmmyy)
            .number("(d+)(dd)(dd),")             // time (hmmss)
            .number("(-?)(d+)(dd)(d{5}),")       // latitude
            .number("(-?)(d+)(dd)(d{5}),")       // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(d+),")                     // odometer
            .number("(d+),")                     // input
            .number("(d+),")                     // output / status
            .number("(d),")
            .number("(d),")
            .number("(d+),")                     // power
            .number("(d+),")                     // battery level
            .number("(d+),")                     // rssi
            .any()
            .compile();

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage("@", remoteAddress));
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

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN_MIN));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN_MIN));
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble() * 0.01));
        position.setCourse(parser.nextDouble() * 0.01);

        position.set(Position.KEY_ODOMETER, parser.nextInt() * 100);

        int input = parser.nextInt();
        int output = parser.nextInt();

        position.addAlarm(BitUtil.check(input, 0) ? Position.ALARM_SOS : null);
        position.set(Position.KEY_IGNITION, BitUtil.check(input, 4));
        position.setValid(BitUtil.check(output, 0));

        position.set(Position.KEY_POWER, parser.nextInt() * 0.001);
        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());
        position.set(Position.KEY_RSSI, parser.nextInt());

        return position;
    }

}
