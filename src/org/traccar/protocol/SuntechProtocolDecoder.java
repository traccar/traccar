/*
 * Copyright 2013 - 2015 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class SuntechProtocolDecoder extends BaseProtocolDecoder {

    public SuntechProtocolDecoder(SuntechProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .expression("S.")
            .number("ddd")
            .expression("(?:[A-Z]{3})?;")        // header
            .expression("([^;]+);").optional()   // type
            .number("(d{6,});")                  // device id
            .number("d+;").optional()
            .number("(d+);")                     // version
            .number("(dddd)(dd)(dd);")           // date
            .number("(dd):(dd):(dd);")           // time
            .number("(x+);").optional()          // cell
            .number("([-+]dd.d+);")              // latitude
            .number("([-+]ddd.d+);")             // longitude
            .number("(ddd.ddd);")                // speed
            .number("(ddd.dd);")                 // course
            .number("d+;").optional()
            .number("(d+.d+)?")                  // battery
            .any()                               // full format
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (parser.hasNext()) {
            String type = parser.next();
            if (type.equals("Alert") || type.equals("Emergency")) {
                position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
            }
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_VERSION, parser.next());

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        parser.next(); // location code + bsic

        position.setValid(true);
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());

        position.set(Position.KEY_BATTERY, parser.next());

        return position;
    }

}
