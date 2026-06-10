/*
 * Copyright 2019 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2020 Roeland Boeters (roeland@geodelta.com)
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
import org.traccar.helper.DateUtil;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class MictrackProtocolDecoder extends BaseProtocolDecoder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyMMddHHmmss").withZone(ZoneOffset.UTC);

    public MictrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_MT700_POSITION = new PatternBuilder()
            .text("#")
            .number("(?:(dd|dddd)|x*)")          // voltage (optional capture)
            .groupBegin()
            .text("#")
            .groupBegin()
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(x+),")                     // lac
            .number("(x+)")                      // cell id
            .groupEnd("?")
            .groupEnd("?")
            .text("$GPRMC,")
            .number("(?:(dd)(dd)(dd).d+)?,")     // time (hhmmss.sss)
            .expression("([AVL]),")              // validity
            .groupBegin()
            .number("(d+)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d+)(dd.d+),")              // longitude
            .number("([EW]),")
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .groupEnd("?")
            .any()
            .compile();

    private static final Pattern PATTERN_MT700_WIFI = new PatternBuilder()
            .text("#")
            .number("(?:(dd|dddd)|x+)")          // voltage (optional capture)
            .expression("#?")
            .groupBegin()
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(x+),")                     // lac
            .number("(x+)")                      // cell id
            .groupEnd("?")
            .text("$WIFI,")
            .number("(dd)(dd)(dd).d+,")          // time (hhmmss.sss)
            .expression("[AVL],")                // validity
            .expression("(.*)")                  // access points
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .text("*")
            .number("xx")                        // checksum
            .compile();

    private static final Pattern PATTERN_LOW_ALTITUDE = new PatternBuilder()
            .number("(dd)(dd)(dd).d+,")          // time (hhmmss.sss)
            .expression("([AV]),")               // validity
            .number("(d+)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d+)(dd.d+),")              // longitude
            .expression("([EW]),")
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .number("(-?d+.?d*)?,")              // altitude
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .compile();

    private String decodeMT700Alarm(String event) {
        return switch (event) {
            case "SHAKE" -> Position.ALARM_VIBRATION;
            case "TOWED" -> Position.ALARM_TOW;
            case "DEF" -> Position.ALARM_TAMPERING;
            case "BLP" -> Position.ALARM_LOW_BATTERY;
            case "SOS" -> Position.ALARM_SOS;
            case "OVERSPEED" -> Position.ALARM_OVERSPEED;
            case "OS" -> Position.ALARM_GEOFENCE_EXIT;
            case "RS" -> Position.ALARM_GEOFENCE_ENTER;
            default -> null;
        };
    }

    private Object decodeMT700(Channel channel, SocketAddress remoteAddress, String sentence) {
        String[] lines = sentence.split("\r?\n");
        if (lines.length < 2) {
            return null;
        }

        String[] header = lines[0].split("#");
        if (header.length < 5) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, header[1]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.addAlarm(decodeMT700Alarm(header[4]));

        String body = lines[1];

        if (body.contains("$GPRMC")) {

            Parser parser = new Parser(PATTERN_MT700_POSITION, body);
            if (!parser.matches()) {
                return null;
            }

            if (parser.hasNext()) {
                int voltage = parser.nextInt();
                position.set(Position.KEY_BATTERY, voltage > 100 ? voltage / 1000.0 : voltage / 10.0);
            }

            if (parser.hasNext(4)) {
                Network network = new Network();
                network.addCellTower(CellTower.from(
                        parser.nextInt(), parser.nextInt(), parser.nextHexInt(), parser.nextHexInt()));
                position.setNetwork(network);
            }

            DateBuilder dateBuilder = new DateBuilder();
            if (parser.hasNext(3)) {
                dateBuilder.setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
            }

            position.setValid(parser.next().equals("A"));

            if (parser.hasNext()) {
                position.setLatitude(parser.nextCoordinate());
                position.setLongitude(parser.nextCoordinate());
                position.setSpeed(parser.nextDouble(0));
                position.setCourse(parser.nextDouble(0));
                dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
                position.setTime(dateBuilder.getDate());
            } else {
                getLastLocation(position, null);
            }

        } else if (body.contains("$WIFI")) {

            Parser parser = new Parser(PATTERN_MT700_WIFI, body);
            if (!parser.matches()) {
                return null;
            }

            if (parser.hasNext()) {
                int voltage = parser.nextInt();
                position.set(Position.KEY_BATTERY, voltage > 100 ? voltage / 1000.0 : voltage / 10.0);
            }

            Network network = new Network();
            if (parser.hasNext(4)) {
                network.addCellTower(CellTower.from(
                        parser.nextInt(), parser.nextInt(), parser.nextHexInt(), parser.nextHexInt()));
            }

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

            for (String ap : parser.next().split(",(?=-)")) {
                String[] parts = ap.split(",", 2);
                if (parts.length == 2 && !parts[1].isEmpty()) {
                    try {
                        String mac = parts[1].replaceAll("(..)", "$1:").substring(0, 17);
                        network.addWifiAccessPoint(WifiAccessPoint.from(mac, Integer.parseInt(parts[0])));
                    } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {
                    }
                }
            }
            position.setNetwork(network);

            dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
            getLastLocation(position, dateBuilder.getDate());

        } else {
            return null;
        }

        return position;
    }

    private String decodeAlarm(int event) {
        return switch (event) {
            case 0 -> Position.ALARM_POWER_ON;
            case 5 -> Position.ALARM_SOS;
            case 8 -> Position.ALARM_LOW_BATTERY;
            case 9 -> Position.ALARM_GEOFENCE_ENTER;
            case 10 -> Position.ALARM_GEOFENCE_EXIT;
            case 12 -> Position.ALARM_POWER_OFF;
            default -> null;
        };
    }

    private void decodeLocation(Position position, String data) {
        int index = 0;
        String[] values = data.split("\\+");

        position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));

        position.setValid(true);
        position.setTime(DateUtil.parse(DATE_FORMAT, values[index++]));
        position.setLatitude(Double.parseDouble(values[index++]));
        position.setLongitude(Double.parseDouble(values[index++]));
        position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(values[index++])));
        position.setCourse(Integer.parseInt(values[index++]));

        int event = Integer.parseInt(values[index++]);
        position.addAlarm(decodeAlarm(event));
        position.set(Position.KEY_EVENT, event);
        position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]) / 1000.0);
    }

    private void decodeCell(Network network, String data) {
        String[] values = data.split(",");
        int length = values.length % 5 == 0 ? 5 : 4;
        for (int i = 0; i < values.length / length; i++) {
            int mnc = Integer.parseInt(values[i * length]);
            int cid = Integer.parseInt(values[i * length + 1]);
            int lac = Integer.parseInt(values[i * length + 2]);
            int mcc = Integer.parseInt(values[i * length + 3]);
            network.addCellTower(CellTower.from(mcc, mnc, lac, cid));
        }
    }

    private void decodeWifi(Network network, String data, boolean hasSsid) {
        String[] values = data.split(",");
        int step = hasSsid ? 3 : 2;
        int offset = hasSsid ? 1 : 0;
        for (int i = 0; i < values.length / step; i++) {
            network.addWifiAccessPoint(WifiAccessPoint.from(
                    values[i * step + offset], Integer.parseInt(values[i * step + offset + 1])));
        }
    }

    private void decodeNetwork(
            Position position, String data, boolean hasWifi, boolean hasSsid, boolean hasCell) {
        int index = 0;
        String[] values = data.split("\\+");

        getLastLocation(position, DateUtil.parse(DATE_FORMAT, values[index++]));

        Network network = new Network();

        if (hasWifi) {
            decodeWifi(network, values[index++], hasSsid);
        }

        if (hasCell) {
            decodeCell(network, values[index++]);
        }

        position.setNetwork(network);

        int event = Integer.parseInt(values[index++]);
        position.addAlarm(decodeAlarm(event));
        position.set(Position.KEY_EVENT, event);
        position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]) / 1000.0);
    }

    private void decodeStatus(Position position, String data) {
        int index = 0;
        String[] values = data.split("\\+");

        position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));

        getLastLocation(position, DateUtil.parse(DATE_FORMAT, values[index++]));

        index += 4; // fix values

        int event = Integer.parseInt(values[index++]);
        position.addAlarm(decodeAlarm(event));
        position.set(Position.KEY_EVENT, event);
        position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]) / 1000.0);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String sentence = ((String) msg).trim();

        if (sentence.startsWith("#")) {
            return decodeMT700(channel, remoteAddress, sentence);
        } else if (sentence.startsWith("MT")) {
            return decodeStandard(channel, remoteAddress, sentence);
        } else if (sentence.contains("$")) {
            return decodeLowAltitude(channel, remoteAddress, sentence);
        } else {
            return decodeResult(channel, remoteAddress, sentence);
        }
    }

    private Object decodeResult(
            Channel channel, SocketAddress remoteAddress, String sentence) {

        if (sentence.matches("\\d{15} .+")) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, sentence.substring(0, 15));
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            position.set(Position.KEY_RESULT, sentence.substring(16, sentence.length() - 1));

            return position;

        } else {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            position.set(Position.KEY_RESULT, sentence.substring(0, sentence.length() - 1));

            return position;

        }
    }

    private Object decodeStandard(
            Channel channel, SocketAddress remoteAddress, String sentence) throws Exception {
        String[] fragments = sentence.split(";");

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, fragments[2]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_TYPE, Integer.parseInt(fragments[1]));

        switch (fragments[3]) {
            case "R0" -> decodeLocation(position, fragments[4]);
            case "R1" -> decodeNetwork(position, fragments[4], true, false, false);
            case "R2", "R3" -> decodeNetwork(position, fragments[4], false, false, true);
            case "R12", "R13" -> decodeNetwork(position, fragments[4], true, false, true);
            case "RH" -> decodeStatus(position, fragments[4]);
            case "Y1" -> decodeNetwork(position, fragments[4], true, true, false);
            default -> {
                return null;
            }
        }

        return position;
    }

    private Object decodeLowAltitude(
            Channel channel, SocketAddress remoteAddress, String sentence) {
        int separator = sentence.indexOf("$");
        if (separator < 0) {
            return null;
        }
        String deviceId = sentence.substring(0, separator);

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, deviceId);
        if (deviceSession == null) {
            return null;
        }

        String[] fragments = sentence.substring(sentence.indexOf("$")).split("\\$");

        List<Position> positions = new LinkedList<>();

        for (String message : fragments) {
            Parser parser = new Parser(PATTERN_LOW_ALTITUDE, message);

            if (parser.matches()) {

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                DateBuilder dateBuilder = new DateBuilder()
                        .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

                position.setValid(parser.next().equals("A"));
                position.setLatitude(parser.nextCoordinate());
                position.setLongitude(parser.nextCoordinate());

                position.setSpeed(parser.nextDouble(0));
                position.setCourse(parser.nextDouble(0));
                position.setAltitude(parser.nextDouble(0));

                dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
                position.setTime(dateBuilder.getDate());

                positions.add(position);
            }
        }

        return positions;
    }

}
