/*
 * Copyright 2015 - 2021 Anton Tananaev (anton@traccar.org)
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class GoSafeProtocolDecoder extends BaseProtocolDecoder {

    public GoSafeProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("*GS")                         // header
            .number("d+,")                       // protocol version
            .number("(d+),")                     // imei
            .expression("([^#]*)#?")             // data
            .compile();

    private static final Pattern PATTERN_OLD = new PatternBuilder()
            .text("*GS")                         // header
            .number("d+,")                       // protocol version
            .number("(d+),")                     // imei
            .text("GPS:")
            .number("(dd)(dd)(dd);")             // time (hhmmss)
            .number("d;").optional()             // fix type
            .expression("([AV]);")               // validity
            .number("([NS])(d+.d+);")            // latitude
            .number("([EW])(d+.d+);")            // longitude
            .number("(d+)?;")                    // speed
            .number("(d+);")                     // course
            .number("(d+.?d*)").optional()       // hdop
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .any()
            .compile();

    private void decodeFragment(Position position, String fragment) {

        int dataIndex = fragment.indexOf(':');
        int index = 0;
        String[] values;
        if (fragment.length() == dataIndex + 1) {
            values = new String[0];
        } else {
            values = fragment.substring(dataIndex + 1).split(";");
        }

        switch (fragment.substring(0, dataIndex)) {
            case "GPS":
                position.setValid(values[index++].equals("A"));
                position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));
                position.setLatitude(Double.parseDouble(values[index].substring(1)));
                if (values[index++].charAt(0) == 'S') {
                    position.setLatitude(-position.getLatitude());
                }
                position.setLongitude(Double.parseDouble(values[index].substring(1)));
                if (values[index++].charAt(0) == 'W') {
                    position.setLongitude(-position.getLongitude());
                }
                if (!values[index++].isEmpty()) {
                    position.setSpeed(UnitsConverter.knotsFromKph(Integer.parseInt(values[index - 1])));
                }
                position.setCourse(Integer.parseInt(values[index++]));
                if (index < values.length && !values[index++].isEmpty()) {
                    position.setAltitude(Integer.parseInt(values[index - 1]));
                }
                if (index < values.length && !values[index++].isEmpty()) {
                    position.set(Position.KEY_HDOP, Double.parseDouble(values[index - 1]));
                }
                if (index < values.length && !values[index++].isEmpty()) {
                    position.set(Position.KEY_VDOP, Double.parseDouble(values[index - 1]));
                }
                break;
            case "GSM":
                index += 1; // registration status
                index += 1; // signal strength
                position.setNetwork(new Network(CellTower.from(
                        Integer.parseInt(values[index++]), Integer.parseInt(values[index++]),
                        Integer.parseInt(values[index++], 16), Integer.parseInt(values[index++], 16),
                        Integer.parseInt(values[index++]))));
                break;
            case "COT":
                if (index < values.length) {
                    position.set(Position.KEY_ODOMETER, Long.parseLong(values[index++]));
                }
                if (index < values.length) {
                    String[] hours = values[index].split("-");
                    position.set(Position.KEY_HOURS, (Integer.parseInt(hours[0]) * 3600
                            + (hours.length > 1 ? Integer.parseInt(hours[1]) * 60 : 0)
                            + (hours.length > 2 ? Integer.parseInt(hours[2]) : 0)) * 1000);
                }
                break;
            case "ADC":
                position.set(Position.KEY_POWER, Double.parseDouble(values[index++]));
                if (index < values.length) {
                    position.set(Position.KEY_BATTERY, Double.parseDouble(values[index++]));
                }
                if (index < values.length) {
                    position.set(Position.PREFIX_ADC + 1, Double.parseDouble(values[index++]));
                }
                if (index < values.length) {
                    position.set(Position.PREFIX_ADC + 2, Double.parseDouble(values[index++]));
                }
                break;
            case "DTT":
                position.set(Position.KEY_STATUS, Integer.parseInt(values[index++], 16));
                if (!values[index++].isEmpty()) {
                    int io = Integer.parseInt(values[index - 1], 16);
                    position.set(Position.KEY_IGNITION, BitUtil.check(io, 0));
                    position.set(Position.PREFIX_IN + 1, BitUtil.check(io, 1));
                    position.set(Position.PREFIX_IN + 2, BitUtil.check(io, 2));
                    position.set(Position.PREFIX_IN + 3, BitUtil.check(io, 3));
                    position.set(Position.PREFIX_IN + 4, BitUtil.check(io, 4));
                    position.set(Position.PREFIX_OUT + 1, BitUtil.check(io, 5));
                    position.set(Position.PREFIX_OUT + 2, BitUtil.check(io, 6));
                    position.set(Position.PREFIX_OUT + 3, BitUtil.check(io, 7));
                }
                position.set(Position.KEY_GEOFENCE, values[index++] + values[index++]);
                position.set("eventStatus", values[index++]);
                if (index < values.length) {
                    position.set("packetType", values[index++]);
                }
                break;
            case "ETD":
                position.set("eventData", values[index++]);
                break;
            case "OBD":
                position.set("obd", values[index++]);
                break;
            case "TAG":
                position.set("tagData", values[index++]);
                break;
            case "IWD":
                while (index < values.length) {
                    int sensorIndex = Integer.parseInt(values[index++]);
                    int dataType = Integer.parseInt(values[index++]);
                    if (dataType == 0) {
                        position.set(Position.KEY_DRIVER_UNIQUE_ID, values[index++]);
                    } else if (dataType == 1) {
                        index += 1; // temperature sensor serial number
                        position.set(Position.PREFIX_TEMP + sensorIndex, Double.parseDouble(values[index++]));
                    }
                }
                break;
            default:
                break;
        }
    }

    private Position decodePosition(DeviceSession deviceSession, String sentence) throws ParseException {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        int index = 0;
        String[] fragments = sentence.split(",");

        if (fragments[index].matches("[0-9]{12}")) {
            position.setTime(new SimpleDateFormat("HHmmssddMMyy").parse(fragments[index++]));
        } else {
            getLastLocation(position, null);
            position.set(Position.KEY_RESULT, fragments[index++]);
        }

        for (; index < fragments.length; index += 1) {
            if (!fragments[index].isEmpty()) {
                if (fragments[index].matches("\\p{XDigit}+")) {
                    position.set(Position.KEY_EVENT, Integer.parseInt(fragments[index], 16));
                } else {
                    decodeFragment(position, fragments[index]);
                }
            }
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage("1234", remoteAddress));
        }

        String sentence = (String) msg;
        Pattern pattern = PATTERN;
        if (sentence.startsWith("*GS02")) {
            pattern = PATTERN_OLD;
        }

        Parser parser = new Parser(pattern, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        if (pattern == PATTERN_OLD) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

            position.setValid(parser.next().equals("A"));
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
            position.setCourse(parser.nextDouble(0));

            position.set(Position.KEY_HDOP, parser.next());

            dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
            position.setTime(dateBuilder.getDate());

            return position;

        } else {

            List<Position> positions = new LinkedList<>();
            for (String item : parser.next().split("\\$")) {
                positions.add(decodePosition(deviceSession, item));
            }
            return positions;

        }
    }

}
