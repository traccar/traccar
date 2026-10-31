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

    private String deviceId;
    private String firmware;
    private String iccid;
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

        int space = sentence.indexOf(' ');
        if (space > 0 && !sentence.startsWith("AA")) {
            deviceId = sentence.substring(0, space);
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
            firmware = info.next();
            iccid = info.next();
            imei = info.next();
            if (deviceId == null) {
                deviceId = imei;
            }
            if (getDeviceSession(channel, remoteAddress, deviceId) == null) {
                deviceId = null;
                firmware = null;
                iccid = null;
                imei = null;
            }
            return null;
        }

        if (sentence.startsWith("AA00")) {
            String icc = field(sentence, "ICC:");
            String modem = field(sentence, "IMEI:");
            if (icc != null) {
                iccid = icc;
            }
            if (modem != null) {
                imei = modem;
            }
            if (deviceId == null) {
                deviceId = modem;
            }
            if (deviceId != null && getDeviceSession(channel, remoteAddress, deviceId) == null) {
                deviceId = null;
                iccid = null;
                imei = null;
            }
            return null;
        }

        Parser parser = new Parser(PATTERN_DATA, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, deviceId);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        int seconds = parser.nextHexInt();
        long latitude = Long.parseLong(parser.next(), 16);
        long longitude = Long.parseLong(parser.next(), 16);
        int speed = parser.nextHexInt();
        int course = parser.nextHexInt();
        int power = parser.nextHexInt();
        int fuel = parser.nextHexInt();
        int accelX = parser.nextHexInt();
        int accelY = parser.nextHexInt();
        int accelZ = parser.nextHexInt();
        int flags = parser.nextHexInt();
        int temperature = parser.nextHexInt();
        long odometer = Long.parseLong(parser.next(), 16);
        int event = parser.nextHexInt();
        long addedInfo = Long.parseLong(parser.next(), 16);
        int packedDate = parser.nextHexInt();

        position.setTime(new DateBuilder()
                .setDate(packedDate / (31 * 12), packedDate % (31 * 12) / 31 + 1, packedDate % 31 + 1)
                .setTime(seconds / 3600, seconds % 3600 / 60, seconds % 60)
                .getDate());
        position.setValid(BitUtil.check(flags, 6));
        position.setLatitude(coordinate(latitude, BitUtil.check(flags, 0)));
        position.setLongitude(coordinate(longitude, BitUtil.check(flags, 1)));
        position.setSpeed(UnitsConverter.knotsFromKph(speed / 10.0));
        position.setCourse(course);

        position.set(Position.KEY_BATTERY, BitUtil.to(power, 7) / 10.0);
        position.set(Position.KEY_CHARGE, !BitUtil.check(power, 7));
        position.set(Position.KEY_FUEL_LEVEL, fuel);
        position.set("x", (int) (byte) accelX);
        position.set("y", (int) (byte) accelY);
        position.set("z", (int) (byte) accelZ);
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
        position.set(Position.KEY_ODOMETER, odometer * 1000);
        position.set(Position.KEY_EVENT, event);
        position.set("addedInfo", addedInfo);

        applyIdentity(position);

        if (sentence.length() == 104 || sentence.length() == 106) {
            decodeCan(position, sentence);
            position.set(Position.KEY_SATELLITES, hex(sentence, 100, 102));
            position.set(Position.KEY_RSSI, hex(sentence, 102, 104));
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

    private void applyIdentity(Position position) {
        if (firmware == null && iccid == null) {
            return;
        }
        if (firmware != null) {
            position.set(Position.KEY_VERSION_FW, firmware);
            firmware = null;
        }
        if (iccid != null) {
            position.set(Position.KEY_ICCID, iccid);
            iccid = null;
        }
        if (imei != null) {
            position.set(KEY_IMEI, imei);
        }
    }

    private void decodeCan(Position position, String frame) {
        if (getCacheManager() == null) {
            return;
        }
        Device device = getCacheManager().getObject(Device.class, position.getDeviceId());
        if (device == null) {
            return;
        }
        boolean configured = device.hasAttribute(ATTRIBUTE_FMS) || device.hasAttribute(ATTRIBUTE_J1708);
        boolean fms;
        boolean j1708;
        if (configured) {
            fms = device.getBoolean(ATTRIBUTE_FMS);
            j1708 = device.getBoolean(ATTRIBUTE_J1708);
        } else if (useFms != null) {
            fms = useFms;
            j1708 = false;
        } else {
            fms = true;
            j1708 = false;
        }
        if (!fms && !j1708) {
            return;
        }

        int fuel = device.getInteger(ATTRIBUTE_FUEL_SOURCE) % 16 == 1
                ? hex(frame, 34, 36) : hex(frame, 70, 72);
        long accumulated = Long.parseLong(frame.substring(74, 82), 16);
        long fuelUsed = Long.parseLong(frame.substring(92, 100), 16);
        position.set(Position.KEY_FUEL_LEVEL, fuel);
        position.set(Position.KEY_RPM, hex(frame, 84, 88));

        if (fms) {
            position.set(Position.KEY_COOLANT_TEMP, hex(frame, 72, 74) - 40);
            position.set(Position.KEY_ODOMETER, Math.round(accumulated * 1000));
            int consumption = hex(frame, 88, 92);
            if (consumption != 0) {
                position.set(Position.KEY_FUEL_CONSUMPTION, 51200 / consumption);
            }
            position.set(Position.KEY_FUEL_USED, fuelUsed / 2.0);
            if (frame.length() == 106) {
                position.set("brakeCount", hex(frame, 102, 104));
            }
        } else {
            position.set(Position.KEY_COOLANT_TEMP, (hex(frame, 72, 74) - 32) * 5 / 9);
            position.set(Position.KEY_ODOMETER, Math.round(accumulated * 1.609344 / 10 * 1000));
            position.set(Position.KEY_FUEL_USED, fuelUsed / 8.0 * 3.78541178);
        }
    }

    private static int hex(String frame, int from, int to) {
        return Integer.parseInt(frame.substring(from, to), 16);
    }

}
