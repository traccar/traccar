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
import org.traccar.helper.model.AttributeUtil;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.helper.BitUtil;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class LaipacProtocolDecoder extends BaseProtocolDecoder {

    public LaipacProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final String DEFAULT_DEVICE_PASSWORD = "00000000";

    private static final Pattern PATTERN_EAVSYS = new PatternBuilder()
            .text("$EAVSYS,")
            .expression("([^,]+),")              // identifier
            .expression("([0-9]+),")             // iccid
            .expression("(\\+?[0-9]+)?,")        // sim phone number
            .expression("(?:[^,]*),")            // owner name
            .expression("([^,]*)?")              // firmware version
            .text("*")
            .number("(xx)")                      // checksum
            .compile();

    private static final Pattern PATTERN_AVRMC = new PatternBuilder()
            .text("$AVRMC,")
            .expression("([^,]+),")              // identifier
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([AVRPavrp]),")         // validity
            .number("(dd)(dd.d+),")              // latitude
            .expression("([NS]),")               // latitude hemisphere
            .number("(ddd)(dd.d+),")             // longitude
            .number("([EW]),")                   // longitude hemisphere
            .number("(d+.d+),")                  // speed
            .number("(d+.d+),")                  // course
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .expression("([0-9A-Za-z]),")        // event code
            .expression("([\\d.]+),")            // battery voltage
            .number("(d+),")                     // current mileage
            .number("(d),")                      // gps status
            .number("(d+),")                     // adc1
            .number("(d+)")                      // adc2
            .number(",(xxxx|x)")                 // lac | lac+cid = 0
            .number("(xxxx),")                   // cid | nothing
            .number("(ddd|d)")                   // mcc | mcc+mnc = 0
            .number("(ddd)")                     // mnc | nothing
            .optional(4)
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
                return Position.ALARM_VIBRATION;
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

    private String decodeEvent(String event, Position position, String model) {

        if (event.length() == 1) {
            char inputStatus = event.charAt(0);
            if (inputStatus >= 'A' && inputStatus <= 'D') {
                int inputStatusInt = inputStatus - 'A';
                position.set(Position.PREFIX_IN + 1, (boolean) BitUtil.check(inputStatusInt, 0));
                position.set(Position.PREFIX_IN + 2, (boolean) BitUtil.check(inputStatusInt, 1));
                if ("SF-Lite".equals(model)) {
                    position.set(Position.PREFIX_IN + 3, false);
                }
                return null;
            } else if (inputStatus >= 'O' && inputStatus <= 'R') {
                int inputStatusInt = inputStatus - 'O';
                position.set(Position.PREFIX_IN + 1, (boolean) BitUtil.check(inputStatusInt, 0));
                position.set(Position.PREFIX_IN + 2, (boolean) BitUtil.check(inputStatusInt, 1));
                if ("SF-Lite".equals(model)) {
                    position.set(Position.PREFIX_IN + 3, true);
                }
                return null;
            }
        }

        return event;

    }

    private void sendEventResponse(
            String event, String devicePassword, Channel channel, SocketAddress remoteAddress) {

        String responseCode = null;

        switch (event) {
            case "3":
                responseCode = "d";
                break;
            case "M":
                responseCode = "m";
                break;
            case "S":
            case "T":
                responseCode = "t";
                break;
            case "X":
            case "4":
                responseCode = "x";
                break;
            case "Y":
                responseCode = "y";
                break;
            case "Z":
                responseCode = "z";
                break;
            default:
                break;
        }

        if (responseCode != null) {
            String response = "$AVCFG," + devicePassword + "," + responseCode;
            response += Checksum.nmea(response.substring(1)) + "\r\n";
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }

    }

    private void sendAcknowledge(
            String status, String event, String checksum, Channel channel, SocketAddress remoteAddress) {

        if (Character.isLowerCase(status.charAt(0))) {
            String response = "$EAVACK," + event + "," + checksum;
            response += Checksum.nmea(response.substring(1)) + "\r\n";
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }

    }

    protected Object decodeEavsys(
            String sentence, Channel channel, SocketAddress remoteAddress) {

        Parser parser = new Parser(PATTERN_EAVSYS, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession =
            getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        position.set(Position.KEY_ICCID, parser.next());
        position.set(Position.KEY_PHONE, parser.next());
        position.set(Position.KEY_VERSION_FW, parser.next());

        return position;
    }

    protected Object decodeAvrmc(
            String sentence, Channel channel, SocketAddress remoteAddress) {

        Parser parser = new Parser(PATTERN_AVRMC, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession =
            getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        String model = getDeviceModel(deviceSession);

        Position position = new Position(getProtocolName());

        position.setDeviceId(deviceSession.getDeviceId());
        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        String status = parser.next();
        String upperCaseStatus = status.toUpperCase();
        position.setValid(upperCaseStatus.equals("A") || upperCaseStatus.equals("R") || upperCaseStatus.equals("P"));
        position.set(Position.KEY_STATUS, status);

        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        String event = parser.next();
        position.set(Position.KEY_ALARM, decodeAlarm(event));
        position.set(Position.KEY_EVENT, decodeEvent(event, position, model));
        position.set(Position.KEY_BATTERY, Double.parseDouble(parser.next().replaceAll("\\.", "")) * 0.001);
        position.set(Position.KEY_ODOMETER, parser.nextDouble() * 1000);
        position.set(Position.KEY_GPS, parser.nextInt());
        position.set(Position.PREFIX_ADC + 1, parser.nextDouble() * 0.001);

        if ("AVL110".equals(model) || "AVL120".equals(model)) {
            position.set(Position.PREFIX_ADC + 2, parser.nextDouble() * 0.001);
        } else {
            parser.next();
        }

        Integer lac = parser.nextHexInt();
        Integer cid = parser.nextHexInt();
        Integer mcc = parser.nextInt();
        Integer mnc = parser.nextInt();
        if (lac != null && cid != null && mcc != null && mnc != null) {
            position.setNetwork(new Network(CellTower.from(mcc, mnc, lac, cid)));
        }

        parser.next(); // unused

        String checksum = parser.next();

        if (channel != null) {

            sendAcknowledge(status, event, checksum, channel, remoteAddress);

            String devicePassword = AttributeUtil.getDevicePassword(
                    getCacheManager(), deviceSession.getDeviceId(), getProtocolName(), DEFAULT_DEVICE_PASSWORD);
            sendEventResponse(event, devicePassword, channel, remoteAddress);
        }

        return position;
    }

    @Override
    protected Object decode(
        Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("$ECHK")) {
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(sentence + "\r\n", remoteAddress));
            }
        } else if (sentence.startsWith("$EAVSYS")) {
            return decodeEavsys(sentence, channel, remoteAddress);
        } else if (sentence.startsWith("$AVRMC")) {
            return decodeAvrmc(sentence, channel, remoteAddress);
        }

        return null;
    }

}
