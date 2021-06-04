/*
 * Copyright 2019 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class MictrackProtocolDecoder extends BaseProtocolDecoder {

    public MictrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_LOW_ALTITUDE = new PatternBuilder()
            .number("(dd)(dd)(dd).d+,")          // time (hhmmss.sss)
            .expression("([AV]),")               // validity
            .number("(d+)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d+)(dd.d+),")              // longitude
            .expression("([EW]),")
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .number("(d+.?d*)?,")                // altitude
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .compile();

    private Date decodeTime(String data) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.parse(data);
    }

    private String decodeAlarm(int event) {
        switch (event) {
            case 0:
                return Position.ALARM_POWER_ON;
            case 5:
                return Position.ALARM_SOS;
            case 8:
                return Position.ALARM_LOW_BATTERY;
            case 9:
                return Position.ALARM_GEOFENCE_ENTER;
            case 10:
                return Position.ALARM_GEOFENCE_EXIT;
            case 12:
                return Position.ALARM_POWER_OFF;
            default:
                return null;
        }
    }

    private void decodeLocation(Position position, String data) throws ParseException {
        int index = 0;
        String[] values = data.split("\\+");

        position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));

        position.setValid(true);
        position.setTime(decodeTime(values[index++]));
        position.setLatitude(Double.parseDouble(values[index++]));
        position.setLongitude(Double.parseDouble(values[index++]));
        position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(values[index++])));
        position.setCourse(Integer.parseInt(values[index++]));

        int event = Integer.parseInt(values[index++]);
        position.set(Position.KEY_ALARM, decodeAlarm(event));
        position.set(Position.KEY_EVENT, event);
        position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]) * 0.001);
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

    private void decodeWifi(Network network, String data) {
        String[] values = data.split(",");
        for (int i = 0; i < values.length / 2; i++) {
            network.addWifiAccessPoint(WifiAccessPoint.from(values[i * 2], Integer.parseInt(values[i * 2 + 1])));
        }
    }

    private void decodeNetwork(Position position, String data, boolean hasWifi, boolean hasCell) throws ParseException {
        int index = 0;
        String[] values = data.split("\\+");

        getLastLocation(position, decodeTime(values[index++]));

        Network network = new Network();

        if (hasWifi) {
            decodeWifi(network, values[index++]);
        }

        if (hasCell) {
            decodeCell(network, values[index++]);
        }

        position.setNetwork(network);

        int event = Integer.parseInt(values[index++]);
        position.set(Position.KEY_ALARM, decodeAlarm(event));
        position.set(Position.KEY_EVENT, event);
        position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]) * 0.001);
    }

    private void decodeStatus(Position position, String data) throws ParseException {
        int index = 0;
        String[] values = data.split("\\+");

        position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));

        getLastLocation(position, decodeTime(values[index++]));

        index += 4; // fix values

        int event = Integer.parseInt(values[index++]);
        position.set(Position.KEY_ALARM, decodeAlarm(event));
        position.set(Position.KEY_EVENT, event);
        position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]) * 0.001);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String sentence = ((String) msg).trim();

        if (sentence.startsWith("MT")) {
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
            case "R0":
                decodeLocation(position, fragments[4]);
                break;
            case "R1":
                decodeNetwork(position, fragments[4], true, false);
                break;
            case "R2":
            case "R3":
                decodeNetwork(position, fragments[4], false, true);
                break;
            case "R12":
            case "R13":
                decodeNetwork(position, fragments[4], true, true);
                break;
            case "RH":
                decodeStatus(position, fragments[4]);
                break;
            default:
                return null;
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
