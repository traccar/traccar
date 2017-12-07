/*
 * Copyright 2012 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class Tk103ProtocolDecoder extends BaseProtocolDecoder {

    private boolean decodeLow;

    public Tk103ProtocolDecoder(Tk103Protocol protocol) {
        super(protocol);
        decodeLow = Context.getConfig().getBoolean(getProtocolName() + ".decodeLow");
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("(").optional()
            .number("(d+)(,)?")                  // device id
            .expression("(.{4}),?")              // command
            .number("(d*)")
            .number("(dd)(dd)(dd),?")            // date (mmddyy if comma-delimited, otherwise yyddmm)
            .expression("([AV]),?")              // validity
            .number("(d+)(dd.d+)")               // latitude
            .expression("([NS]),?")
            .number("(d+)(dd.d+)")               // longitude
            .expression("([EW]),?")
            .number("(d+.d)(?:d*,)?")            // speed
            .number("(dd)(dd)(dd),?")            // time (hhmmss)
            .number("(d+.?d{1,2}),?")            // course
            .groupBegin()
            .number("([01])")                    // charge
            .number("([01])")                    // ignition
            .number("(x)")                       // io
            .number("(x)")                       // io
            .number("(x)")                       // io
            .number("(xxx),?")                   // fuel
            .groupEnd("?")
            .number("(?:L(x+))?")                // odometer
            .any()
            .number("([+-]ddd.d)?")              // temperature
            .groupBegin()
            .number("([+-]?d+.d{1,2}),")         // altitude
            .number("(d+)$")                     // number of visible satellites
            .groupEnd("?")
            .text(")").optional()
            .compile();

    private static final Pattern PATTERN_BATTERY = new PatternBuilder()
            .text("(").optional()
            .number("(d+),")                     // device id
            .text("ZC20,")
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(d+),")                     // battery level
            .number("(d+),")                     // battery voltage
            .number("(d+),")                     // power voltage
            .number("d+")                        // installed
            .any()
            .compile();

    private static final Pattern PATTERN_NETWORK = new PatternBuilder()
            .text("(").optional()
            .number("(d{12})")                   // device id
            .text("BZ00,")
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(x+),")                     // lac
            .number("(x+),")                     // cid
            .any()
            .compile();

    private static final Pattern PATTERN_LBSWIFI = new PatternBuilder()
            .text("(").optional()
            .number("(d+),")                     // device id
            .expression("(.{4}),")               // command
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(d+),")                     // lac
            .number("(d+),")                     // cid
            .number("(d+),")                     // number of wifi macs
            .number("((?:(?:xx:){5}(?:xx)\\*[-+]?d+\\*d+,)*)")
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .any()
            .compile();

    private static final Pattern PATTERN_COMMAND_RESULT = new PatternBuilder()
            .text("(").optional()
            .number("(d+),")                     // device id
            .expression(".{4},")                 // command
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("\\$([\\s\\S]*?)(?:\\$|$)") // message
            .any()
            .compile();

    private String decodeAlarm(int value) {
        switch (value) {
            case 1:
                return Position.ALARM_ACCIDENT;
            case 2:
                return Position.ALARM_SOS;
            case 3:
                return Position.ALARM_VIBRATION;
            case 4:
                return Position.ALARM_LOW_SPEED;
            case 5:
                return Position.ALARM_OVERSPEED;
            case 6:
                return Position.ALARM_GEOFENCE_EXIT;
            default:
                return null;
        }
    }

    private void decodeType(Position position, String type, String data) {
        switch (type) {
            case "BO01":
                position.set(Position.KEY_ALARM, decodeAlarm(data.charAt(0) - '0'));
                break;
            case "ZC11":
            case "DW31":
            case "DW51":
                position.set(Position.KEY_ALARM, Position.ALARM_MOVEMENT);
                break;
            case "ZC12":
            case "DW32":
            case "DW52":
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                break;
            case "ZC13":
            case "DW33":
            case "DW53":
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
                break;
            case "ZC15":
            case "DW35":
            case "DW55":
                position.set(Position.KEY_IGNITION, true);
                break;
            case "ZC16":
            case "DW36":
            case "DW56":
                position.set(Position.KEY_IGNITION, false);
                break;
            case "ZC29":
            case "DW42":
            case "DW62":
                position.set(Position.KEY_IGNITION, true);
                break;
            case "ZC17":
            case "DW37":
            case "DW57":
                position.set(Position.KEY_ALARM, Position.ALARM_REMOVING);
                break;
            case "ZC25":
            case "DW3E":
            case "DW5E":
                position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                break;
            case "ZC26":
            case "DW3F":
            case "DW5F":
                position.set(Position.KEY_ALARM, Position.ALARM_TAMPERING);
                break;
            case "ZC27":
            case "DW40":
            case "DW60":
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_POWER);
                break;
            default:
                break;
        }
    }

    private Integer decodeBattery(int value) {
        switch (value) {
            case 6:
                return 100;
            case 5:
                return 80;
            case 4:
                return 50;
            case 3:
                return 20;
            case 2:
                return 10;
            default:
                return null;
        }
    }

    private Position decodeBattery(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_BATTERY, sentence);
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

        getLastLocation(position, parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        int batterylevel = parser.nextInt(0);
        if (batterylevel != 255) {
            position.set(Position.KEY_BATTERY_LEVEL, decodeBattery(batterylevel));
        }

        int battery = parser.nextInt(0);
        if (battery != 65535) {
            position.set(Position.KEY_BATTERY, battery * 0.01);
        }

        int power = parser.nextInt(0);
        if (power != 65535) {
            position.set(Position.KEY_POWER, power * 0.1);
        }

        return position;
    }

    private Position decodeNetwork(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_NETWORK, sentence);
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

        getLastLocation(position, null);

        position.setNetwork(new Network(CellTower.from(
                parser.nextInt(0), parser.nextInt(0), parser.nextHexInt(0), parser.nextHexInt(0))));

        return position;
    }

    private Position decodeLbsWifi(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_LBSWIFI, sentence);
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

        decodeType(position, parser.next(), "0");

        getLastLocation(position, null);

        Network network = new Network();

        network.addCellTower(CellTower.from(
                parser.nextInt(), parser.nextInt(), parser.nextInt(), parser.nextInt()));

        int wifiCount = parser.nextInt();
        if (parser.hasNext()) {
            String[] wifimacs = parser.next().split(",");
            if (wifimacs.length == wifiCount) {
                for (int i = 0; i < wifiCount; i++) {
                    String[] wifiinfo = wifimacs[i].split("\\*");
                    network.addWifiAccessPoint(WifiAccessPoint.from(
                            wifiinfo[0], Integer.parseInt(wifiinfo[1]), Integer.parseInt(wifiinfo[2])));
                }
            }
        }

        if (network.getCellTowers() != null || network.getWifiAccessPoints() != null) {
            position.setNetwork(network);
        }

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        return position;
    }

    private Position decodeCommandResult(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_COMMAND_RESULT, sentence);
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

        getLastLocation(position, parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.set(Position.KEY_RESULT, parser.next());

        return position;

    }

@Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (channel != null) {
            String id = sentence.substring(1, 13);
            String type = sentence.substring(13, 17);
            if (type.equals("BP00")) {
                channel.write("(" + id + "AP01HSO)");
                return null;
            } else if (type.equals("BP05")) {
                channel.write("(" + id + "AP05)");
            }
        }

        if (sentence.contains("ZC20")) {
            return decodeBattery(channel, remoteAddress, sentence);
        } else if (sentence.contains("BZ00")) {
            return decodeNetwork(channel, remoteAddress, sentence);
        } else if (sentence.contains("ZC03")) {
            return decodeCommandResult(channel, remoteAddress, sentence);
        } else if (sentence.contains("DW5")) {
            return decodeLbsWifi(channel, remoteAddress, sentence);
        }

        Parser parser = new Parser(PATTERN, sentence);
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

        boolean alternative = parser.next() != null;

        decodeType(position, parser.next(), parser.next());

        DateBuilder dateBuilder = new DateBuilder();
        if (alternative) {
            dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        } else {
            dateBuilder.setDate(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        }

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        position.setSpeed(convertSpeed(parser.nextDouble(0), "kmh"));

        dateBuilder.setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        position.setCourse(parser.nextDouble(0));

        if (parser.hasNext(6)) {
            position.set(Position.KEY_CHARGE, parser.nextInt() == 0);
            position.set(Position.KEY_IGNITION, parser.nextInt() == 1);

            int mask1 = parser.nextHexInt();
            position.set(Position.PREFIX_IN + 2, BitUtil.check(mask1, 0) ? 1 : 0);
            position.set("panic", BitUtil.check(mask1, 1) ? 1 : 0);
            position.set(Position.PREFIX_OUT + 2, BitUtil.check(mask1, 2) ? 1 : 0);
            if (decodeLow || BitUtil.check(mask1, 3)) {
                position.set(Position.KEY_BLOCKED, BitUtil.check(mask1, 3) ? 1 : 0);
            }

            int mask2 = parser.nextHexInt();
            for (int i = 0; i < 3; i++) {
                if (decodeLow || BitUtil.check(mask2, i)) {
                    position.set("hs" + (3 - i), BitUtil.check(mask2, i) ? 1 : 0);
                }
            }
            if (decodeLow || BitUtil.check(mask2, 3)) {
                position.set(Position.KEY_DOOR, BitUtil.check(mask2, 3) ? 1 : 0);
            }

            int mask3 = parser.nextHexInt();
            for (int i = 1; i <= 3; i++) {
                if (decodeLow || BitUtil.check(mask3, i)) {
                    position.set("ls" + (3 - i + 1), BitUtil.check(mask3, i) ? 1 : 0);
                }
            }

            position.set(Position.KEY_FUEL_LEVEL, parser.nextHexInt());
        }

        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextLong(16, 0));
        }

        if (parser.hasNext()) {
            position.set(Position.PREFIX_TEMP + 1, parser.nextDouble(0));
        }

        if (parser.hasNext()) {
            position.setAltitude(parser.nextDouble(0));
        }

        if (parser.hasNext()) {
            position.set(Position.KEY_SATELLITES_VISIBLE, parser.nextInt(0));
        }

        return position;
    }

}
