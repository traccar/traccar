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
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class AustinNbProtocolDecoder extends BaseProtocolDecoder {

    public AustinNbProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(d+);")                     // imei
            .number("(dddd)-(dd)-(dd) ")         // date
            .number("(dd):(dd):(dd);")           // time
            .number("(-?d+,d+);")                // latitude
            .number("(-?d+,d+);")                // longitude
            .number("(d+);")                     // azimuth
            .number("(d+);")                     // angle
            .number("(d+);")                     // range
            .number("(d+);")                     // out of range
            .expression("(.*)")                  // operator
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

        position.setTime(parser.nextDateTime());

        position.setValid(true);
        position.setLatitude(Double.parseDouble(parser.next().replace(',', '.')));
        position.setLongitude(Double.parseDouble(parser.next().replace(',', '.')));
        position.setCourse(parser.nextInt());
        position.set("angle", parser.nextInt());
        position.set("range", parser.nextInt());
        position.set("outOfRange", parser.nextInt());
        position.set("carrier", parser.next());

        return position;
    }

}
