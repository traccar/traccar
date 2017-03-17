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
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class TrakMateProtocolDecoder extends BaseProtocolDecoder {

    private final TimeZone timeZone = TimeZone.getTimeZone("UTC");

    public TrakMateProtocolDecoder(TrakMateProtocol protocol) {
        super(protocol);
        timeZone.setRawOffset(Context.getConfig().getInteger(getProtocolName() + ".timezone") * 1000);
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
            .text("^TMPER|")
            .expression("([^ ]+)|")              // uid
            .number("(d+)|")                     // seq
            .number("(d+.d+)|")                  // latitude
            .number("(d+.d+)|")                  // longitude
            .number("(dd)(dd)(dd)|")             // time (hhmmss)
            .number("(dd)(dd)(dd)|")             // date (ddmmyy)
            .number("(d+.d+)|")                  // speed
            .number("(d+.d+)|")                  // heading
            .number("(d+)|")                     // ignition
            .number("(d+)|")                     // dop1
            .number("(d+)|")                     // dop2
            .number("(d+.d+)|")                  // analog
            .number("(d+.d+)|")                  // internal battery
            .number("(d+.d+)|")                  // vehicle battery
            .number("(d+.d+)|")                  // gps odometer
            .number("(d+.d+)|")                  // pulse odometer
            .number("(d+)|")                     // main power status
            .number("(d+)|")                     // gps data validity
            .number("(d+)|")                     // live or cache
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
        switch (value) {
            case 1:
                return Position.ALARM_SOS;
            case 3:
                return Position.ALARM_GEOFENCE;
            case 4:
                return Position.ALARM_POWER_CUT;
            default:
                return null;
        }
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

        Position position = new Position();
        position.setProtocol(getProtocolName());
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

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        parser.next(); // seq
        position.set(Position.KEY_ALARM, decodeAlarm(parser.nextInt()));
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

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        parser.next(); // seq

        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY));

        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        position.set(Position.KEY_IGNITION, parser.nextInt() == 1);
        position.set("dop1", parser.next());
        position.set("dop2", parser.next());
        position.set(Position.KEY_INPUT, parser.next());
        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.KEY_POWER, parser.next());
        position.set(Position.KEY_ODOMETER, parser.nextDouble());
        position.set("pulseOdometer", parser.next());
        position.set(Position.KEY_STATUS, parser.nextInt());

        position.setValid(parser.nextInt() != 0);

        position.set(Position.KEY_ARCHIVE, parser.nextInt() == 1);

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
        switch (type) {
            case "ALT":
                return decodeAlt(channel, remoteAddress, sentence);
            case "SRT":
                return decodeSrt(channel, remoteAddress, sentence);
            case "PER":
                return decodePer(channel, remoteAddress, sentence);
            default:
                return null;
        }
    }

}
