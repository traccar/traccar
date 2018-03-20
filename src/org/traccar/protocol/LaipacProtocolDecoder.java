/*
 * Copyright 2013 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.DeviceSession;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class LaipacProtocolDecoder extends BaseProtocolDecoder {

    public LaipacProtocolDecoder(LaipacProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$AVRMC,")
            .expression("([^,]+),")              // identifier
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([AVRPavrp]),")         // validity
            .number("(dd)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(ddd)(dd.d+),")             // longitude
            .number("([EW]),")
            .number("(d+.d+),")                  // speed
            .number("(d+.d+),")                  // course
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .expression("([abZXTSMHFE86430]),")  // event code
            .number("(d+)").expression("(\\.?)").number("(d*),")    // battery voltage
            .number("(d+),")                     // current mileage
            .number("(d),")                      // GPS on/off (1 = on, 0 = off)
            .number("(d+),")                     // Analog port 1
            .number("(d+)")                      // Analog port 2
            .expression(",([0-9a-fA-F]{4})")     // Cell 1 - Cell Net Code
            .expression("([0-9a-fA-F]{4}),")     // Cell 1 - Cell ID Code
            .number("(d{3})")                    // Cell 2 - Country Code
            .number("(d{3})")                    // Cell 2 - Operator Code
            .optional(4)
            .text("*")
            .number("(xx)")                      // checksum
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("$ECHK") && channel != null) {
            channel.write(sentence + "\r\n"); // heartbeat
            return null;
        }

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        String status = parser.next();
        position.setValid(status.toUpperCase().equals("A"));
        position.set(Position.KEY_STATUS, status);

        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        String eventCode = parser.next();
        String decodedAlarm = decodeAlarm(eventCode);
        if (decodedAlarm != null) {
            position.set(Position.KEY_ALARM, decodeAlarm(eventCode));
        }
        position.set(Position.KEY_EVENT, eventCode);

        double batteryVoltage = parser.nextDouble();
        if (parser.next().isEmpty()) {
            parser.next();
            batteryVoltage *= 0.001;
        } else {
            batteryVoltage += parser.nextDouble() * 0.001;
        }
        position.set(Position.KEY_BATTERY, batteryVoltage);

        position.set(Position.KEY_TOTAL_DISTANCE, parser.nextDouble());
        position.set(Position.KEY_GPS, parser.nextInt());
        position.set(Position.PREFIX_ADC + 1, parser.nextDouble() * 0.001);
        position.set(Position.PREFIX_ADC + 2, parser.nextDouble() * 0.001);

        String cellNetCodeString = parser.next();
        String cellIdString = parser.next();
        String countryCodeString = parser.next();
        String operatorCodeString = parser.next();
        if (cellNetCodeString != null && cellIdString != null && countryCodeString != null && operatorCodeString != null) {
            int cellNetCode = Integer.parseInt(cellNetCodeString, 16);
            Long cellId = Long.parseLong(cellIdString, 16);
            int countryCode = Integer.parseInt(countryCodeString);
            int operatorCode = Integer.parseInt(operatorCodeString);
            position.setNetwork(new Network(CellTower.from(countryCode, operatorCode, cellNetCode, cellId)));
        }

        String checksum = parser.next();
        String result = sentence.replaceAll("^\\$(.*)\\*[0-9a-fA-F]{2}$", "$1");
        if (checksum == null || Integer.parseInt(checksum, 16) != Checksum.xor(result)) {
            return null;
        }

        if (channel != null) {
            if (eventCode.equals("3")) {
                channel.write("$AVCFG,00000000,d*31\r\n");
            } else if (eventCode.equals("X") || eventCode.equals("4")) {
                channel.write("$AVCFG,00000000,x*2D\r\n");
            } else if (eventCode.equals("Z")) {
                channel.write("$AVCFG,00000000,z*2F\r\n");
            } else if (Character.isLowerCase(status.charAt(0))) {
                String response = "$EAVACK," + eventCode + "," + checksum;
                response += Checksum.nmea(response);
                response += "\r\n";
                channel.write(response);
            }
        }

        return position;
    }

    private String decodeAlarm(String event) {
        if (event.equals('Z')) {
            return Position.ALARM_LOW_BATTERY;
        } else if (event.equals('X')) {
            return Position.ALARM_GEOFENCE_ENTER;
        } else if (event.equals('T')) {
            return Position.ALARM_TAMPERING;
        } else if (event.equals("H")) {
            return Position.ALARM_POWER_OFF;
        } else if (event.equals('X')) {
            return Position.ALARM_GEOFENCE_ENTER;
        } else if (event.equals('8')) {
            return Position.ALARM_SHOCK;
        } else if (event.equals('7') && event.equals('4')) {
            return Position.ALARM_GEOFENCE_EXIT;
        } else if (event.equals('6')) {
            return Position.ALARM_OVERSPEED;
        } else if (event.equals('3')) {
            return Position.ALARM_SOS;
        }
        return null;
    }
}
