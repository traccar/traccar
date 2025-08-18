/*
 * Copyright 2012 - 2023 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DataConverter;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.config.Keys;
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

    public Tk103ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected void init() {
        decodeLow = getConfig().getBoolean(Keys.PROTOCOL_DECODE_LOW.withPrefix(getProtocolName()));
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("(").optional()
            .groupBegin()
            .expression("(.{12})")               // device id
            .or()
            .expression("([^,]+),")              // device id
            .groupEnd()
            .expression("(.{4}),?")              // command
            .groupBegin()
            .number("(d*)")
            .or()
            .text(",ALARM,")
            .number("(d),")                      // alarm type
            .number("d+,")
            .groupEnd()
            .number("(dd)(dd)(dd),?")            // date (mmddyy if comma-delimited, otherwise yyddmm)
            .expression("([AV]),?")              // validity
            .number(" *(d*)(dd.d+)")             // latitude
            .expression("([NS]),?")
            .number(" *(d*)(dd.d+)")             // longitude
            .expression("([EW]),?")
            .number("([ d.]{1,5})(?:d*,)?")      // speed
            .number("(dd)(dd)(dd),?")            // time (hhmmss)
            .groupBegin()
            .number("(?:([ d.]{6})|(dd)),?")     // course
            .number("([01])")                    // charge
            .number("([01])")                    // ignition
            .number("(x)")                       // io
            .number("(x)")                       // io
            .number("(x)")                       // io
            .number("(xxx)")                     // fuel
            .number("L(x+)")                     // odometer
            .or()
            .number("(d+.d+)")                   // course
            .groupEnd()
            .any()
            .number("([+-]ddd.d)?")              // temperature
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

    private static final Pattern PATTERN_CELL = new PatternBuilder()
            .text("(")
            .number("(d{12})")                   // device id
            .expression(".{4}")                  // type
            .number("(?:d{15})?,")               // imei
            .expression("(.+),")                 // cell
            .number("(d{8})")                    // odometer
            .text(")")
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

    private static final Pattern PATTERN_VIN = new PatternBuilder()
            .text("(")
            .number("(d+)")                      // device id
            .expression("BV00")                  // command
            .expression("(.{17})")               // vin
            .text(")")
            .compile();

    private String decodeAlarm(int value) {
        return switch (value) {
            case 1 -> Position.ALARM_ACCIDENT;
            case 2 -> Position.ALARM_SOS;
            case 3 -> Position.ALARM_VIBRATION;
            case 4 -> Position.ALARM_LOW_SPEED;
            case 5 -> Position.ALARM_OVERSPEED;
            case 6 -> Position.ALARM_GEOFENCE_EXIT;
            default -> null;
        };
    }

    private void decodeType(Position position, String type, String data) {
        switch (type) {
            case "BQ81" -> {
                switch (Integer.parseInt(data)) {
                    case 0 -> position.addAlarm(Position.ALARM_LOW_BATTERY);
                    case 1 -> position.addAlarm(Position.ALARM_OVERSPEED);
                    case 2 -> position.addAlarm(Position.ALARM_IDLE);
                    case 3 -> position.addAlarm(Position.ALARM_ACCELERATION);
                    case 4 -> position.addAlarm(Position.ALARM_BRAKING);
                    case 5 -> position.addAlarm(Position.ALARM_TEMPERATURE);
                }
            }
            case "BO01" -> position.addAlarm(decodeAlarm(data.charAt(0) - '0'));
            case "ZC11", "DW31", "DW51" -> position.addAlarm(Position.ALARM_MOVEMENT);
            case "ZC12", "DW32", "DW52" -> position.addAlarm(Position.ALARM_LOW_BATTERY);
            case "ZC13", "DW33", "DW53" -> position.addAlarm(Position.ALARM_POWER_CUT);
            case "ZC15", "DW35", "DW55" -> position.set(Position.KEY_IGNITION, true);
            case "ZC16", "DW36", "DW56" -> position.set(Position.KEY_IGNITION, false);
            case "ZC29", "DW42", "DW62" -> position.set(Position.KEY_IGNITION, true);
            case "ZC17", "DW37", "DW57" -> position.addAlarm(Position.ALARM_REMOVING);
            case "ZC25", "DW3E", "DW5E" -> position.addAlarm(Position.ALARM_SOS);
            case "ZC26", "DW3F", "DW5F" -> position.addAlarm(Position.ALARM_TAMPERING);
            case "ZC27", "DW40", "DW60" -> position.addAlarm(Position.ALARM_LOW_POWER);
        }
    }

    private Integer decodeBattery(int value) {
        return switch (value) {
            case 6 -> 100;
            case 5 -> 80;
            case 4 -> 50;
            case 3 -> 20;
            case 2 -> 10;
            default -> null;
        };
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

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        int batteryLevel = parser.nextInt(0);
        if (batteryLevel != 255) {
            position.set(Position.KEY_BATTERY_LEVEL, decodeBattery(batteryLevel));
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

    private Position decodeCell(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_CELL, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        Network network = new Network();

        String[] cells = parser.next().split("\n");
        for (String cell : cells) {
            String[] values = cell.substring(1, cell.length() - 1).split(",");
            network.addCellTower(CellTower.from(
                    Integer.parseInt(values[0]), Integer.parseInt(values[1]),
                    Integer.parseInt(values[2]), Integer.parseInt(values[3])));
        }

        position.setNetwork(network);

        position.set(Position.KEY_ODOMETER, parser.nextLong(16, 0));

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

        Position position = new Position(getProtocolName());
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

        Position position = new Position(getProtocolName());
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

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

        position.set(Position.KEY_RESULT, parser.next());

        return position;
    }

    private Position decodeVin(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_VIN, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        position.set(Position.KEY_VIN, parser.next());

        return position;
    }

    private Position decodeBms(Channel channel, SocketAddress remoteAddress, String sentence) {
        String id = sentence.substring(1, 13);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        String payload = sentence.substring(1 + 12 + 4, sentence.length() - 1);

        if (sentence.startsWith("BS50", 1 + 12)) {

            ByteBuf buf = Unpooled.wrappedBuffer(DataConverter.parseHex(payload));

            buf.readUnsignedByte();
            buf.readUnsignedByte();
            buf.readUnsignedByte(); // header

            int batteryCount = buf.readUnsignedByte();
            for (int i = 1; i <= 24; i++) {
                int voltage = buf.readUnsignedShortLE();
                if (i <= batteryCount) {
                    position.set("battery" + i, voltage * 0.001);
                }
            }

            position.set(Position.KEY_CHARGE, buf.readUnsignedByte() == 0);
            position.set("current", buf.readUnsignedShortLE() * 0.1);
            position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.01);
            position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
            position.set("batteryOverheat", buf.readUnsignedByte() > 0);
            position.set("chargeProtection", buf.readUnsignedByte() > 0);
            position.set("dischargeProtection", buf.readUnsignedByte() > 0);
            buf.readUnsignedByte(); // drop line
            buf.readUnsignedByte(); // balanced
            position.set("cycles", buf.readUnsignedShortLE());
            position.set("faultAlarm", buf.readUnsignedByte());

            buf.skipBytes(6);

            int temperatureCount = buf.readUnsignedByte();
            position.set("powerTemp", buf.readUnsignedByte() - 40);
            position.set("equilibriumTemp", buf.readUnsignedByte() - 40);
            for (int i = 1; i <= 7; i++) {
                int temperature = buf.readUnsignedByte() - 40;
                if (i <= temperatureCount) {
                    position.set("batteryTemp" + i, temperature);
                }
            }

            position.set("calibrationCapacity", buf.readUnsignedShortLE() * 0.01);
            position.set("dischargeCapacity", buf.readUnsignedIntLE());

        } else {

            String[] values = payload.split(",");
            for (String value : values) {
                String[] pair = value.split(":");
                int key = Integer.parseInt(pair[0], 16);
                ByteBuf buf = Unpooled.wrappedBuffer(DataConverter.parseHex(pair[1]));
                switch (key) {
                    case 0x90 -> {
                        position.set("cumulativeVoltage", buf.readUnsignedShortLE() * 0.1);
                        position.set("gatherVoltage", buf.readUnsignedShortLE() * 0.1);
                        position.set("current", (buf.readUnsignedShortLE() - 30000) * 0.1);
                        position.set("soc", buf.readUnsignedShortLE() * 0.1);
                    }
                    case 0x91 -> {
                        position.set("maxCellVoltage", buf.readUnsignedShortLE() * 0.001);
                        position.set("maxCellVoltageCount", buf.readUnsignedByte());
                        position.set("minCellVoltage", buf.readUnsignedShortLE() * 0.001);
                        position.set("minCellVoltageCount", buf.readUnsignedByte());
                    }
                    case 0x92 -> {
                        position.set("maxTemp", buf.readUnsignedByte() - 40);
                        position.set("maxTempCount", buf.readUnsignedByte());
                        position.set("minTemp", buf.readUnsignedByte() - 40);
                        position.set("minTempCount", buf.readUnsignedByte());
                    }
                    case 0x96 -> {
                        buf.readUnsignedByte(); // frame
                        while (buf.isReadable()) {
                            position.set("cellTemp" + buf.readerIndex(), buf.readUnsignedByte() - 40);
                        }
                    }
                }

            }

        }

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
                channel.writeAndFlush(new NetworkMessage("(" + id + "AP01HSO)", remoteAddress));
                return null;
            } else if (type.equals("BP05")) {
                channel.writeAndFlush(new NetworkMessage("(" + id + "AP05)", remoteAddress));
            }
        }

        if (sentence.indexOf('{') > 0 && sentence.indexOf('}') > 0) {
            return decodeCell(channel, remoteAddress, sentence);
        } else if (sentence.contains("ZC20")) {
            return decodeBattery(channel, remoteAddress, sentence);
        } else if (sentence.contains("BZ00")) {
            return decodeNetwork(channel, remoteAddress, sentence);
        } else if (sentence.contains("ZC03")) {
            return decodeCommandResult(channel, remoteAddress, sentence);
        } else if (sentence.contains("DW5")) {
            return decodeLbsWifi(channel, remoteAddress, sentence);
        } else if (sentence.contains("BV00")) {
            return decodeVin(channel, remoteAddress, sentence);
        } else if (sentence.contains("BS50") || sentence.contains("BS51")) {
            return decodeBms(channel, remoteAddress, sentence);
        }

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        String id = null;
        boolean alternative = false;
        if (parser.hasNext()) {
            id = parser.next();
        }
        if (parser.hasNext()) {
            id = parser.next();
            alternative = true;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        String type = parser.next();
        String data = null;
        if (parser.hasNext()) {
            data = parser.next();
        }
        if (parser.hasNext()) {
            data = parser.next();
        }
        decodeType(position, type, data);

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

        if (parser.hasNext()) {
            position.setCourse(parser.nextDouble());
        }
        if (parser.hasNext()) {
            position.setCourse(parser.nextDouble());
        }

        if (parser.hasNext(7)) {
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
            position.set(Position.KEY_ODOMETER, parser.nextLong(16, 0));
        }

        if (parser.hasNext()) {
            position.setCourse(parser.nextDouble());
        }

        if (parser.hasNext()) {
            position.set(Position.PREFIX_TEMP + 1, parser.nextDouble(0));
        }

        return position;
    }

}
