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
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class AccentProtocolDecoder extends BaseProtocolDecoder {

    private static final String ATTRIBUTE_FMS = "useFms";
    private static final String ATTRIBUTE_J1708 = "useJ1708";
    private static final String ATTRIBUTE_FUEL_SOURCE = "accent.fuelSrc";
    private static final String KEY_IMEI = "imei";
    private static final String TYPE_INF = "INF";
    private static final String TYPE_PNA = "PNA";

    private String imei;
    private Boolean useFms;

    public AccentProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_INFO = new PatternBuilder()
            .text("AA01")
            .expression("([^,]*), ICC:")        // firmware version
            .expression("([^,]*), IMEI:")       // iccid
            .number("(d+)")                     // imei
            .any()
            .compile();

    private static final Pattern PATTERN_DATA = new PatternBuilder()
            .text("AA")
            .number("d")                         // type
            .number("x")                         // version
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
            .number("xx")                        // reserved
            .number("(xxxx)")                    // rpm
            .number("(xxxx)")                    // consumption
            .number("(x{8})")                    // fuel used
            .number("(xx)")                      // satellites
            .number("(xx)")                      // rssi
            .number("(xx)").optional()           // 106-char frame
            .groupEnd("?")
            .any()
            .compile();

    private void sendAck(Channel channel, SocketAddress remoteAddress) {
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage("AA06\r\n", remoteAddress));
        }
    }

    private static double coordinate(long raw, boolean positive) {
        double value = raw / 10000.0;
        int degrees = (int) value / 100;
        double result = degrees + (value - degrees * 100) / 60;
        return positive ? result : -result;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = ((String) msg).trim();
        if (sentence.isEmpty()) {
            return null;
        }

        String uniqueId = null;
        int space = sentence.indexOf(' ');
        if (space > 0 && !sentence.startsWith("AA")) {
            uniqueId = sentence.substring(0, space);
            sentence = sentence.substring(space + 1).trim();
        }

        if (sentence.equals("AAAA")) {
            sendAck(channel, remoteAddress);
            return null;
        }

        if (sentence.startsWith("AA01MF2")) {
            useFms = !sentence.startsWith("AA01MF2_XS");
            return null;
        }

        Parser info = new Parser(PATTERN_INFO, sentence);
        if (info.matches()) {
            String firmware = info.next();
            String iccid = info.next();
            imei = info.next();
            DeviceSession deviceSession = getDeviceSession(
                    channel, remoteAddress, uniqueId != null ? uniqueId : imei);
            if (deviceSession == null) {
                imei = null;
                return null;
            }
            Position position = createPosition(deviceSession, TYPE_PNA);
            if (!firmware.isEmpty()) {
                position.set(Position.KEY_VERSION_FW, firmware);
            }
            if (!iccid.isEmpty()) {
                position.set(Position.KEY_ICCID, iccid);
            }
            position.set(KEY_IMEI, imei);
            return position;
        }

        if (sentence.startsWith("AA00")) {
            String iccid = field(sentence, "ICC:");
            String modem = field(sentence, "IMEI:");
            if (modem != null) {
                imei = modem;
            }
            String id = uniqueId != null ? uniqueId : imei;
            if (id == null) {
                return null;
            }
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
            if (deviceSession == null) {
                imei = null;
                return null;
            }
            Position position = createPosition(deviceSession, TYPE_INF);
            if (iccid != null) {
                position.set(Position.KEY_ICCID, iccid);
            }
            if (imei != null) {
                position.set(KEY_IMEI, imei);
            }
            return position;
        }

        Parser parser = new Parser(PATTERN_DATA, sentence);
        if (!parser.matches()) {
            return null;
        }

        String id = uniqueId != null ? uniqueId : imei;
        DeviceSession deviceSession = id != null
                ? getDeviceSession(channel, remoteAddress, id)
                : getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        int seconds = parser.nextHexInt();
        long latitude = Long.parseLong(parser.next(), 16);
        long longitude = Long.parseLong(parser.next(), 16);
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextHexInt() / 10.0));
        position.setCourse(parser.nextHexInt());
        int power = parser.nextHexInt();
        position.set(Position.KEY_FUEL_LEVEL, parser.nextHexInt());
        position.set("x", (int) (byte) parser.nextHexInt(0));
        position.set("y", (int) (byte) parser.nextHexInt(0));
        position.set("z", (int) (byte) parser.nextHexInt(0));
        int flags = parser.nextHexInt();
        int temperature = parser.nextHexInt();
        position.set(Position.KEY_ODOMETER, Long.parseLong(parser.next(), 16) * 1000);
        position.set(Position.KEY_EVENT, parser.nextHexInt());
        position.set("addedInfo", Long.parseLong(parser.next(), 16));
        int packedDate = parser.nextHexInt();

        position.setTime(new DateBuilder()
                .setDate(packedDate / (31 * 12), packedDate % (31 * 12) / 31 + 1, packedDate % 31 + 1)
                .setTime(seconds / 3600, seconds % 3600 / 60, seconds % 60)
                .getDate());
        position.setValid(BitUtil.check(flags, 6));
        position.setLatitude(coordinate(latitude, BitUtil.check(flags, 0)));
        position.setLongitude(coordinate(longitude, BitUtil.check(flags, 1)));

        position.set(Position.KEY_BATTERY, BitUtil.to(power, 7) / 10.0);
        position.set(Position.KEY_CHARGE, !BitUtil.check(power, 7));
        position.set(Position.KEY_IGNITION, BitUtil.check(flags, 2));
        position.set(Position.PREFIX_IN + 1, BitUtil.check(flags, 2));
        position.set(Position.PREFIX_IN + 2, BitUtil.check(flags, 3));
        position.set(Position.PREFIX_IN + 3, BitUtil.check(flags, 4));
        position.set(Position.PREFIX_OUT + 1, BitUtil.check(flags, 5));
        position.set(Position.KEY_ARCHIVE, !BitUtil.check(flags, 7));
        if ((temperature & 0xFF00) == 0x8000) {
            position.set(Position.PREFIX_ADC + 1, temperature & 0xFF);
        } else {
            position.set(Position.PREFIX_TEMP + 1, (short) temperature / 16.0);
        }

        if (parser.hasNext(8)) {
            decodeCan(position, parser);
        }

        return position;
    }

    private static String field(String sentence, String label) {
        int index = sentence.indexOf(label);
        if (index < 0) {
            return null;
        }
        int start = index + label.length();
        int end = sentence.indexOf(',', start);
        String value = (end < 0 ? sentence.substring(start) : sentence.substring(start, end)).trim();
        return value.isEmpty() ? null : value;
    }

    private Position createPosition(DeviceSession deviceSession, String type) {
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_TYPE, type);
        getLastLocation(position, null);
        return position;
    }

    private void decodeCan(Position position, Parser parser) {
        Device device = getCacheManager() != null
                ? getCacheManager().getObject(Device.class, position.getDeviceId()) : null;
        boolean configured = device != null
                && (device.hasAttribute(ATTRIBUTE_FMS) || device.hasAttribute(ATTRIBUTE_J1708));
        boolean fms;
        boolean j1708;
        if (device == null) {
            fms = false;
            j1708 = false;
        } else if (configured) {
            fms = device.getBoolean(ATTRIBUTE_FMS);
            j1708 = device.getBoolean(ATTRIBUTE_J1708);
        } else if (useFms != null) {
            fms = useFms;
            j1708 = false;
        } else {
            fms = true;
            j1708 = false;
        }

        if ((fms || j1708) && device.getInteger(ATTRIBUTE_FUEL_SOURCE) % 16 != 1) {
            position.set(Position.KEY_FUEL_LEVEL, parser.nextHexInt());
        } else {
            parser.skip(1);
        }
        if (fms) {
            position.set(Position.KEY_COOLANT_TEMP, parser.nextHexInt() - 40);
        } else if (j1708) {
            position.set(Position.KEY_COOLANT_TEMP, (parser.nextHexInt() - 32) * 5 / 9);
        } else {
            parser.skip(1);
        }
        if (fms) {
            position.set(Position.KEY_ODOMETER, Math.round(Long.parseLong(parser.next(), 16) * 1000));
        } else if (j1708) {
            position.set(Position.KEY_ODOMETER, Math.round(Long.parseLong(parser.next(), 16) * 1.609344 / 10 * 1000));
        } else {
            parser.skip(1);
        }
        if (fms || j1708) {
            position.set(Position.KEY_RPM, parser.nextHexInt());
        } else {
            parser.skip(1);
        }
        int consumption = parser.nextHexInt();
        if (fms && consumption != 0) {
            position.set(Position.KEY_FUEL_CONSUMPTION, 51200 / consumption);
        }
        if (fms) {
            position.set(Position.KEY_FUEL_USED, Long.parseLong(parser.next(), 16) / 2.0);
        } else if (j1708) {
            position.set(Position.KEY_FUEL_USED, Long.parseLong(parser.next(), 16) / 8.0 * 3.78541178);
        } else {
            parser.skip(1);
        }
        position.set(Position.KEY_SATELLITES, parser.nextHexInt());
        int rssi = parser.nextHexInt();
        position.set(Position.KEY_RSSI, rssi);
        if (parser.hasNext() && fms) {
            parser.next();
            position.set("brakeCount", rssi);
        }
    }

}
