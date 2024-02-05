/*
 * Copyright 2014 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class MiniFinderProtocolDecoder extends BaseProtocolDecoder {

    public MiniFinderProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_FIX = new PatternBuilder()
            .number("(d+)/(d+)/(d+),")           // date (dd/mm/yy)
            .number("(d+):(d+):(d+),")           // time (hh:mm:ss)
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+),")                // longitude
            .compile();

    private static final Pattern PATTERN_STATE = new PatternBuilder()
            .number("(d+.?d*),")                 // speed (km/h)
            .number("(d+.?d*),")                 // course
            .number("(x+),")                     // flags
            .number("(-?d+.d+),")                // altitude (meters)
            .number("(d+),")                     // battery (percentage)
            .compile();

    private static final Pattern PATTERN_A = new PatternBuilder()
            .text("!A,")
            .expression(PATTERN_FIX.pattern())
            .any()                               // unknown 3 fields
            .compile();

   private static final Pattern PATTERN_C = new PatternBuilder()
            .text("!C,")
            .expression(PATTERN_FIX.pattern())
            .expression(PATTERN_STATE.pattern())
            .any()                               // unknown 3 fields
            .compile();

    private static final Pattern PATTERN_BD = new PatternBuilder()
            .expression("![BD],")                // B - buffered, D - live
            .expression(PATTERN_FIX.pattern())
            .expression(PATTERN_STATE.pattern())
            .number("(d+),")                     // satellites in use
            .number("(d+),")                     // satellites in view
            .number("(d+.?d*)")                  // hdop
            .compile();

    private void decodeFix(Position position, Parser parser) {

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setLatitude(parser.nextDouble(0));
        position.setLongitude(parser.nextDouble(0));
    }

    private void decodeFlags(Position position, int flags) {

        position.setValid(BitUtil.to(flags, 2) > 0);
        if (BitUtil.check(flags, 1)) {
            position.set(Position.KEY_APPROXIMATE, true);
        }

        if (BitUtil.check(flags, 2)) {
            position.set(Position.KEY_ALARM, Position.ALARM_FAULT);
        }
        if (BitUtil.check(flags, 6)) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        }
        if (BitUtil.check(flags, 7)) {
            position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
        }
        if (BitUtil.check(flags, 8)) {
            position.set(Position.KEY_ALARM, Position.ALARM_FALL_DOWN);
        }
        if (BitUtil.check(flags, 9) || BitUtil.check(flags, 10) || BitUtil.check(flags, 11)) {
            position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE);
        }
        if (BitUtil.check(flags, 12)) {
            position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
        }
        if (BitUtil.check(flags, 15) || BitUtil.check(flags, 14)) {
            position.set(Position.KEY_ALARM, Position.ALARM_MOVEMENT);
        }

        position.set(Position.KEY_RSSI, BitUtil.between(flags, 16, 21));
        position.set(Position.KEY_CHARGE, BitUtil.check(flags, 22));
    }

    private void decodeState(Position position, Parser parser) {

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));

        position.setCourse(parser.nextDouble(0));
        if (position.getCourse() > 360) {
            position.setCourse(0);
        }

        decodeFlags(position, parser.nextHexInt(0));

        position.setAltitude(parser.nextDouble(0));

        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt(0));
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("!1,")) {
            int index = sentence.indexOf(',', 3);
            if (index < 0) {
                index = sentence.length();
            }
            getDeviceSession(channel, remoteAddress, sentence.substring(3, index));
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null || !sentence.matches("![345A-D],.*")) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        String type = sentence.substring(1, 2);
        position.set(Position.KEY_TYPE, type);

        if (type.equals("3")) {

            getLastLocation(position, null);

            position.set(Position.KEY_RESULT, sentence.substring(3));

            return position;

        } else if (type.equals("4")) {

            String[] values = sentence.split(",");

            getLastLocation(position, null);

            for (int i = 1; i <= 3; i++) {
                if (!values[i + 1].isEmpty()) {
                    position.set("phone" + i, values[i + 1]);
                }
            }

            return position;

        } else if (type.equals("5")) {

            String[] values = sentence.split(",");

            getLastLocation(position, null);

            position.set(Position.KEY_RSSI, Integer.parseInt(values[1]));
            if (values.length >= 4) {
                position.set(Position.KEY_BATTERY_LEVEL, Integer.parseInt(values[3]));
            }

            return position;

        } else if (type.equals("B") || type.equals("D")) {

            Parser parser = new Parser(PATTERN_BD, sentence);
            if (!parser.matches()) {
                return null;
            }

            decodeFix(position, parser);
            decodeState(position, parser);

            position.set(Position.KEY_SATELLITES, parser.nextInt(0));
            position.set(Position.KEY_SATELLITES_VISIBLE, parser.nextInt(0));
            position.set(Position.KEY_HDOP, parser.nextDouble(0));

            return position;

        } else if (type.equals("C")) {

            Parser parser = new Parser(PATTERN_C, sentence);
            if (!parser.matches()) {
                return null;
            }

            decodeFix(position, parser);
            decodeState(position, parser);

            return position;

        } else if (type.equals("A")) {

            Parser parser = new Parser(PATTERN_A, sentence);
            if (!parser.matches()) {
                return null;
            }

            decodeFix(position, parser);

            return position;

        }

        return null;
    }

}
