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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.net.SocketAddress;
import java.util.Calendar;
import java.util.Date;

public class AccentProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccentProtocolDecoder.class);

    public static final String ATTRIBUTE_FMS = "useFms";
    public static final String ATTRIBUTE_J1708 = "accent.j1708";
    public static final String ATTRIBUTE_FUEL_SOURCE = "accent.fuelSrc";
    public static final String ATTRIBUTE_FIX_DATE_1999 = "accent.fixDate1999";

    private static final long DATE_1999_LIMIT = 1199149200000L;
    private static final int DATE_1999_SHIFT_DAYS = 7 * 1024;

    private Storage storage;

    public AccentProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @jakarta.inject.Inject
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String line = ((String) msg).replace("\r", "").replace("\n", "").trim();
        if (line.isEmpty() || line.startsWith("$") || line.startsWith("T0") || line.startsWith("TX")) {
            return null;
        }
        if (line.contains("AAAA") && !line.startsWith("AA1") && !line.startsWith("AA2")) {
            line = line.replace("AAAA", "").trim();
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage("AA06\r\n", remoteAddress));
            }
            if (line.isEmpty() || line.startsWith("AA3")) {
                return null;
            }
        }
        if (line.startsWith("AAAA") || line.endsWith(" AAAA")) {
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage("AA06\r\n", remoteAddress));
            }
            return null;
        }
        if (line.contains("AA01MF2")) {
            boolean useFms = !line.contains("AA01MF2_XS");
            DeviceSession deviceSession = sessionFor(channel, remoteAddress, line);
            if (deviceSession != null) {
                updateUseFms(deviceSession, useFms);
            }
            return null;
        }
        if (line.startsWith("AA3")) {
            return null;
        }
        if (line.startsWith("AA1") || line.startsWith("AA2")) {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null || line.length() < 70) {
                return null;
            }
            int sendFlag = hex(line, 56, 58);
            if (sendFlag == 15) {
                return decodeTaxi(deviceSession, line, sendFlag);
            }
            return decodePosition(deviceSession, line, sendFlag);
        }

        int space = line.indexOf(' ');
        if (space <= 0) {
            return null;
        }
        String uniqueId = line.substring(0, space).replace("\t", "");
        String frame = line.substring(space + 1).trim();
        if (frame.startsWith("AA00")) {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, uniqueId);
            if (deviceSession != null) {
                String channelId = channel != null ? channel.id().asShortText() : "";
                LOGGER.info("[{}] connected id: {}", channelId, deviceSession.getUniqueId());
            }
            return null;
        }
        if (frame.startsWith("AA3") || (!frame.startsWith("AA1") && !frame.startsWith("AA2"))) {
            return null;
        }
        if (frame.length() < 70) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, uniqueId);
        if (deviceSession == null) {
            return null;
        }

        int sendFlag = hex(frame, 56, 58);
        if (sendFlag == 15) {
            return decodeTaxi(deviceSession, frame, sendFlag);
        }
        return decodePosition(deviceSession, frame, sendFlag);
    }

    private Position decodePosition(DeviceSession deviceSession, String frame, int sendFlag) {
        int lineLength = frame.length();
        int status = hex(frame, 42, 44);
        boolean valid = (status & 64) != 0;

        int seconds = hex(frame, 4, 10);
        int packedDate = hex(frame, 66, 70);
        int day = packedDate % 31 + 1;
        int month = packedDate % (31 * 12) / 31 + 1;
        int year = packedDate / (31 * 12);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000 + year, month - 1, day, seconds / 3600, seconds % 3600 / 60, seconds % 60);
        calendar.set(Calendar.MILLISECOND, 0);
        if (attribute(deviceSession.getDeviceId(), ATTRIBUTE_FIX_DATE_1999, true)
                && valid && calendar.getTimeInMillis() < DATE_1999_LIMIT) {
            calendar.add(Calendar.DAY_OF_YEAR, DATE_1999_SHIFT_DAYS);
        }

        double latitude = nmea(hex(frame, 10, 18));
        if ((status & 1) == 0) {
            latitude = -latitude;
        }
        double longitude = nmea(hex(frame, 18, 26));
        if ((status & 2) == 0) {
            longitude = -longitude;
        }

        int speedRaw = hex(frame, 26, 30);
        int speedKph = (int) Math.round(Integer.parseInt(speedRaw / 10 + "" + speedRaw % 10) * 0.1852);
        int power = hex(frame, 32, 34);

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(calendar.getTime());
        position.setValid(valid);
        position.setLatitude(latitude);
        position.setLongitude(longitude);
        position.setSpeed(UnitsConverter.knotsFromKph(speedKph));
        position.setCourse(hex(frame, 30, 32) * 16);
        position.set(Position.KEY_IGNITION, (status & 4) != 0);
        position.set(Position.PREFIX_IN + 1, (status & 8) != 0);
        position.set(Position.PREFIX_IN + 2, (status & 16) != 0);
        position.set("accelX", signed(hex(frame, 36, 38)));
        position.set("accelY", signed(hex(frame, 38, 40)));
        position.set("accelZ", signed(hex(frame, 40, 42)));
        int batteryTenths = power % 128;
        position.set(Position.KEY_CHARGE, power < 128);
        position.set(Position.KEY_BATTERY, batteryTenths / 10.0);
        position.set(Position.KEY_BATTERY_LEVEL, batteryLevel(batteryTenths));
        position.set(Position.KEY_FUEL_LEVEL, hex(frame, 34, 36));
        position.set(Position.PREFIX_TEMP + 1, temperature(frame.substring(44, 48)));
        position.set(Position.KEY_ODOMETER, metersFromKilometers(hex(frame, 48, 56)));
        position.set("sendFlag", sendFlag);
        position.set("addedInfo", (long) hex(frame, 58, 66));

        if (lineLength == 74) {
            position.set(Position.KEY_SATELLITES, hex(frame, 70, 72));
            position.set(Position.KEY_RSSI, hex(frame, 72, 74));
        } else if (lineLength == 104 || lineLength == 106) {
            decodeCan(position, frame, lineLength);
            position.set(Position.KEY_SATELLITES, hex(frame, 100, 102));
            position.set(Position.KEY_RSSI, hex(frame, 102, 104));
        }
        return position;
    }

    private void decodeCan(Position position, String frame, int lineLength) {
        boolean fms = attribute(position.getDeviceId(), ATTRIBUTE_FMS, false);
        boolean j1708 = attribute(position.getDeviceId(), ATTRIBUTE_J1708, false);
        if (!fms && !j1708) {
            return;
        }
        int fuelSource = attributeInt(position.getDeviceId(), ATTRIBUTE_FUEL_SOURCE, 0);
        int fuel = (fuelSource % 16) == 1 ? hex(frame, 34, 36) : hex(frame, 70, 72);
        position.set(Position.KEY_FUEL_LEVEL, fuel);
        long accumulated = Long.parseLong(frame.substring(74, 82), 16);
        if (fms) {
            position.set(Position.KEY_COOLANT_TEMP, hex(frame, 72, 74) - 40);
            position.set(Position.KEY_ODOMETER, metersFromKilometers(accumulated));
            position.set(Position.KEY_RPM, hex(frame, 84, 88));
            int rawConsumption = hex(frame, 88, 92);
            if (rawConsumption != 0) {
                position.set(Position.KEY_FUEL_CONSUMPTION, 51200 / rawConsumption);
            }
            position.set(Position.KEY_FUEL_USED, Long.parseLong(frame.substring(92, 100), 16) / 2.0);
            if (lineLength == 106) {
                position.set("brakeCount", hex(frame, 102, 104));
            }
        } else {
            position.set(Position.KEY_COOLANT_TEMP, (hex(frame, 72, 74) - 32) * 5 / 9);
            position.set(Position.KEY_ODOMETER, metersFromKilometers(accumulated * 1.609344 / 10));
            position.set(Position.KEY_RPM, hex(frame, 84, 88));
            position.set(Position.KEY_FUEL_USED, Long.parseLong(frame.substring(92, 100), 16) / 8.0 * 3.78541178);
        }
    }

    private Position decodeTaxi(DeviceSession deviceSession, String frame, int sendFlag) {
        int state = hex(frame, 34, 36);
        if (state == 0 || state > 3) {
            state = 0;
        }
        Date gpsTime = packedTimestamp(frame.substring(4, 10));

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(gpsTime != null ? gpsTime : new Date());
        position.setValid(false);
        position.setLatitude(0);
        position.setLongitude(0);
        position.set("sendFlag", sendFlag);
        position.set("tripId", hex(frame, 66, 70));
        position.set("tripState", state);
        position.set("tripPrice", hex(frame, 26, 30));
        position.set("tripTotalPrice", hex(frame, 44, 48));
        position.set(Position.KEY_ODOMETER, metersFromKilometers(hex(frame, 48, 56)));
        Date start = packedTimestamp(frame.substring(10, 18));
        Date end = packedTimestamp(frame.substring(18, 26));
        if (start != null) {
            position.set("tripStart", start.getTime());
        }
        if (end != null) {
            position.set("tripEnd", end.getTime());
        }
        return position;
    }

    private DeviceSession sessionFor(Channel channel, SocketAddress remoteAddress, String line) {
        int space = line.indexOf(' ');
        if (space > 0) {
            String uniqueId = line.substring(0, space).replace("\t", "");
            if (!uniqueId.startsWith("AA")) {
                return getDeviceSession(channel, remoteAddress, uniqueId);
            }
        }
        return getDeviceSession(channel, remoteAddress);
    }

    private void updateUseFms(DeviceSession deviceSession, boolean useFms) {
        if (getCacheManager() == null || storage == null) {
            return;
        }
        Device device = getCacheManager().getObject(Device.class, deviceSession.getDeviceId());
        if (device == null || (device.hasAttribute(ATTRIBUTE_FMS) && device.getBoolean(ATTRIBUTE_FMS) == useFms)) {
            return;
        }
        device.set(ATTRIBUTE_FMS, useFms);
        try {
            storage.updateObject(device, new Request(
                    new Columns.Include("attributes"),
                    new Condition.Equals("id", device.getId())));
            LOGGER.info("Device {} useFms={}", device.getUniqueId(), useFms);
        } catch (StorageException error) {
            LOGGER.warn("Failed to store useFms", error);
        }
    }

    private boolean attribute(long deviceId, String key, boolean defaultValue) {
        if (getCacheManager() == null) {
            return defaultValue;
        }
        Device device = getCacheManager().getObject(Device.class, deviceId);
        if (device == null || !device.hasAttribute(key)) {
            return defaultValue;
        }
        return device.getBoolean(key);
    }

    private int attributeInt(long deviceId, String key, int defaultValue) {
        if (getCacheManager() == null) {
            return defaultValue;
        }
        Device device = getCacheManager().getObject(Device.class, deviceId);
        if (device == null || !device.hasAttribute(key)) {
            return defaultValue;
        }
        return device.getInteger(key);
    }

    private static int batteryLevel(int voltageTenths) {
        int level = voltageTenths * 100 / 42;
        return Math.min(level, 100);
    }

    private static long metersFromKilometers(double kilometers) {
        return Math.round(kilometers * 1000);
    }

    private static int hex(String frame, int from, int to) {
        return Integer.parseInt(frame.substring(from, to), 16);
    }

    private static int signed(int value) {
        return value > 128 ? value - 256 : value;
    }

    private static double nmea(int raw) {
        double value = raw / 10000 + (raw % 10000) / 10000.0;
        int degrees = (int) value / 100;
        return degrees + (value - degrees * 100) / 60;
    }

    private static double temperature(String rawHex) {
        float temp = Integer.parseInt(rawHex, 16);
        if (temp > 10000 && temp < 10600) {
            float rawTemp = temp * 100 / 16;
            temp = ((rawTemp - 65536) / 100) * 16;
        }
        if (Math.floor(temp / 256) != 128) {
            if (temp > 32768) {
                temp = temp - 65536;
            }
        } else {
            temp = 0;
        }
        return temp / 16.0;
    }

    private static Date packedTimestamp(String hexValue) {
        long start = Long.parseLong(hexValue, 16);
        long year = start / (60L * 24 * 31 * 12);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        if (year < 99) {
            start -= year * 60 * 24 * 31 * 12;
            long month = start / (60 * 24 * 31);
            start -= month * 60 * 24 * 31;
            long day = start / (60 * 24);
            start -= day * 60 * 24;
            long hour = start / 60;
            long minute = start - hour * 60;
            calendar.set(2000 + (int) year, (int) month, (int) day + 1, (int) hour, (int) minute, 0);
            return calendar.getTime();
        }
        start -= 99L * 60 * 24 * 31 * 12;
        calendar.set(2008, Calendar.JANUARY, 1, 0, 0, 0);
        return new Date(calendar.getTimeInMillis() + start * 60 * 1000);
    }

}
