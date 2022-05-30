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
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class MegastekProtocolDecoder extends BaseProtocolDecoder {

    public MegastekProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_GPRMC = new PatternBuilder()
            .text("$GPRMC,")
            .number("(dd)(dd)(dd).(ddd),")       // time (hhmmss.sss)
            .expression("([AV]),")               // validity
            .number("(d+)(dd.d+),([NS]),")       // latitude
            .number("(d+)(dd.d+),([EW]),")       // longitude
            .number("(d+.d+)?,")                 // speed
            .number("(d+.d+)?,")                 // course
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .any()                               // checksum
            .compile();

    private static final Pattern PATTERN_SIMPLE = new PatternBuilder()
            .expression("[FL],")                 // flag
            .expression("([^,]*),")              // alarm
            .number("imei:(d+),")                // imei
            .number("(d+/?d*)?,")                // satellites
            .number("(d+.d+)?,")                 // altitude
            .number("Battery=(d+)%,,?")          // battery
            .number("(d)?,")                     // charger
            .number("(d+)?,")                    // mcc
            .number("(d+)?,")                    // mnc
            .number("(xxxx),")                   // lac
            .number("(xxxx);")                   // cid
            .any()                               // checksum
            .compile();

    private static final Pattern PATTERN_ALTERNATIVE = new PatternBuilder()
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(xxxx),")                   // lac
            .number("(xxxx),")                   // cid
            .number("(d+),")                     // gsm signal
            .number("(d+),")                     // battery
            .number("(d+),")                     // flags
            .number("(d+),")                     // inputs
            .number("(?:(d+),)?")                // outputs
            .number("(d.?d*),")                  // adc 1
            .groupBegin()
            .number("(d.dd),")                   // adc 2
            .number("(d.dd),")                   // adc 3
            .groupEnd("?")
            .expression("([^;]+);")              // alarm
            .any()                               // checksum
            .compile();

    private boolean parseLocation(String location, Position position) {

        Parser parser = new Parser(PATTERN_GPRMC, location);
        if (!parser.matches()) {
            return false;
        }

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        return true;
    }

    private Position decodeOld(Channel channel, SocketAddress remoteAddress, String sentence) {

        // Detect type
        boolean simple = sentence.charAt(3) == ',' || sentence.charAt(6) == ',';

        // Split message
        String id;
        String location;
        String status;
        if (simple) {

            int beginIndex = sentence.indexOf(',') + 1;
            int endIndex = sentence.indexOf(',', beginIndex);
            id = sentence.substring(beginIndex, endIndex);

            beginIndex = endIndex + 1;
            endIndex = sentence.indexOf('*', beginIndex);
            if (endIndex != -1) {
                endIndex += 3;
            } else {
                endIndex = sentence.length();
            }
            location = sentence.substring(beginIndex, endIndex);

            beginIndex = endIndex + 1;
            if (beginIndex > sentence.length()) {
                beginIndex = endIndex;
            }
            status = sentence.substring(beginIndex);

        } else {

            int beginIndex = 3;
            int endIndex = beginIndex + 16;
            id = sentence.substring(beginIndex, endIndex).trim();

            beginIndex = endIndex + 2;
            endIndex = sentence.indexOf('*', beginIndex) + 3;
            if (endIndex < 0) {
                return null;
            }
            location = sentence.substring(beginIndex, endIndex);

            beginIndex = endIndex + 1;
            status = sentence.substring(beginIndex);

        }

        Position position = new Position(getProtocolName());
        if (!parseLocation(location, position)) {
            return null;
        }

        if (simple) {

            Parser parser = new Parser(PATTERN_SIMPLE, status);
            if (parser.matches()) {

                position.set(Position.KEY_ALARM, decodeAlarm(parser.next()));

                DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next(), id);
                if (deviceSession == null) {
                    return null;
                }
                position.setDeviceId(deviceSession.getDeviceId());

                String sat = parser.next();
                if (sat.contains("/")) {
                    position.set(Position.KEY_SATELLITES, Integer.parseInt(sat.split("/")[0]));
                    position.set(Position.KEY_SATELLITES_VISIBLE, Integer.parseInt(sat.split("/")[1]));
                } else {
                    position.set(Position.KEY_SATELLITES, Integer.parseInt(sat));
                }

                position.setAltitude(parser.nextDouble(0));

                position.set(Position.KEY_BATTERY_LEVEL, parser.nextDouble(0));

                String charger = parser.next();
                if (charger != null) {
                    position.set(Position.KEY_CHARGE, Integer.parseInt(charger) == 1);
                }

                if (parser.hasNext(4)) {
                    position.setNetwork(new Network(CellTower.from(
                            parser.nextInt(0), parser.nextInt(0), parser.nextHexInt(0), parser.nextHexInt(0))));
                }

            } else {

                DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
                if (deviceSession == null) {
                    return null;
                }
                position.setDeviceId(deviceSession.getDeviceId());

            }

        } else {

            Parser parser = new Parser(PATTERN_ALTERNATIVE, status);
            if (parser.matches()) {

                DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
                if (deviceSession == null) {
                    return null;
                }
                position.setDeviceId(deviceSession.getDeviceId());

                position.setNetwork(new Network(CellTower.from(parser.nextInt(0), parser.nextInt(0),
                        parser.nextHexInt(0), parser.nextHexInt(0), parser.nextInt(0))));

                position.set(Position.KEY_BATTERY_LEVEL, parser.nextDouble());

                position.set(Position.KEY_FLAGS, parser.next());
                position.set(Position.KEY_INPUT, parser.next());
                position.set(Position.KEY_OUTPUT, parser.next());
                position.set(Position.PREFIX_ADC + 1, parser.next());
                position.set(Position.PREFIX_ADC + 2, parser.next());
                position.set(Position.PREFIX_ADC + 3, parser.next());
                position.set(Position.KEY_ALARM, decodeAlarm(parser.next()));

            }
        }

        return position;
    }

    private static final Pattern PATTERN_NEW = new PatternBuilder()
            .number("dddd").optional()
            .text("$MGV")
            .number("ddd,")
            .number("(d+),")                     // imei
            .expression("[^,]*,")                // name
            .expression("([RS]),")
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([AV]),")               // validity
            .number("(d+)(dd.d+),([NS]),")       // latitude
            .number("(d+)(dd.d+),([EW]),")       // longitude
            .number("dd,")
            .number("(dd),")                     // satellites
            .number("dd,")
            .number("(d+.d+)?,")                 // hdop
            .number("(d+.d+)?,")                 // speed
            .number("(d+.d+)?,")                 // course
            .number("(-?d+.d+)?,")               // altitude
            .number("(d+.d+)?,")                 // odometer
            .number("(d+)?,")                    // mcc
            .number("(d+)?,")                    // mnc
            .number("(xxxx)?,")                  // lac
            .number("(x+)?,")                    // cid
            .number("(d+)?,")                    // gsm
            .groupBegin()
            .number("([01]{4})?,")               // input
            .number("([01]{4})?,")               // output
            .number("(d+)?,")                    // adc1
            .number("(d+)?,")                    // adc2
            .number("(d+)?,")                    // adc3
            .or()
            .number("(d+),")                     // input
            .number("(d+),")                     // output
            .number("(d+),")                     // adc1
            .number("(d+),")                     // adc2
            .number("(d+),")                     // adc3
            .groupEnd()
            .groupBegin()
            .number("(-?d+.?d*)")                // temperature 1
            .or().text(" ")
            .groupEnd("?").text(",")
            .groupBegin()
            .number("(-?d+.?d*)")                // temperature 2
            .or().text(" ")
            .groupEnd("?").text(",")
            .number("(d+)?,")                    // rfid
            .number("([01])(d)?").optional()     // charge and belt status
            .expression("[^,]*,")
            .number("(d+)?,")                    // battery
            .expression("([^,]*)[,;]")           // alert
            .any()
            .compile();

    private Position decodeNew(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_NEW, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (parser.next().equals("S")) {
            position.set(Position.KEY_ARCHIVE, true);
        }

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        position.set(Position.KEY_SATELLITES, parser.nextInt(0));
        position.set(Position.KEY_HDOP, parser.nextDouble(0));

        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));
        position.setAltitude(parser.nextDouble(0));

        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextDouble(0) * 1000);
        }

        if (parser.hasNext(5)) {
            int mcc = parser.nextInt();
            int mnc = parser.nextInt();
            Integer lac = parser.nextHexInt();
            Integer cid = parser.nextHexInt();
            Integer rssi = parser.nextInt();
            if (lac != null && cid != null) {
                CellTower tower = CellTower.from(mcc, mnc, lac, cid);
                if (rssi != null) {
                    tower.setSignalStrength(rssi);
                }
                position.setNetwork(new Network(tower));
            }
        }

        if (parser.hasNext(5)) {
            position.set(Position.KEY_INPUT, parser.nextBinInt(0));
            position.set(Position.KEY_OUTPUT, parser.nextBinInt(0));
            for (int i = 1; i <= 3; i++) {
                position.set(Position.PREFIX_ADC + i, parser.nextInt(0));
            }
        }

        if (parser.hasNext(5)) {
            position.set(Position.KEY_HEART_RATE, parser.nextInt());
            position.set(Position.KEY_STEPS, parser.nextInt());
            position.set("activityTime", parser.nextInt());
            position.set("lightSleepTime", parser.nextInt());
            position.set("deepSleepTime", parser.nextInt());
        }

        for (int i = 1; i <= 2; i++) {
            String adc = parser.next();
            if (adc != null) {
                position.set(Position.PREFIX_TEMP + i, Double.parseDouble(adc));
            }
        }

        position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());

        if (parser.hasNext()) {
            position.set(Position.KEY_CHARGE, parser.nextInt() > 0);
        }
        if (parser.hasNext()) {
            position.set("belt", parser.nextInt());
        }

        String battery = parser.next();
        if (battery != null) {
            position.set(Position.KEY_BATTERY, Integer.parseInt(battery));
        }

        position.set(Position.KEY_ALARM, decodeAlarm(parser.next()));

        return position;
    }

    private String decodeAlarm(String value) {
        value = value.toLowerCase();
        if (value.startsWith("geo")) {
            if (value.endsWith("in")) {
                return Position.ALARM_GEOFENCE_ENTER;
            } else if (value.endsWith("out")) {
                return Position.ALARM_GEOFENCE_EXIT;
            }
        }
        switch (value) {
            case "pw on":
            case "poweron":
                return Position.ALARM_POWER_ON;
            case "poweroff":
                return Position.ALARM_POWER_OFF;
            case "sos":
            case "help":
                return Position.ALARM_SOS;
            case "over speed":
            case "overspeed":
                return Position.ALARM_OVERSPEED;
            case "lowspeed":
                return Position.ALARM_LOW_SPEED;
            case "low battery":
            case "lowbattery":
                return Position.ALARM_LOW_BATTERY;
            case "low extern voltage":
                return Position.ALARM_LOW_POWER;
            case "gps cut":
                return Position.ALARM_GPS_ANTENNA_CUT;
            case "vib":
                return Position.ALARM_VIBRATION;
            case "move in":
                return Position.ALARM_GEOFENCE_ENTER;
            case "move out":
                return Position.ALARM_GEOFENCE_EXIT;
            case "corner":
                return Position.ALARM_CORNERING;
            case "fatigue":
                return Position.ALARM_FATIGUE_DRIVING;
            case "psd":
                return Position.ALARM_POWER_CUT;
            case "psr":
                return Position.ALARM_POWER_RESTORED;
            case "hit":
                return Position.ALARM_VIBRATION;
            case "belt on":
            case "belton":
                return Position.ALARM_LOCK;
            case "belt off":
            case "beltoff":
                return Position.ALARM_UNLOCK;
            case "error":
                return Position.ALARM_FAULT;
            default:
                return null;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.contains("$MG")) {
            return decodeNew(channel, remoteAddress, sentence);
        } else {
            return decodeOld(channel, remoteAddress, sentence);
        }
    }

}
