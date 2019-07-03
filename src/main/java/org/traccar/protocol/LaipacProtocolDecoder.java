/*
 * Copyright 2013 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Device;
import org.traccar.model.Command;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.database.ConnectionManager;

import java.net.SocketAddress;
import java.util.regex.Pattern;
import java.util.Date;

public class LaipacProtocolDecoder extends BaseProtocolDecoder {

    private final String defaultDevicePassword;

    public LaipacProtocolDecoder(Protocol protocol) {
        super(protocol);
        defaultDevicePassword = Context.getConfig().getString(
            getProtocolName() + ".defaultPassword", "00000000");
    }

    private static final Pattern PATTERN_ECHK = new PatternBuilder()
            .text("$ECHK")
            .expression(",([^,]+)")             // identifier
            .number(",(d+)")                    // sequence number
            .text("*")
            .number("(xx)")                     // checksum
            .compile();

    private static final Pattern PATTERN_AVRMC = new PatternBuilder()
            .text("$AVRMC")
            .expression(",([^,]+)")              // identifier
            .number(",(dd)(dd)(dd)")             // time (hhmmss)
            .expression(",([AVRPavrp])")         // validity
            .number(",(dd)(dd.d+)")              // latitude
            .expression(",([NS])")               // latitude hemisphere
            .number(",(ddd)(dd.d+)")             // longitude
            .number(",([EW])")                   // longitude hemisphere
            .number(",(d+.d+)")                  // speed
            .number(",(d+.d+)")                  // course
            .number(",(dd)(dd)(dd)")             // date (ddmmyy)
            .expression(",([0-9A-Za-z])")        // event code
            .expression(",([\\d.]+)")            // battery voltage
            .number(",(d+)")                     // current mileage
            .number(",(d)")                      // gps status
            .number(",(d+)")                     // adc1
            .number(",(d+)")                     // adc2
            .number(",(x{1}|x{8})")              // lac+cid
            .number(",(d{1}|d{6})")              // mcc+mnc
            .optional(2)
            .expression(",([^*]*)")              // anything remaining (be forward compatible)
            .optional(1)
            .text("*")
            .number("(xx)")                      // checksum
            .compile();

    private String decodeAlarm(String event) {
        switch (event) {
            case "Z":
                return Position.ALARM_LOW_BATTERY;
            case "Y":
                return Position.ALARM_TOW;
            case "X":
                return Position.ALARM_GEOFENCE_ENTER;
            case "T":
                return Position.ALARM_TAMPERING;
            case "H":
                return Position.ALARM_POWER_OFF;
            case "8":
                return Position.ALARM_SHOCK;
            case "7":
            case "4":
                return Position.ALARM_GEOFENCE_EXIT;
            case "6":
                return Position.ALARM_OVERSPEED;
            case "5":
                return Position.ALARM_POWER_CUT;
            case "3":
                return Position.ALARM_SOS;
            default:
                return null;
        }
    }

    private String decodeEvent(String event, Position position) {

        if (event.length() == 1) {
            char inputStatus = event.charAt(0);
            if (inputStatus >= 'A' && inputStatus <= 'D') {
                int inputStatusInt = inputStatus - 'A';
                position.set(Position.PREFIX_IN + 1, inputStatusInt & 1);
                position.set(Position.PREFIX_IN + 2, inputStatusInt & 2);
                position.set(Position.KEY_IGNITION, ((inputStatusInt & 1) != 0) ? true : false);
                return null;
            }
        }

        return event;
    }

    private String getDevicePassword(DeviceSession deviceSession) {

        String devicePassword = defaultDevicePassword;

        Device device = Context.getIdentityManager().getById(deviceSession.getDeviceId());
        if (device != null) {
            String password = device.getString(Command.KEY_DEVICE_PASSWORD);
            if (password != null) {
                devicePassword = password;
            }
        }

        return devicePassword;
    }

    private Object handleEchk(
            String sentence, int checksum, Parser parser, Channel channel, SocketAddress remoteAddress) {

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(sentence + "\r\n", remoteAddress));
        }

        DeviceSession deviceSession =
            getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession != null) {
            ConnectionManager cm = Context.getConnectionManager();
            if (cm != null) {
                cm.updateDevice(deviceSession.getDeviceId(),
                    Device.STATUS_ONLINE, new Date());
            }
        }

        return null;
    }

    protected Object handleAvrmc(
            String sentence, int checksum, Parser parser, Channel channel, SocketAddress remoteAddress) {

        DeviceSession deviceSession =
            getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());

        // Device ID
        position.setDeviceId(deviceSession.getDeviceId());

        // Time
        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        // Status [ V: Invalid | A: Valid | R: Not realtime | P: Parked ]
        String status = parser.next();
        String upperCaseStatus = status.toUpperCase();
        position.setValid(upperCaseStatus.equals("A"));
        position.set(Position.KEY_STATUS, status);

        // Position
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        // Speed
        position.setSpeed(parser.nextDouble(0));

        // Course
        position.setCourse(parser.nextDouble(0));

        // Date
        dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        // Alarm / Event
        String event = parser.next();
        position.set(Position.KEY_ALARM, decodeAlarm(event));
        position.set(Position.KEY_EVENT, decodeEvent(event, position));

        // Battery
        position.set(Position.KEY_BATTERY, Double.parseDouble(parser.next().replaceAll("\\.", "")) * 0.001);

        // Odometer
        position.set(Position.KEY_ODOMETER, parser.nextDouble());

        // GPS status
        position.set(Position.KEY_GPS, parser.nextInt());

        // ADC1 / ADC2
        position.set(Position.PREFIX_ADC + 1, parser.nextDouble() * 0.001);
        position.set(Position.PREFIX_ADC + 2, parser.nextDouble() * 0.001);

        // LAC, CID, MCC, MNC
        Integer laccid = parser.nextHexInt();
        Integer mccmnc = parser.nextInt();
        if (laccid != null && laccid != 0 && mccmnc != null && mccmnc != 0) {
            Integer lac = (laccid >> 16) & 0xFFFF;
            Integer cid = laccid & 0xFFFF;
            Integer mcc = mccmnc / 10000;
            Integer mnc = mccmnc % 100;
            position.setNetwork(new Network(CellTower.from(mcc, mnc, lac, cid)));
        }

        // Skip remaining parameters
        String unused = parser.next();

        // Checksum
        String checksumStr = parser.next();
        if (checksum != Integer.parseInt(checksumStr, 16)) {
            return null;
        }

        if (channel != null) {

            if (Character.isLowerCase(status.charAt(0))) {
                String ack = "$EAVACK," + event + "," + checksumStr;
                ack += Checksum.nmea(ack) + "\r\n";
                channel.writeAndFlush(new NetworkMessage(ack, remoteAddress));
            }

            String response = "";
            String devicePassword = getDevicePassword(deviceSession);

            if (event.equals("3")) {
                response = "$AVCFG," + devicePassword + ",d";
            } else if (event.equals("S") || event.equals("T")) {
                response = "$AVCFG," + devicePassword + ",t";
            } else if (event.equals("X") || event.equals("4")) {
                response = "$AVCFG," + devicePassword + ",x";
            } else if (event.equals("Y")) {
                response = "$AVCFG," + devicePassword + ",y";
            } else if (event.equals("Z")) {
                response = "$AVCFG," + devicePassword + ",z";
            }

            if (response.length() > 0) {
                response += Checksum.nmea(response) + "\r\n";
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }
        }

        return position;
    }

    @Override
    protected Object decode(
        Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        // Validate sentence length
        int slen = sentence.length();
        if (slen <= 5) {
            return null;
        }

        // Validate sentence format
        if (sentence.charAt(0) != '$') {
            return null;
        }
        if (sentence.charAt(slen - 3) != '*') {
            return null;
        }

        // Verify sentence checksum
        int checksum = Integer.parseInt(sentence.substring(slen - 2), 16);
        if (checksum != Checksum.xor(sentence.substring(1, slen - 3))) {
            return null;
        }

        // Handle ECHK sentences
        Parser parser = new Parser(PATTERN_ECHK, sentence);
        if (parser.matches()) {
            return handleEchk(sentence, checksum, parser, channel, remoteAddress);
        }

        // Handle AVRMC sentences
        parser = new Parser(PATTERN_AVRMC, sentence);
        if (parser.matches()) {
            return handleAvrmc(sentence, checksum,  parser, channel, remoteAddress);
        }

        return null;
    }


}
