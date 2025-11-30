/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.Date;
import java.util.regex.Pattern;

public class Fa66sProtocolDecoder extends BaseProtocolDecoder {

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$")
            .expression("FA66S,")
            .expression("([^,]+),")                  // device identifier (IMEI/ID)
            .number("(dddd)(dd)(dd),")               // date (yyyyMMdd)
            .number("(dd)(dd)(dd),")                 // time (HHmmss)
            .expression("([AV]),")                   // validity
            .number("(-?d+.?d*),")                   // latitude
            .number("(-?d+.?d*),")                   // longitude
            .number("(d+.?d*),")                     // speed (km/h)
            .number("(d+.?d*),")                     // course
            .number("(d+.?d*),")                     // battery level
            .number("(d+),")                         // heart rate
            .number("(d+.?d*),")                     // body temperature
            .number("(d+),")                         // steps
            .number("(d+),")                         // sleep status
            .number("(d)")                           // SOS flag
            .any()
            .compile();

    public Fa66sProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private Date decodeTime(Parser parser) {
        DateBuilder dateBuilder = new DateBuilder()
                .setYear(parser.nextInt(0))
                .setMonth(parser.nextInt(0))
                .setDay(parser.nextInt(0))
                .setHour(parser.nextInt(0))
                .setMinute(parser.nextInt(0))
                .setSecond(parser.nextInt(0));
        return dateBuilder.getDate();
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null; // Ignore messages that do not match expected FA66S format
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(decodeTime(parser));
        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextDouble(0));
        position.setLongitude(parser.nextDouble(0));
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
        position.setCourse(parser.nextDouble(0));

        position.set(Position.KEY_BATTERY_LEVEL, parser.nextDouble(0));

        int heartRate = parser.nextInt(0);
        position.set("heartRate", heartRate);

        double bodyTemp = parser.nextDouble(0);
        position.set("bodyTemp", bodyTemp);

        int steps = parser.nextInt(0);
        position.set("steps", steps);

        int sleepStatus = parser.nextInt(0);
        position.set("sleepStatus", sleepStatus);

        int sosFlag = parser.nextInt(0);
        if (sosFlag > 0) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        }

        return position;
    }
}
