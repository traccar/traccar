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
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class Stl060ProtocolDecoder extends BaseProtocolDecoder {

    public Stl060ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .any()
            .text("$1,")
            .number("(d+),")                     // imei
            .text("D001,")                       // type
            .expression("[^,]*,")                // vehicle
            .number("(dd)/(dd)/(dd),")           // date (dd/mm/yy)
            .number("(dd):(dd):(dd),")           // time (hh:mm:ss)
            .number("(dd)(dd).?(d+)([NS]),")     // latitude
            .number("(ddd)(dd).?(d+)([EW]),")    // longitude
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*),")                 // course
            .groupBegin()
            .number("(d+),")                     // odometer
            .number("(d+),")                     // Ignition
            .number("(d+),")                     // di1
            .number("(d+),")                     // di2
            .number("(d+),")                     // fuel
            .or()
            .expression("([01]),")               // charging
            .expression("([01]),")               // ignition
            .expression("0,0,")                  // reserved
            .number("(d+),")                     // di
            .expression("([^,]+),")              // rfid
            .number("(d+),")                     // odometer
            .number("(d+),")                     // temperature
            .number("(d+),")                     // fuel
            .expression("([01]),")               // accelerometer
            .expression("([01]),")               // do1
            .expression("([01]),")               // do2
            .groupEnd()
            .expression("([AV])")                // validity
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

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_MIN_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_MIN_HEM));
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        // Old format
        if (parser.hasNext(5)) {
            position.set(Position.KEY_ODOMETER, parser.nextInt(0));
            position.set(Position.KEY_IGNITION, parser.nextInt(0) == 1);
            position.set(Position.KEY_INPUT, parser.nextInt(0) + parser.nextInt(0) << 1);
            position.set(Position.KEY_FUEL_LEVEL, parser.nextInt(0));
        }

        // New format
        if (parser.hasNext(10)) {
            position.set(Position.KEY_CHARGE, parser.nextInt(0) == 1);
            position.set(Position.KEY_IGNITION, parser.nextInt(0) == 1);
            position.set(Position.KEY_INPUT, parser.nextInt(0));
            position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());
            position.set(Position.KEY_ODOMETER, parser.nextInt(0));
            position.set(Position.PREFIX_TEMP + 1, parser.nextInt(0));
            position.set(Position.KEY_FUEL_LEVEL, parser.nextInt(0));
            position.set(Position.KEY_ACCELERATION, parser.nextInt(0) == 1);
            position.set(Position.KEY_OUTPUT, parser.nextInt(0) + parser.nextInt(0) << 1);
        }

        position.setValid(parser.next().equals("A"));

        return position;
    }

}
