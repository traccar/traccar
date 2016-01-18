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

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class GoSafeProtocolDecoder extends BaseProtocolDecoder {

    public GoSafeProtocolDecoder(GoSafeProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("*GS")                         // header
            .number("d+,")                       // protocol version
            .number("(d+),")                     // imei
            .number("(dd)(dd)(dd)")              // time
            .number("(dd)(dd)(dd),")             // date
            .optional(2)
            .expression("(.*)#?")                // data
            .compile();

    private static final Pattern PATTERN_ITEM = new PatternBuilder()
            .number("(x+)?,").optional()         // event
            .groupBegin()
            .text("SYS:")
            .expression("[^,]*,")
            .groupEnd("?")
            .groupBegin()
            .text("GPS:")
            .expression("([AV]);")               // validity
            .number("(d+);")                     // satellites
            .number("([NS])(d+.d+);")            // latitude
            .number("([EW])(d+.d+);")            // longitude
            .number("(d+)?;")                    // speed
            .number("(d+);")                     // course
            .number("(d+);")                     // altitude
            .number("(d+.d+)")                   // hdop
            .number("(?:;d+.d+)?")               // vdop
            .expression(",?")
            .groupEnd()
            .groupBegin()
            .text("GSM:").expression("[^,]*,?")
            .groupEnd("?")
            .groupBegin()
            .text("COT:")
            .number("(d+)")                      // odometer
            .number("(?:;d+:d+:d+)?")            // engine hours
            .expression(",?")
            .groupEnd("?")
            .groupBegin()
            .text("ADC:")
            .number("(d+.d+);")                  // power
            .number("(d+.d+),?")                 // battery
            .groupEnd("?")
            .groupBegin()
            .text("DTT:")
            .number("(x+);")                     // status
            .expression("[^;]*;")
            .number("x+;")                       // geo-fence 0-119
            .number("x+;")                       // geo-fence 120-155
            .number("x+,?")                      // event status
            .groupEnd("?")
            .groupBegin()
            .text("ETD:").expression("[^,]*,?")
            .groupEnd("?")
            .groupBegin()
            .text("OBD:")
            .number("(x+),?")
            .groupEnd("?")
            .groupBegin()
            .text("FUL:").expression("[^,]*,?")
            .groupEnd("?")
            .groupBegin()
            .text("TRU:").expression("[^,]*,?")
            .groupEnd("?")
            .compile();

    private Position decodePosition(Parser parser, Date time) {

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(getDeviceId());

        if (time != null) {
            position.setTime(time);
        }

        position.set(Event.KEY_EVENT, parser.next());

        position.setValid(parser.next().equals("A"));
        position.set(Event.KEY_SATELLITES, parser.next());

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());
        position.setAltitude(parser.nextDouble());

        position.set(Event.KEY_HDOP, parser.next());
        position.set(Event.KEY_ODOMETER, parser.next());
        position.set(Event.KEY_POWER, parser.next());
        position.set(Event.KEY_BATTERY, parser.next());

        String status = parser.next();
        if (status != null) {
            position.set(Event.KEY_IGNITION, BitUtil.check(Integer.parseInt(status, 16), 13));
            position.set(Event.KEY_STATUS, status);
        }

        if (parser.hasNext()) {
            position.set("obd", parser.next());
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        if (channel != null) {
            channel.write("1234");
        }

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        if (!identify(parser.next(), channel, remoteAddress)) {
            return null;
        }

        Date time = null;
        if (parser.hasNext(6)) {
            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt())
                    .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
            time = dateBuilder.getDate();
        }

        List<Position> positions = new LinkedList<>();
        Parser itemParser = new Parser(PATTERN_ITEM, parser.next());
        while (itemParser.find()) {
            positions.add(decodePosition(itemParser, time));
        }

        return positions;
    }

}
