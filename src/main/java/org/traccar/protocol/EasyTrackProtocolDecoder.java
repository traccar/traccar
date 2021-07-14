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
import java.util.regex.Pattern;

public class EasyTrackProtocolDecoder extends BaseProtocolDecoder {

    public EasyTrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("*").expression("..,")         // manufacturer
            .number("(d+),")                     // imei
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
            .number("(x+),")                     // fuel
            .number("(x+)")                      // odometer
            .groupBegin()
            .number(",(x+)")                     // altitude
            .groupBegin()
            .number(",d+")                       // gps data
            .number(",(d*)")                     // rfid
            .number(",(x+)")                     // temperature
            .number(",(d+.d+)")                  // adc
            .number(",(d+)")                     // satellites
            .groupEnd("?")
            .groupEnd("?")
            .any()
            .compile();

    private static final Pattern PATTERN_CELL = new PatternBuilder()
            .text("*").expression("..,")         // manufacturer
            .number("(d+),")                     // imei
            .text("JZ,")                         // command
            .number("([01]),")                   // result
            .number("(d+),")                     // cid
            .number("(d+),")                     // lac
            .number("(d+),")                     // mcc
            .number("(d+)")                      // mnc
            .any()
            .compile();

    private String decodeAlarm(long status) {
        if ((status & 0x02000000) != 0) {
            return Position.ALARM_GEOFENCE_ENTER;
        }
        if ((status & 0x04000000) != 0) {
            return Position.ALARM_GEOFENCE_EXIT;
        }
        if ((status & 0x08000000) != 0) {
            return Position.ALARM_LOW_BATTERY;
        }
        if ((status & 0x20000000) != 0) {
            return Position.ALARM_VIBRATION;
        }
        if ((status & 0x80000000) != 0) {
            return Position.ALARM_OVERSPEED;
        }
        if ((status & 0x00010000) != 0) {
            return Position.ALARM_SOS;
        }
        if ((status & 0x00040000) != 0) {
            return Position.ALARM_POWER_CUT;
        }
        return null;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        String type = sentence.substring(20, 22);

        if (channel != null && (type.equals("TX") || type.equals("MQ"))) {
            channel.writeAndFlush(new NetworkMessage(sentence + "#", remoteAddress));
        }

        if (type.equals("JZ")) {
            return decodeCell(channel, remoteAddress, sentence);
        } else {
            return decodeLocation(channel, remoteAddress, sentence);
        }
    }

    private Position decodeLocation(Channel channel, SocketAddress remoteAddress, String sentence) {

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
        position.set(Position.KEY_ALARM, decodeAlarm(status));
        position.set(Position.KEY_BLOCKED, (status & 0x00080000) > 0);
        position.set(Position.KEY_IGNITION, (status & 0x00800000) > 0);
        position.set(Position.KEY_STATUS, status);

        position.set(Position.KEY_RSSI, parser.nextHexInt());
        position.set(Position.KEY_POWER, parser.nextDouble());
        position.set(Position.KEY_FUEL_LEVEL, parser.nextHexInt());
        position.set(Position.KEY_ODOMETER, parser.nextHexInt() * 100);

        position.setAltitude(parser.nextDouble(0));

        if (parser.hasNext(4)) {
            position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());
            position.set(Position.PREFIX_TEMP + 1, parser.nextHexInt() * 0.01);
            position.set(Position.PREFIX_ADC + 1, parser.nextDouble());
            position.set(Position.KEY_SATELLITES, parser.nextInt());
        }

        return position;
    }

    private Position decodeCell(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_CELL, sentence);
        if (!parser.matches()) {
            return null;
        }

        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        if (channel != null && parser.nextInt() > 0) {
            String response = String.format("*ET,%s,JZ,undefined#", imei);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }

        int cid = parser.nextInt();
        int lac = parser.nextInt();
        int mcc = parser.nextInt();
        int mnc = parser.nextInt();
        position.setNetwork(new Network(CellTower.from(mcc, mnc, lac, cid)));

        return position;
    }

}
