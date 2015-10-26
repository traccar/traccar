/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class VisiontekProtocolDecoder extends BaseProtocolDecoder {

    public VisiontekProtocolDecoder(VisiontekProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$1,")
            .expression("([^,]+),")              // identifier
            .number("(d+),").optional()          // imei
            .number("(dd),(dd),(dd),")           // date
            .number("(dd),(dd),(dd),")           // time
            .number("(dd)(dd).?(dddd)")          // latitude
            .expression("([NS]),")
            .number("(ddd)(dd).?(dddd)")         // longitude
            .expression("([EW]),")
            .number("(d+.?d+),")                 // speed
            .number("(d+),")                     // course
            .groupBegin()
            .number("(d+),")                     // altitude
            .number("(d+),")                     // satellites
            .groupEnd("?")
            .number("(d+),")                     // odometer
            .groupBegin()
            .number("(d),")                      // ignition
            .number("(d),")                      // input 1
            .number("(d),")                      // input 2
            .number("(d),")                      // immobilizer
            .number("(d),")                      // external battery status
            .number("(d+),")                     // gsm
            .groupEnd("?")
            .expression("([AV]),?")              // validity
            .number("(d+)?")                     // rfid
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

        String id = parser.next();
        String imei = parser.next();
        if (!identify(id, channel, null, false) && !identify(imei, channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_MIN_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_MIN_HEM));

        position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(
                parser.next().replace(".", "")) / 10));

        position.setCourse(parser.nextDouble());
        position.setAltitude(parser.nextDouble());

        position.set(Event.KEY_SATELLITES, parser.next());
        position.set(Event.KEY_ODOMETER, parser.next());
        position.set(Event.KEY_IGNITION, parser.next());
        position.set(Event.PREFIX_IO + 1, parser.next());
        position.set(Event.PREFIX_IO + 2, parser.next());
        position.set("immobilizer", parser.next());
        position.set(Event.KEY_POWER, parser.next());
        position.set(Event.KEY_GSM, parser.next());

        position.setValid(parser.next().equals("A"));

        position.set(Event.KEY_RFID, parser.next());

        return position;
    }

}
