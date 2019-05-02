/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class TechTltProtocolDecoder extends BaseProtocolDecoder {

    public TechTltProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_POSITION = new PatternBuilder()
            .number("(d+)")                      // id
            .text("*POS=Y,")
            .number("(dd):(dd):(dd),")           // time
            .number("(dd)/(dd)/(dd),")           // date
            .number("(dd)(dd.d+)")               // latitude
            .expression("([NS]),")
            .number("(ddd)(dd.d+)")              // longitude
            .expression("([EW]),")
            .number("(d+.d+),")                  // speed
            .number("(d+.d+),")                  // course
            .number("(d+.d+),")                  // altitude
            .number("(d+),")                     // satellites
            .number("(d+),")                     // lac
            .number("(d+)")                      // cid
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN_POSITION, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setValid(true);
        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());
        position.setAltitude(parser.nextDouble());

        position.set(Position.KEY_SATELLITES, parser.nextInt());

        position.setNetwork(new Network(CellTower.fromLacCid(parser.nextInt(), parser.nextInt())));

        return position;
    }

}
