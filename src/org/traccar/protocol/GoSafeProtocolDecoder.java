/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class GoSafeProtocolDecoder extends BaseProtocolDecoder {

    public GoSafeProtocolDecoder(GoSafeProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("*GS")                         // header
            .number("d+,")                       // protocol version
            .number("(d+),")                     // imei
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
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
            .number(";(d+.d+)").optional()       // vdop
            .expression(",?")
            .groupEnd()
            .groupBegin()
            .text("GSM:")
            .number("d*;")                       // registration
            .number("d*;")                       // gsm signal
            .number("(d+);")                     // mcc
            .number("(d+);")                     // mnc
            .number("(x+);")                     // lac
            .number("(x+);")                     // cid
            .number("(-d+)")                     // rssi
            .expression("[^,]*,?")
            .groupEnd("?")
            .groupBegin()
            .text("COT:")
            .number("(d+)")                      // odometer
            .number("(?:;d+:d+:d+)?")            // engine hours
            .expression(",?")
            .groupEnd("?")
            .groupBegin()
            .text("ADC:")
            .number("(d+.d+)")                   // power
            .number("(?:;(d+.d+))?,?")           // battery
            .groupEnd("?")
            .groupBegin()
            .text("DTT:")
            .number("(x+);")                     // status
            .number("(x+)?;")                    // io
            .number("(x+);")                     // geo-fence 0-119
            .number("(x+);")                     // geo-fence 120-155
            .number("(x+)")                      // event status
            .number("(?:;(x+))?,?")              // packet type
            .groupEnd("?")
            .groupBegin()
            .text("ETD:").expression("([^,]+),?")
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
            .groupBegin()
            .text("TAG:").expression("([^,]+),?")
            .groupEnd("?")
            .compile();

    private static final Pattern PATTERN_OLD = new PatternBuilder()
            .text("*GS")                         // header
            .number("d+,")                       // protocol version
            .number("(d+),")                     // imei
            .text("GPS:")
            .number("(dd)(dd)(dd);")             // time (hhmmss)
            .number("d;").optional()             // fix type
            .expression("([AV]);")               // validity
            .number("([NS])(d+.d+);")            // latitude
            .number("([EW])(d+.d+);")            // longitude
            .number("(d+)?;")                    // speed
            .number("(d+);")                     // course
            .number("(d+.?d*)").optional()       // hdop
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .any()
            .compile();

    private Position decodePosition(DeviceSession deviceSession, Parser parser, Date time) {

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (time != null) {
            position.setTime(time);
        }

        position.set(Position.KEY_EVENT, parser.next());

        position.setValid(parser.next().equals("A"));
        position.set(Position.KEY_SATELLITES, parser.next());

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());
        position.setAltitude(parser.nextDouble());

        position.set(Position.KEY_HDOP, parser.nextDouble());
        position.set(Position.KEY_VDOP, parser.nextDouble());

        if (parser.hasNext(5)) {
            position.setNetwork(new Network(CellTower.from(
                    parser.nextInt(), parser.nextInt(), parser.nextInt(16), parser.nextInt(16), parser.nextInt())));
        }
        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextInt());
        }
        position.set(Position.KEY_POWER, parser.next());
        position.set(Position.KEY_BATTERY, parser.next());

        if (parser.hasNext(6)) {
            long status = parser.nextLong(16);
            position.set(Position.KEY_IGNITION, BitUtil.check(status, 13));
            position.set(Position.KEY_STATUS, status);
            position.set("ioStatus", parser.next());
            position.set(Position.KEY_GEOFENCE, parser.next() + parser.next());
            position.set("eventStatus", parser.next());
            position.set("packetType", parser.next());
        }

        if (parser.hasNext()) {
            position.set("eventData", parser.next());
        }

        if (parser.hasNext()) {
            position.set("obd", parser.next());
        }

        if (parser.hasNext()) {
            position.set("tagData", parser.next());
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        if (channel != null) {
            channel.write("1234");
        }

        String sentence = (String) msg;
        Pattern pattern = PATTERN;
        if (sentence.startsWith("*GS02")) {
            pattern = PATTERN_OLD;
        }

        Parser parser = new Parser(pattern, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        if (pattern == PATTERN_OLD) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

            position.setValid(parser.next().equals("A"));
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
            position.setCourse(parser.nextDouble());

            position.set(Position.KEY_HDOP, parser.next());

            dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
            position.setTime(dateBuilder.getDate());

            return position;

        } else {

            Date time = null;
            if (parser.hasNext(6)) {
                time = parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY);
            }

            List<Position> positions = new LinkedList<>();
            Parser itemParser = new Parser(PATTERN_ITEM, parser.next());
            while (itemParser.find()) {
                positions.add(decodePosition(deviceSession, itemParser, time));
            }

            return positions;

        }
    }

}
