/*
 * Copyright 2012 - 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class Gps103ProtocolDecoder extends BaseProtocolDecoder {

    public Gps103ProtocolDecoder(Gps103Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("imei:")
            .number("(d+),")                     // imei
            .expression("([^,]+),")              // alarm
            .number("(dd)/?(dd)/?(dd) ?")        // local date (yymmdd)
            .number("(dd):?(dd)(?:dd)?,")        // local time (hhmmss)
            .expression("([^,]+)?,")             // rfid
            .expression("[FL],")                 // full / low
            .groupBegin()
            .number("(dd)(dd)(dd).d+")           // time utc (hhmmss)
            .or()
            .number("(?:d{1,5}.d+)?")
            .groupEnd()
            .text(",")
            .expression("([AV]),")               // validity
            .expression("([NS]),").optional()
            .number("(d+)(dd.d+),")              // latitude (ddmm.mmmm)
            .expression("([NS]),").optional()
            .expression("([EW]),").optional()
            .number("(d+)(dd.d+),")              // longitude (dddmm.mmmm)
            .expression("([EW])?,").optional()
            .number("(d+.?d*)?,?")               // speed
            .number("(d+.?d*)?,?")               // course
            .number("(d+.?d*)?,?")               // altitude
            .expression("([^,;]+)?,?")
            .expression("([^,;]+)?,?")
            .expression("([^,;]+)?,?")
            .expression("([^,;]+)?,?")
            .expression("([^,;]+)?,?")
            .any()
            .compile();

    private static final Pattern PATTERN_NETWORK = new PatternBuilder()
            .text("imei:")
            .number("(d+),")                     // imei
            .expression("[^,]+,")                // alarm
            .number("d*,,")
            .text("L,,,")
            .number("(x+),,")                    // lac
            .number("(x+),,,")                   // cid
            .any()
            .compile();

    private static final Pattern PATTERN_HANDSHAKE = new PatternBuilder()
            .number("##,imei:(d+),A")
            .compile();

    private static final Pattern PATTERN_OBD = new PatternBuilder()
            .text("imei:")
            .number("(d+),")                     // imei
            .expression("OBD,")                  // type
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(d+),")                     // odometer
            .number("(d+.d+)?,")                 // fuel instant
            .number("(d+.d+)?,")                 // fuel average
            .number("(d+),")                     // hours
            .number("(d+),")                     // speed
            .number("d+.?d*%,")                  // power load
            .number("(d+),")                     // temperature
            .number("(d+.?d*%),")                // throttle
            .number("(d+),")                     // rpm
            .number("(d+.d+),")                  // battery
            .number("([^;]*)")                   // dtcs
            .any()
            .compile();

    private String decodeAlarm(String value) {
        if (value.startsWith("T:")) {
            return Position.ALARM_TEMPERATURE;
        } else if (value.startsWith("oil")) {
            return Position.ALARM_OIL_LEAK;
        }
        switch (value) {
            case "tracker":
                return null;
            case "help me":
                return Position.ALARM_SOS;
            case "low battery":
                return Position.ALARM_LOW_BATTERY;
            case "stockade":
                return Position.ALARM_GEOFENCE;
            case "move":
                return Position.ALARM_MOVEMENT;
            case "speed":
                return Position.ALARM_OVERSPEED;
            case "acc on":
                return Position.ALARM_POWER_ON;
            case "acc off":
                return Position.ALARM_POWER_OFF;
            case "door alarm":
                return Position.ALARM_DOOR;
            case "ac alarm":
                return Position.ALARM_POWER_CUT;
            case "accident alarm":
                return Position.ALARM_ACCIDENT;
            case "sensor alarm":
                return Position.ALARM_SHOCK;
            case "bonnet alarm":
                return Position.ALARM_BONNET;
            case "footbrake alarm":
                return Position.ALARM_FOOT_BRAKE;
            case "DTC":
                return Position.ALARM_FAULT;
            default:
                return null;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        // Send response #1
        if (sentence.contains("##")) {
            if (channel != null) {
                channel.write("LOAD", remoteAddress);
                Parser handshakeParser = new Parser(PATTERN_HANDSHAKE, sentence);
                if (handshakeParser.matches()) {
                    getDeviceSession(channel, remoteAddress, handshakeParser.next());
                }
            }
            return null;
        }

        // Send response #2
        if (!sentence.isEmpty() && Character.isDigit(sentence.charAt(0))) {
            if (channel != null) {
                channel.write("ON", remoteAddress);
            }
            int start = sentence.indexOf("imei:");
            if (start >= 0) {
                sentence = sentence.substring(start);
            } else {
                return null;
            }
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        Parser parser = new Parser(PATTERN_NETWORK, sentence);
        if (parser.matches()) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            position.setNetwork(new Network(
                    CellTower.fromLacCid(parser.nextHexInt(0), parser.nextHexInt(0))));

            return position;

        }

        parser = new Parser(PATTERN_OBD, sentence);
        if (parser.matches()) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, parser.nextDateTime());

            position.set(Position.KEY_ODOMETER, parser.nextInt(0));
            parser.nextDouble(0); // instant fuel consumption
            position.set(Position.KEY_FUEL_CONSUMPTION, parser.nextDouble(0));
            position.set(Position.KEY_HOURS, parser.nextInt(0));
            position.set(Position.KEY_OBD_SPEED, parser.nextInt(0));
            position.set(Position.PREFIX_TEMP + 1, parser.nextInt(0));
            position.set(Position.KEY_THROTTLE, parser.next());
            position.set(Position.KEY_RPM, parser.nextInt(0));
            position.set(Position.KEY_BATTERY, parser.nextDouble(0));
            position.set(Position.KEY_DTCS, parser.next().replace(',', ' ').trim());

            return position;

        }

        parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        String alarm = parser.next();
        position.set(Position.KEY_ALARM, decodeAlarm(alarm));
        if (alarm.equals("help me")) {
            if (channel != null) {
                channel.write("**,imei:" + imei + ",E;", remoteAddress);
            }
        } else if (alarm.equals("acc on")) {
            position.set(Position.KEY_IGNITION, true);
        } else if (alarm.equals("acc off")) {
            position.set(Position.KEY_IGNITION, false);
        } else if (alarm.startsWith("T:")) {
            position.set(Position.PREFIX_TEMP + 1, alarm.substring(2));
        } else if (alarm.startsWith("oil ")) {
            position.set("oil", alarm.substring(4));
        }

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        int localHours = parser.nextInt(0);
        int localMinutes = parser.nextInt(0);

        String rfid = parser.next();
        if (alarm.equals("rfid")) {
            position.set(Position.KEY_RFID, rfid);
        }

        String utcHours = parser.next();
        String utcMinutes = parser.next();

        dateBuilder.setTime(localHours, localMinutes, parser.nextInt(0));

        // Timezone calculation
        if (utcHours != null && utcMinutes != null) {
            int deltaMinutes = (localHours - Integer.parseInt(utcHours)) * 60;
            deltaMinutes += localMinutes - Integer.parseInt(utcMinutes);
            if (deltaMinutes <= -12 * 60) {
                deltaMinutes += 24 * 60;
            } else if (deltaMinutes > 12 * 60) {
                deltaMinutes -= 24 * 60;
            }
            dateBuilder.addMinute(-deltaMinutes);
        }
        position.setTime(dateBuilder.getDate());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN_HEM));
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));
        position.setAltitude(parser.nextDouble(0));

        for (int i = 1; i <= 5; i++) {
            position.set(Position.PREFIX_IO + i, parser.next());
        }

        return position;
    }

}
