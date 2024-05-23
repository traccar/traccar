/*
 * Copyright 2018 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class ItsProtocolDecoder extends BaseProtocolDecoder {

    // * CUSTOM CODE START * //
    private static final Logger LOGGER = LoggerFactory.getLogger(ItsProtocolDecoder.class);
    // * CUSTOM CODE END * //

    public ItsProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .expression("[^$]*")
            .text("$")
            .expression(",?[^,]+,")              // event / Header
            .groupBegin()
            .expression("[^,]*,")                // vendor / iTriangle
            .expression("[^,]+,")                // firmware version - "1_37T02B0164MAIS_6" OR "BHARAT101C_0267"
            .expression("(..),")                 // status / Packet Type
            .number("(d+),").optional()          // event / Message ID
            .expression("([LH]),")               // history / Packet Status
            .or()
            .expression("([^,]+),")              // type
            .groupEnd()
            .number("(d{15}),")                  // imei / IMEI (Device Unique ID)
            .groupBegin()
            .expression("([^,]{2}),")            // status
            .or()
            .expression("[^,]*,")                // vehicle registration / Vehicle Reg. No
            .number("([01]),").optional()        // valid / GPS Fix
            .groupEnd()
            .number("(dd),?(dd),?(d{2,4}),?")    // date (ddmmyyyy) / Date
            .number("(dd),?(dd),?(dd),")         // time (hhmmss) / Time
            .expression("([01AV]),").optional()  // valid
            .number("(d+.d+),([NS]),")           // latitude / Latitude + Latitude Dir
            .number("(d+.d+),([EW]),")           // longitude / Longitude + Longitude Dir
            .groupBegin()
            .number("(d+.?d*),")                 // speed / Speed
            .number("(d+.?d*),")                 // course / Heading
            .number("(d+),")                     // satellites / No of Satellites
            .groupBegin()
            .number("(d+.?d*),")                 // altitude / Altitude
            .number("(d+.?d*),")                 // pdop / PDOP
            .number("(d+.?d*),")                 // hdop / HDOP
            .expression("([^,]+)?,")             // operator / Network Operator Name
            .number("([01]),")                   // ignition / Ignition Status
            .number("([01]),")                   // charging / Main Power Status
            .number("(d+.?d*),")                 // power / Main Input Voltage
            .number("(d+.?d*),")                 // battery / Internal Battery Voltage
            .number("([01]),")                   // emergency / Emergency Status
            .expression("[COYN]?,")              // tamper / Tamper Alert (Optional)
            .expression("(.*),")                 // cells / GSM Signal Strength
            // Ignoring 31 to 46
            .number("([012]{4}),")               // inputs / Digital Input Status
            .number("([01]{2}),")                // outputs / Digital Output Status
            .groupBegin()
            .number("d+,")                       // index
            .number("(d+.?d*),")                 // odometer
            .number("(d+.?d*),")                 // adc1
            .number("(-?d+.?d*),")               // acceleration x
            .number("(-?d+.?d*),")               // acceleration y
            .number("(-?d+.?d*),")               // acceleration z
            .number("(-?d+),")                   // tilt y
            .number("(-?d+),")                   // tilt x
            .or()
            .number("d+,")                       // index
            .number("(d+.?d*),")                 // odometer
            .number("x+,")                       // checksum
            .or()
            .number("d+,")                       // index / Frame Number
            .number("(d+.?d*),")                 // adc1 / Analog Input 1
            .number("(d+.?d*),")                 // adc2 / Analog Input 2
            // * CUSTOM CODE START * //
            .number("(d+.?d*),")                 // - / Delta Distance (ADDED ON TOP OF TRACCAR'S DEFAULT ITS DECODER)
            // * CUSTOM CODE END * //
            .or()
            .number("(d+.d+),")                  // adc1
            .number("(d+),")                     // odometer
            .number("(d{6}),")                   // index
            .expression("([^,]+),")              // response format
            .expression("([^,]+),")              // response
            .groupEnd("?")
            .groupEnd("?")
            .or()
            .number("(-?d+.d+),")                // altitude
            .number("(d+.d+),")                  // speed
            .groupEnd()
            .any()
            .compile();

    private String decodeAlarm(String status) {
        switch (status) {
            case "WD":
            case "EA":
                return Position.ALARM_SOS;
            case "BL":
                return Position.ALARM_LOW_BATTERY;
            case "HB":
                return Position.ALARM_BRAKING;
            case "HA":
                return Position.ALARM_ACCELERATION;
            case "RT":
                return Position.ALARM_CORNERING;
            case "OS":
                return Position.ALARM_OVERSPEED;
            case "TA":
                return Position.ALARM_TAMPERING;
            case "BD":
                return Position.ALARM_POWER_CUT;
            case "BR":
                return Position.ALARM_POWER_RESTORED;
            default:
                return null;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (channel != null) {
            if (sentence.startsWith("$,01,")) {
                channel.writeAndFlush(new NetworkMessage("$,1,*", remoteAddress));
            } else if (sentence.startsWith("$,LGN,")) {
                DateFormat dateFormat = new SimpleDateFormat("ddMMyyyyHHmmss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String time = dateFormat.format(new Date());
                channel.writeAndFlush(new NetworkMessage("$LGN" + time + "*", remoteAddress));
            } else if (sentence.startsWith("$,HBT,")) {
                channel.writeAndFlush(new NetworkMessage("$HBT*", remoteAddress));
            }
        }

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        // * CUSTOM CODE START * //

        // String status = parser.next();
        // Integer event = parser.nextInt();
        // boolean history = "H".equals(parser.next());
        // String type = parser.next();

        String vendor = parser.next(); // vendor / iTriangle
        String firmwareVersion = parser.next(); // firmware version - "1_37T02B0164MAIS_6" OR "BHARAT101C_0267"

        String status = parser.next(); // status / Packet Type

        Integer event = parser.nextInt(); // event / Message ID
        boolean history = "H".equals(parser.next()); // history / Packet Status
        String type = parser.next(); // type

        var deviceUniqueId = parser.next(); // imei / IMEI (Device Unique ID)

        if (vendor != null && vendor.contains("iTriangle")
                && firmwareVersion != null && !firmwareVersion.startsWith("BHARAT101")) {
            // Ignoring messages not coming from firmware version "BHARAT101..."
            // As they are not coming from primary server configuration on the device.

            LOGGER.warn("Ignoring message from iTriangle device with firmware version: " + firmwareVersion
                    + ". Device Unique ID: " + deviceUniqueId);

            return null;
        }
        
        // DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, deviceUniqueId);
       
        // * CUSTOM CODE END * //

        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (type != null && type.equals("EMR")) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        }

        if (event != null) {
            position.set(Position.KEY_EVENT, event);
        }
        if (history) {
            position.set(Position.KEY_ARCHIVE, true);
        }

        if (parser.hasNext()) {
            status = parser.next(); // status
        }
        if (status != null) {
            if (status.equals("IN")) {
                position.set(Position.KEY_IGNITION, true);
            } else if (status.equals("IF")) {
                position.set(Position.KEY_IGNITION, false);
            } else {
                position.set(Position.KEY_ALARM, decodeAlarm(status));
            }
        }

        if (parser.hasNext()) {
            position.setValid(parser.nextInt() == 1); // valid / GPS Fix
        }
        position.setTime(parser.nextDateTime(
                Parser.DateTimeFormat.DMY_HMS, getTimeZone(deviceSession.getDeviceId()).getID())); // date (ddmmyyyy) / Date + time (hhmmss) / Time
        if (parser.hasNext()) {
            position.setValid(parser.next().matches("[1A]")); // valid
        }
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM)); // latitude / Latitude + Latitude Dir
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM)); // longitude / Longitude + Longitude Dir

        if (parser.hasNext(3)) {
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble())); // speed / Speed
            position.setCourse(parser.nextDouble()); // course / Heading
            position.set(Position.KEY_SATELLITES, parser.nextInt()); // satellites / No of Satellites
        }

        if (parser.hasNext(8)) {
            position.setAltitude(parser.nextDouble()); // altitude / Altitude
            position.set(Position.KEY_PDOP, parser.nextDouble()); // pdop / PDOP
            position.set(Position.KEY_HDOP, parser.nextDouble()); // hdop / HDOP
            position.set(Position.KEY_OPERATOR, parser.next()); // operator / Network Operator Name
            position.set(Position.KEY_IGNITION, parser.nextInt() > 0); // ignition / Ignition Status
            position.set(Position.KEY_CHARGE, parser.nextInt() > 0); // charging / Main Power Status
            position.set(Position.KEY_POWER, parser.nextDouble()); // power / Main Input Voltage
            position.set(Position.KEY_BATTERY, parser.nextDouble()); // battery / Internal Battery Voltage

            position.set("emergency", parser.nextInt() > 0); // emergency / Emergency Status

            String cellsString = parser.next(); // cells / GSM Signal Strength
            if (!cellsString.contains("x")) {
                String[] cells = cellsString.split(",");
                int mcc = Integer.parseInt(cells[1]);
                int mnc = Integer.parseInt(cells[2]);
                int lac = Integer.parseInt(cells[3], 16);
                int cid = Integer.parseInt(cells[4], 16);
                Network network = new Network(CellTower.from(mcc, mnc, lac, cid, Integer.parseInt(cells[0])));
                if (cells.length > 5 && !cells[5].startsWith("(")) {
                    for (int i = 0; i < 4; i++) {
                        lac = Integer.parseInt(cells[5 + 3 * i + 1], 16);
                        cid = Integer.parseInt(cells[5 + 3 * i + 2], 16);
                        if (lac > 0 && cid > 0) {
                            network.addCellTower(CellTower.from(mcc, mnc, lac, cid));
                        }
                    }
                }
                position.setNetwork(network);
            }

            String input = parser.next(); // inputs / Digital Input Status
            if (input.charAt(input.length() - 1) == '2') {
                input = input.substring(0, input.length() - 1) + '0';
            }
            position.set(Position.KEY_INPUT, Integer.parseInt(input, 2));
            position.set(Position.KEY_OUTPUT, parser.nextBinInt()); // outputs / Digital Output Status
        }

        if (parser.hasNext(7)) {
            position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);
            position.set(Position.PREFIX_ADC + 1, parser.nextDouble());
            position.set(Position.KEY_G_SENSOR,
                    "[" + parser.nextDouble() + "," + parser.nextDouble() + "," + parser.nextDouble() + "]");
            position.set("tiltY", parser.nextInt());
            position.set("tiltX", parser.nextInt());
        }

        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);
        }

        // if (parser.hasNext(2)) {
        //     position.set(Position.PREFIX_ADC + 1, parser.nextDouble());
        //     position.set(Position.PREFIX_ADC + 2, parser.nextDouble());
        // }

        // * CUSTOM CODE START * //
        if (parser.hasNext(3)) {
            position.set(Position.PREFIX_ADC + 1, parser.nextDouble()); // adc1 / Analog Input 1
            position.set(Position.PREFIX_ADC + 2, parser.nextDouble()); // adc2 / Analog Input 2
            var distValue = parser.nextDouble();
            position.set(Position.KEY_DISTANCE, distValue); // - / Delta Distance (ADDED ON TOP OF TRACCAR'S DEFAULT ITS DECODER)
            position.setDistance(distValue);
        }
        // * CUSTOM CODE END * //

        if (parser.hasNext(5)) {
            position.set(Position.PREFIX_ADC + 1, parser.nextDouble());
            position.set(Position.KEY_ODOMETER, parser.nextInt());
            position.set(Position.KEY_INDEX, parser.nextInt());
            position.set("responseFormat", parser.next());
            position.set("response", parser.next());
        }

        if (parser.hasNext(2)) {
            position.setAltitude(parser.nextDouble());
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        }

        return position;
    }

}
