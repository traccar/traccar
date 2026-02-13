/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class NtoProtocolDecoder extends BaseProtocolDecoder {

    public NtoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("^NB,")                        // manufacturer
            .number("(d+),")                     // imei
            .expression("(...),")                // type
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([AVM]),")              // validity
            .number("([NS]),(dd)(dd.d+),")       // latitude
            .number("([EW]),(ddd)(dd.d+),")      // longitude
            .number("(d+.?d*),")                 // speed
            .number("(d+),")                     // course
            .number("(x+),")                     // status
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

        position.set(Position.KEY_TYPE, parser.next());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextInt());

        long status = parser.nextHexLong();
        position.set(Position.KEY_STATUS, status);
        position.addAlarm(BitUtil.check(status, 1) ? Position.ALARM_JAMMING : null);
        position.addAlarm(BitUtil.check(status, 3 * 8 + 1) ? Position.ALARM_POWER_CUT : null);
        position.addAlarm(BitUtil.check(status, 3 * 8 + 2) ? Position.ALARM_OVERSPEED : null);
        position.addAlarm(BitUtil.check(status, 3 * 8 + 3) ? Position.ALARM_VIBRATION : null);
        position.addAlarm(BitUtil.check(status, 3 * 8 + 4) ? Position.ALARM_GEOFENCE_ENTER : null);
        position.addAlarm(BitUtil.check(status, 3 * 8 + 5) ? Position.ALARM_GEOFENCE_EXIT : null);
        position.addAlarm(BitUtil.check(status, 4 * 8) ? Position.ALARM_LOW_BATTERY : null);
        position.addAlarm(BitUtil.check(status, 4 * 8 + 4) ? Position.ALARM_DOOR : null);

        return position;
    }

}
