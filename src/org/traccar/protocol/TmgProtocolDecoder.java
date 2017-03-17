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
import org.traccar.helper.BitUtil;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class TmgProtocolDecoder extends BaseProtocolDecoder {

    public TmgProtocolDecoder(TmgProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$")
            .expression("(...),")                // type
            .expression("[LH],")                 // history
            .number("(d+),")                     // imei
            .number("(dd)(dd)(dddd),")           // date (ddmmyyyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(d),")                      // status
            .number("(dd)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(ddd)(dd.d+),")             // longitude
            .expression("([EW]),")
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*),")                 // course
            .number("(-?d+.?d*),")               // altitude
            .number("(d+.d+),")                  // hdop
            .number("(d+),")                     // satellites
            .number("(d+),")                     // visible satellites
            .number("([^,]*),")                  // operator
            .number("(d+),")                     // rssi
            .number("[^,]*,")                    // cid
            .expression("([01]),")               // ignition
            .number("(d+.?d*),")                 // battery
            .number("(d+.?d*),")                 // power
            .expression("([01]+),")              // input
            .expression("([01]+),")              // output
            .expression("[01]+,")                // temper status
            .number("(d+.?d*)[^,]*,")            // adc1
            .number("(d+.?d*)[^,]*,")            // adc2
            .number("d+.?d*,")                   // trip meter
            .expression("([^,]*),")              // software version
            .expression("([^,]*),").optional()   // rfid
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        String type = parser.next();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        switch (type) {
            case "rmv":
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
                break;
            case "ebl":
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_POWER);
                break;
            case "ibl":
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                break;
            case "tmp":
            case "smt":
            case "btt":
                position.set(Position.KEY_ALARM, Position.ALARM_TAMPERING);
                break;
            case "ion":
                position.set(Position.KEY_IGNITION, true);
                break;
            case "iof":
                position.set(Position.KEY_IGNITION, false);
                break;
            default:
                break;
        }

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.setValid(parser.nextInt() > 0);
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());
        position.setAltitude(parser.nextDouble());

        position.set(Position.KEY_HDOP, parser.nextDouble());
        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_SATELLITES_VISIBLE, parser.nextInt());
        position.set(Position.KEY_OPERATOR, parser.next());
        position.set(Position.KEY_RSSI, parser.nextInt());
        position.set(Position.KEY_IGNITION, parser.nextInt() == 1);
        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.KEY_POWER, parser.nextDouble());

        int input = parser.nextInt(2);
        int output = parser.nextInt(2);

        if (!BitUtil.check(input, 0)) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        }

        position.set(Position.KEY_INPUT, input);
        position.set(Position.KEY_OUTPUT, output);

        position.set(Position.PREFIX_ADC + 1, parser.nextDouble());
        position.set(Position.PREFIX_ADC + 2, parser.nextDouble());
        position.set(Position.KEY_VERSION_FW, parser.next());
        position.set(Position.KEY_RFID, parser.next());

        return position;
    }

}
