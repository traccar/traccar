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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.regex.Pattern;

public class WondexProtocolDecoder extends BaseProtocolDecoder {

    public WondexProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("[^d]*")                     // header
            .number("(d+),")                     // device identifier
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(-?d+.d+),")                // longitude
            .number("(-?d+.d+),")                // latitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(-?d+.?d*),")               // altitude
            .number("(d+),")                     // satellites
            .number("(d+),?")                    // event
            .number("(d+.d+)V,").optional()      // battery
            .number("(d+.d+)?,?")                // odometer
            .number("(d+)?,?")                   // input
            .number("(d+.d+)?,?")                // adc1
            .number("(d+.d+)?,?")                // adc2
            .number("(d+)?")                     // output
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getUnsignedByte(0) == 0xD0) {

            long deviceId = ((Long.reverseBytes(buf.getLong(0))) >> 32) & 0xFFFFFFFFL;
            getDeviceSession(channel, remoteAddress, String.valueOf(deviceId));

            return null;
        } else if (buf.toString(StandardCharsets.US_ASCII).startsWith("$OK:")
                || buf.toString(StandardCharsets.US_ASCII).startsWith("$ERR:")
                || buf.toString(StandardCharsets.US_ASCII).startsWith("$MSG:")) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            getLastLocation(position, new Date());
            position.set(Position.KEY_RESULT, buf.toString(StandardCharsets.US_ASCII));

            return position;

        } else {

            Parser parser = new Parser(PATTERN, buf.toString(StandardCharsets.US_ASCII));
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position(getProtocolName());

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());

            position.setTime(parser.nextDateTime());

            position.setLongitude(parser.nextDouble(0));
            position.setLatitude(parser.nextDouble(0));
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
            position.setCourse(parser.nextDouble(0));
            position.setAltitude(parser.nextDouble(0));

            int satellites = parser.nextInt(0);
            position.setValid(satellites != 0);
            position.set(Position.KEY_SATELLITES, satellites);

            position.set(Position.KEY_EVENT, parser.next());
            position.set(Position.KEY_BATTERY, parser.nextDouble());
            if (parser.hasNext()) {
                position.set(Position.KEY_ODOMETER, parser.nextDouble(0) * 1000);
            }
            position.set(Position.KEY_INPUT, parser.next());
            position.set(Position.PREFIX_ADC + 1, parser.next());
            position.set(Position.PREFIX_ADC + 2, parser.next());
            position.set(Position.KEY_OUTPUT, parser.next());

            return position;

        }

    }

}
