/*
 * Copyright 2013 - 2024 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WialonProtocolDecoder extends BaseProtocolDecoder {

    public WialonProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_ANY = new PatternBuilder()
            .number("d.d;").optional()
            .expression("([^#]+)?")              // imei
            .text("#")                           // start byte
            .expression("([^#]+)")               // type
            .text("#")                           // separator
            .expression("(.*)")                  // message
            .compile();

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(?:NA|(dd)(dd)(dd));")      // date (ddmmyy)
            .number("(?:NA|(dd)(dd)(dd));")      // time (hhmmss)
            .number("(?:NA|(d+)(dd.d+));")       // latitude
            .expression("(?:NA|([NS]));")
            .number("(?:NA|(d+)(dd.d+));")       // longitude
            .expression("(?:NA|([EW]));")
            .number("(?:NA|(d+.?d*))?;")         // speed
            .number("(?:NA|(d+.?d*))?;")         // course
            .number("(?:NA|(-?d+.?d*));")        // altitude
            .number("(?:NA|(d+))")               // satellites
            .groupBegin().text(";")
            .number("(?:NA|(d+.?d*));")          // hdop
            .number("(?:NA|(d+));")              // inputs
            .number("(?:NA|(d+));")              // outputs
            .expression("(?:NA|([^;]*));")       // adc
            .expression("(?:NA|([^;]*));")       // ibutton
            .expression("(?:NA|([^;]*))")        // params
            .groupEnd("?")
            .any()
            .compile();

    private void sendResponse(Channel channel, SocketAddress remoteAddress, String type, Integer number) {
        if (channel != null) {
            StringBuilder response = new StringBuilder("#A");
            response.append(type);
            response.append("#");
            if (number != null) {
                response.append(number);
            }
            response.append("\r\n");
            channel.writeAndFlush(new NetworkMessage(response.toString(), remoteAddress));
        }
    }

    private Position decodePosition(Channel channel, SocketAddress remoteAddress, String id, String substring) {

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        Parser parser = new Parser(PATTERN, substring);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (parser.hasNext(6)) {
            position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        } else {
            position.setTime(new Date());
        }

        if (parser.hasNextAny(9)) {
            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
            position.setCourse(parser.nextDouble(0));
            position.setAltitude(parser.nextDouble(0));
        } else {
            getLastLocation(position, position.getDeviceTime());
        }

        if (parser.hasNext()) {
            int satellites = parser.nextInt(0);
            position.setValid(satellites >= 3);
            position.set(Position.KEY_SATELLITES, satellites);
        }

        position.set(Position.KEY_HDOP, parser.nextDouble());
        position.set(Position.KEY_INPUT, parser.next());
        position.set(Position.KEY_OUTPUT, parser.next());

        if (parser.hasNext()) {
            String[] values = parser.next().split(",");
            for (int i = 0; i < values.length; i++) {
                position.set(Position.PREFIX_ADC + (i + 1), values[i]);
            }
        }

        position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());

        if (parser.hasNext()) {
            String[] values = parser.next().split(",");
            for (String param : values) {
                Matcher paramParser = Pattern.compile("(.*):[1-3]:(.*)").matcher(param);
                if (paramParser.matches()) {
                    String key = paramParser.group(1).toLowerCase();
                    String value = paramParser.group(2);
                    try {
                        if (key.equals("accuracy")) {
                            position.setAccuracy(Double.parseDouble(value));
                        } else {
                            position.set(key, Double.parseDouble(value));
                        }
                    } catch (NumberFormatException e) {
                        if (value.equalsIgnoreCase("true")) {
                            position.set(key, true);
                        } else if (value.equalsIgnoreCase("false")) {
                            position.set(key, false);
                        } else {
                            position.set(key, value);
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

        Parser parser = new Parser(PATTERN_ANY, sentence);
        if (!parser.matches()) {
            return null;
        }

        String id = parser.next();
        String type = parser.next();
        String data = parser.next();

        DeviceSession deviceSession;
        Position position;

        switch (type) {

            case "L":
                String[] values = data.split(";");
                String imei = values[0].indexOf('.') >= 0 ? values[1] : values[0];
                deviceSession = getDeviceSession(channel, remoteAddress, imei);
                if (deviceSession != null) {
                    sendResponse(channel, remoteAddress, type, 1);
                }
                break;

            case "P":
                sendResponse(channel, remoteAddress, type, null); // heartbeat
                break;

            case "D":
            case "SD":
                position = decodePosition(channel, remoteAddress, id, data);
                if (position != null) {
                    sendResponse(channel, remoteAddress, "D", 1);
                    return position;
                }
                break;

            case "B":
                String[] messages = data.split("\\|");
                List<Position> positions = new LinkedList<>();

                for (String message : messages) {
                    position = decodePosition(channel, remoteAddress, id, message);
                    if (position != null) {
                        position.set(Position.KEY_ARCHIVE, true);
                        positions.add(position);
                    }
                }

                sendResponse(channel, remoteAddress, type, messages.length);
                if (!positions.isEmpty()) {
                    return positions;
                }
                break;

            case "M":
                deviceSession = getDeviceSession(channel, remoteAddress, id);
                if (deviceSession != null) {
                    position = new Position(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());
                    getLastLocation(position, new Date());
                    position.setValid(false);
                    position.set(Position.KEY_RESULT, data);
                    sendResponse(channel, remoteAddress, type, 1);
                    return position;
                }
                break;

            default:
                break;

        }

        return null;
    }

}
