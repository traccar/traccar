/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class FlextrackProtocolDecoder extends BaseProtocolDecoder {

    public FlextrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_LOGON = new PatternBuilder()
            .number("(-?d+),")                   // index
            .text("LOGON,")
            .number("(d+),")                     // node id
            .number("(d+)")                      // iccid
            .compile();

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(-?d+),")                   // index
            .text("UNITSTAT,")
            .number("(dddd)(dd)(dd),")           // date (yyyymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("d+,")                       // node id
            .number("([NS])(d+).(d+.d+),")       // latitude
            .number("([EW])(d+).(d+.d+),")       // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(d+),")                     // satellites
            .number("(d+),")                     // battery
            .number("(-?d+),")                   // gsm
            .number("(x+),")                     // state
            .number("(ddd)")                     // mcc
            .number("(dd),")                     // mnc
            .number("(-?d+),")                   // altitude
            .number("(d+),")                     // hdop
            .number("(x+),")                     // cell
            .number("d+,")                       // gps fix time
            .number("(x+),")                     // lac
            .number("(d+)")                      // odometer
            .compile();

    private void sendAcknowledgement(Channel channel, SocketAddress remoteAddress, String index) {
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(index + ",ACK\r", remoteAddress));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.contains("LOGON")) {

            Parser parser = new Parser(PATTERN_LOGON, sentence);
            if (!parser.matches()) {
                return null;
            }

            sendAcknowledgement(channel, remoteAddress, parser.next());

            String id = parser.next();
            String iccid = parser.next();

            getDeviceSession(channel, remoteAddress, iccid, id);

        } else if (sentence.contains("UNITSTAT")) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Parser parser = new Parser(PATTERN, sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            sendAcknowledgement(channel, remoteAddress, parser.next());

            position.setTime(parser.nextDateTime());

            position.setValid(true);
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt(0)));
            position.setCourse(parser.nextInt(0));

            position.set(Position.KEY_SATELLITES, parser.nextInt(0));
            position.set(Position.KEY_BATTERY, parser.nextInt(0));
            int rssi = parser.nextInt(0);
            position.set(Position.KEY_STATUS, parser.nextHexInt(0));

            int mcc = parser.nextInt(0);
            int mnc = parser.nextInt(0);

            position.setAltitude(parser.nextInt(0));

            position.set(Position.KEY_HDOP, parser.nextInt(0) * 0.1);

            position.setNetwork(new Network(CellTower.from(
                    mcc, mnc, parser.nextHexInt(0), parser.nextHexInt(0), rssi)));

            position.set(Position.KEY_ODOMETER, parser.nextInt(0));

            return position;
        }

        return null;
    }

}
