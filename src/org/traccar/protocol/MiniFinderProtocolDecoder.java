/*
 * Copyright 2014 - 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class MiniFinderProtocolDecoder extends BaseProtocolDecoder {

    public MiniFinderProtocolDecoder(MiniFinderProtocol protocol) {
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

    private static final Pattern PATTERN_GPS_PRECISION = new PatternBuilder()
            .number("(d+),")                     // satellites in use
            .number("(d+),")                     // satellites in view
            .number("(d+.?d*)")                  // hdop
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

    // The !B (buffered data) records are the same as !D (live data) records.
    private static final Pattern PATTERN_BD = new PatternBuilder()
            .expression("![BD],")
            .expression(PATTERN_FIX.pattern())
            .expression(PATTERN_STATE.pattern())
            .expression(PATTERN_GPS_PRECISION.pattern())
            .compile();

    private void decodeFix(Position position, Parser parser) {

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
    }

    private void decodeFlags(Position position, int flags) {

        position.setValid(BitUtil.check(flags, 0));

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

        position.set(Position.KEY_RSSI, BitUtil.between(flags, 16, 20));
        position.set(Position.KEY_CHARGE, BitUtil.check(flags, 22));
    }

    private void decodeState(Position position, Parser parser) {

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

        position.setCourse(parser.nextDouble());
        if (position.getCourse() > 360) {
            position.setCourse(0);
        }

        decodeFlags(position, parser.nextInt(16));

        position.setAltitude(parser.nextDouble());

        position.set(Position.KEY_BATTERY, parser.nextInt());
    }

    private void decodeGPSPrecision(Position position, Parser parser) {

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_SATELLITES_VISIBLE, parser.nextInt());
        position.set(Position.KEY_HDOP, parser.nextDouble());
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("!1,")) {

            getDeviceSession(channel, remoteAddress, sentence.substring(3, sentence.length()));

            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        if (!sentence.matches("![A-D],.*")) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        String recordType = sentence.substring(1, 2);
        position.set(Position.KEY_TYPE, recordType);

        if (recordType.matches("[BD]")) {
            Parser parser = new Parser(PATTERN_BD, sentence);
            if (!parser.matches()) {
                return null;
            }

            decodeFix(position, parser);
            decodeState(position, parser);
            decodeGPSPrecision(position, parser);

            return position;
        }

        if (recordType.matches("C")) {
            Parser parser = new Parser(PATTERN_C, sentence);
            if (!parser.matches()) {
                return null;
            }

            decodeFix(position, parser);
            decodeState(position, parser);

            return position;
        }

        if (recordType.matches("A")) {
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
