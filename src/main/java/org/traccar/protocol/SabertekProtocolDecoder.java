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

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;
import java.util.regex.Pattern;

public class SabertekProtocolDecoder extends BaseProtocolDecoder {

    public SabertekProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text(",")
            .number("(d+),")                     // id
            .number("d,")                        // type
            .groupBegin()
            .number("d+,")                       // imei
            .number("d+,")                       // scid
            .expression("[^,]*,")                // phone
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .groupEnd("?")
            .number("(d+),")                     // battery
            .number("(d+),")                     // rssi
            .number("(d+),")                     // state
            .number("(d+),")                     // events
            .number("(d),")                      // valid
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+),")                // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(d+),")                     // altitude
            .number("(d+),")                     // satellites
            .number("(d+),")                     // odometer
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    Unpooled.wrappedBuffer(new byte[]{(byte) (deviceSession != null ? 0x06 : 0x15)}), remoteAddress));
        }
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (parser.hasNext(6)) {
            position.setTime(parser.nextDateTime());
        } else {
            position.setTime(new Date());
        }

        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());
        position.set(Position.KEY_RSSI, parser.nextInt());

        int state = parser.nextInt();

        position.set(Position.KEY_IGNITION, BitUtil.check(state, 0));
        position.set(Position.KEY_CHARGE, BitUtil.check(state, 1));

        if (BitUtil.check(state, 2)) {
            position.addAlarm(Position.ALARM_JAMMING);
        }
        if (BitUtil.check(state, 3)) {
            position.addAlarm(Position.ALARM_TAMPERING);
        }

        int events = parser.nextInt();

        if (BitUtil.check(events, 0)) {
            position.addAlarm(Position.ALARM_BRAKING);
        }
        if (BitUtil.check(events, 1)) {
            position.addAlarm(Position.ALARM_OVERSPEED);
        }
        if (BitUtil.check(events, 2)) {
            position.addAlarm(Position.ALARM_ACCIDENT);
        }
        if (BitUtil.check(events, 3)) {
            position.addAlarm(Position.ALARM_CORNERING);
        }

        position.setValid(parser.nextInt() == 1);
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());
        position.setAltitude(parser.nextInt());

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_ODOMETER, parser.nextInt() * 1000L);

        return position;
    }

}
