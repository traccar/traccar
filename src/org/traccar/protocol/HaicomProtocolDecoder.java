/*
 * Copyright 2014 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.SocketAddress;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class HaicomProtocolDecoder extends BaseProtocolDecoder {

    public HaicomProtocolDecoder(HaicomProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$GPRS")
            .number("(d+),")                     // imei
            .expression("([^,]+),")              // version
            .number("(dd)(dd)(dd),")             // date
            .number("(dd)(dd)(dd),")             // time
            .number("(d)")                       // flags
            .number("(dd)(d{5})")                // latitude
            .number("(ddd)(d{5}),")              // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(d+),")                     // status
            .number("(d+)?,")                    // gprs counting value
            .number("(d+)?,")                    // gps power saving counting value
            .number("(d+),")                     // switch status
            .number("(d+)")                      // relay status
            .expression("(?:[LH]{2})?")          // power status
            .number("#V(d+)")                    // battery
            .any()
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

        if (!identify(parser.next(), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        position.set(Event.KEY_VERSION, parser.next());

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        int flags = parser.nextInt();

        position.setValid(BitUtil.check(flags, 0));

        double latitude = parser.nextDouble() + parser.nextDouble() / 60000;
        if (BitUtil.check(flags, 2)) {
            position.setLatitude(latitude);
        } else {
            position.setLatitude(-latitude);
        }

        double longitude = parser.nextDouble() + parser.nextDouble() / 60000;
        if (BitUtil.check(flags, 1)) {
            position.setLongitude(longitude);
        } else {
            position.setLongitude(-longitude);
        }

        position.setSpeed(parser.nextDouble() / 10);
        position.setCourse(parser.nextDouble() / 10);

        position.set(Event.KEY_STATUS, parser.next());
        position.set(Event.KEY_GSM, parser.next());
        position.set(Event.KEY_GPS, parser.next());
        position.set(Event.KEY_INPUT, parser.next());
        position.set(Event.KEY_OUTPUT, parser.next());
        position.set(Event.KEY_BATTERY, parser.nextDouble() / 10);

        return position;
    }

}
