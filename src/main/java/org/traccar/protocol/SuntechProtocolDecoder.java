/*
 * Copyright 2013 - 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.config.Keys;
import org.traccar.helper.BufferUtil;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class SuntechProtocolDecoder extends BaseProtocolDecoder {

    private boolean universal;
    private String prefix;

    private int protocolType;
    private boolean hbm;
    private boolean includeAdc;
    private boolean includeRpm;
    private boolean includeTemp;

    private ByteBuf crash;

    public SuntechProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public boolean getUniversal() {
        return universal;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setProtocolType(int protocolType) {
        this.protocolType = protocolType;
    }

    public int getProtocolType(long deviceId) {
        Integer value = AttributeUtil.lookup(getCacheManager(), Keys.PROTOCOL_TYPE, deviceId);
        return value != null ? value : protocolType;
    }

    public void setHbm(boolean hbm) {
        this.hbm = hbm;
    }

    public boolean isHbm(long deviceId) {
        Boolean value = AttributeUtil.lookup(getCacheManager(), Keys.PROTOCOL_HBM, deviceId);
        return value != null ? value : hbm;
    }

    public void setIncludeAdc(boolean includeAdc) {
        this.includeAdc = includeAdc;
    }

    public boolean isIncludeAdc(long deviceId) {
        Boolean value = AttributeUtil.lookup(
                getCacheManager(), Keys.PROTOCOL_INCLUDE_ADC.withPrefix(getProtocolName()), deviceId);
        return value != null ? value : includeAdc;
    }

    public void setIncludeRpm(boolean includeRpm) {
        this.includeRpm = includeRpm;
    }

    public boolean isIncludeRpm(long deviceId) {
        Boolean value = AttributeUtil.lookup(
                getCacheManager(), Keys.PROTOCOL_INCLUDE_RPM.withPrefix(getProtocolName()), deviceId);
        return value != null ? value : includeRpm;
    }

    public void setIncludeTemp(boolean includeTemp) {
        this.includeTemp = includeTemp;
    }

    public boolean isIncludeTemp(long deviceId) {
        Boolean value = AttributeUtil.lookup(
                getCacheManager(), Keys.PROTOCOL_INCLUDE_TEMPERATURE.withPrefix(getProtocolName()), deviceId);
        return value != null ? value : includeTemp;
    }

    private Position decode9(
            Channel channel, SocketAddress remoteAddress, String[] values) throws ParseException {
        int index = 1;

        String type = values[index++];

        if (!type.equals("Location") && !type.equals("Emergency") && !type.equals("Alert")) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[index++]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (type.equals("Emergency") || type.equals("Alert")) {
            position.addAlarm(Position.ALARM_GENERAL);
        }

        if (!type.equals("Alert") || getProtocolType(deviceSession.getDeviceId()) == 0) {
            position.set(Position.KEY_VERSION_FW, values[index++]);
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(values[index++] + values[index++]));

        if (getProtocolType(deviceSession.getDeviceId()) == 1) {
            index += 1; // cell
        }

        position.setLatitude(Double.parseDouble(values[index++]));
        position.setLongitude(Double.parseDouble(values[index++]));
        position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(values[index++])));
        position.setCourse(Double.parseDouble(values[index++]));

        position.setValid(values[index++].equals("1"));

        if (getProtocolType(deviceSession.getDeviceId()) == 1) {
            position.set(Position.KEY_ODOMETER, Integer.parseInt(values[index++]));
        }

        return position;
    }

    private String decodeEmergency(int value) {
        return switch (value) {
            case 1 -> Position.ALARM_SOS;
            case 2 -> Position.ALARM_PARKING;
            case 3 -> Position.ALARM_POWER_CUT;
            case 5, 6 -> Position.ALARM_DOOR;
            case 7 -> Position.ALARM_MOVEMENT;
            case 8 -> Position.ALARM_VIBRATION;
            default -> null;
        };
    }

    private String decodeAlert(int value) {
        return switch (value) {
            case 1 -> Position.ALARM_OVERSPEED;
            case 5 -> Position.ALARM_GEOFENCE_EXIT;
            case 6 -> Position.ALARM_GEOFENCE_ENTER;
            case 14 -> Position.ALARM_LOW_BATTERY;
            case 15 -> Position.ALARM_VIBRATION;
            case 16 -> Position.ALARM_ACCIDENT;
            case 40 -> Position.ALARM_POWER_RESTORED;
            case 41 -> Position.ALARM_POWER_CUT;
            case 42 -> Position.ALARM_SOS;
            case 46 -> Position.ALARM_ACCELERATION;
            case 47 -> Position.ALARM_BRAKING;
            case 50 -> Position.ALARM_JAMMING;
            case 132 -> Position.ALARM_DOOR;
            default -> null;
        };
    }
    private Position decode4(
            Channel channel, SocketAddress remoteAddress, String[] values) throws ParseException {
        int index = 0;

        String type = values[index++].substring(5);

        if (!type.equals("STT") && !type.equals("ALT")) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[index++]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_TYPE, type);

        position.set(Position.KEY_VERSION_FW, values[index++]);
        int model = Integer.parseInt(values[index++]);
        if (model == 41) {
            index += 1; // variant
        }

        Network network = new Network();

        for (int i = 0; i < 7; i++) {
            int cid = Integer.parseInt(values[index++]);
            int mcc = Integer.parseInt(values[index++]);
            int mnc = Integer.parseInt(values[index++]);
            int lac, rssi;
            if (i == 0) {
                rssi = Integer.parseInt(values[index++]);
                lac = Integer.parseInt(values[index++]);
            } else {
                lac = Integer.parseInt(values[index++]);
                rssi = Integer.parseInt(values[index++]);
            }
            index += 1; // timing advance
            if (cid > 0) {
                network.addCellTower(CellTower.from(mcc, mnc, lac, cid, rssi));
            }
        }

        position.setNetwork(network);

        position.set(Position.KEY_BATTERY, Double.parseDouble(values[index++]));
        position.set(Position.KEY_ARCHIVE, values[index++].equals("0") ? true : null);
        position.set(Position.KEY_INDEX, Integer.parseInt(values[index++]));
        position.set(Position.KEY_STATUS, Integer.parseInt(values[index++]));

        if (values[index].length() == 3) {
            index += 1; // collaborative network
        }

        if (model == 41) {
            index += 1; // collaborative network
            index += 1; // temperature
            position.set(Position.KEY_MOTION, Integer.parseInt(values[index++]) == 2);
        }

        if (values[index].isEmpty()) {

            getLastLocation(position, null);

        } else {

            DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            position.setTime(dateFormat.parse(values[index++] + values[index++]));

            position.setLatitude(Double.parseDouble(values[index++]));
            position.setLongitude(Double.parseDouble(values[index++]));
            position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(values[index++])));
            position.setCourse(Double.parseDouble(values[index++]));

            position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));

            position.setValid(values[index++].equals("1"));

        }

        return position;
    }

    private int decodeSerialData(Position position, String[] values, int index) {

        int remaining = Integer.parseInt(values[index++]);
        double totalFuel = 0;
        while (remaining > 0) {
            String attribute = values[index++];
            if (attribute.startsWith("CabAVL")) {
                String[] data = attribute.split(",");
                double fuel1 = Double.parseDouble(data[2]);
                if (fuel1 > 0) {
                    totalFuel += fuel1;
                    position.set("fuel1", fuel1);
                }
                double fuel2 = Double.parseDouble(data[3]);
                if (fuel2 > 0) {
                    totalFuel += fuel2;
                    position.set("fuel2", fuel2);
                }
            } else if (attribute.startsWith("GTSL")) {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, attribute.split("\\|")[4]);
            } else if (attribute.contains("=")) {
                String[] pair = attribute.split("=");
                if (pair.length >= 2) {
                    String value = pair[1].trim();
                    if (value.contains(".")) {
                        value = value.substring(0, value.indexOf('.'));
                    }
                    switch (pair[0].charAt(0)) {
                        case 't' -> position.set(Position.PREFIX_TEMP + pair[0].charAt(2), Integer.parseInt(value, 16));
                        case 'N' -> {
                            int fuel = Integer.parseInt(value, 16);
                            totalFuel += fuel;
                            position.set("fuel" + pair[0].charAt(2), fuel);
                        }
                        case 'Q' -> position.set("drivingQuality", Integer.parseInt(value, 16));
                    }
                }
            } else {
                position.set("serial", attribute.trim());
            }
            remaining -= attribute.length() + 1;
        }
        if (totalFuel > 0) {
            position.set(Position.KEY_FUEL_LEVEL, totalFuel);
        }
        return index + 1; // checksum
    }

    private Position decode2356(
            Channel channel, SocketAddress remoteAddress, String protocol, String[] values) throws ParseException {
        int index = 0;

        String type = values[index++].substring(5);

        boolean result = values[index].equals("Res");
        if (result) {
            index += 1;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[index++]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_TYPE, type);

        if (result) {
            position.set(Position.KEY_RESULT, String.join(";", Arrays.copyOfRange(values, index, values.length)));
            return position;
        }

        if (!type.equals("STT") && !type.equals("EMG") && !type.equals("EVT")
                && !type.equals("ALT") && !type.equals("UEX")) {
            return null;
        }

        if (protocol.startsWith("ST3") || protocol.equals("ST500") || protocol.equals("ST600")) {
            index += 1; // model
        }

        position.set(Position.KEY_VERSION_FW, values[index++]);

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(values[index++] + values[index++]));

        if (!protocol.equals("ST500")) {
            long cid = Long.parseLong(values[index++], 16);
            if (protocol.equals("ST600")) {
                position.setNetwork(new Network(CellTower.from(
                        Integer.parseInt(values[index++]), Integer.parseInt(values[index++]),
                        Integer.parseInt(values[index++], 16), cid, Integer.parseInt(values[index++]))));
            }
        }

        position.setLatitude(Double.parseDouble(values[index++]));
        position.setLongitude(Double.parseDouble(values[index++]));
        position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(values[index++])));
        position.setCourse(Double.parseDouble(values[index++]));

        position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));

        position.setValid(values[index++].equals("1"));

        position.set(Position.KEY_ODOMETER, Integer.parseInt(values[index++]));
        position.set(Position.KEY_POWER, Double.parseDouble(values[index++]));

        String io = values[index++];
        if (io.length() >= 6) {
            position.set(Position.KEY_IGNITION, io.charAt(0) == '1');
            position.set(Position.PREFIX_IN + 1, io.charAt(1) == '1');
            position.set(Position.PREFIX_IN + 2, io.charAt(2) == '1');
            position.set(Position.PREFIX_IN + 3, io.charAt(3) == '1');
            position.set(Position.PREFIX_OUT + 1, io.charAt(4) == '1');
            position.set(Position.PREFIX_OUT + 2, io.charAt(5) == '1');
        }

        switch (type) {
            case "STT" -> {
                position.set(Position.KEY_STATUS, Integer.parseInt(values[index++]));
                position.set(Position.KEY_INDEX, Integer.parseInt(values[index++]));
            }
            case "EMG" -> position.addAlarm(decodeEmergency(Integer.parseInt(values[index++])));
            case "EVT" -> position.set(Position.KEY_EVENT, Integer.parseInt(values[index++]));
            case "ALT" -> position.addAlarm(decodeAlert(Integer.parseInt(values[index++])));
            case "UEX" -> index = decodeSerialData(position, values, index);
        }

        if (isHbm(deviceSession.getDeviceId())) {

            if (index < values.length) {
                position.set(Position.KEY_HOURS, UnitsConverter.msFromMinutes(Integer.parseInt(values[index++])));
            }

            if (index < values.length) {
                position.set(Position.KEY_BATTERY, Double.parseDouble(values[index++]));
            }

            if (index < values.length && values[index++].equals("0")) {
                position.set(Position.KEY_ARCHIVE, true);
            }

            if (isIncludeAdc(deviceSession.getDeviceId())) {
                for (int i = 1; i <= 3; i++) {
                    if (index < values.length && !values[index++].isEmpty()) {
                        position.set(Position.PREFIX_ADC + i, Double.parseDouble(values[index - 1]));
                    }
                }
            }

            if (isIncludeRpm(deviceSession.getDeviceId()) && index < values.length) {
                position.set(Position.KEY_RPM, Integer.parseInt(values[index++]));
            }

            if (values.length - index >= 2) {
                String driverUniqueId = values[index++];
                if (!driverUniqueId.isEmpty()) {
                    position.set(Position.KEY_DRIVER_UNIQUE_ID, driverUniqueId);
                }
                index += 1; // registered
            }

            if (isIncludeTemp(deviceSession.getDeviceId())) {
                for (int i = 1; i <= 3; i++) {
                    String temperature = values[index++];
                    String value = temperature.substring(temperature.indexOf(':') + 1);
                    if (!value.isEmpty()) {
                        position.set(Position.PREFIX_TEMP + i, Double.parseDouble(value));
                    }
                }

            }

        }

        return position;
    }

    private Position decodeUniversal(
            Channel channel, SocketAddress remoteAddress, String[] values) throws ParseException {
        int index = 0;

        String type = values[index++];

        if (!type.equals("STT") && !type.equals("ALT") && !type.equals("BLE") && !type.equals("RES")
                && !type.equals("UEX")) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[index++]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_TYPE, type);

        if (type.equals("RES")) {
            getLastLocation(position, null);
            position.set(
                    Position.KEY_RESULT,
                    Arrays.stream(values, index, values.length).collect(Collectors.joining(";")));
            return position;
        }

        int mask;
        if (type.equals("BLE")) {
            mask = 0b1100000110110;
        } else {
            mask = Integer.parseInt(values[index++], 16);
        }

        if (BitUtil.check(mask, 1)) {
            index += 1; // model
        }

        if (BitUtil.check(mask, 2)) {
            position.set(Position.KEY_VERSION_FW, values[index++]);
        }

        if (BitUtil.check(mask, 3) && values[index++].equals("0")) {
            position.set(Position.KEY_ARCHIVE, true);
        }

        if (BitUtil.check(mask, 4) && BitUtil.check(mask, 5)) {
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            position.setTime(dateFormat.parse(values[index++] + values[index++]));
        }

        CellTower cellTower = new CellTower();
        if (BitUtil.check(mask, 6)) {
            cellTower.setCellId(Long.parseLong(values[index++], 16));
        }
        if (BitUtil.check(mask, 7)) {
            cellTower.setMobileCountryCode(Integer.parseInt(values[index++]));
        }
        if (BitUtil.check(mask, 8)) {
            cellTower.setMobileNetworkCode(Integer.parseInt(values[index++]));
        }
        if (BitUtil.check(mask, 9)) {
            cellTower.setLocationAreaCode(Integer.parseInt(values[index++], 16));
        }
        if (cellTower.getCellId() != null) {
            position.setNetwork(new Network(cellTower));
        }

        if (BitUtil.check(mask, 10)) {
            position.set(Position.KEY_RSSI, Integer.parseInt(values[index++]));
        }

        if (BitUtil.check(mask, 11)) {
            position.setLatitude(Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 12)) {
            position.setLongitude(Double.parseDouble(values[index++]));
        }

        if (type.equals("BLE")) {

            position.setValid(true);

            int count = Integer.parseInt(values[index++]);

            for (int i = 1; i <= count; i++) {
                position.set("tag" + i + "Rssi", Integer.parseInt(values[index++]));
                index += 1; // rssi min
                index += 1; // rssi max
                position.set("tag" + i + "Id", values[index++]);
                position.set("tag" + i + "Samples", Integer.parseInt(values[index++]));
                position.set("tag" + i + "Major", Integer.parseInt(values[index++]));
                position.set("tag" + i + "Minor", Integer.parseInt(values[index++]));
            }

        } else {

            if (BitUtil.check(mask, 13)) {
                position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(values[index++])));
            }

            if (BitUtil.check(mask, 14)) {
                position.setCourse(Double.parseDouble(values[index++]));
            }

            if (BitUtil.check(mask, 15)) {
                position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));
            }

            if (BitUtil.check(mask, 16)) {
                position.setValid(values[index++].equals("1"));
            }

            if (BitUtil.check(mask, 17)) {
                int input = Integer.parseInt(values[index++]);
                position.set(Position.KEY_IGNITION, BitUtil.check(input, 0));
                position.set(Position.KEY_INPUT, input);
            }

            if (BitUtil.check(mask, 18)) {
                position.set(Position.KEY_OUTPUT, Integer.parseInt(values[index++]));
            }

            switch (type) {
                case "ALT" -> {
                    if (BitUtil.check(mask, 19)) {
                        int alertId = Integer.parseInt(values[index++]);
                        position.addAlarm(decodeAlert(alertId));
                    }
                    if (BitUtil.check(mask, 20)) {
                        position.set("alertModifier", values[index++]);
                    }
                    if (BitUtil.check(mask, 21)) {
                        position.set("alertData", values[index++]);
                    }
                }
                case "UEX" -> index = decodeSerialData(position, values, index);
                default -> {
                    if (BitUtil.check(mask, 19)) {
                        position.set("mode", Integer.parseInt(values[index++]));
                    }
                    if (BitUtil.check(mask, 20)) {
                        position.set("reason", Integer.parseInt(values[index++]));
                    }
                    if (BitUtil.check(mask, 21)) {
                        position.set(Position.KEY_INDEX, Integer.parseInt(values[index++]));
                    }
                }
            }

            if (BitUtil.check(mask, 22)) {
                index += 1; // reserved
            }

            if (BitUtil.check(mask, 23) && !type.equals("UEX")) {
                int assignMask = Integer.parseInt(values[index++], 16);
                for (int i = 0; i <= 30; i++) {
                    if (BitUtil.check(assignMask, i)) {
                        position.set(Position.PREFIX_IO + (i + 1), values[index++]);
                    }
                }
            }

        }

        return position;
    }

    private Position decodeBinary(
            Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        int type = buf.readUnsignedByte();
        buf.readUnsignedShort(); // length

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, ByteBufUtil.hexDump(buf.readSlice(5)));
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        int mask = buf.readUnsignedMedium();

        if (BitUtil.check(mask, 1)) {
            buf.readUnsignedByte(); // model
        }

        if (BitUtil.check(mask, 2)) {
            position.set(Position.KEY_VERSION_FW, String.format("%d.%d.%d",
                    buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()));
        }

        if (BitUtil.check(mask, 3) && buf.readUnsignedByte() == 0) {
            position.set(Position.KEY_ARCHIVE, true);
        }

        if (BitUtil.check(mask, 4) && BitUtil.check(mask, 5)) {
            position.setTime(new DateBuilder()
                    .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .getDate());
        }

        if (BitUtil.check(mask, 6)) {
            buf.readUnsignedInt(); // cell
        }

        if (BitUtil.check(mask, 7)) {
            buf.readUnsignedShort(); // mcc
        }

        if (BitUtil.check(mask, 8)) {
            buf.readUnsignedShort(); // mnc
        }

        if (BitUtil.check(mask, 9)) {
            buf.readUnsignedShort(); // lac
        }

        if (BitUtil.check(mask, 10)) {
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        }

        if (BitUtil.check(mask, 11)) {
            position.setLatitude(BufferUtil.readSignedMagnitudeInt(buf) / 1000000.0);
        }

        if (BitUtil.check(mask, 12)) {
            position.setLongitude(BufferUtil.readSignedMagnitudeInt(buf) / 1000000.0);
        }

        if (BitUtil.check(mask, 13)) {
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort() / 100.0));
        }

        if (BitUtil.check(mask, 14)) {
            position.setCourse(buf.readUnsignedShort() / 100.0);
        }

        if (BitUtil.check(mask, 15)) {
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        }

        if (BitUtil.check(mask, 16)) {
            position.setValid(buf.readUnsignedByte() > 0);
        }

        if (BitUtil.check(mask, 17)) {
            int input = buf.readUnsignedByte();
            position.set(Position.KEY_IGNITION, BitUtil.check(input, 0));
            position.set(Position.KEY_INPUT, input);
        }

        if (BitUtil.check(mask, 18)) {
            position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
        }

        int alertId = 0;
        if (BitUtil.check(mask, 19)) {
            alertId = buf.readUnsignedByte();
            if (type == 0x82) {
                position.addAlarm(decodeAlert(alertId));
            }
        }

        if (BitUtil.check(mask, 20)) {
            buf.readUnsignedShort(); // alert modifier
        }

        if (BitUtil.check(mask, 21) && alertId == 59) {
            position.set(Position.KEY_DRIVER_UNIQUE_ID, ByteBufUtil.hexDump(buf.readSlice(8)));
        }

        return position;
    }

    private Position decodeTravelReport(Channel channel, SocketAddress remoteAddress, String[] values) {
        int index = 1;

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[index++]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        position.set(Position.KEY_DRIVER_UNIQUE_ID, values[values.length - 1]);

        return position;
    }

    private Collection<Position> decodeCrashReport(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        if (buf.getByte(buf.readerIndex() + 3) != ';') {
            return null;
        }

        String[] values = buf.readCharSequence(23, StandardCharsets.US_ASCII).toString().split(";");

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[1]);
        if (deviceSession == null) {
            return null;
        }

        int currentIndex = Integer.parseInt(values[2]);
        int totalIndex = Integer.parseInt(values[3]);

        if (crash == null) {
            crash = Unpooled.buffer();
        }

        crash.writeBytes(buf.readSlice(buf.readableBytes() - 3));

        if (currentIndex == totalIndex) {

            LinkedList<Position> positions = new LinkedList<>();

            Date crashTime = new DateBuilder()
                    .setDate(crash.readUnsignedByte(), crash.readUnsignedByte(), crash.readUnsignedByte())
                    .setTime(crash.readUnsignedByte(), crash.readUnsignedByte(), crash.readUnsignedByte())
                    .getDate();

            List<Date> times = Arrays.asList(
                    new Date(crashTime.getTime() - 3000),
                    new Date(crashTime.getTime() - 2000),
                    new Date(crashTime.getTime() - 1000),
                    new Date(crashTime.getTime() + 1000));

            for (Date time : times) {

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                position.setValid(true);
                position.setTime(time);
                position.setLatitude(crash.readIntLE() * 0.0000001);
                position.setLongitude(crash.readIntLE() * 0.0000001);
                position.setSpeed(UnitsConverter.knotsFromKph(crash.readUnsignedShort() * 0.01));
                position.setCourse(crash.readUnsignedShort() * 0.01);

                StringBuilder value = new StringBuilder("[");
                for (int i = 0; i < 100; i++) {
                    if (value.length() > 1) {
                        value.append(",");
                    }
                    value.append("[");
                    value.append(crash.readShortLE());
                    value.append(",");
                    value.append(crash.readShortLE());
                    value.append(",");
                    value.append(crash.readShortLE());
                    value.append("]");
                }
                value.append("]");

                position.set(Position.KEY_G_SENSOR, value.toString());

                positions.add(position);

            }

            crash.release();
            crash = null;

            return positions;

        } else {

            return null;

        }

    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getByte(buf.readerIndex() + 1) == 0) {

            universal = true;
            return decodeBinary(channel, remoteAddress, buf);

        } else {

            String[] values = buf.toString(StandardCharsets.US_ASCII).split(";", -1);
            prefix = values[0];

            if (prefix.equals("CRR")) {
                return decodeCrashReport(channel, remoteAddress, buf);
            } else if (prefix.length() < 5) {
                universal = true;
                return decodeUniversal(channel, remoteAddress, values);
            } else if (prefix.endsWith("HTE")) {
                return decodeTravelReport(channel, remoteAddress, values);
            } else if (prefix.startsWith("ST9")) {
                return decode9(channel, remoteAddress, values);
            } else if (prefix.startsWith("ST4")) {
                return decode4(channel, remoteAddress, values);
            } else {
                return decode2356(channel, remoteAddress, prefix.substring(0, 5), values);
            }
        }
    }

}
