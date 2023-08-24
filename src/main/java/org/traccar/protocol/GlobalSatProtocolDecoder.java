/*
 * Copyright 2013 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class GlobalSatProtocolDecoder extends BaseProtocolDecoder {

    private String format0;
    private String format1;

    public GlobalSatProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected void init() {
        format0 = getConfig().getString(getProtocolName() + ".format0", "TSPRXAB27GHKLMnaicz*U!");
        format1 = getConfig().getString(getProtocolName() + ".format1", "SARY*U!");
    }

    public void setFormat0(String format) {
        format0 = format;
    }

    public void setFormat1(String format) {
        format1 = format;
    }

    private Double decodeVoltage(String value) {
        if (value.endsWith("mV")) {
            return Integer.parseInt(value.substring(0, value.length() - 2)) / 1000.0;
        } else if (value.endsWith("V")) {
            return Double.parseDouble(value.substring(0, value.length() - 1));
        } else {
            return null;
        }
    }

    private String decodeAlarm(int value) {
        if (BitUtil.check(value, 0)) {
            return Position.ALARM_SOS;
        }
        if (BitUtil.check(value, 3) || BitUtil.check(value, 4)) {
            return Position.ALARM_GEOFENCE;
        }
        if (BitUtil.check(value, 5)) {
            return Position.ALARM_OVERSPEED;
        }
        if (BitUtil.check(value, 6)) {
            return Position.ALARM_POWER_CUT;
        }
        if (BitUtil.check(value, 7)) {
            return Position.ALARM_LOW_POWER;
        }
        return null;
    }

    private Position decodeOriginal(Channel channel, SocketAddress remoteAddress, String sentence) {

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage("ACK\r", remoteAddress));
        }

        String format;
        if (sentence.startsWith("GSr") || sentence.startsWith("GSb")) {
            format = format0;
        } else if (sentence.startsWith("GSh")) {
            format = format1;
        } else {
            return null;
        }

        // Check that message contains required parameters
        if (!format.contains("B") || !format.contains("S") || !(format.contains("1")
                || format.contains("2") || format.contains("3")) || !(format.contains("6")
                || format.contains("7") || format.contains("8"))) {
            return null;
        }

        if (format.contains("*")) {
            format = format.substring(0, format.indexOf('*'));
            sentence = sentence.substring(0, sentence.indexOf('*'));
        }
        String[] values = sentence.split(",");

        Position position = new Position(getProtocolName());

        CellTower cellTower = new CellTower();

        for (int formatIndex = 0, valueIndex = 1; formatIndex < format.length()
                && valueIndex < values.length; formatIndex++) {
            String value = values[valueIndex].replace("\"", "");

            switch (format.charAt(formatIndex)) {
                case 'S':
                    DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, value);
                    if (deviceSession == null) {
                        return null;
                    }
                    position.setDeviceId(deviceSession.getDeviceId());
                    break;
                case 'A':
                    if (value.isEmpty()) {
                        position.setValid(false);
                    } else {
                        position.setValid(Integer.parseInt(value) != 1);
                    }
                    break;
                case 'B':
                    DateBuilder dateBuilder = new DateBuilder()
                            .setDay(Integer.parseInt(value.substring(0, 2)))
                            .setMonth(Integer.parseInt(value.substring(2, 4)))
                            .setYear(Integer.parseInt(value.substring(4)));
                    value = values[++valueIndex];
                    dateBuilder
                            .setHour(Integer.parseInt(value.substring(0, 2)))
                            .setMinute(Integer.parseInt(value.substring(2, 4)))
                            .setSecond(Integer.parseInt(value.substring(4)));
                    position.setTime(dateBuilder.getDate());
                    break;
                case 'C':
                    valueIndex += 1;
                    break;
                case '1':
                    double longitude = Double.parseDouble(value.substring(1));
                    if (value.charAt(0) == 'W') {
                        longitude = -longitude;
                    }
                    position.setLongitude(longitude);
                    break;
                case '2':
                    longitude = Double.parseDouble(value.substring(4)) / 60;
                    longitude += Integer.parseInt(value.substring(1, 4));
                    if (value.charAt(0) == 'W') {
                        longitude = -longitude;
                    }
                    position.setLongitude(longitude);
                    break;
                case '3':
                    position.setLongitude(Double.parseDouble(value) * 0.000001);
                    break;
                case '6':
                    double latitude = Double.parseDouble(value.substring(1));
                    if (value.charAt(0) == 'S') {
                        latitude = -latitude;
                    }
                    position.setLatitude(latitude);
                    break;
                case '7':
                    latitude = Double.parseDouble(value.substring(3)) / 60;
                    latitude += Integer.parseInt(value.substring(1, 3));
                    if (value.charAt(0) == 'S') {
                        latitude = -latitude;
                    }
                    position.setLatitude(latitude);
                    break;
                case '8':
                    position.setLatitude(Double.parseDouble(value) * 0.000001);
                    break;
                case 'G':
                    position.setAltitude(Double.parseDouble(value));
                    break;
                case 'H':
                    position.setSpeed(Double.parseDouble(value));
                    break;
                case 'I':
                    position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(value)));
                    break;
                case 'J':
                    position.setSpeed(UnitsConverter.knotsFromMph(Double.parseDouble(value)));
                    break;
                case 'K':
                    position.setCourse(Double.parseDouble(value));
                    break;
                case 'L':
                    position.set(Position.KEY_SATELLITES, Integer.parseInt(value));
                    break;
                case 'P':
                    if (value.length() == 4) {
                        position.set(Position.KEY_ALARM, decodeAlarm(Integer.parseInt(value, 16)));
                    }
                    break;
                case 'Z':
                    if (!value.isEmpty()) {
                        position.set("geofence", value);
                    }
                    break;
                case 'Y':
                    int io = Integer.parseInt(value, 16);
                    position.set(Position.PREFIX_IN + 1, BitUtil.check(io, 1));
                    position.set(Position.KEY_MOTION, BitUtil.check(io, 7));
                    position.set(Position.PREFIX_OUT + 1, BitUtil.check(io, 9));
                    position.set(Position.KEY_IGNITION, BitUtil.check(io, 13));
                    position.set(Position.KEY_CHARGE, BitUtil.check(io, 15));
                    break;
                case 'c':
                    cellTower.setSignalStrength(Integer.parseInt(value));
                    break;
                case 'm':
                    position.set(Position.KEY_POWER, decodeVoltage(value));
                    break;
                case 'n':
                case 'N':
                    position.set(Position.KEY_BATTERY, decodeVoltage(value));
                    break;
                case 't':
                    cellTower.setMobileCountryCode(Integer.parseInt(value));
                    break;
                case 'u':
                    cellTower.setMobileNetworkCode(Integer.parseInt(value));
                    break;
                case 'v':
                    cellTower.setLocationAreaCode(Integer.parseInt(value, 16));
                    break;
                case 'w':
                    cellTower.setCellId(Long.parseLong(value, 16));
                    break;
                default:
                    // Unsupported
                    break;
            }

            valueIndex += 1;
        }

        if (cellTower.getCellId() != null) {
            position.setNetwork(new Network(cellTower));
        }

        return position;
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$")
            .number("(d+),")                     // imei
            .number("d+,")                       // mode
            .number("(d+),")                     // fix
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([EW])")
            .number("(ddd)(dd.d+),")             // longitude (dddmm.mmmm)
            .expression("([NS])")
            .number("(dd)(dd.d+),")              // latitude (ddmm.mmmm)
            .number("(d+.?d*),")                 // altitude
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*)?,")                // course
            .number("(d+)[,*]")                  // satellites
            .number("(d+.?d*)")                  // hdop
            .compile();

    private Position decodeAlternative(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        position.setValid(!parser.next().equals("1"));
        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setAltitude(parser.nextDouble(0));
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        position.set(Position.KEY_SATELLITES, parser.nextInt(0));
        position.set(Position.KEY_HDOP, parser.nextDouble());

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("GS")) {
            return decodeOriginal(channel, remoteAddress, sentence);
        } else if (sentence.startsWith("$")) {
            return decodeAlternative(channel, remoteAddress, sentence);
        }

        return null;
    }

}
