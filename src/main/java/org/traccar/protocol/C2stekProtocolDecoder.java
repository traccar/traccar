/*
 * Copyright 2018 - 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class C2stekProtocolDecoder extends BaseProtocolDecoder {

    public C2stekProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("PA$")
            .number("(d+)")                      // imei
            .text("$")
            .expression(".#")                    // data type
            .number("(dd)(dd)(dd)#")             // date (yymmdd)
            .number("(dd)(dd)(dd)#")             // time (hhmmss)
            .number("([01])#")                   // valid
            .number("([+-]?d+.d+)#")             // latitude
            .number("([+-]?d+.d+)#")             // longitude
            .number("(d+.d+)#")                  // speed
            .number("(d+.d+)#")                  // course
            .number("(-?d+.d+)#")                // altitude
            .number("(d+)#")                     // battery
            .number("d+#")                       // geo area alarm
            .number("(x+)#")                     // alarm
            .number("([01])?")                   // armed
            .number("([01])")                    // door
            .number("([01])#")                   // ignition
            .any()
            .text("$AP")
            .compile();

    private String decodeAlarm(int alarm) {
        switch (alarm) {
            case 0x2:
                return Position.ALARM_VIBRATION;
            case 0x3:
                return Position.ALARM_POWER_CUT;
            case 0x4:
                return Position.ALARM_OVERSPEED;
            case 0x5:
                return Position.ALARM_SOS;
            case 0x6:
                return Position.ALARM_DOOR;
            case 0xA:
                return Position.ALARM_LOW_BATTERY;
            case 0xB:
                return Position.ALARM_FAULT;
            default:
                return null;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        if (sentence.contains("$20$") && channel != null) {
            channel.writeAndFlush(new NetworkMessage(sentence, remoteAddress));
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
        position.setValid(parser.nextInt() > 0);
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());
        position.setAltitude(parser.nextDouble());

        position.set(Position.KEY_BATTERY, parser.nextInt() * 0.001);
        position.set(Position.KEY_ALARM, decodeAlarm(parser.nextHexInt()));

        if (parser.hasNext()) {
            position.set(Position.KEY_ARMED, parser.nextInt() > 0);
        }
        position.set(Position.KEY_DOOR, parser.nextInt() > 0);
        position.set(Position.KEY_IGNITION, parser.nextInt() > 0);

        return position;
    }

}
