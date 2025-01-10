/*
 * Copyright 2013 - 2025 Anton Tananaev (anton@traccar.org)
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
import java.util.Set;
import java.util.regex.Pattern;

public class EasyTrackProtocolDecoder extends BaseProtocolDecoder {

    public EasyTrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("*").expression("..,")         // manufacturer
            .number("d+,")                       // imei
            .expression("([^,]{2}),")            // command
            .expression("([AV]),")               // validity
            .number("(xx)(xx)(xx),")             // date (yymmdd)
            .number("(xx)(xx)(xx),")             // time (hhmmss)
            .number("(x)(x{7}),")                // latitude
            .number("(x)(x{7}),")                // longitude
            .number("(x{4}),")                   // speed
            .number("(x{4}),")                   // course
            .number("(x{8}),")                   // status
            .number("(x+),")                     // signal
            .number("(d+),")                     // power
            .number("(x+),")                     // fuel / index
            .number("(x+)")                      // odometer
            .groupBegin()
            .number(",(x+)")                     // altitude
            .groupBegin()
            .number(",d+")                       // gps data
            .number(",(x*)")                     // rfid
            .number(",(x+)")                     // temperature
            .number(",(d+.d+)")                  // adc
            .number(",(d+)")                     // satellites
            .groupEnd("?")
            .groupEnd("?")
            .any()
            .compile();

    private static final Pattern PATTERN_CELL = new PatternBuilder()
            .text("*").expression("..,")         // manufacturer
            .number("d+,")                       // imei
            .text("JZ,")                         // command
            .number("[01],")                     // result
            .number("(d+),")                     // cid
            .number("(d+),")                     // lac
            .number("(d+),")                     // mcc
            .number("(d+)")                      // mnc
            .any()
            .compile();

    private static final Pattern PATTERN_OBD = new PatternBuilder()
            .text("*").expression("..,")         // manufacturer
            .number("d+,")                       // imei
            .text("OB,")                         // command
            .text("BD$")
            .number("V(d+.d);")                  // battery
            .number("R(d+);")                    // rpm
            .number("S(d+);")                    // speed
            .number("P(d+.d);")                  // throttle
            .number("O(d+.d);")                  // engine load
            .number("C(d+);")                    // coolant temperature
            .number("L(d+.d);")                  // fuel level
            .number("[XY][MH]d+.d+;")
            .number("Md+.?d*;")                  // mileage
            .number("F(d+.d+);")                 // fuel consumption
            .number("T(d+);")                    // engine time
            .any()
            .compile();

    private void decodeAlarm(Position position, long status, String model) {
        if ((status & 0x02000000L) != 0) {
            position.addAlarm(Position.ALARM_GEOFENCE_ENTER);
        }
        if ((status & 0x04000000L) != 0) {
            position.addAlarm(Position.ALARM_GEOFENCE_EXIT);
        }
        if ((status & 0x08000000L) != 0) {
            position.addAlarm(Position.ALARM_LOW_BATTERY);
        }
        if ((status & 0x10000000L) != 0 || (status & 0x00000008L) != 0) {
            position.addAlarm(Position.ALARM_JAMMING);
        }
        if ((status & 0x20000000L) != 0) {
            position.addAlarm(Position.ALARM_VIBRATION);
        }
        if ((status & 0x80000000L) != 0) {
            position.addAlarm(Position.ALARM_OVERSPEED);
        }
        if ((status & 0x00010000L) != 0 || (status & 0x00000400L) != 0) {
            position.addAlarm(Position.ALARM_SOS);
        }
        if ((status & 0x00040000L) != 0) {
            position.addAlarm("E3+4G".equals(model) ? Position.ALARM_TAMPERING : Position.ALARM_POWER_CUT);
        }
        if ((status & 0x00040000L) != 0) {
            position.addAlarm(Position.ALARM_POWER_CUT);
        }
        if ((status & 0x00004000L) != 0) {
            position.addAlarm(Position.ALARM_LOW_POWER);
        }
        if ((status & 0x00008000L) != 0) {
            position.addAlarm(Position.ALARM_TEMPERATURE);
        }
        if ((status & 0x00000100L) != 0) {
            position.addAlarm(Position.ALARM_REMOVING);
        }
        if ((status & 0x00000001L) != 0) {
            position.addAlarm(Position.ALARM_BRAKING);
        }
        if ((status & 0x00000002L) != 0) {
            position.addAlarm(Position.ALARM_ACCELERATION);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        int typeIndex = sentence.indexOf(',', 4) + 1;
        String imei = sentence.substring(4, typeIndex - 1);
        String type = sentence.substring(typeIndex, typeIndex + 2);

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        if (channel != null) {
            if (type.equals("TX") || type.equals("MQ")) {
                channel.writeAndFlush(new NetworkMessage(sentence + "#", remoteAddress));
            } else if ("E3+4G".equals(getDeviceModel(deviceSession))
                    && Set.of("HB", "CC", "AM", "DW", "JZ").contains(type)) {
                channel.writeAndFlush(new NetworkMessage(sentence.substring(0, typeIndex + 3) + "ACK#", remoteAddress));
            }
        }

        if (type.equals("OB")) {
            return decodeObd(deviceSession, sentence);
        } else if (type.equals("JZ")) {
            if (channel != null && Integer.parseInt(sentence.substring(typeIndex + 3, typeIndex + 4)) > 0) {
                channel.writeAndFlush(new NetworkMessage(String.format("*ET,%s,JZ,undefined#", imei), remoteAddress));
            }
            return decodeCell(deviceSession, sentence);
        } else {
            return decodeLocation(deviceSession, sentence);
        }
    }

    private Position decodeLocation(DeviceSession deviceSession, String sentence) {

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        String model = getDeviceModel(deviceSession);

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_COMMAND, parser.next());

        position.setValid(parser.next().equals("A"));

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextHexInt(), parser.nextHexInt(), parser.nextHexInt())
                .setTime(parser.nextHexInt(), parser.nextHexInt(), parser.nextHexInt());
        position.setTime(dateBuilder.getDate());

        if (BitUtil.check(parser.nextHexInt(), 3)) {
            position.setLatitude(-parser.nextHexInt() / 600000.0);
        } else {
            position.setLatitude(parser.nextHexInt() / 600000.0);
        }

        if (BitUtil.check(parser.nextHexInt(), 3)) {
            position.setLongitude(-parser.nextHexInt() / 600000.0);
        } else {
            position.setLongitude(parser.nextHexInt() / 600000.0);
        }

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextHexInt() / 100.0));
        double course = parser.nextHexInt() * 0.01;
        if (course < 360) {
            position.setCourse(course);
        }

        long status = parser.nextHexLong();
        decodeAlarm(position, status, model);
        position.set(Position.KEY_BLOCKED, (status & 0x00080000) > 0);
        position.set(Position.KEY_IGNITION, (status & 0x00800000) > 0);
        position.set(Position.KEY_STATUS, status);

        position.set(Position.KEY_RSSI, parser.nextHexInt());
        position.set(Position.KEY_POWER, parser.nextDouble());

        if ("E3+4G".equals(model)) {
            position.set(Position.KEY_INDEX, parser.nextHexInt());
        } else {
            position.set(Position.KEY_FUEL_LEVEL, parser.nextHexInt());
        }

        position.set(Position.KEY_ODOMETER, parser.nextHexInt() * 100);

        position.setAltitude(parser.nextDouble(0));

        if (parser.hasNext(4)) {
            if ("E3+4G".equals(model)) {
                position.set(Position.KEY_HOURS, parser.nextHexLong() * 60000);
            } else {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());
            }

            position.set(Position.PREFIX_TEMP + 1, parser.nextHexInt() * 0.01);
            position.set(Position.PREFIX_ADC + 1, parser.nextDouble());
            position.set(Position.KEY_SATELLITES, parser.nextInt());
        }

        return position;
    }

    private Position decodeCell(DeviceSession deviceSession, String sentence) {

        Parser parser = new Parser(PATTERN_CELL, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        int cid = parser.nextInt();
        int lac = parser.nextInt();
        int mcc = parser.nextInt();
        int mnc = parser.nextInt();
        position.setNetwork(new Network(CellTower.from(mcc, mnc, lac, cid)));

        return position;
    }

    private Position decodeObd(DeviceSession deviceSession, String sentence) {

        Parser parser = new Parser(PATTERN_OBD, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.KEY_RPM, parser.nextInt());
        position.set(Position.KEY_OBD_SPEED, parser.nextInt());
        position.set(Position.KEY_THROTTLE, parser.nextDouble());
        position.set(Position.KEY_ENGINE_LOAD, parser.nextDouble());
        position.set(Position.KEY_COOLANT_TEMP, parser.nextInt());
        position.set(Position.KEY_FUEL_LEVEL, parser.nextDouble());
        position.set(Position.KEY_FUEL_CONSUMPTION, parser.nextDouble());
        position.set(Position.KEY_HOURS, parser.nextInt());

        return position;
    }

}
