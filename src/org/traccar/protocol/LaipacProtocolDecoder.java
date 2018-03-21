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
            .expression("([\\d.]+),")            // battery voltage
            .number("(d+),")                     // current mileage
            .number("(d),")                      // gps status
            .number("(d+),")                     // adc1
            .number("(d+)")                      // adc2
            .number(",(xxxx)")                   // lac
            .number("(xxxx),")                   // cid
            .number("(ddd)")                     // mcc
            .number("(ddd)")                     // mnc
            .optional(4)
            .text("*")
            .number("(xx)")                      // checksum
            .compile();

    private String decodeAlarm(String event) {
        switch (event) {
            case "Z":
                return Position.ALARM_LOW_BATTERY;
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
            case "3":
                return Position.ALARM_SOS;
            default:
                return null;
        }
    }

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

        String event = parser.next();
        position.set(Position.KEY_ALARM, decodeAlarm(event));
        position.set(Position.KEY_EVENT, event);
        position.set(Position.KEY_BATTERY, Double.parseDouble(parser.next().replaceAll("\\.", "")) * 0.001);
        position.set(Position.KEY_ODOMETER, parser.nextDouble());
        position.set(Position.KEY_GPS, parser.nextInt());
        position.set(Position.PREFIX_ADC + 1, parser.nextDouble() * 0.001);
        position.set(Position.PREFIX_ADC + 2, parser.nextDouble() * 0.001);

        Integer lac = parser.nextHexInt();
        Integer cid = parser.nextHexInt();
        Integer mcc = parser.nextInt();
        Integer mnc = parser.nextInt();
        if (lac != null && cid != null && mcc != null && mnc != null) {
            position.setNetwork(new Network(CellTower.from(mcc, mnc, lac, cid)));
        }

        String checksum = parser.next();

        if (channel != null) {
            if (event.equals("3")) {
                channel.write("$AVCFG,00000000,d*31\r\n");
            } else if (event.equals("X") || event.equals("4")) {
                channel.write("$AVCFG,00000000,x*2D\r\n");
            } else if (event.equals("Z")) {
                channel.write("$AVCFG,00000000,z*2F\r\n");
            } else if (Character.isLowerCase(status.charAt(0))) {
                String response = "$EAVACK," + event + "," + checksum;
                response += Checksum.nmea(response) + "\r\n";
                channel.write(response);
            }
        }

        return position;
    }

}
