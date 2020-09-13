/*
 * Copyright 2013 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class GpsGateProtocolDecoder extends BaseProtocolDecoder {

    public GpsGateProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_GPRMC = new PatternBuilder()
            .text("$GPRMC,")
            .number("(dd)(dd)(dd).?d*,")         // time (hhmmss)
            .expression("([AV]),")               // validity
            .number("(dd)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(ddd)(dd.d+),")             // longitude
            .expression("([EW]),")
            .number("(d+.d+)?,")                 // speed
            .number("(d+.d+)?,")                 // course
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .any()
            .compile();

    private static final Pattern PATTERN_FRCMD = new PatternBuilder()
            .text("$FRCMD,")
            .number("(d+),")                     // imei
            .expression("[^,]*,")                // command
            .expression("[^,]*,")
            .number("(d+)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d+)(dd.d+),")              // longitude
            .expression("([EW]),")
            .number("(d+.?d*),")                 // altitude
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*)?,")                // course
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd).?d*,")         // time (hhmmss)
            .expression("([01])")                // validity
            .any()
            .compile();

    private void send(Channel channel, SocketAddress remoteAddress, String message) {
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    message + Checksum.nmea(message.substring(1)) + "\r\n", remoteAddress));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("$FRLIN,")) {

            int beginIndex = sentence.indexOf(',', 7);
            if (beginIndex != -1) {
                beginIndex += 1;
                int endIndex = sentence.indexOf(',', beginIndex);
                if (endIndex != -1) {
                    String imei = sentence.substring(beginIndex, endIndex);
                    DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
                    if (deviceSession != null) {
                        if (channel != null) {
                            send(channel, remoteAddress, "$FRSES," + channel.id().asShortText());
                        }
                    } else {
                        send(channel, remoteAddress, "$FRERR,AuthError,Unknown device");
                    }
                } else {
                    send(channel, remoteAddress, "$FRERR,AuthError,Parse error");
                }
            } else {
                send(channel, remoteAddress, "$FRERR,AuthError,Parse error");
            }

        } else if (sentence.startsWith("$FRVER,")) {

            send(channel, remoteAddress, "$FRVER,1,0,GpsGate Server 1.0");

        } else if (sentence.startsWith("$GPRMC,")) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Parser parser = new Parser(PATTERN_GPRMC, sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

            position.setValid(parser.next().equals("A"));
            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());
            position.setSpeed(parser.nextDouble(0));
            position.setCourse(parser.nextDouble(0));

            dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
            position.setTime(dateBuilder.getDate());

            return position;

        } else if (sentence.startsWith("$FRCMD,")) {

            Parser parser = new Parser(PATTERN_FRCMD, sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position(getProtocolName());

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());

            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());
            position.setAltitude(parser.nextDouble(0));
            position.setSpeed(parser.nextDouble(0));
            position.setCourse(parser.nextDouble(0));

            position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

            position.setValid(parser.next().equals("1"));

            return position;

        }

        return null;
    }

}
