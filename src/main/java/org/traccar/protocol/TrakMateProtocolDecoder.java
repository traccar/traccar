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

public class TrakMateProtocolDecoder extends BaseProtocolDecoder {

    public TrakMateProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_SRT = new PatternBuilder()
            .text("^TMSRT|")
            .expression("([^ ]+)|")              // uid
            .number("(d+.d+)|")                  // latitude
            .number("(d+.d+)|")                  // longitude
            .number("(dd)(dd)(dd)|")             // time (hhmmss)
            .number("(dd)(dd)(dd)|")             // date (ddmmyy)
            .number("(d+.d+)|")                  // software ver
            .number("(d+.d+)|")                  // Hardware ver
            .any()
            .compile();

    private static final Pattern PATTERN_PER = new PatternBuilder()
            .text("^TM")
            .expression("...|")                  // type
            .expression("([^ ]+)|")              // uid
            .number("(d+)|")                     // seq
            .number("(d+.d+)|")                  // latitude
            .number("(d+.d+)|")                  // longitude
            .number("(dd)(dd)(dd)|")             // time (hhmmss)
            .number("(dd)(dd)(dd)|")             // date (ddmmyy)
            .number("(d+.d+)|")                  // speed
            .number("(d+.d+)|")                  // heading
            .number("(d+)|").optional()          // satellites
            .number("([01])|")                   // ignition
            .groupBegin()
            .number("(d+)|")                     // dop1
            .number("(d+)|")                     // dop2
            .number("(d+.d+)|")                  // analog
            .number("(d+.d+)|")                  // internal battery
            .or()
            .number("-?d+ -?d+ -?d+|")           // accelerometer
            .number("([01])|")                   // movement
            .groupEnd()
            .number("(d+.d+)|")                  // vehicle battery
            .number("(d+.d+)|")                  // gps odometer
            .number("(d+.d+)|").optional()       // pulse odometer
            .number("([01])|")                   // main power status
            .number("([01])|")                   // gps data validity
            .number("([01])|")                   // live or cache
            .any()
            .compile();

    private static final Pattern PATTERN_ALT = new PatternBuilder()
            .text("^TMALT|")
            .expression("([^ ]+)|")              // uid
            .number("(d+)|")                     // seq
            .number("(d+)|")                     // Alert type
            .number("(d+)|")                     // Alert status
            .number("(d+.d+)|")                  // latitude
            .number("(d+.d+)|")                  // longitude
            .number("(dd)(dd)(dd)|")             // time (hhmmss)
            .number("(dd)(dd)(dd)|")             // date (ddmmyy)
            .number("(d+.d+)|")                  // speed
            .number("(d+.d+)|")                  // heading
            .any()
            .compile();

    private String decodeAlarm(int value) {
        return switch (value) {
            case 1 -> Position.ALARM_SOS;
            case 3 -> Position.ALARM_GEOFENCE;
            case 4 -> Position.ALARM_POWER_CUT;
            default -> null;
        };
    }

    private Object decodeSrt(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_SRT, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY));

        position.set(Position.KEY_VERSION_FW, parser.next());
        position.set(Position.KEY_VERSION_HW, parser.next());

        return position;
    }

    private Object decodeAlt(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_ALT, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        parser.next(); // seq
        position.addAlarm(decodeAlarm(parser.nextInt()));
        parser.next(); // alert status or data

        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY));

        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        return position;
    }

    private Object decodePer(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN_PER, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        parser.next(); // seq

        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY));

        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_IGNITION, parser.nextInt() > 0);

        if (parser.hasNext(4)) {
            position.set("dop1", parser.nextInt());
            position.set("dop2", parser.nextInt());
            position.set(Position.PREFIX_ADC + 1, parser.nextDouble());
            position.set(Position.KEY_BATTERY, parser.nextDouble());
        }

        if (parser.hasNext()) {
            position.set(Position.KEY_MOTION, parser.nextInt(0) > 0);
        }

        position.set(Position.KEY_POWER, parser.nextDouble());
        position.set(Position.KEY_ODOMETER, parser.nextDouble());
        position.set("pulseOdometer", parser.nextDouble());
        position.set(Position.KEY_STATUS, parser.nextInt());

        position.setValid(parser.nextInt() > 0);

        position.set(Position.KEY_ARCHIVE, parser.nextInt() > 0);

        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        int typeIndex = sentence.indexOf("^TM");
        if (typeIndex < 0) {
            return null;
        }

        String type = sentence.substring(typeIndex + 3, typeIndex + 6);
        return switch (type) {
            case "ALT" -> decodeAlt(channel, remoteAddress, sentence);
            case "SRT" -> decodeSrt(channel, remoteAddress, sentence);
            default -> decodePer(channel, remoteAddress, sentence);
        };
    }

}
