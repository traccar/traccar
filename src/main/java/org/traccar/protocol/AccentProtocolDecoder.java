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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class AccentProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccentProtocolDecoder.class);

    private static final String ATTRIBUTE_FMS = "useFms";
    private static final String ATTRIBUTE_J1708 = "accent.j1708";
    private static final String ATTRIBUTE_FUEL_SOURCE = "accent.fuelSrc";

    private Storage storage;

    public AccentProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Inject
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    private static final Pattern PATTERN_INFO = new PatternBuilder()
            .text("AA01")
            .expression("([^,]*)")              // firmware
            .text(", ICC:")
            .expression("([^,]*)")              // iccid
            .text(", IMEI:")
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

        if (sentence.contains("AA01MF2")) {
            DeviceSession deviceSession = uniqueId == null
                    ? getDeviceSession(channel, remoteAddress)
                    : getDeviceSession(channel, remoteAddress, uniqueId);
            if (deviceSession != null) {
                updateUseFms(deviceSession, !sentence.contains("AA01MF2_XS"));
            }
            return null;
        }

        Parser info = new Parser(PATTERN_INFO, sentence);
        if (info.matches()) {
            String firmware = info.next();
            String iccid = info.next();
            String imei = info.next();
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
            if (deviceSession != null) {
                updateIdentity(deviceSession, firmware, iccid, imei);
            }
            return null;
        }

        if (sentence.startsWith("AA00")) {
            if (uniqueId != null) {
                getDeviceSession(channel, remoteAddress, uniqueId);
            }
            return null;
        }

        Parser parser = new Parser(PATTERN_DATA, sentence);
        if (!parser.matches()) {
            return null;
        }

        sendAck(channel, remoteAddress);

        DeviceSession deviceSession = uniqueId == null
                ? getDeviceSession(channel, remoteAddress)
                : getDeviceSession(channel, remoteAddress, uniqueId);
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

        if (sentence.length() == 104 || sentence.length() == 106) {
            decodeCan(position, sentence);
            position.set(Position.KEY_SATELLITES, hex(sentence, 100, 102));
            position.set(Position.KEY_RSSI, hex(sentence, 102, 104));
        }

        return position;
    }

    private void updateUseFms(DeviceSession deviceSession, boolean useFms) {
        Device device = deviceFor(deviceSession);
        if (device == null || (device.hasAttribute(ATTRIBUTE_FMS) && device.getBoolean(ATTRIBUTE_FMS) == useFms)) {
            return;
        }
        device.set(ATTRIBUTE_FMS, useFms);
        saveAttributes(device);
    }

    private void updateIdentity(DeviceSession deviceSession, String firmware, String iccid, String imei) {
        Device device = deviceFor(deviceSession);
        if (device == null) {
            return;
        }
        boolean changed = setIfChanged(device, Position.KEY_VERSION_FW, firmware);
        changed |= setIfChanged(device, Position.KEY_ICCID, iccid);
        changed |= setIfChanged(device, "imei", imei);
        if (changed) {
            saveAttributes(device);
        }
    }

    private Device deviceFor(DeviceSession deviceSession) {
        if (getCacheManager() == null || storage == null) {
            return null;
        }
        return getCacheManager().getObject(Device.class, deviceSession.getDeviceId());
    }

    private static boolean setIfChanged(Device device, String key, String value) {
        if (value == null || value.isEmpty() || value.equals(device.getString(key))) {
            return false;
        }
        device.set(key, value);
        return true;
    }

    private void saveAttributes(Device device) {
        try {
            storage.updateObject(device, new Request(
                    new Columns.Include("attributes"),
                    new Condition.Equals("id", device.getId())));
        } catch (StorageException e) {
            LOGGER.warn("Update device attributes error", e);
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
        boolean fms = device.getBoolean(ATTRIBUTE_FMS);
        boolean j1708 = device.getBoolean(ATTRIBUTE_J1708);
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
