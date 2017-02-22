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
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class CguardProtocolDecoder extends BaseProtocolDecoder {

    public CguardProtocolDecoder(CguardProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_NV = new PatternBuilder()
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

    private static final Pattern PATTERN_BC = new PatternBuilder()
            .text("BC:")
            .number("(dd)(dd)(dd) ")             // date
            .number("(dd)(dd)(dd):")             // time
            .expression("(.+)")                  // data
            .compile();

    private Position decodePosition(DeviceSession deviceSession, String sentence) {

        Parser parser = new Parser(PATTERN_NV, sentence);
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

        position.setAccuracy(parser.nextDouble());

        position.setCourse(parser.nextDouble());
        position.setAltitude(parser.nextDouble());

        return position;
    }

    private Position decodeStatus(DeviceSession deviceSession, String sentence) {

        Parser parser = new Parser(PATTERN_BC, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

        getLastLocation(position, dateBuilder.getDate());

        String[] data = parser.next().split(":");
        for (int i = 0; i < data.length / 2; i++) {
            String key = data[i * 2];
            String value = data[i * 2 + 1];
            switch (key) {
                case "CSQ1":
                    position.set(Position.KEY_RSSI, Integer.parseInt(value));
                    break;
                case "NSQ1":
                    position.set(Position.KEY_SATELLITES, Integer.parseInt(value));
                    break;
                case "BAT1":
                    position.set(Position.KEY_BATTERY, Integer.parseInt(value) + "%");
                    break;
                case "PWR1":
                    position.set(Position.KEY_POWER, Double.parseDouble(value));
                    break;
                default:
                    position.set(key.toLowerCase(), value);
                    break;
            }
        }

        return position;
    }

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

        if (sentence.startsWith("NV:")) {
            return decodePosition(deviceSession, sentence);
        } else if (sentence.startsWith("BC:")) {
            return decodeStatus(deviceSession, sentence);
        }

        return null;
    }

}
