/*
 * Copyright 2014 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class VisiontekProtocolDecoder extends BaseProtocolDecoder {

    public VisiontekProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$1,")
            .expression("([^,]+),")              // identifier
            .number("(d+),").optional()          // imei
            .number("(dd),(dd),(dd),")           // date (dd,mm,yy)
            .number("(dd),(dd),(dd),")           // time (hh,mm,ss)
            .groupBegin()
            .number("(dd)(dd).?(d+)([NS]),")     // latitude
            .number("(ddd)(dd).?(d+)([EW]),")    // longitude
            .or()
            .number("(dd.d+)([NS]),")            // latitude
            .number("(ddd.d+)([EW]),")           // longitude
            .groupEnd()
            .number("(d+.?d+),")                 // speed
            .number("(d+),")                     // course
            .groupBegin()
            .number("(d+),")                     // altitude
            .number("(d+),")                     // satellites
            .number("(d+),")                     // odometer
            .number("([01]),")                   // ignition
            .number("([01]),")                   // input 1
            .number("([01]),")                   // input 2
            .number("([01]),")                   // immobilizer
            .number("([01]),")                   // external battery status
            .number("(d+),")                     // gsm
            .or()
            .number("(d+.d),")                   // hdop
            .number("(d+),")                     // altitude
            .number("(d+),")                     // odometer
            .number("([01],[01],[01],[01]),")    // input
            .number("([01],[01],[01],[01]),")    // output
            .number("(d+.?d*),")                 // adc 1
            .number("(d+.?d*),")                 // adc 2
            .groupEnd("?")
            .any()
            .expression("([AV])")                // validity
            .number(",(d{10})").optional()       // rfid
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next(), parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        if (parser.hasNext(8)) {
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_MIN_HEM));
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_MIN_HEM));
        }
        if (parser.hasNext(4)) {
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        }

        position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(
                parser.next().replace(".", "")) / 10));

        position.setCourse(parser.nextDouble(0));

        if (parser.hasNext(9)) {
            position.setAltitude(parser.nextDouble(0));
            position.set(Position.KEY_SATELLITES, parser.nextInt());
            position.set(Position.KEY_ODOMETER, parser.nextInt(0) * 1000);
            position.set(Position.KEY_IGNITION, parser.next().equals("1"));
            position.set(Position.PREFIX_IO + 1, parser.next());
            position.set(Position.PREFIX_IO + 2, parser.next());
            position.set("immobilizer", parser.next());
            position.set(Position.KEY_CHARGE, parser.next().equals("1"));
            position.set(Position.KEY_RSSI, parser.nextDouble());
        }

        if (parser.hasNext(7)) {
            position.set(Position.KEY_HDOP, parser.nextDouble());
            position.setAltitude(parser.nextDouble(0));
            position.set(Position.KEY_ODOMETER, parser.nextInt(0) * 1000);
            position.set(Position.KEY_INPUT, parser.next());
            position.set(Position.KEY_OUTPUT, parser.next());
            position.set(Position.PREFIX_ADC + 1, parser.next());
            position.set(Position.PREFIX_ADC + 2, parser.next());
        }

        position.setValid(parser.next().equals("A"));

        position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());

        return position;
    }

}
