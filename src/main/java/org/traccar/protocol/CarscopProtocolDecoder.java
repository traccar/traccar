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
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class CarscopProtocolDecoder extends BaseProtocolDecoder {

    public CarscopProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("*")
            .any()
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .expression("([AV])")                // validity
            .number("(dd)(dd.dddd)")             // latitude
            .expression("([NS])")
            .number("(ddd)(dd.dddd)")            // longitude
            .expression("([EW])")
            .number("(ddd.d)")                   // speed
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(ddd.dd)")                  // course
            .groupBegin()
            .number("(d{8})")                    // state
            .number("L(d{6})")                   // odometer
            .groupEnd("?")
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        DeviceSession deviceSession;
        int index = sentence.indexOf("UB05");
        if (index != -1) {
            String imei = sentence.substring(index + 4, index + 4 + 15);
            deviceSession = getDeviceSession(channel, remoteAddress, imei);
        } else {
            deviceSession = getDeviceSession(channel, remoteAddress);
        }
        if (deviceSession == null) {
            return null;
        }

        Parser parser = new Parser(PATTERN, sentence);
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

        dateBuilder.setDate(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        position.setCourse(parser.nextDouble(0));

        if (parser.hasNext(2)) {
            position.set(Position.KEY_STATUS, parser.next());
            position.set(Position.KEY_ODOMETER, parser.nextInt(0));
        }

        return position;
    }

}
