/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class ArmoliProtocolDecoder extends BaseProtocolDecoder {

    public ArmoliProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("[M")                          // start
            .number("(d{15})")                   // imei
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .number("([NS])(dd.d{6})")           // latitude
            .number("([EW])(ddd.d{6})")          // longitude
            .number("(d)")                       // valid
            .number("(x)")                       // satellites
            .number("(xx)")                      // speed
            .number("(ddd)")                     // course
            .number("(xxx)")                     // adc 1
            .number("(xxx)")                     // adc 2
            .number("(xx)")                      // status
            .number("(xx)")                      // max speed
            .number("(x{6})")                    // distance
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        char type = sentence.charAt(1);

        if (type != 'M') {
            if (channel != null && (type == 'Q' || type == 'L')) {
                channel.writeAndFlush(new NetworkMessage("[TX,];;", remoteAddress));
            }
            return null;
        }

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
        position.setValid(parser.nextInt() > 0);

        position.set(Position.KEY_SATELLITES, parser.nextHexInt());

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextHexInt()));
        position.setCourse(parser.nextInt());

        position.set(Position.PREFIX_ADC + 1, parser.nextHexInt() / 27.0 * 1000);
        position.set(Position.PREFIX_ADC + 1, parser.nextHexInt() / 27.0 * 1000);
        position.set(Position.KEY_STATUS, parser.nextHexInt());
        position.set("maxSpeed", parser.nextHexInt());
        position.set(Position.KEY_ODOMETER, parser.nextHexInt());

        return position;
    }

}
