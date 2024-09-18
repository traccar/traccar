/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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

public class ArknavProtocolDecoder extends BaseProtocolDecoder {

    public ArknavProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(d+),")                     // imei
            .expression(".{6},")                 // id code
            .number("ddd,")                      // status
            .number("Lddd,")                     // version
            .expression("([AV]),")               // validity
            .number("(dd)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(ddd)(dd.d+),")             // longitude
            .expression("([EW]),")
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*),")                 // course
            .number("(d+.?d*),")                 // hdop
            .number("(dd):(dd):(dd) ")           // time (hh:mm:ss)
            .number("(dd)-(dd)-(dd),")           // date (dd-mm-yy)
            .any()
            .compile();

    private static final Pattern PATTERN_R33 = new PatternBuilder()
            .number("(d+),")                     // imei
            .expression(".{6},")                 // id code
            .number("ddd,")                      // status code
            .text("PT33,")                       // model
            .expression("([AV]),")               // Status
            .number("(dd)(dd.d+),")              // latitude (ddmm.mmmm)
            .expression("([NS]),")
            .number("(ddd)(dd.d+),")             // longitude (dddmm.mmmm)
            .expression("([EW]),")
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*),")                 // course
            .number("(d+.?d*),")                 // hdop
            .number("(dd):(dd):(dd) ")           // time (hh:mm:ss)
            .number("(dd)-(dd)-(dd),")           // date (dd-mm-yy)
            .number("(d+.?d*),")                 // Unit Version number
            .number("(d+)")                      // Battery level
            .any()
            .compile();

    private Position decodeR9(Channel channel, SocketAddress remoteAddress, Object msg) {

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

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        position.set(Position.KEY_HDOP, parser.nextDouble(0));

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY));

        return position;
    }
    private Position decodeR33(Channel channel, SocketAddress remoteAddress, Object msg) {

        Parser parser = new Parser(PATTERN_R33, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        position.set(Position.KEY_HDOP, parser.nextDouble(0));

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY));
        parser.next();
        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.indexOf("PT33", 20) > 0) {
            return decodeR33(channel, remoteAddress, sentence);
        } else {
            return decodeR9(channel, remoteAddress, sentence);
        }

    }

}
