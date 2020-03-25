/*
 * Copyright 2012 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class FalcomProtocolDecoder extends BaseProtocolDecoder {

    public FalcomProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$T0001,")
            .number("(d+),")                        // event (int)
            .number("(dd).(dd).(dddd),")            // date (31.12.2019)
            .number("(dd):(dd):(dd),")              // time (11:22:33)
            .number("(-?d+.d+),")                   // latitude (d.d)
            .number("(-?d+.d+),")                   // longitude (d.d)
            .number("(d+),")                        // speed (int)
            .number("(d+.d+),")                     // course (double)
            .number("(d+),")                        // fix (int)
            .number("(d+),")                        // satellites (int)
            .number("(d+.d+),")                     // power (double)
            .number("(d+),")                        // ignition (int)
            .number("(x+)*")                        // ignition (int)
            //.expression("([0-9a-fA-F]{8})*")        // driverkey
            .any()
            .compile();

    private Position decodeT0001(DeviceSession deviceSession, String sentence) {

        Parser parser = new Parser(PATTERN, sentence);

        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());

        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_EVENT, parser.nextInt(0));
        DateBuilder dateBuilder = new DateBuilder()
            .setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0))
            .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setSpeed(parser.nextInt(0) * 1.94384449); //m/s to knot
        position.setCourse(Math.round(parser.nextDouble()));
        position.setValid(parser.nextInt() > 0);
        position.set(Position.KEY_SATELLITES, Math.round(parser.nextInt()));
        position.set(Position.KEY_POWER, parser.nextDouble());
        position.set(Position.KEY_IGNITION, parser.nextInt() == 1);
        position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        DeviceSession deviceSession;

        if (!sentence.startsWith("$") && sentence.contains("$")) {
            int index = sentence.indexOf("$");
            String id = sentence.substring(0, index);
            if (id.endsWith(",")) {
                id = id.substring(0, id.length() - 1);
            } else if (id.endsWith("/")) {
                id = id.substring(id.indexOf('/') + 1, id.length() - 1);
            }
            deviceSession = getDeviceSession(channel, remoteAddress, id);
            sentence = sentence.substring(index);
        } else {
            deviceSession = getDeviceSession(channel, remoteAddress);
        }

        if (sentence.startsWith("$IMEI")) {
            getDeviceSession(channel, remoteAddress, sentence.substring(6));
        } else if (sentence.startsWith("$T0001")) {
            return decodeT0001(deviceSession, sentence);
        }

        return null;
    }

}
