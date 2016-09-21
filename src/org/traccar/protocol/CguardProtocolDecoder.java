/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class CguardProtocolDecoder extends BaseProtocolDecoder {

    public CguardProtocolDecoder(CguardProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("NV:")
            .number("(dd)(dd)(dd) ")             // date
            .number("(dd)(dd)(dd):")             // time
            .number("(-?d+.d+):")                // longitude
            .number("(-?d+.d+):")                // latitude
            .number("(d+.?d*):")                 // speed
            .number("(?:NAN|(d+.?d*)):")         // accuracy
            .number("(?:NAN|(d+.?d*)):")         // course
            .number("(?:NAN|(d+.?d*))")          // altitude
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("ID:") || sentence.startsWith("IDRO:")) {
            getDeviceSession(channel, remoteAddress, sentence.substring(sentence.indexOf(':') + 1));
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setValid(true);
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

        position.set(Position.KEY_ACCURACY, parser.nextDouble());

        position.setCourse(parser.nextDouble());
        position.setAltitude(parser.nextDouble());

        return position;
    }

}
