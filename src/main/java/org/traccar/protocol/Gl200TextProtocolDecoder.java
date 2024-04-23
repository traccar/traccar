/*
 * Copyright 2012 - 2024 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.config.Keys;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DataConverter;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Gl200TextProtocolDecoder extends BaseProtocolDecoder {

    private static final Map<String, String> PROTOCOL_MODELS = Map.ofEntries(
            Map.entry("02", "GL200"),
            Map.entry("04", "GV200"),
            Map.entry("06", "GV300"),
            Map.entry("08", "GMT100"),
            Map.entry("09", "GV50P"),
            Map.entry("0F", "GV55"),
            Map.entry("10", "GV55 LITE"),
            Map.entry("11", "GL500"),
            Map.entry("1A", "GL300"),
            Map.entry("1F", "GV500"),
            Map.entry("25", "GV300"),
            Map.entry("27", "GV300W"),
            Map.entry("28", "GL300VC"),
            Map.entry("2C", "GL300W"),
            Map.entry("2F", "GV55"),
            Map.entry("30", "GL300"),
            Map.entry("31", "GV65"),
            Map.entry("35", "GV200"),
            Map.entry("36", "GV500"),
            Map.entry("3F", "GMT100"),
            Map.entry("40", "GL500"),
            Map.entry("41", "GV75W"),
            Map.entry("42", "GT501"),
            Map.entry("44", "GL530"),
            Map.entry("45", "GB100"),
            Map.entry("50", "GV55W"),
            Map.entry("52", "GL50"),
            Map.entry("55", "GL50B"),
            Map.entry("5E", "GV500MAP"),
            Map.entry("6E", "GV310LAU"),
            Map.entry("BD", "CV200"),
            Map.entry("C2", "GV600M"),
            Map.entry("DC", "GV600MG"),
            Map.entry("DE", "GL500M"),
            Map.entry("F1", "GV350M"),
            Map.entry("F8", "GV800W"),
            Map.entry("FC", "GV600W"),
            Map.entry("802004", "GV58LAU"),
            Map.entry("802005", "GV355CEU"));

    private boolean ignoreFixTime;

    private final DateFormat dateFormat;

    public Gl200TextProtocolDecoder(Protocol protocol) {
        super(protocol);
        dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    protected void init() {
        ignoreFixTime = getConfig().getBoolean(Keys.PROTOCOL_IGNORE_FIX_TIME.withPrefix(getProtocolName()));
    }

    private String getDeviceModel(DeviceSession deviceSession, String protocolVersion) {
        String declaredModel = getDeviceModel(deviceSession);
        if (declaredModel != null) {
            return declaredModel.toUpperCase();
        }
        String versionPrefix;
        if (protocolVersion.length() > 6) {
            versionPrefix = protocolVersion.substring(0, 6);
        } else {
            versionPrefix = protocolVersion.substring(0, 2);
        }
        String model = PROTOCOL_MODELS.get(versionPrefix);
        return model != null ? model : "";
    }

    private Position initPosition(Parser parser, Channel channel, SocketAddress remoteAddress) {
        if (parser.matches()) {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
            if (deviceSession != null) {
                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());
                return position;
            }
        }
        return null;
    }

    private void decodeDeviceTime(Position position, Parser parser) {
        if (parser.hasNext(6)) {
            if (ignoreFixTime) {
                position.setTime(parser.nextDateTime());
            } else {
                position.setDeviceTime(parser.nextDateTime());
            }
        }
    }

    private Long parseHours(String hoursString) {
        if (hoursString != null && !hoursString.isEmpty()) {
            String[] hours = hoursString.split(":");
            return (Integer.parseInt(hours[0]) * 3600L
                    + (hours.length > 1 ? Integer.parseInt(hours[1]) * 60L : 0)
                    + (hours.length > 2 ? Integer.parseInt(hours[2]) : 0)) * 1000;
        }
        return null;
    }

    private Position decodeAck(Channel channel, SocketAddress remoteAddress, String[] values) throws ParseException {
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[2]);
        if (deviceSession == null) {
            return null;
        }
        if (values[0].equals("+ACK:GTHBD")) {
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(
                        "+SACK:GTHBD," + values[1] + "," + values[values.length - 1] + "$", remoteAddress));
            }
        } else {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            getLastLocation(position, dateFormat.parse(values[values.length - 2]));
            position.setValid(false);
            position.set(Position.KEY_RESULT, values[0]);
            return position;
        }
        return null;
    }

    private static final Pattern PATTERN_INF = new PatternBuilder()
            .text("+").expression("(?:RESP|BUFF):GTINF,")
            .expression("(.{6}|.{10})?,")        // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("(?:[0-9A-Z]{17},)?")    // vin
            .expression("(?:[^,]+)?,")           // device name
            .number("(xx),")                     // state
            .expression("([0-9Ff]{20})?,")       // iccid
            .number("(d{1,2}),")                 // rssi
            .number("d{1,2},")
            .expression("[01]{1,2},")            // external power
            .number("([d.]+)?,")                 // odometer or external power
            .number("d*,")                       // backup battery or lightness
            .number("(d+.d+),")                  // battery
            .expression("([01]),")               // charging
            .number("(?:d),")                    // led
            .number("(?:d)?,")                   // gps on need
            .number("(?:d)?,")                   // gps antenna type
            .number("(?:d)?,").optional()        // gps antenna state
            .number("d{14},")                    // last fix time
            .groupBegin()
            .number("(d+),")                     // battery percentage
            .number("[d.]*,")                    // flash type / power
            .number("(-?[d.]+)?,,,")             // temperature
            .or()
            .expression("(?:[01])?,").optional() // pin15 mode
            .number("(d+)?,")                    // adc1
            .number("(d+)?,").optional()         // adc2
            .number("(d+)?,").optional()         // adc3
            .number("(xx)?,")                    // digital input
            .number("(xx)?,")                    // digital output
            .number("[-+]dddd,")                 // timezone
            .expression("[01],")                 // daylight saving
            .or()
            .any()
            .groupEnd()
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(xxxx)")                    // counter
            .text("$").optional()
            .compile();

    private Object decodeInf(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_INF, sentence);
        if (!parser.matches()) {
            return null;
        }
        String protocolVersion = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        switch (parser.nextHexInt()) {
            case 0x16:
            case 0x1A:
            case 0x12:
                position.set(Position.KEY_IGNITION, false);
                position.set(Position.KEY_MOTION, true);
                break;
            case 0x11:
                position.set(Position.KEY_IGNITION, false);
                position.set(Position.KEY_MOTION, false);
                break;
            case 0x21:
                position.set(Position.KEY_IGNITION, true);
                position.set(Position.KEY_MOTION, false);
                break;
            case 0x22:
                position.set(Position.KEY_IGNITION, true);
                position.set(Position.KEY_MOTION, true);
                break;
            case 0x41:
                position.set(Position.KEY_MOTION, false);
                break;
            case 0x42:
                position.set(Position.KEY_MOTION, true);
                break;
            default:
                break;
        }

        position.set(Position.KEY_ICCID, parser.next());
        position.set(Position.KEY_RSSI, parser.nextInt());

        String model = getDeviceModel(deviceSession, protocolVersion);
        if (model.equals("GV310LAU")) {
            position.set(Position.KEY_POWER, parser.nextDouble() / 1000);
        } else {
            parser.next(); // odometer or external power
        }

        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.KEY_CHARGE, parser.nextInt() == 1);

        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());

        position.set(Position.PREFIX_TEMP + 1, parser.next());

        position.set(Position.PREFIX_ADC + 1, parser.next());
        position.set(Position.PREFIX_ADC + 2, parser.next());
        if (model.equals("GV310LAU")) {
            position.set(Position.PREFIX_ADC + 3, parser.next());
        }

        position.set(Position.KEY_INPUT, parser.next());
        position.set(Position.KEY_OUTPUT, parser.next());

        getLastLocation(position, parser.nextDateTime());

        position.set(Position.KEY_INDEX, parser.nextHexInt());

        return position;
    }

    private static final Pattern PATTERN_VER = new PatternBuilder()
            .text("+").expression("(?:RESP|BUFF):GTVER,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("[^,]*,")                // device name
            .expression("([^,]*),")              // device type
            .number("(xxxx),")                   // firmware version
            .number("(xxxx),")                   // hardware version
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(xxxx)")                    // counter
            .text("$").optional()
            .compile();

    private Object decodeVer(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_VER, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        position.set("deviceType", parser.next());
        position.set(Position.KEY_VERSION_FW, parser.nextHexInt());
        position.set(Position.KEY_VERSION_HW, parser.nextHexInt());

        getLastLocation(position, parser.nextDateTime());

        return position;
    }

    private void skipLocation(Parser parser) {
        parser.skip(20);
    }

    private static final Pattern PATTERN_LOCATION = new PatternBuilder()
            .number("(d{1,2}.?d?)?,")            // hdop
            .number("(d{1,3}.d)?,")              // speed
            .number("(d{1,3}.?d?)?,")            // course
            .number("(-?d{1,5}.d)?,")            // altitude
            .number("(-?d{1,3}.d{6})?,")         // longitude
            .number("(-?d{1,2}.d{6})?,")         // latitude
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .groupBegin()
            .number(",d+")                       // wifi count
            .number("((?:,x{12},-d+,,,)+)")      // wifi
            .groupEnd("?")
            .text(",")
            .number("(d+)?,")                    // mcc
            .number("(d+)?,")                    // mnc
            .groupBegin()
            .number("(d+),")                     // lac
            .number("(d+),")                     // cid
            .or()
            .number("(x+)?,")                    // lac
            .number("(x+)?,")                    // cid
            .groupEnd()
            .number("(?:d+|(d+.d))?,")           // rssi / odometer
            .compile();

    private void decodeLocation(Position position, Parser parser) {
        Double hdop = parser.nextDouble();
        position.setValid(hdop == null || hdop > 0);
        position.set(Position.KEY_HDOP, hdop);

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
        position.setCourse(parser.nextDouble(0));
        position.setAltitude(parser.nextDouble(0));

        if (parser.hasNext(8)) {
            position.setValid(true);
            position.setLongitude(parser.nextDouble());
            position.setLatitude(parser.nextDouble());
            position.setTime(parser.nextDateTime());
        } else {
            getLastLocation(position, null);
        }

        Network network = new Network();

        if (parser.hasNext()) {
            String[] values = parser.next().split(",");
            for (int i = 0; i < values.length; i += 5) {
                String mac = values[i + 1].replaceAll("(..)", "$1:");
                network.addWifiAccessPoint(WifiAccessPoint.from(
                        mac.substring(0, mac.length() - 1), Integer.parseInt(values[i + 2])));
            }
        }

        if (parser.hasNext(6)) {
            int mcc = parser.nextInt();
            int mnc = parser.nextInt();
            if (parser.hasNext(2)) {
                network.addCellTower(CellTower.from(mcc, mnc, parser.nextInt(), parser.nextInt()));
            }
            if (parser.hasNext(2)) {
                network.addCellTower(CellTower.from(mcc, mnc, parser.nextHexInt(), parser.nextHexInt()));
            }
        }

        if (network.getWifiAccessPoints() != null || network.getCellTowers() != null) {
            position.setNetwork(network);
        }

        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);
        }
    }

    private int decodeLocation(Position position, String model, String[] values, int index) throws ParseException {
        double hdop = values[index++].isEmpty() ? 0 : Double.parseDouble(values[index - 1]);
        position.set(Position.KEY_HDOP, hdop);

        position.setSpeed(UnitsConverter.knotsFromKph(
                values[index++].isEmpty() ? 0 : Double.parseDouble(values[index - 1])));
        position.setCourse(values[index++].isEmpty() ? 0 : Integer.parseInt(values[index - 1]));
        position.setAltitude(values[index++].isEmpty() ? 0 : Double.parseDouble(values[index - 1]));

        if (!values[index].isEmpty()) {
            position.setValid(true);
            position.setLongitude(values[index++].isEmpty() ? 0 : Double.parseDouble(values[index - 1]));
            position.setLatitude(values[index++].isEmpty() ? 0 : Double.parseDouble(values[index - 1]));
            position.setTime(dateFormat.parse(values[index++]));
        } else {
            index += 3;
            getLastLocation(position, null);
        }

        Network network = new Network();

        if (!values[index].isEmpty()) {
            network.addCellTower(CellTower.from(
                    Integer.parseInt(values[index++]),
                    Integer.parseInt(values[index++]),
                    Integer.parseInt(values[index++], 16),
                    Long.parseLong(values[index++], 16)));
        } else {
            index += 4;
        }

        if (network.getWifiAccessPoints() != null || network.getCellTowers() != null) {
            position.setNetwork(network);
        }

        if (model.startsWith("GL5")) {
            index += 1; // csq rssi
            index += 1; // csq ber
        }

        if (!values[index++].isEmpty()) {
            int appendMask = Integer.parseInt(values[index - 1]);
            if (BitUtil.check(appendMask, 0)) {
                position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));
            }
            if (BitUtil.check(appendMask, 1)) {
                index += 1; // trigger type
            }
        }

        return index;
    }

    private static final Pattern PATTERN_OBD = new PatternBuilder()
            .text("+RESP:GTOBD,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("(?:[0-9A-Z]{17})?,")    // vin
            .expression("[^,]{0,20},")           // device name
            .expression("[01],")                 // report type
            .number("x{1,8},")                   // report mask
            .expression("(?:[0-9A-Z]{17})?,")    // vin
            .number("[01],")                     // obd connect
            .number("(?:d{1,5})?,")              // obd voltage
            .number("(?:x{8})?,")                // support pids
            .number("(d{1,5})?,")                // engine rpm
            .number("(d{1,3})?,")                // speed
            .number("(-?d{1,3})?,")              // coolant temp
            .number("(d+.?d*|Inf|NaN)?,")        // fuel consumption
            .number("(d{1,5})?,")                // dtcs cleared distance
            .number("(?:d{1,5})?,")
            .expression("([01])?,")              // obd connect
            .number("(d{1,3})?,")                // number of dtcs
            .number("(x*),")                     // dtcs
            .number("(d{1,3})?,")                // throttle
            .number("(?:d{1,3})?,")              // engine load
            .number("(d{1,3})?,")                // fuel level
            .expression("(?:[0-9A],)?")          // obd protocol
            .number("(d+),")                     // odometer
            .expression(PATTERN_LOCATION.pattern())
            .number("(d{1,7}.d)?,")              // odometer
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeObd(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_OBD, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        position.set(Position.KEY_RPM, parser.nextInt());
        position.set(Position.KEY_OBD_SPEED, parser.nextInt());
        position.set(Position.PREFIX_TEMP + 1, parser.nextInt());
        position.set(Position.KEY_FUEL_CONSUMPTION, parser.next());
        position.set("dtcsClearedDistance", parser.nextInt());
        if (parser.hasNext()) {
            position.set("odbConnect", parser.nextInt() == 1);
        }
        position.set("dtcsNumber", parser.nextInt());
        position.set("dtcsCodes", parser.next());
        position.set(Position.KEY_THROTTLE, parser.nextInt());
        position.set(Position.KEY_FUEL_LEVEL, parser.nextInt());
        if (parser.hasNext()) {
            position.set(Position.KEY_OBD_ODOMETER, parser.nextInt() * 1000);
        }

        decodeLocation(position, parser);

        if (parser.hasNext()) {
            position.set(Position.KEY_OBD_ODOMETER, (int) (parser.nextDouble() * 1000));
        }

        decodeDeviceTime(position, parser);

        return position;
    }

    private Object decodeCan(Channel channel, SocketAddress remoteAddress, String[] v) throws ParseException {
        int index = 0;
        index += 1; // header
        String protocolVersion = v[index++]; // protocol version
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, v[index++]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        String model = getDeviceModel(deviceSession, protocolVersion);
        index += 1; // device name
        index += 1; // report type
        index += 1; // can bus state
        long reportMask = Long.parseLong(v[index++], 16);

        if (BitUtil.check(reportMask, 0)) {
            position.set(Position.KEY_VIN, v[index++]);
        }
        if (BitUtil.check(reportMask, 1) && !v[index++].isEmpty()) {
            position.set(Position.KEY_IGNITION, Integer.parseInt(v[index - 1]) > 0);
        }
        if (BitUtil.check(reportMask, 2) && !v[index++].isEmpty()) {
            position.set(Position.KEY_OBD_ODOMETER, Integer.parseInt(v[index - 1].substring(1)));
        }
        if (BitUtil.check(reportMask, 3) && !v[index++].isEmpty()) {
            position.set(Position.KEY_FUEL_USED, Double.parseDouble(v[index - 1]));
        }
        if (BitUtil.check(reportMask, 5) && !v[index++].isEmpty()) {
            position.set(Position.KEY_RPM, Integer.parseInt(v[index - 1]));
        }
        if (BitUtil.check(reportMask, 4) && !v[index++].isEmpty()) {
            position.set(Position.KEY_OBD_SPEED, Integer.parseInt(v[index - 1]));
        }
        if (BitUtil.check(reportMask, 6) && !v[index++].isEmpty()) {
            position.set(Position.KEY_COOLANT_TEMP, Integer.parseInt(v[index - 1]));
        }
        if (BitUtil.check(reportMask, 7) && !v[index++].isEmpty()) {
            String value = v[index - 1];
            if (value.startsWith("L/H")) {
                position.set(Position.KEY_FUEL_CONSUMPTION, Double.parseDouble(value.substring(3)));
            }
        }
        if (BitUtil.check(reportMask, 8) && !v[index++].isEmpty()) {
            position.set(Position.KEY_FUEL_LEVEL, Double.parseDouble(v[index - 1].substring(1)));
        }
        if (BitUtil.check(reportMask, 9) && !v[index++].isEmpty()) {
            position.set("range", Long.parseLong(v[index - 1]) * 100);
        }
        if (BitUtil.check(reportMask, 10) && !v[index++].isEmpty()) {
            position.set(Position.KEY_THROTTLE, Integer.parseInt(v[index - 1]));
        }
        if (BitUtil.check(reportMask, 11) && !v[index++].isEmpty()) {
            position.set(Position.KEY_HOURS, UnitsConverter.msFromHours(Double.parseDouble(v[index - 1])));
        }
        if (BitUtil.check(reportMask, 12) && !v[index++].isEmpty()) {
            position.set(Position.KEY_DRIVING_TIME, Double.parseDouble(v[index - 1]));
        }
        if (BitUtil.check(reportMask, 13) && !v[index++].isEmpty()) {
            position.set("idleHours", Double.parseDouble(v[index - 1]));
        }
        if (BitUtil.check(reportMask, 14) && !v[index++].isEmpty()) {
            position.set("idleFuelConsumption", Double.parseDouble(v[index - 1]));
        }
        if (BitUtil.check(reportMask, 15) && !v[index++].isEmpty()) {
            position.set(Position.KEY_AXLE_WEIGHT, Integer.parseInt(v[index - 1]));
        }
        if (BitUtil.check(reportMask, 16) && !v[index++].isEmpty()) {
            position.set("tachographInfo", Integer.parseInt(v[index - 1], 16));
        }
        if (BitUtil.check(reportMask, 17) && !v[index++].isEmpty()) {
            position.set("indicators", Integer.parseInt(v[index - 1], 16));
        }
        if (BitUtil.check(reportMask, 18) && !v[index++].isEmpty()) {
            position.set("lights", Integer.parseInt(v[index - 1], 16));
        }
        if (BitUtil.check(reportMask, 19) && !v[index++].isEmpty()) {
            position.set("doors", Integer.parseInt(v[index - 1], 16));
        }
        if (BitUtil.check(reportMask, 20) && !v[index++].isEmpty()) {
            position.set("vehicleOverspeed", Double.parseDouble(v[index - 1]));
        }
        if (BitUtil.check(reportMask, 21) && !v[index++].isEmpty()) {
            position.set("engineOverspeed", Double.parseDouble(v[index - 1]));
        }
        if ("GV350M".equals(model)) {
            if (BitUtil.check(reportMask, 22)) {
                index += 1; // impulse distance
            }
            if (BitUtil.check(reportMask, 23)) {
                index += 1; // gross vehicle weight
            }
            if (BitUtil.check(reportMask, 24)) {
                index += 1; // catalyst liquid level
            }
        } else if ("GV355CEU".equals(model)) {
            if (BitUtil.check(reportMask, 22)) {
                index += 1; // impulse distance
            }
            if (BitUtil.check(reportMask, 23)) {
                index += 1; // engine cold starts
            }
            if (BitUtil.check(reportMask, 24)) {
                index += 1; // engine all starts
            }
            if (BitUtil.check(reportMask, 25)) {
                index += 1; // engine starts by ignition
            }
            if (BitUtil.check(reportMask, 26)) {
                index += 1; // total engine cold running time
            }
            if (BitUtil.check(reportMask, 27)) {
                index += 1; // handbrake applies during ride
            }
            if (BitUtil.check(reportMask, 28)) {
                index += 1; // electric report mask
            }
        }

        long reportMaskExt = 0;
        if (BitUtil.check(reportMask, 29) && !v[index++].isEmpty()) {
            reportMaskExt = Long.parseLong(v[index - 1], 16);
        }
        if (BitUtil.check(reportMaskExt, 0) && !v[index++].isEmpty()) {
            position.set("adBlueLevel", Integer.parseInt(v[index - 1]));
        }
        if (BitUtil.check(reportMaskExt, 1) && !v[index++].isEmpty()) {
            position.set("axleWeight1", Integer.parseInt(v[index - 1]));
        }
        if (BitUtil.check(reportMaskExt, 2) && !v[index++].isEmpty()) {
            position.set("axleWeight3", Integer.parseInt(v[index - 1]));
        }
        if (BitUtil.check(reportMaskExt, 3) && !v[index++].isEmpty()) {
            position.set("axleWeight4", Integer.parseInt(v[index - 1]));
        }
        if (BitUtil.check(reportMaskExt, 4)) {
            index += 1; // tachograph overspeed
        }
        if (BitUtil.check(reportMaskExt, 5)) {
            index += 1; // tachograph motion
        }
        if (BitUtil.check(reportMaskExt, 6)) {
            index += 1; // tachograph direction
        }
        if (BitUtil.check(reportMaskExt, 7) && !v[index++].isEmpty()) {
            position.set(Position.PREFIX_ADC + 1, Integer.parseInt(v[index - 1]) * 0.001);
        }
        if (BitUtil.check(reportMaskExt, 8)) {
            index += 1; // pedal breaking factor
        }
        if (BitUtil.check(reportMaskExt, 9)) {
            index += 1; // engine breaking factor
        }
        if (BitUtil.check(reportMaskExt, 10)) {
            index += 1; // total accelerator kick-downs
        }
        if (BitUtil.check(reportMaskExt, 11)) {
            index += 1; // total effective engine speed
        }
        if (BitUtil.check(reportMaskExt, 12)) {
            index += 1; // total cruise control time
        }
        if (BitUtil.check(reportMaskExt, 13)) {
            index += 1; // total accelerator kick-down time
        }
        if (BitUtil.check(reportMaskExt, 14)) {
            index += 1; // total brake application
        }
        if (BitUtil.check(reportMaskExt, 15) && !v[index++].isEmpty()) {
            position.set("driver1Card", v[index - 1]);
        }
        if (BitUtil.check(reportMaskExt, 16) && !v[index++].isEmpty()) {
            position.set("driver2Card", v[index - 1]);
        }
        if (BitUtil.check(reportMaskExt, 17) && !v[index++].isEmpty()) {
            position.set("driver1Name", v[index - 1]);
        }
        if (BitUtil.check(reportMaskExt, 18) && !v[index++].isEmpty()) {
            position.set("driver2Name", v[index - 1]);
        }
        if (BitUtil.check(reportMaskExt, 19) && !v[index++].isEmpty()) {
            position.set("registration", v[index - 1]);
        }
        if (BitUtil.check(reportMaskExt, 20)) {
            index += 1; // expansion information
        }
        if (BitUtil.check(reportMaskExt, 21)) {
            index += 1; // rapid brakings
        }
        if (BitUtil.check(reportMaskExt, 22)) {
            index += 1; // rapid accelerations
        }
        if (BitUtil.check(reportMaskExt, 23)) {
            index += 1; // engine torque
        }
        if (BitUtil.check(reportMaskExt, 24)) {
            index += 1; // service distance
        }
        if (BitUtil.check(reportMaskExt, 25)) {
            index += 1; // ambient temperature
        }
        if (BitUtil.check(reportMaskExt, 26)) {
            index += 1; // tachograph driver1 working time mask
        }
        if (BitUtil.check(reportMaskExt, 27)) {
            index += 1; // tachograph driver2 working time mask
        }
        if (BitUtil.check(reportMaskExt, 28)) {
            index += 1; // dtc codes
        }
        if (BitUtil.check(reportMaskExt, 29)) {
            index += 1; // gaseous fuel level
        }
        if (BitUtil.check(reportMaskExt, 30)) {
            index += 1; // tachograph information expand
        }

        long reportMaskCan = 0;
        if (BitUtil.check(reportMaskExt, 31) && !v[index++].isEmpty()) {
            reportMaskCan = Long.parseLong(v[index - 1], 16);
        }
        if (BitUtil.check(reportMaskCan, 0)) {
            index += 1; // retarder usage
        }
        if (BitUtil.check(reportMaskCan, 1)) {
            index += 1; // power mode
        }
        if (BitUtil.check(reportMaskCan, 2)) {
            index += 1; // tachograph timestamp
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (!"GV355CEU".equals(model) && BitUtil.check(reportMask, 30)) {
            while (v[index].isEmpty()) {
                index += 1;
            }
            position.setValid(Integer.parseInt(v[index++]) > 0);
            if (!v[index].isEmpty()) {
                position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(v[index++])));
                position.setCourse(Integer.parseInt(v[index++]));
                position.setAltitude(Double.parseDouble(v[index++]));
                position.setLongitude(Double.parseDouble(v[index++]));
                position.setLatitude(Double.parseDouble(v[index++]));
                position.setTime(dateFormat.parse(v[index++]));
            } else {
                index += 6; // no location
                getLastLocation(position, null);
            }
        } else {
            getLastLocation(position, null);
        }

        if (BitUtil.check(reportMask, 31)) {
            index += 4; // cell
            index += 1; // reserved
        }

        index = v.length - 2;
        if (ignoreFixTime) {
            position.setTime(dateFormat.parse(v[index]));
        } else {
            position.setDeviceTime(dateFormat.parse(v[index]));
        }

        return position;
    }

    private void decodeStatus(Position position, long value) {
        long ignition = BitUtil.between(value, 2 * 8, 3 * 8);
        if (BitUtil.check(ignition, 4)) {
            position.set(Position.KEY_IGNITION, false);
        } else if (BitUtil.check(ignition, 5)) {
            position.set(Position.KEY_IGNITION, true);
        }
        long input = BitUtil.between(value, 8, 2 * 8);
        long output = BitUtil.to(value, 8);
        position.set(Position.KEY_INPUT, input);
        position.set(Position.PREFIX_IN + 1, BitUtil.check(input, 1));
        position.set(Position.PREFIX_IN + 2, BitUtil.check(input, 2));
        position.set(Position.KEY_OUTPUT, output);
        position.set(Position.PREFIX_OUT + 1, BitUtil.check(output, 0));
        position.set(Position.PREFIX_OUT + 2, BitUtil.check(output, 1));
    }

    private static final Pattern PATTERN_FRI = new PatternBuilder()
            .text("+").expression("(?:RESP|BUFF):GT...,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("(?:([0-9A-Z]{17}),)?")  // vin
            .expression("[^,]*,")                // device name
            .number("(d+)?,")                    // power
            .number("(d{1,2}),").optional()      // report type
            .number("d{1,2},").optional()        // count
            .number("d*,").optional()            // reserved
            .number("(d+),").optional()          // battery
            .expression("((?:")
            .expression(PATTERN_LOCATION.pattern())
            .expression(")+)")
            .groupBegin()
            .number("d{1,2},")
            .number("(d{1,5})?,")                // battery
            .number("(d{1,3}),")                 // battery level
            .number("[01],")                     // mode
            .number("(?:[01])?,")                // motion
            .number("(-?d{1,2}.d)?,")            // temperature
            .or()
            .number("(d{1,7}.d)?,")              // odometer
            .number("(d{5}:dd:dd)?,")            // hour meter
            .number("(x+)?,")                    // adc 1
            .number("(x+)?,")                    // adc 2
            .number("(d{1,3})?,")                // battery
            .number("(x{6})?,")                  // device status
            .number("(d+)?,")                    // rpm
            .number("(?:d+.?d*|Inf|NaN)?,")      // fuel consumption
            .number("(d+)?,")                    // fuel level
            .or()
            .number("(-?d),")                    // rssi
            .number("(d{1,3}),")                 // battery
            .or()
            .number("(d{1,7}.d)?,").optional()   // odometer
            .number("(d{1,3})?,")                // battery
            .groupEnd()
            .any()
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeFri(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_FRI, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        LinkedList<Position> positions = new LinkedList<>();

        String vin = parser.next();
        Integer power = parser.nextInt();
        Integer reportType = parser.nextInt();
        Integer battery = parser.nextInt();

        Parser itemParser = new Parser(PATTERN_LOCATION, parser.next());
        while (itemParser.find()) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.set(Position.KEY_VIN, vin);

            decodeLocation(position, itemParser);

            positions.add(position);
        }

        Position position = positions.getLast();

        skipLocation(parser);

        if (power != null && power > 10) {
            position.set(Position.KEY_POWER, power * 0.001); // only on some devices
        }
        if (battery != null) {
            position.set(Position.KEY_BATTERY_LEVEL, battery);
        }

        if (parser.hasNext()) {
            position.set(Position.KEY_BATTERY, parser.nextInt() * 0.001);
        }
        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());
        position.set(Position.PREFIX_TEMP + 1, parser.nextDouble());

        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);
        }
        position.set(Position.KEY_HOURS, parseHours(parser.next()));
        position.set(Position.PREFIX_ADC + 1, parser.next());
        position.set(Position.PREFIX_ADC + 2, parser.next());
        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());

        if (parser.hasNext()) {
            decodeStatus(position, parser.nextHexLong());
        }

        position.set(Position.KEY_RPM, parser.nextInt());
        position.set(Position.KEY_FUEL_LEVEL, parser.nextInt());

        if (parser.hasNext(2)) {
            if (reportType != null) {
                position.set(Position.KEY_MOTION, BitUtil.check(reportType, 0));
                position.set(Position.KEY_CHARGE, BitUtil.check(reportType, 1));
            }
            position.set(Position.KEY_RSSI, parser.nextInt());
            position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());
        }
        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);
        }
        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());

        decodeDeviceTime(position, parser);
        if (ignoreFixTime) {
            positions.clear();
            positions.add(position);
        }

        return positions;
    }

    private Object decodeEri(Channel channel, SocketAddress remoteAddress, String[] v) throws ParseException {
        int index = 0;
        index += 1; // header
        String protocolVersion = v[index++]; // protocol version
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, v[index++]);
        if (deviceSession == null) {
            return null;
        }

        String model = getDeviceModel(deviceSession, protocolVersion);
        index += 1; // device name
        long mask = Long.parseLong(v[index++], 16);
        Double power = v[index++].isEmpty() ? null : Integer.parseInt(v[index - 1]) * 0.001;
        index += 1; // report type

        int count = Integer.parseInt(v[index++]);
        LinkedList<Position> positions = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            index = decodeLocation(position, model, v, index);
            positions.add(position);
        }

        Position position = positions.getLast();
        position.set(Position.KEY_POWER, power);

        if (!model.startsWith("GL5")) {
            position.set(Position.KEY_ODOMETER, v[index++].isEmpty() ? null : Double.parseDouble(v[index - 1]) * 1000);
            position.set(Position.KEY_HOURS, parseHours(v[index++]));
            position.set(Position.PREFIX_ADC + 1, v[index++].isEmpty() ? null : Integer.parseInt(v[index - 1]) * 0.001);
        }
        if (model.startsWith("GV") && !model.startsWith("GV6")) {
            position.set(Position.PREFIX_ADC + 2, v[index++].isEmpty() ? null : Integer.parseInt(v[index - 1]) * 0.001);
        }
        if (model.startsWith("GV355CEU")) {
            index += 1; // reserved
        }

        position.set(Position.KEY_BATTERY_LEVEL, v[index++].isEmpty() ? null : Integer.parseInt(v[index - 1]));
        if (model.startsWith("GL5")) {
            index += 1; // mode selection
            position.set(Position.KEY_MOTION, v[index++].isEmpty() ? null : Integer.parseInt(v[index - 1]) > 0);
        } else {
            if (!v[index++].isEmpty()) {
                decodeStatus(position, Long.parseLong(v[index - 1]));
            }
            index += 1; // reserved / uart device type
        }

        if (BitUtil.check(mask, 0)) {
            position.set(Position.KEY_FUEL_LEVEL, Integer.parseInt(v[index++], 16));
        }

        if (BitUtil.check(mask, 1)) {
            int deviceCount = Integer.parseInt(v[index++]);
            for (int i = 1; i <= deviceCount; i++) {
                index += 1; // id
                index += 1; // type
                if (!v[index++].isEmpty()) {
                    position.set(Position.PREFIX_TEMP + i, (short) Integer.parseInt(v[index - 1], 16) * 0.0625);
                }
            }
        }

        if (BitUtil.check(mask, 2)) {
            index += 1; // can data
        }

        if (BitUtil.check(mask, 3) || BitUtil.check(mask, 4)) {
            int deviceCount = Integer.parseInt(v[index++]);
            for (int i = 1; i <= deviceCount; i++) {
                index += 1; // type
                if (BitUtil.check(mask, 3)) {
                    position.set(Position.KEY_FUEL_LEVEL, Double.parseDouble(v[index++]));
                }
                if (BitUtil.check(mask, 4)) {
                    index += 1; // volume
                }
            }
        }

        Date time = dateFormat.parse(v[v.length - 2]);
        if (ignoreFixTime) {
            position.setTime(time);
            positions.clear();
            positions.add(position);
        } else {
            position.setDeviceTime(time);
        }

        return positions;
    }

    private static final Pattern PATTERN_IGN = new PatternBuilder()
            .text("+").expression("(?:RESP|BUFF):GT[IV]G[NF],")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("[^,]*,")                // device name
            .number("d+,")                       // ignition off duration
            .expression(PATTERN_LOCATION.pattern())
            .number("(d{5}:dd:dd)?,")            // hour meter
            .number("(d{1,7}.d)?,")              // odometer
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeIgn(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_IGN, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        decodeLocation(position, parser);

        position.set(Position.KEY_IGNITION, sentence.contains("GN"));
        position.set(Position.KEY_HOURS, parseHours(parser.next()));
        position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);

        decodeDeviceTime(position, parser);

        return position;
    }

    private static final Pattern PATTERN_LSW = new PatternBuilder()
            .text("+RESP:").expression("GT[LT]SW,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("[^,]*,")                // device name
            .number("[01],")                     // type
            .number("([01]),")                   // state
            .expression(PATTERN_LOCATION.pattern())
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeLsw(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_LSW, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        position.set(Position.PREFIX_IN + (sentence.contains("LSW") ? 1 : 2), parser.nextInt() == 1);

        decodeLocation(position, parser);

        decodeDeviceTime(position, parser);

        return position;
    }

    private static final Pattern PATTERN_IDA = new PatternBuilder()
            .text("+RESP:GTIDA,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("[^,]*,,")               // device name
            .number("([^,]+),")                  // rfid
            .expression("[01],")                 // report type
            .number("1,")                        // count
            .expression(PATTERN_LOCATION.pattern())
            .number("(d+.d),")                   // odometer
            .text(",,,,")
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeIda(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_IDA, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());

        decodeLocation(position, parser);

        position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);

        decodeDeviceTime(position, parser);

        return position;
    }

    private static final Pattern PATTERN_WIF = new PatternBuilder()
            .text("+RESP:GTWIF,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("[^,]*,")                // device name
            .number("(d+),")                     // count
            .number("((?:x{12},-?d+,,,,)+),,,,") // wifi
            .number("(d{1,3}),")                 // battery
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeWif(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_WIF, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        getLastLocation(position, null);

        Network network = new Network();

        parser.nextInt(); // count
        Matcher matcher = Pattern.compile("([0-9a-fA-F]{12}),(-?\\d+),,,,").matcher(parser.next());
        while (matcher.find()) {
            String mac = matcher.group(1).replaceAll("(..)", "$1:");
            network.addWifiAccessPoint(WifiAccessPoint.from(
                    mac.substring(0, mac.length() - 1), Integer.parseInt(matcher.group(2))));
        }

        position.setNetwork(network);

        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());

        return position;
    }

    private static final Pattern PATTERN_GSM = new PatternBuilder()
            .text("+RESP:GTGSM,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("(?:STR|CTN|NMR|RTL),")  // fix type
            .expression("(.*)")                  // cells
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeGsm(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_GSM, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        getLastLocation(position, null);

        Network network = new Network();

        String[] data = parser.next().split(",");
        for (int i = 0; i < 6; i++) {
            if (!data[i * 6].isEmpty()) {
                network.addCellTower(CellTower.from(
                        Integer.parseInt(data[i * 6]), Integer.parseInt(data[i * 6 + 1]),
                        Integer.parseInt(data[i * 6 + 2], 16), Integer.parseInt(data[i * 6 + 3], 16),
                        Integer.parseInt(data[i * 6 + 4])));
            }
        }

        position.setNetwork(network);

        return position;
    }

    private static final Pattern PATTERN_PNA = new PatternBuilder()
            .text("+RESP:GT").expression("P[NF]A,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("[^,]*,")                // device name
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodePna(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_PNA, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        getLastLocation(position, null);

        position.set(Position.KEY_ALARM, sentence.contains("PNA") ? Position.ALARM_POWER_ON : Position.ALARM_POWER_OFF);

        return position;
    }

    private static final Pattern PATTERN_DAR = new PatternBuilder()
            .text("+RESP:GTDAR,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("[^,]*,")                // device name
            .number("(d),")                      // warning type
            .number("(d{1,2}),,,")               // fatigue degree
            .expression(PATTERN_LOCATION.pattern())
            .any()
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeDar(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_DAR, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        int warningType = parser.nextInt();
        int fatigueDegree = parser.nextInt();
        if (warningType == 1) {
            position.set(Position.KEY_ALARM, Position.ALARM_FATIGUE_DRIVING);
            position.set("fatigueDegree", fatigueDegree);
        } else {
            position.set("warningType", warningType);
        }

        decodeLocation(position, parser);

        decodeDeviceTime(position, parser);

        return position;
    }

    private static final Pattern PATTERN_DTT = new PatternBuilder()
            .text("+RESP:GTDTT,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("[^,]*,,,")              // device name
            .number("d,")                        // data type
            .number("d+,")                       // data length
            .number("(x+),")                     // data
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeDtt(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_DTT, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        getLastLocation(position, null);

        String data = Unpooled.wrappedBuffer(DataConverter.parseHex(parser.next()))
                .toString(StandardCharsets.US_ASCII);
        if (data.contains("COMB")) {
            position.set(Position.KEY_FUEL_LEVEL, Double.parseDouble(data.split(",")[2]));
        } else {
            position.set(Position.KEY_RESULT, data);
        }

        decodeDeviceTime(position, parser);

        return position;
    }

    private static final Pattern PATTERN_BAA = new PatternBuilder()
            .text("+RESP:GTBAA,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("[^,]*,")                // device name
            .number("x+,")                       // index
            .number("d,")                        // accessory type
            .number("d,")                        // accessory model
            .number("x+,")                       // alarm type
            .number("(x{4}),")                   // append mask
            .expression("((?:[^,]+,){0,6})")     // accessory optionals
            .expression(PATTERN_LOCATION.pattern())
            .any()
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeBaa(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_BAA, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        int mask = parser.nextHexInt();
        String[] values = parser.next().split(",");
        int index = 0;
        if (BitUtil.check(mask, 0)) {
            position.set("accessoryName", values[index++]);
        }
        if (BitUtil.check(mask, 1)) {
            position.set("accessoryMac", values[index++]);
        }
        if (BitUtil.check(mask, 2)) {
            position.set("accessoryStatus", Integer.parseInt(values[index++]));
        }
        if (BitUtil.check(mask, 3)) {
            position.set("accessoryVoltage", Integer.parseInt(values[index++]) * 0.001);
        }
        if (BitUtil.check(mask, 4)) {
            position.set("accessoryTemp", Integer.parseInt(values[index++]));
        }
        if (BitUtil.check(mask, 5)) {
            position.set("accessoryHumidity", Integer.parseInt(values[index]));
        }

        decodeLocation(position, parser);

        decodeDeviceTime(position, parser);

        return position;
    }

    private static final Pattern PATTERN_BID = new PatternBuilder()
            .text("+RESP:GTBID,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("[^,]*,")                // device name
            .number("d,")                        // count
            .number("d,")                        // accessory model
            .number("(x{4}),")                   // append mask
            .expression("((?:[^,]+,){0,2})")     // accessory optionals
            .expression(PATTERN_LOCATION.pattern())
            .any()
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeBid(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_BID, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        int mask = parser.nextHexInt();
        String[] values = parser.next().split(",");
        int index = 0;
        if (BitUtil.check(mask, 1)) {
            position.set("accessoryMac", values[index++]);
        }
        if (BitUtil.check(mask, 3)) {
            position.set("accessoryVoltage", Integer.parseInt(values[index]) * 0.001);
        }

        decodeLocation(position, parser);

        decodeDeviceTime(position, parser);

        return position;
    }

    private static final Pattern PATTERN_LSA = new PatternBuilder()
            .text("+RESP:GTLSA,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("[^,]*,")                // device name
            .number("d,")                        // event state 1
            .number("d,")                        // event state 2
            .number("d+,")                       // number
            .expression(PATTERN_LOCATION.pattern())
            .number("d+,")                       // bit error rate
            .number("(d),")                      // light level
            .number("(d+),")                     // battery level
            .number("[01],")                     // mode selection
            .number("[01]?,")                    // movement status
            .number("(-?d+.d)?,")                // temperature
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeLsa(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_LSA, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        decodeLocation(position, parser);

        position.set("lightLevel", parser.nextInt());
        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());
        position.set(Position.PREFIX_TEMP + 1, parser.nextDouble());

        decodeDeviceTime(position, parser);

        return position;
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("+").expression("(?:RESP|BUFF):GT...,")
            .expression("(?:.{6}|.{10})?,")      // protocol version
            .number("(d{15}|x{14}),")            // imei
            .expression("[^,]*,")                // device name
            .number("d*,")
            .number("(x{1,2}),")                 // report type
            .number("d{1,2},")                   // count
            .number("d*,").optional()            // reserved
            .expression(PATTERN_LOCATION.pattern())
            .groupBegin()
            .number("(?:(d{1,7}.d)|0)?,").optional() // odometer
            .number("(d{1,3})?,")                // battery
            .or()
            .number("(d{1,7}.d)?,")              // odometer
            .groupEnd()
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeOther(Channel channel, SocketAddress remoteAddress, String sentence, String type) {
        Parser parser = new Parser(PATTERN, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        int reportType = parser.nextHexInt();
        if (type.equals("NMR")) {
            position.set(Position.KEY_MOTION, reportType == 1);
        } else if (type.equals("SOS")) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        } else if (type.equals("DIS")) {
            position.set(Position.PREFIX_IN + reportType / 0x10, reportType % 0x10 == 1);
        } else if (type.equals("IGL")) {
            position.set(Position.KEY_IGNITION, reportType % 0x10 == 1);
        } else if (type.equals("HBM")) {
            switch (reportType % 0x10) {
                case 0:
                case 3:
                    position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
                    break;
                case 1:
                case 4:
                    position.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
                    break;
                case 2:
                    position.set(Position.KEY_ALARM, Position.ALARM_CORNERING);
                    break;
                default:
                    break;
            }
        }

        decodeLocation(position, parser);

        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);
        }
        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());

        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);
        }

        decodeDeviceTime(position, parser);

        return position;
    }

    private static final Pattern PATTERN_BASIC = new PatternBuilder()
            .text("+").expression("(?:RESP|BUFF)").text(":")
            .expression("GT...,")
            .expression("[^,]+,").optional()     // protocol version
            .number("(d{15}|x{14}),")            // imei
            .any()
            .text(",")
            .number("(d{1,2}),")                 // hdop
            .groupBegin()
            .number("(d{1,3}.d),")               // speed
            .number("(d{1,3}),")                 // course
            .number("(-?d{1,5}.d),")             // altitude
            .number("(-?d{1,3}.d{6}),")          // longitude
            .number("(-?d{1,2}.d{6}),")          // latitude
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .text(",")
            .or()
            .text(",,,,,,")
            .groupEnd()
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(x+),")                     // lac
            .number("(x+),").optional(4)         // cell
            .any()
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)").optional(2)  // time (hhmmss)
            .text(",")
            .number("(xxxx)")                    // count number
            .text("$").optional()
            .compile();

    private Object decodeBasic(Channel channel, SocketAddress remoteAddress, String sentence, String type) {
        Parser parser = new Parser(PATTERN_BASIC, sentence);
        Position position = initPosition(parser, channel, remoteAddress);
        if (position == null) {
            return null;
        }

        if (parser.hasNext()) {
            int hdop = parser.nextInt();
            position.setValid(hdop > 0);
            position.set(Position.KEY_HDOP, hdop);
        }

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
        position.setCourse(parser.nextDouble(0));
        position.setAltitude(parser.nextDouble(0));

        if (parser.hasNext(2)) {
            position.setLongitude(parser.nextDouble());
            position.setLatitude(parser.nextDouble());
        } else {
            getLastLocation(position, null);
        }

        if (parser.hasNext(6)) {
            position.setTime(parser.nextDateTime());
        }

        if (parser.hasNext(4)) {
            position.setNetwork(new Network(CellTower.from(
                    parser.nextInt(), parser.nextInt(), parser.nextHexInt(), parser.nextHexInt())));
        }

        decodeDeviceTime(position, parser);

        switch (type) {
            case "TOW":
                position.set(Position.KEY_ALARM, Position.ALARM_TOW);
                break;
            case "IDL":
                position.set(Position.KEY_ALARM, Position.ALARM_IDLE);
                break;
            case "PNA":
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_ON);
                break;
            case "PFA":
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_OFF);
                break;
            case "EPN":
            case "MPN":
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_RESTORED);
                break;
            case "EPF":
            case "MPF":
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
                break;
            case "BPL":
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                break;
            case "STT":
                position.set(Position.KEY_ALARM, Position.ALARM_MOVEMENT);
                break;
            case "SWG":
                position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE);
                break;
            case "TMP":
            case "TEM":
                position.set(Position.KEY_ALARM, Position.ALARM_TEMPERATURE);
                break;
            case "JDR":
            case "JDS":
                position.set(Position.KEY_ALARM, Position.ALARM_JAMMING);
                break;
            default:
                break;
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = ((ByteBuf) msg).toString(StandardCharsets.US_ASCII).replaceAll("\\$$", "");

        int typeIndex = sentence.indexOf(":GT");
        if (typeIndex < 0) {
            return null;
        }

        String[] values = sentence.split(",");

        Object result;
        String type = sentence.substring(typeIndex + 3, typeIndex + 6);
        if (sentence.startsWith("+ACK")) {
            result = decodeAck(channel, remoteAddress, values);
        } else {
            switch (type) {
                case "INF":
                    result = decodeInf(channel, remoteAddress, sentence);
                    break;
                case "OBD":
                    result = decodeObd(channel, remoteAddress, sentence);
                    break;
                case "CAN":
                    result = decodeCan(channel, remoteAddress, values);
                    break;
                case "CTN":
                case "FRI":
                case "GEO":
                case "RTL":
                case "DOG":
                case "STR":
                    result = decodeFri(channel, remoteAddress, sentence);
                    break;
                case "ERI":
                    result = decodeEri(channel, remoteAddress, values);
                    break;
                case "IGN":
                case "IGF":
                case "VGN":
                case "VGF":
                    result = decodeIgn(channel, remoteAddress, sentence);
                    break;
                case "LSW":
                case "TSW":
                    result = decodeLsw(channel, remoteAddress, sentence);
                    break;
                case "IDA":
                    result = decodeIda(channel, remoteAddress, sentence);
                    break;
                case "WIF":
                    result = decodeWif(channel, remoteAddress, sentence);
                    break;
                case "GSM":
                    result = decodeGsm(channel, remoteAddress, sentence);
                    break;
                case "VER":
                    result = decodeVer(channel, remoteAddress, sentence);
                    break;
                case "PNA":
                case "PFA":
                    result = decodePna(channel, remoteAddress, sentence);
                    break;
                case "DAR":
                    result = decodeDar(channel, remoteAddress, sentence);
                    break;
                case "DTT":
                    result = decodeDtt(channel, remoteAddress, sentence);
                    break;
                case "BAA":
                    result = decodeBaa(channel, remoteAddress, sentence);
                    break;
                case "BID":
                    result = decodeBid(channel, remoteAddress, sentence);
                    break;
                case "LSA":
                    result = decodeLsa(channel, remoteAddress, sentence);
                    break;
                default:
                    result = decodeOther(channel, remoteAddress, sentence, type);
                    break;
            }

            if (result == null) {
                result = decodeBasic(channel, remoteAddress, sentence, type);
            }

            if (result != null) {
                if (result instanceof Position) {
                    ((Position) result).set(Position.KEY_TYPE, type);
                } else {
                    for (Position p : (List<Position>) result) {
                        p.set(Position.KEY_TYPE, type);
                    }
                }
            }
        }

        if (channel != null && getConfig().getBoolean(Keys.PROTOCOL_ACK.withPrefix(getProtocolName()))) {
            channel.writeAndFlush(new NetworkMessage("+SACK:" + values[values.length - 1] + "$", remoteAddress));
        }

        return result;
    }

}
