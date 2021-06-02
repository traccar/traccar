/*
 * Copyright 2013 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class Tlt2hProtocolDecoder extends BaseProtocolDecoder {

    public Tlt2hProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_HEADER = new PatternBuilder()
            .number("#(d+)")                     // imei
            .expression("#[^#]*")                // user
            .number("#d*")                       // password
            .groupBegin()
            .number("#([01])")                   // door
            .number("#(d+)")                     // fuel voltage
            .number("#(d+)")                     // power
            .number("#(d+)")                     // battery
            .number("#(d+)")                     // temperature
            .groupEnd("?")
            .expression("#([^#]+)")              // status
            .number("#d+")                       // number of records
            .compile();

    private static final Pattern PATTERN_POSITION = new PatternBuilder()
            .text("#")
            .number("(?:(dd)|x*)")               // cell or voltage
            .text("$GPRMC,")
            .number("(dd)(dd)(dd).d+,")          // time (hhmmss.sss)
            .expression("([AVL]),")              // validity
            .number("(d+)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d+)(dd.d+),")              // longitude
            .number("([EW]),")
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .any()
            .compile();

    private static final Pattern PATTERN_WIFI = new PatternBuilder()
            .text("#")
            .number("(?:(dd)|x+)")               // cell or voltage
            .text("$WIFI,")
            .number("(dd)(dd)(dd).d+,")          // time (hhmmss.sss)
            .expression("[AVL],")                // validity
            .expression("(.*)")                  // access points
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .text("*")
            .number("xx")                        // checksum
            .compile();

    private void decodeStatus(Position position, String status) {
        switch (status) {
            case "AUTOSTART":
            case "AUTO":
                position.set(Position.KEY_IGNITION, true);
                break;
            case "AUTOSTOP":
            case "AUTOLOW":
                position.set(Position.KEY_IGNITION, false);
                break;
            case "TOWED":
                position.set(Position.KEY_ALARM, Position.ALARM_TOW);
                break;
            case "SOS":
                position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                break;
            case "DEF":
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
                break;
            case "BLP":
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                break;
            case "CLP":
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_POWER);
                break;
            case "OS":
                position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE_EXIT);
                break;
            case "RS":
                position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE_ENTER);
                break;
            case "OVERSPEED":
                position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
                break;
            default:
                break;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        sentence = sentence.trim();

        String header = sentence.substring(0, sentence.indexOf('\r'));
        Parser parser = new Parser(PATTERN_HEADER, header);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Boolean door = null;
        Double adc = null;
        Double power = null;
        Double battery = null;
        Double temperature = null;
        if (parser.hasNext(5)) {
            door = parser.nextInt() == 1;
            adc = parser.nextInt() * 0.1;
            power = parser.nextInt() * 0.1;
            battery = parser.nextInt() * 0.1;
            temperature = parser.nextInt() * 0.1;
        }

        String status = parser.next();

        String[] messages = sentence.substring(sentence.indexOf('\n') + 1).split("\r\n");
        List<Position> positions = new LinkedList<>();

        for (String message : messages) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            if (message.contains("$GPRMC")) {

                parser = new Parser(PATTERN_POSITION, message);
                if (parser.matches()) {

                    if (parser.hasNext()) {
                        position.set(Position.KEY_BATTERY, parser.nextInt() * 0.1);
                    }

                    DateBuilder dateBuilder = new DateBuilder()
                            .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

                    position.setValid(parser.next().equals("A"));
                    position.setLatitude(parser.nextCoordinate());
                    position.setLongitude(parser.nextCoordinate());
                    position.setSpeed(parser.nextDouble(0));
                    position.setCourse(parser.nextDouble(0));

                    dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
                    position.setTime(dateBuilder.getDate());

                } else {
                    continue;
                }

            } else if (message.contains("$WIFI")) {

                parser = new Parser(PATTERN_WIFI, message);
                if (parser.matches()) {

                    position.set(Position.KEY_BATTERY, parser.nextInt() * 0.1);

                    DateBuilder dateBuilder = new DateBuilder()
                            .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

                    String[] values = parser.next().split(",");
                    Network network = new Network();
                    for (int i = 0; i < values.length / 2; i++) {
                        String mac = values[i * 2 + 1].replaceAll("(..)", "$1:").substring(0, 17);
                        network.addWifiAccessPoint(WifiAccessPoint.from(mac, Integer.parseInt(values[i * 2])));
                    }
                    position.setNetwork(network);

                    dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());

                    getLastLocation(position, dateBuilder.getDate());
                }

            } else {

                getLastLocation(position, null);

            }

            position.set(Position.KEY_DOOR, door);
            position.set(Position.PREFIX_ADC + 1, adc);
            position.set(Position.KEY_POWER, power);
            position.set(Position.KEY_BATTERY, battery);
            position.set(Position.PREFIX_TEMP + 1, temperature);
            decodeStatus(position, status);

            positions.add(position);
        }

        return positions.isEmpty() ? null : positions;
    }

}
