/*
 * Copyright 2013 - 2016 Anton Tananaev (anton@traccar.org)
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

public class MegastekProtocolDecoder extends BaseProtocolDecoder {

    public MegastekProtocolDecoder(MegastekProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_GPRMC = new PatternBuilder()
            .text("$GPRMC,")
            .number("(dd)(dd)(dd).d+,")          // time
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
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
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
            location = sentence.substring(beginIndex, endIndex);

            beginIndex = endIndex + 1;
            status = sentence.substring(beginIndex);

        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
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

                position.set(Position.KEY_SATELLITES, parser.next());

                position.setAltitude(parser.nextDouble());

                position.set(Position.KEY_POWER, parser.nextDouble());

                String charger = parser.next();
                if (charger != null) {
                    position.set(Position.KEY_CHARGE, Integer.parseInt(charger) == 1);
                }

                if (parser.hasNext(4)) {
                    position.setNetwork(new Network(CellTower.from(
                            parser.nextInt(), parser.nextInt(), parser.nextInt(16), parser.nextInt(16))));
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

                position.setNetwork(new Network(CellTower.from(
                        parser.nextInt(), parser.nextInt(), parser.nextInt(16), parser.nextInt(16), parser.nextInt())));

                position.set(Position.KEY_BATTERY, Double.parseDouble(parser.next()));

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
            .number("(dd)(dd)(dd),")             // time
            .expression("([AV]),")               // validity
            .number("(d+)(dd.d+),([NS]),")       // latitude
            .number("(d+)(dd.d+),([EW]),")       // longitude
            .number("dd,")
            .number("(dd),")                     // satellites
            .number("dd,")
            .number("(d+.d+),")                  // hdop
            .number("(d+.d+)?,")                 // speed
            .number("(d+.d+)?,")                 // course
            .number("(-?d+.d+),")                // altitude
            .number("(d+.d+)?,")                 // odometer
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(xxxx),")                   // lac
            .number("(xxxx),")                   // cid
            .number("(d+)?,")                    // gsm
            .expression("([01]+)?,")             // input
            .expression("([01]+)?,")             // output
            .number("(d+)?,")                    // adc1
            .number("(d+)?,")                    // adc2
            .number("(d+)?,")                    // adc3
            .groupBegin()
            .number("(-?d+.?d*)")                // temperature 1
            .or().text(" ")
            .groupEnd("?").text(",")
            .groupBegin()
            .number("(-?d+.?d*)")                // temperature 2
            .or().text(" ")
            .groupEnd("?").text(",")
            .number("(d+)?,")                    // rfid
            .number("d*,")
            .number("(d+)?,")                    // battery
            .expression("([^,]*);")              // alert
            .any()
            .compile();

    private Position decodeNew(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_NEW, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        if (parser.next().equals("S")) {
            position.set(Position.KEY_ARCHIVE, true);
        }

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_HDOP, parser.nextDouble());

        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());
        position.setAltitude(parser.nextDouble());

        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);
        }

        position.setNetwork(new Network(CellTower.from(
                parser.nextInt(), parser.nextInt(), parser.nextInt(16), parser.nextInt(16), parser.nextInt())));

        position.set(Position.KEY_INPUT, parser.nextInt(2));
        position.set(Position.KEY_OUTPUT, parser.nextInt(2));

        for (int i = 1; i <= 3; i++) {
            position.set(Position.PREFIX_ADC + i, parser.nextInt());
        }

        for (int i = 1; i <= 2; i++) {
            String adc = parser.next();
            if (adc != null) {
                position.set(Position.PREFIX_TEMP + i, Double.parseDouble(adc));
            }
        }

        position.set(Position.KEY_RFID, parser.next());

        String battery = parser.next();
        if (battery != null) {
            position.set(Position.KEY_BATTERY, Integer.parseInt(battery));
        }

        position.set(Position.KEY_ALARM, decodeAlarm(parser.next()));

        return position;
    }

    private String decodeAlarm(String value) {
        switch (value) {
            case "SOS":
            case "Help":
                return Position.ALARM_SOS;
            case "Over Speed":
            case "OverSpeed":
                return Position.ALARM_OVERSPEED;
            case "LowSpeed":
                return Position.ALARM_LOW_SPEED;
            case "Low Battery":
            case "LowBattery":
                return Position.ALARM_LOW_BATTERY;
            case "VIB":
                return Position.ALARM_VIBRATION;
            case "Move in":
            case "Geo in":
            case "Geo1 in":
            case "Geo2 in":
                return Position.ALARM_GEOFENCE_ENTER;
            case "Move out":
            case "Geo out":
            case "Geo1 out":
            case "Geo2 out":
                return Position.ALARM_GEOFENCE_EXIT;
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
