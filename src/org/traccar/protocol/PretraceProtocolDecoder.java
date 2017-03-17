/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class PretraceProtocolDecoder extends BaseProtocolDecoder {

    public PretraceProtocolDecoder(PretraceProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("(")
            .number("(d{15})")                   // imei
            .number("Uddd")                      // type
            .number("d")                         // gps type
            .expression("([AV])")                // validity
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .number("(dd)(dd.dddd)")             // latitude
            .expression("([NS])")
            .number("(ddd)(dd.dddd)")            // longitude
            .expression("([EW])")
            .number("(ddd)")                     // speed
            .number("(ddd)")                     // course
            .number("(xxx)")                     // altitude
            .number("(x{8})")                    // odometer
            .number("(x)")                       // satellites
            .number("(dd)")                      // hdop
            .number("(dd)")                      // gsm
            .expression("(.{8})")                // state
            .any()
            .text("^")
            .number("xx")                        // checksum
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

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setValid(parser.next().equals("A"));

        position.setTime(parser.nextDateTime());

        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());
        position.setAltitude(parser.nextInt(16));

        position.set(Position.KEY_ODOMETER, parser.nextInt(16));
        position.set(Position.KEY_SATELLITES, parser.nextInt(16));
        position.set(Position.KEY_HDOP, parser.nextInt());
        position.set(Position.KEY_RSSI, parser.nextInt());

        return position;
    }

}
