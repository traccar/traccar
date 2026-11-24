/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class AccentProtocolDecoder extends BaseProtocolDecoder {

    public AccentProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_CONNECT = new PatternBuilder()
            .text("AA00")
            .groupBegin()
            .any()
            .text("ICC:").expression("([^,]*)")  // iccid
            .text(", IMEI:").expression("([^,]*)") // imei
            .groupEnd("?")
            .any()
            .compile();

    private static final Pattern PATTERN_INFO = new PatternBuilder()
            .text("AA01")
            .expression("([^,]*), ICC:")         // firmware version
            .expression("([^,]*), IMEI:")        // iccid
            .number("(d+)")                      // imei
            .any()
            .compile();

    private static final Pattern PATTERN_DATA = new PatternBuilder()
            .text("AA")
            .number("d")                         // type
            .number("(x)")                       // version
            .number("(x{6})")                    // time of day
            .number("(x{8})")                    // latitude
            .number("(x{8})")                    // longitude
            .number("(x{4})")                    // speed
            .number("(xx)")                      // course
            .number("(xx)")                      // power
            .number("(xx)")                      // fuel
            .number("(xx)")                      // accelerometer x
            .number("(xx)")                      // accelerometer y
            .number("(xx)")                      // accelerometer z
            .number("(xx)")                      // flags
            .number("(x{4})")                    // temperature
            .number("(x{8})")                    // odometer
            .number("(xx)")                      // event
            .number("(x{8})")                    // added info
            .number("(x{4})")                    // date
            .groupBegin()
            .number("(xx)")                      // CAN fuel
            .number("(xx)")                      // coolant
            .number("(x{8})")                    // accumulated odometer
            .number("xx")                        // FMS speed
            .number("(xxxx)")                    // rpm
            .number("(xxxx)")                    // consumption
            .number("(x{8})")                    // fuel used
            .groupEnd("?")
            .number("(xx)").optional()           // satellites
            .number("(xx)").optional()           // rssi
            .any()
            .compile();

    private double decodeCoordinate(Parser parser) {
        double value = Long.parseLong(parser.next(), 16) / 10000.0;
        int degrees = (int) value / 100;
        return degrees + (value - degrees * 100) / 60;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        if (sentence.isEmpty()) {
            return null;
        }

        String uniqueId = null;
        int space = sentence.indexOf(' ');
        if (space > 0 && !sentence.startsWith("AA")) {
            uniqueId = sentence.substring(0, space);
            sentence = sentence.substring(space + 1);
        }

        if (sentence.equals("AAAA")) {
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage("AA06\r\n", remoteAddress));
            }
            return null;
        }

        String type = sentence.substring(2, 4);
        return switch (type) {
            case "00" -> decodeConnect(channel, remoteAddress, sentence, uniqueId);
            case "01" -> decodeInfo(channel, remoteAddress, sentence, uniqueId);
            default -> decodeData(channel, remoteAddress, sentence, uniqueId);
        };
    }

    private Position decodeConnect(
            Channel channel, SocketAddress remoteAddress, String sentence, String uniqueId) {

        Parser parser = new Parser(PATTERN_CONNECT, sentence);
        if (!parser.matches()) {
            return null;
        }

        String iccid = parser.next();
        String imei = parser.next();

        String id = uniqueId != null ? uniqueId : imei;
        if (id == null || iccid == null) {
            return null;
        }
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        position.set(Position.KEY_ICCID, iccid);

        return position;
    }

    private Position decodeInfo(
            Channel channel, SocketAddress remoteAddress, String sentence, String uniqueId) {

        Parser parser = new Parser(PATTERN_INFO, sentence);
        if (!parser.matches()) {
            return null;
        }

        String firmware = parser.next();
        String iccid = parser.next();
        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(
                channel, remoteAddress, uniqueId != null ? uniqueId : imei);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        position.addAlarm(Position.ALARM_POWER_ON);
        position.set(Position.KEY_VERSION_FW, firmware);
        position.set(Position.KEY_ICCID, iccid);

        return position;
    }

    private Position decodeData(
            Channel channel, SocketAddress remoteAddress, String sentence, String uniqueId) {

        Parser parser = new Parser(PATTERN_DATA, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = uniqueId != null
                ? getDeviceSession(channel, remoteAddress, uniqueId)
                : getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        int version = parser.nextHexInt();
        int seconds = parser.nextHexInt();

        double latitude = decodeCoordinate(parser);
        double longitude = decodeCoordinate(parser);

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextHexInt() / 10.0));

        int course = parser.nextHexInt();
        position.setCourse(version == 3 ? course * 8 : course);

        int power = parser.nextHexInt();
        position.set(Position.KEY_BATTERY, BitUtil.to(power, 7) / 10.0);
        if (!BitUtil.check(power, 7)) {
            position.set(Position.KEY_CHARGE, true);
        }

        position.set(Position.PREFIX_ADC + 1, parser.nextHexInt());
        position.set(Position.KEY_G_SENSOR,
                "[" + parser.nextHexInt(0) + "," + parser.nextHexInt(0) + "," + parser.nextHexInt(0) + "]");

        int flags = parser.nextHexInt();
        position.setValid(BitUtil.check(flags, 6));
        position.setLatitude(BitUtil.check(flags, 0) ? latitude : -latitude);
        position.setLongitude(BitUtil.check(flags, 1) ? longitude : -longitude);
        position.set(Position.KEY_IGNITION, BitUtil.check(flags, 2));
        position.set(Position.PREFIX_IN + 1, BitUtil.check(flags, 2));
        position.set(Position.PREFIX_IN + 2, BitUtil.check(flags, 3));
        position.set(Position.PREFIX_IN + 3, BitUtil.check(flags, 4));
        position.set(Position.PREFIX_OUT + 1, BitUtil.check(flags, 5));
        if (!BitUtil.check(flags, 7)) {
            position.set(Position.KEY_ARCHIVE, true);
        }

        int temperature = parser.nextHexInt();
        if ((temperature & 0xFF00) == 0x8000) {
            position.set(Position.PREFIX_ADC + 2, temperature & 0xFF);
        } else {
            position.set(Position.PREFIX_TEMP + 1, (short) temperature / 16.0);
        }

        position.set(Position.KEY_ODOMETER, Long.parseLong(parser.next(), 16) * 1000);
        position.set(Position.KEY_EVENT, parser.nextHexInt());
        position.set("addedInfo", Long.parseLong(parser.next(), 16));

        int packedDate = parser.nextHexInt();
        position.setTime(new DateBuilder()
                .setDate(packedDate / (31 * 12), packedDate / 31 % 12 + 1, packedDate % 31 + 1)
                .addSeconds(seconds)
                .getDate());

        if (parser.hasNext(6)) {
            position.set(Position.KEY_FUEL_LEVEL, parser.nextHexInt());
            position.set(Position.KEY_COOLANT_TEMP, parser.nextHexInt() - 40);
            position.set(Position.KEY_ODOMETER, Long.parseLong(parser.next(), 16) * 1000);
            position.set(Position.KEY_RPM, parser.nextHexInt());
            position.set(Position.KEY_FUEL_CONSUMPTION, parser.nextHexInt());
            position.set(Position.KEY_FUEL_USED, Long.parseLong(parser.next(), 16) / 2.0);
        }

        position.set(Position.KEY_SATELLITES, parser.nextHexInt());
        position.set(Position.KEY_RSSI, parser.nextHexInt());

        return position;
    }

}
