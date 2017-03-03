/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class Tt8850ProtocolDecoder extends BaseProtocolDecoder {

    public Tt8850ProtocolDecoder(Tt8850Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .binary("0004,")
            .number("xxxx,")
            .expression("[01],")
            .expression("GT...,")
            .number("(?:[0-9A-Z]{2}xxxx)?,")     // protocol version
            .expression("([^,]+),")              // imei
            .any()
            .number("(d{1,2})?,")                // gps accuracy
            .number("(d{1,3}.d)?,")              // speed
            .number("(d{1,3})?,")                // course
            .number("(-?d{1,5}.d)?,")            // altitude
            .number("(-?d{1,3}.d{6}),")          // longitude
            .number("(-?d{1,2}.d{6}),")          // latitude
            .number("(dddd)(dd)(dd)")            // date
            .number("(dd)(dd)(dd),")             // time
            .number("(0ddd)?,")                  // mcc
            .number("(0ddd)?,")                  // mnc
            .number("(xxxx)?,")                  // lac
            .number("(xxxx)?,")                  // cell
            .any()
            .number("(dddd)(dd)(dd)")            // date
            .number("(dd)(dd)(dd),")             // time
            .number("(xxxx)")
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

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        position.setValid(true);
        position.setAccuracy(parser.nextInt());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());
        position.setAltitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setLatitude(parser.nextDouble());

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        if (parser.hasNext(4)) {
            position.setNetwork(new Network(
                    CellTower.from(parser.nextInt(), parser.nextInt(), parser.nextInt(16), parser.nextInt(16))));
        }

        return position;
    }

}
