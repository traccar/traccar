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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class TrackboxProtocolDecoder extends BaseProtocolDecoder {

    public TrackboxProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(dd)(dd)(dd).(ddd),")       // time (hhmmss.sss)
            .number("(dd)(dd.dddd)([NS]),")      // latitude
            .number("(ddd)(dd.dddd)([EW]),")     // longitude
            .number("(d+.d),")                   // hdop
            .number("(-?d+.?d*),")               // altitude
            .number("(d),")                      // fix type
            .number("(d+.d+),")                  // course
            .number("d+.d+,")                    // speed (kph)
            .number("(d+.d+),")                  // speed (knots)
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(d+)")                      // satellites
            .compile();

    private void sendResponse(Channel channel, SocketAddress remoteAddress) {
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage("=OK=\r\n", remoteAddress));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("a=connect")) {
            String id = sentence.substring(sentence.indexOf("i=") + 2);
            if (getDeviceSession(channel, remoteAddress, id) != null) {
                sendResponse(channel, remoteAddress);
            }
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }
        sendResponse(channel, remoteAddress);

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        position.set(Position.KEY_HDOP, parser.nextDouble());

        position.setAltitude(parser.nextDouble(0));

        int fix = parser.nextInt(0);
        position.set(Position.KEY_GPS, fix);
        position.setValid(fix > 0);

        position.setCourse(parser.nextDouble(0));
        position.setSpeed(parser.nextDouble(0));

        dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        position.set(Position.KEY_SATELLITES, parser.nextInt());

        return position;
    }

}
