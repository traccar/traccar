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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class StarLinkProtocolDecoder extends BaseProtocolDecoder {

    public StarLinkProtocolDecoder(StarLinkProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .expression(".")                     // protocol head
            .text("SLU")                         // message head
            .number("(x{6}|d{15}),")             // id
            .number("(d+),")                     // type
            .number("(d+),")                     // index
            .number("(dd)(dd)(dd)")              // event date
            .number("(dd)(dd)(dd),")             // event time
            .number("(d+),")                     // event
            .number("(dd)(dd)(dd)")              // fix date
            .number("(dd)(dd)(dd),")             // fix time
            .number("([-+])(dd)(dd.d+),")        // latitude
            .number("([-+])(ddd)(dd.d+),")       // longitude
            .number("(d+.d+),")                  // speed
            .number("(d+),")                     // course
            .number("(d+),")                     // odometer
            .number("(d+),")                     // lac
            .number("(d+),")                     // cid
            .number("(d+.d+),")                  // power
            .number("(d+.d+)")                   // battery
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

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_TYPE, parser.nextInt());
        position.set(Position.KEY_INDEX, parser.nextInt());

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setDeviceTime(dateBuilder.getDate());

        position.set(Position.KEY_EVENT, parser.nextInt());

        dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setFixTime(dateBuilder.getDate());

        position.setValid(true);
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));

        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextInt());

        position.set(Position.KEY_ODOMETER, parser.nextInt());

        position.setNetwork(new Network(CellTower.fromLacCid(parser.nextInt(), parser.nextInt())));

        position.set(Position.KEY_POWER, parser.nextDouble());
        position.set(Position.KEY_BATTERY, parser.nextDouble());

        return position;
    }

}
