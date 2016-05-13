/*
 * Copyright 2015 - 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XirgoProtocolDecoder extends BaseProtocolDecoder {

    public XirgoProtocolDecoder(XirgoProtocol protocol) {
        super(protocol);
    }

    private boolean newFormat;

    private static final Pattern FIRMWARE = Pattern.compile("\\w{4}-([0-9A-F]{7})");

    private static final Pattern PATTERN_OLD = new PatternBuilder()
            .text("$$")
            .number("(d+),")                     // imei
            .number("(d+),")                     // event
            .number("(dddd)/(dd)/(dd),")         // date
            .number("(dd):(dd):(dd),")           // time
            .number("(-?d+.?d*),")               // latitude
            .number("(-?d+.?d*),")               // longitude
            .number("(-?d+.?d*),")               // altitude
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*),")                 // course
            .number("(d+),")                     // satellites
            .number("(d+.?d*),")                 // hdop
            .number("(d+.d+),")                  // battery
            .number("(d+),")                     // gsm
            .number("(d+.?d*),")                 // odometer
            .number("(d+),")                     // gps
            .any()
            .compile();

    private static final Pattern PATTERN_NEW = new PatternBuilder()
            .text("$$")
            .number("(d+),")                     // imei
            .number("(d+),")                     // event
            .number("(dddd)/(dd)/(dd),")         // date
            .number("(dd):(dd):(dd),")           // time
            .number("(-?d+.?d*),")               // latitude
            .number("(-?d+.?d*),")               // longitude
            .number("(-?d+.?d*),")               // altitude
            .number("(d+.?d*),")                 // speed
            .number("d+.?d*,")                   // acceleration
            .number("d+.?d*,")                   // deceleration
            .number("d+,")
            .number("(d+.?d*),")                 // course
            .number("(d+),")                     // satellites
            .number("(d+.?d*),")                 // hdop
            .number("(d+.?d*),")                 // odometer
            .number("d+.?d*,")                   // fuel consumption
            .number("(d+.d+),")                  // battery
            .number("(d+),")                     // gsm
            .number("(d+),")                     // gps
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        Matcher matcher = FIRMWARE.matcher(sentence);
        if (matcher.find()) {
            String type = matcher.group(1);
            if (type.equals("1137CD1") || type.equals("1137CC1") || type.equals("1137CA3")) {
                newFormat = true;
            }
        }

        Parser parser;
        if (newFormat) {
            parser = new Parser(PATTERN_NEW, sentence);
        } else {
            parser = new Parser(PATTERN_OLD, sentence);
        }

        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (!identify(parser.next(), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        position.set(Event.KEY_EVENT, parser.next());

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setAltitude(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromMph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());

        position.set(Event.KEY_SATELLITES, parser.next());
        position.set(Event.KEY_HDOP, parser.next());

        if (newFormat) {
            position.set(Event.KEY_ODOMETER, parser.next());
        }

        position.set(Event.KEY_BATTERY, parser.next());
        position.set(Event.KEY_GSM, parser.next());

        if (!newFormat) {
            position.set(Event.KEY_ODOMETER, parser.next());
        }

        position.setValid(parser.nextInt() == 1);

        return position;
    }

}
