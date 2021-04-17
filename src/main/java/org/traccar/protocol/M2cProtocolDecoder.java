/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.handler.StandardLoggingHandler;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class M2cProtocolDecoder extends BaseProtocolDecoder {

    public M2cProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    // old ASCII v5 version
    private static final Pattern PATTERN = new PatternBuilder()
            .text("M2C,")
            .expression("[^,]+,")                // model
            .expression("[^,]+,")                // firmware
            .number("d+,")                       // protocol
            .number("(d+),")                     // imei
            .number("(d+),")                     // index
            .expression("([LH]),")               // archive
            .number("d+,")                       // priority
            .number("(d+),")                     // event
            .number("(dd)(dd)(dd),")             // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+),")                // longitude
            .number("(-?d+),")                   // altitude
            .number("(d+),")                     // course
            .number("(d+.d+),")                  // speed
            .number("(d+),")                     // satellites
            .number("(d+),")                     // odometer
            .number("(d+),")                     // input
            .number("(d+),")                     // output
            .number("(d+),")                     // power
            .number("(d+),")                     // battery
            .number("(d+),")                     // adc 1
            .number("(d+),")                     // adc 2
            .number("(d+.?d*),")                 // temperature
            .expression("[^,]+,")                  // rfid
            .expression("[^,]+,")                  // barcode
            .expression("[^,]+,")                  // camera pic
            .expression("[^,]+,")                  // mcc
            .expression("[^,]+,")                  // mnc
            .expression("[^,]+,")                  // lac
            .expression("[^,]+,")                  // cell id
            .expression("[^,]+,")                  // signal strength
//            .expression("[^,]+,")                  // reg status -- one extra param in doc, we dont know which
            .expression("([^,]+)")                  // rfid
            .any()
            .compile();

    // 2020a version
    private static final Pattern PATTERN2020A = new PatternBuilder()
            .expression("[^,]+,")               // model or header
            .expression("[^,]+,")                // model
            .expression("([^,]+),")                // firmware
            .expression("([^,]+),")                // packet type
            .number("(d+),")                     // Alert Id
            .expression("([LH]),")               // Packet status / archive
            .number("(d+),")                     // imei
            .expression("([^,]+),")                // m2m sim iccid number
            .number("([01]),")                   // GPS Fix
            .number("(dd)(dd)(dddd),")             // date (ddmmyy) in UTC
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(-?d+.d+),")                // latitude
            .expression("([NS]),")               // direction
            .number("(-?d+.d+),")                // longitude
            .expression("([EW]),")               // direction
            .number("(d+.d+),")                  // speed
            .number("(d+.d+),")                  // heading
            .number("(d+),")                     // satellites
            .number("(-?d+.d+),")                // altitude
            .number("(d+.?d+?),")                  // PDOP
            .number("(d+.?d+?),")                // HDOP
            .expression("([^,]+)?,")             // operator
            .number("([01]),")                   // Ignition
            .number("([01]),")                   // Power Status
            .number("(d+.d+),")                  // input voltage
            .number("(d+.d+),")                  // battery voltage
            .number("([01]),")                   // Emergency status
            .expression("([CO]),")               // Tamper switch
            .number("d+,")                       // gsm signal strength
            .expression("[^,]+,")                // MCC
            .expression("[^,]+,")                // MNC
            .expression("[^,]+,")                // LAC
            .expression("[^,]+,")                // Cell ID
            .expression("[^,]+,")                // NMR1 LAC
            .expression("[^,]+,")                // NMR1 Cell id
            .expression("[^,]+,")                       // nmr1 signal strength
            .expression("[^,]+,")                // NMR2 LAC
            .expression("[^,]+,")                // NMR2 Cell id
            .expression("[^,]+,")                       // nmr2 signal strength
            .expression("[^,]+,")                // NMR3 LAC
            .expression("[^,]+,")                // NMR3 Cell id
            .expression("[^,]+,")                       // nmr3 signal strength
            .expression("[^,]+,")                // NMR4 LAC
            .expression("[^,]+,")                // NMR4 Cell id
            .expression("[^,]+,")                       // nmr4 signal strength
            .number("(d+),")                    // Digital input
            .number("(d+),")                   // Digital Output
            .number("(d+),")                   // Frame number
            .any()
            .compile();


    // 2025a version
    private static final Pattern PATTERN2025A = new PatternBuilder()
            .expression("[^,]+,")               // model or header
            .expression("[^,]+,")                // model
            .expression("([^,]+),")                // firmware
            .expression("([^,]+),")                // packet type
            .number("(d+),")                     // Alert Id
            .expression("([LH]),")               // Packet status / archive
            .number("(d+),")                     // imei
            .expression("([^,]+),")                // m2m sim iccid number
            .expression("([CO]),")               // Tamper switch
            .number("([01]),")                   // GPS Fix
            .number("(dd)(dd)(dddd),")             // date (ddmmyy) in UTC
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(-?d+.d+),")                // latitude
            .expression("([NS]),")               // direction
            .number("(-?d+.d+),")                // longitude
            .expression("([EW]),")               // direction
            .number("(d+.d+),")                  // speed
            .number("(d+.d+),")                  // heading
            .number("(d+),")                     // satellites
            .number("(-?d+.d+),")                // altitude
            .expression("([^,]+)?,")             // operator
            .number("(d+),")                       // gsm signal strength
            .number("([01]),")                   // Ignition
            .number("([01]),")                    // AC/panic Digital input
            .number("([01]),")                   // Digital Output
            .number("([01]),")                   // Power Status
            .number("(d+.d+),")                  // input voltage
            .number("(d+.d+),")                  // battery voltage
            .number("(d+.d+),")                  // analog
            .expression("([^,]+),")                  // temperature
            .any()
            .compile();

    private String decodeAlarm(int value) {
        switch (value) {
            case 3:
                return Position.ALARM_POWER_CUT;
            case 4:
                return Position.ALARM_LOW_BATTERY;
            case 5:
                return Position.ALARM_GENERAL;
            case 6:
            case 7:
            case 23:
                return Position.ALARM_POWER_ON;
            case 8:
                return Position.ALARM_POWER_OFF;
            case 9:
            case 16:
                return Position.ALARM_TAMPERING;
            case 10:
                return Position.ALARM_SOS;
            case 13:
                return Position.ALARM_ACCELERATION;
            case 14:
                return Position.ALARM_BRAKING;
            case 15:
                return Position.ALARM_CORNERING;
            case 17:
                return Position.ALARM_OVERSPEED;
            default:
                return null;
        }
    }

    private Position decodePosition(Channel channel, SocketAddress remoteAddress, String line) {

        Parser parser = new Parser(PATTERN, line);
        if (!parser.matches()) {
            String prefix = "[0,1";

            channel.writeAndFlush(new NetworkMessage(
                    prefix + "," + "0*276" + "]", channel.remoteAddress())); // failure to send negative ack
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            String prefix = "[0,1";

            channel.writeAndFlush(new NetworkMessage(
                    prefix + "," + "0*276" + "]", channel.remoteAddress()));
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_INDEX, parser.nextInt());

        if (parser.next().equals("H")) {
            position.set(Position.KEY_ARCHIVE, true);
        }

        position.set(Position.KEY_EVENT, parser.nextInt());

        position.setValid(true);
        position.setTime(parser.nextDateTime());
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setAltitude(parser.nextInt());
        position.setCourse(parser.nextInt());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_ODOMETER, parser.nextLong());
        position.set(Position.KEY_INPUT, parser.nextInt());
        position.set(Position.KEY_OUTPUT, parser.nextInt());
        position.set(Position.KEY_POWER, parser.nextInt() * 0.001);
        position.set(Position.KEY_BATTERY, parser.nextInt() * 0.001);
        position.set(Position.PREFIX_ADC + 1, parser.nextInt());
        position.set(Position.PREFIX_ADC + 2, parser.nextInt());
        position.set(Position.PREFIX_TEMP + 1, parser.nextDouble());
        position.set(Position.KEY_ORIGINAL, line);

        String prefix = "[0,1";
        channel.writeAndFlush(new NetworkMessage(
                prefix + "," + "0*275" + "]", channel.remoteAddress()));

        return position;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardLoggingHandler.class);

    private Position decodePosition2020a(Channel channel, SocketAddress remoteAddress, String line) {
        Position position = new Position(getProtocolName());
        try {
            Parser parser = new Parser(PATTERN2020A, line);

            if (!parser.matches()) {
                return null;
            }

            position.set(Position.KEY_VERSION_FW, parser.next());
            position.set(Position.KEY_EVENT, parser.next());

            Integer alertId = parser.nextInt();

            position.set(Position.KEY_ALARM, decodeAlarm(alertId));
            position.set(Position.KEY_INDEX, alertId);


            if (parser.next().equals("H")) {
                position.set(Position.KEY_ARCHIVE, true);
            }

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());

            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());
            position.set(Position.KEY_VIN, parser.next());
            position.setValid(parser.nextInt() == 1);
            position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
            position.setCourse(parser.nextDouble());
            position.set(Position.KEY_SATELLITES, parser.nextInt());
            position.setAltitude(parser.nextDouble());
            position.set(Position.KEY_PDOP, parser.nextDouble());
            position.set(Position.KEY_HDOP, parser.nextDouble());
            position.set(Position.KEY_OPERATOR, parser.next());
            position.set(Position.KEY_IGNITION, parser.nextInt() == 1);
            position.set(Position.KEY_CHARGE, parser.next().equals("1")); // main power status
            position.set(Position.KEY_INPUT, parser.nextDouble());
            position.set(Position.KEY_BATTERY, parser.nextDouble());
            if (parser.nextInt() == 1)
                position.set(Position.KEY_ALARM, Position.ALARM_SOS);
            if (parser.next().equals("O"))
                position.set(Position.KEY_ALARM, Position.ALARM_TAMPERING);

            CellTower cellTower = new CellTower();
            cellTower.setSignalStrength(parser.nextInt());

            position.setNetwork(new Network(cellTower));
            position.set(Position.KEY_ORIGINAL, line);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            throw e;
        }

        return position;
    }

    private Position decodePosition2025a(Channel channel, SocketAddress remoteAddress, String line) {
        try {
            Parser parser = new Parser(PATTERN2025A, line);

            if (!parser.matches()) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.set(Position.KEY_VERSION_FW, parser.next());
            position.set(Position.KEY_EVENT, parser.next());

            Integer alertId = parser.nextInt();

            position.set(Position.KEY_ALARM, decodeAlarm(alertId));
            position.set(Position.KEY_INDEX, alertId);


            if (parser.next().equals("H")) {
                position.set(Position.KEY_ARCHIVE, true);
            }

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());

            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());
            position.set(Position.KEY_VIN, parser.next());
            if (parser.next().equals("O"))
                position.set(Position.KEY_ALARM, Position.ALARM_TAMPERING);
            position.setValid(parser.nextInt() == 1);
            position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
            position.setCourse(parser.nextDouble());
            position.set(Position.KEY_SATELLITES, parser.nextInt());
            position.setAltitude(parser.nextDouble());
            position.set(Position.KEY_OPERATOR, parser.next());

            CellTower cellTower = new CellTower();
            cellTower.setSignalStrength(parser.nextInt());

            position.setNetwork(new Network(cellTower));
            position.set(Position.KEY_IGNITION, parser.nextInt() == 1);
            if (parser.nextInt() == 1)
                position.set(Position.KEY_ALARM, Position.ALARM_SOS);
            position.set("immobilizer", parser.nextInt() == 1);
            position.set(Position.KEY_CHARGE, parser.next().equals("1")); // main power status
            position.set(Position.KEY_INPUT, parser.nextDouble());
            position.set(Position.KEY_BATTERY, parser.nextDouble());
            position.set(Position.PREFIX_ADC + 1, parser.next());

            String temp = parser.next();

            if (!temp.toLowerCase().contains("x")) {
                position.set(Position.KEY_DEVICE_TEMP, Double.parseDouble(temp));
            }
            position.set(Position.KEY_ORIGINAL, line);

            return position;
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            throw e;
        }

    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        sentence = sentence.substring(1); // remove start symbol
        sentence = sentence.replace("#", "").replace("]", "");

        List<Position> positions = new LinkedList<>();

        for (String line : sentence.split("\r\n")) {
            if (!line.isEmpty()) {
                line = line.replace("xx", "00").replace("XX", "00");
                line = line.replace(".x", ".0").replace(".X", ".0");

                Position position;
                String lower = line.toLowerCase();
                if (lower.contains(",2020a,") || lower.contains("030,") || lower.contains(",2025,")
                        || lower.contains(",at369,") || lower.contains(",adti,")) {
                    position = decodePosition2020a(channel, remoteAddress, line);
                } else if (lower.contains(",2025a,")) { // check if latest 2020a model protocol
                    position = decodePosition2025a(channel, remoteAddress, line);
                } else {
                    position = decodePosition(channel, remoteAddress, line);
                }

                if (position != null) {
                    positions.add(position);
                }
            }
        }

        return positions;
    }

}