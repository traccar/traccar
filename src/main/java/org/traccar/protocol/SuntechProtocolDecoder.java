/*
 * Copyright 2013 - 2020 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
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
import java.util.TimeZone;

public class SuntechProtocolDecoder extends BaseProtocolDecoder {

    private String prefix;

    private int protocolType;
    private boolean hbm;
    private boolean includeAdc;
    private boolean includeRpm;
    private boolean includeTemp;

    public SuntechProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setProtocolType(int protocolType) {
        this.protocolType = protocolType;
    }

    public int getProtocolType(long deviceId) {
        return Context.getIdentityManager().lookupAttributeInteger(
                deviceId, getProtocolName() + ".protocolType", protocolType, false, true);
    }

    public void setHbm(boolean hbm) {
        this.hbm = hbm;
    }

    public boolean isHbm(long deviceId) {
        return Context.getIdentityManager().lookupAttributeBoolean(
                deviceId, getProtocolName() + ".hbm", hbm, false, true);
    }

    public void setIncludeAdc(boolean includeAdc) {
        this.includeAdc = includeAdc;
    }

    public boolean isIncludeAdc(long deviceId) {
        return Context.getIdentityManager().lookupAttributeBoolean(
                deviceId, getProtocolName() + ".includeAdc", includeAdc, false, true);
    }

    public void setIncludeRpm(boolean includeRpm) {
        this.includeRpm = includeRpm;
    }

    public boolean isIncludeRpm(long deviceId) {
        return Context.getIdentityManager().lookupAttributeBoolean(
                deviceId, getProtocolName() + ".includeRpm", includeRpm, false, true);
    }

    public void setIncludeTemp(boolean includeTemp) {
        this.includeTemp = includeTemp;
    }

    public boolean isIncludeTemp(long deviceId) {
        return Context.getIdentityManager().lookupAttributeBoolean(
                deviceId, getProtocolName() + ".includeTemp", includeTemp, false, true);
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
            position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
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
        switch (value) {
            case 1:
                return Position.ALARM_SOS;
            case 2:
                return Position.ALARM_PARKING;
            case 3:
                return Position.ALARM_POWER_CUT;
            case 5:
            case 6:
                return Position.ALARM_DOOR;
            case 7:
                return Position.ALARM_MOVEMENT;
            case 8:
                return Position.ALARM_SHOCK;
            default:
                return null;
        }
    }

    private String decodeAlert(int value) {
        switch (value) {
            case 1:
                return Position.ALARM_OVERSPEED;
            case 5:
                return Position.ALARM_GEOFENCE_EXIT;
            case 6:
                return Position.ALARM_GEOFENCE_ENTER;
            case 14:
                return Position.ALARM_LOW_BATTERY;
            case 15:
                return Position.ALARM_SHOCK;
            case 16:
                return Position.ALARM_ACCIDENT;
            case 40:
                return Position.ALARM_POWER_RESTORED;
            case 41:
                return Position.ALARM_POWER_CUT;
            case 46:
                return Position.ALARM_ACCELERATION;
            case 47:
                return Position.ALARM_BRAKING;
            case 50:
                return Position.ALARM_JAMMING;
            default:
                return null;
        }
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
        index += 1; // model

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

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(values[index++] + values[index++]));

        position.setLatitude(Double.parseDouble(values[index++]));
        position.setLongitude(Double.parseDouble(values[index++]));
        position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(values[index++])));
        position.setCourse(Double.parseDouble(values[index++]));

        position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));

        position.setValid(values[index++].equals("1"));

        return position;
    }

    private Position decode2356(
            Channel channel, SocketAddress remoteAddress, String protocol, String[] values) throws ParseException {
        int index = 0;

        String type = values[index++].substring(5);

        if (!type.equals("STT") && !type.equals("EMG") && !type.equals("EVT")
                && !type.equals("ALT") && !type.equals("UEX")) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[index++]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_TYPE, type);

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
            case "STT":
                position.set(Position.KEY_STATUS, Integer.parseInt(values[index++]));
                position.set(Position.KEY_INDEX, Integer.parseInt(values[index++]));
                break;
            case "EMG":
                position.set(Position.KEY_ALARM, decodeEmergency(Integer.parseInt(values[index++])));
                break;
            case "EVT":
                position.set(Position.KEY_EVENT, Integer.parseInt(values[index++]));
                break;
            case "ALT":
                position.set(Position.KEY_ALARM, decodeAlert(Integer.parseInt(values[index++])));
                break;
            case "UEX":
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
                    } else {
                        String[] pair = attribute.split("=");
                        if (pair.length >= 2) {
                            String value = pair[1].trim();
                            if (value.contains(".")) {
                                value = value.substring(0, value.indexOf('.'));
                            }
                            switch (pair[0].charAt(0)) {
                                case 't':
                                    position.set(Position.PREFIX_TEMP + pair[0].charAt(2), Integer.parseInt(value, 16));
                                    break;
                                case 'N':
                                    int fuel = Integer.parseInt(value, 16);
                                    totalFuel += fuel;
                                    position.set("fuel" + pair[0].charAt(2), fuel);
                                    break;
                                case 'Q':
                                    position.set("drivingQuality", Integer.parseInt(value, 16));
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    remaining -= attribute.length() + 1;
                }
                if (totalFuel > 0) {
                    position.set(Position.KEY_FUEL_LEVEL, totalFuel);
                }
                index += 1; // checksum
                break;
            default:
                break;
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
                if (values[index++].equals("1") && !driverUniqueId.isEmpty()) {
                    position.set(Position.KEY_DRIVER_UNIQUE_ID, driverUniqueId);
                }
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

        int mask = Integer.parseInt(values[index++], 16);

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
            position.set(Position.KEY_INPUT, Integer.parseInt(values[index++]));
        }

        if (BitUtil.check(mask, 18)) {
            position.set(Position.KEY_OUTPUT, Integer.parseInt(values[index++]));
        }

        if (type.equals("ALT")) {
            if (BitUtil.check(mask, 19)) {
                position.set("alertId", values[index++]);
            }
            if (BitUtil.check(mask, 20)) {
                position.set("alertModifier", values[index++]);
            }
            if (BitUtil.check(mask, 21)) {
                position.set("alertData", values[index++]);
            }
        } else {
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

        if (BitUtil.check(mask, 22)) {
            index += 1; // reserved
        }

        if (BitUtil.check(mask, 23)) {
            int assignMask = Integer.parseInt(values[index++], 16);
            for (int i = 0; i <= 30; i++) {
                if (BitUtil.check(assignMask, i)) {
                    position.set(Position.PREFIX_IO + (i + 1), values[index++]);
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
            long value = buf.readUnsignedInt();
            if (BitUtil.check(value, 31)) {
                value = -BitUtil.to(value, 31);
            }
            position.setLatitude(value / 1000000.0);
        }

        if (BitUtil.check(mask, 12)) {
            long value = buf.readUnsignedInt();
            if (BitUtil.check(value, 31)) {
                value = -BitUtil.to(value, 31);
            }
            position.setLongitude(value / 1000000.0);
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
                position.set(Position.KEY_ALARM, decodeAlert(alertId));
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

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getByte(buf.readerIndex() + 1) == 0) {

            return decodeBinary(channel, remoteAddress, buf);

        } else {

            String[] values = buf.toString(StandardCharsets.US_ASCII).split(";");
            prefix = values[0];

            if (prefix.length() < 5) {
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
