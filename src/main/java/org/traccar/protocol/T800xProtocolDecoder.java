/*
 * Copyright 2015 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class T800xProtocolDecoder extends BaseProtocolDecoder {

    private short header = DEFAULT_HEADER;

    public short getHeader() {
        return header;
    }

    public T800xProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final short DEFAULT_HEADER = 0x2323;

    public static final int MSG_LOGIN = 0x01;
    public static final int MSG_GPS = 0x02;
    public static final int MSG_HEARTBEAT = 0x03;
    public static final int MSG_ALARM = 0x04;
    public static final int MSG_NETWORK = 0x05; // 0x2727
    public static final int MSG_DRIVER_BEHAVIOR_1 = 0x05; // 0x2626
    public static final int MSG_DRIVER_BEHAVIOR_2 = 0x06; // 0x2626
    public static final int MSG_BLE = 0x10;
    public static final int MSG_NETWORK_2 = 0x11;
    public static final int MSG_GPS_2 = 0x13;
    public static final int MSG_ALARM_2 = 0x14;
    public static final int MSG_COMMAND = 0x81;

    private void sendResponse(Channel channel, short header, int type, int index, ByteBuf imei, int alarm) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer(alarm > 0 ? 16 : 15);
            response.writeShort(header);
            response.writeByte(type);
            response.writeShort(response.capacity()); // length
            response.writeShort(index);
            response.writeBytes(imei);
            if (alarm > 0) {
                response.writeByte(alarm);
            }
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    private String decodeAlarm1(int value) {
        switch (value) {
            case 1:
                return Position.ALARM_POWER_CUT;
            case 2:
                return Position.ALARM_LOW_BATTERY;
            case 3:
                return Position.ALARM_SOS;
            case 4:
                return Position.ALARM_OVERSPEED;
            case 5:
                return Position.ALARM_GEOFENCE_ENTER;
            case 6:
                return Position.ALARM_GEOFENCE_EXIT;
            case 7:
                return Position.ALARM_TOW;
            case 8:
            case 10:
                return Position.ALARM_VIBRATION;
            case 21:
                return Position.ALARM_JAMMING;
            case 23:
                return Position.ALARM_POWER_RESTORED;
            case 24:
                return Position.ALARM_LOW_POWER;
            default:
                return null;
        }
    }

    private String decodeAlarm2(int value) {
        switch (value) {
            case 1:
            case 4:
                return Position.ALARM_REMOVING;
            case 2:
                return Position.ALARM_TAMPERING;
            case 3:
                return Position.ALARM_SOS;
            case 5:
                return Position.ALARM_FALL_DOWN;
            case 6:
                return Position.ALARM_LOW_BATTERY;
            case 14:
                return Position.ALARM_GEOFENCE_ENTER;
            case 15:
                return Position.ALARM_GEOFENCE_EXIT;
            default:
                return null;
        }
    }

    private Date readDate(ByteBuf buf) {
        return new DateBuilder()
                .setYear(BcdUtil.readInteger(buf, 2))
                .setMonth(BcdUtil.readInteger(buf, 2))
                .setDay(BcdUtil.readInteger(buf, 2))
                .setHour(BcdUtil.readInteger(buf, 2))
                .setMinute(BcdUtil.readInteger(buf, 2))
                .setSecond(BcdUtil.readInteger(buf, 2))
                .getDate();
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        header = buf.readShort();
        int type = buf.readUnsignedByte();
        buf.readUnsignedShort(); // length
        int index = buf.readUnsignedShort();
        ByteBuf imei = buf.readSlice(8);

        DeviceSession deviceSession = getDeviceSession(
                channel, remoteAddress, ByteBufUtil.hexDump(imei).substring(1));
        if (deviceSession == null) {
            return null;
        }

        boolean positionType = type == MSG_GPS || type == MSG_GPS_2 || type == MSG_ALARM || type == MSG_ALARM_2;
        if (!positionType) {
            sendResponse(channel, header, type, index, imei, 0);
        }

        if (positionType) {

            return decodePosition(channel, deviceSession, buf, type, index, imei);

        } else if (type == MSG_NETWORK && header == 0x2727 || type == MSG_NETWORK_2) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, readDate(buf));

            position.set(Position.KEY_OPERATOR, buf.readCharSequence(
                    buf.readUnsignedByte(), StandardCharsets.UTF_16LE).toString());
            position.set("networkTechnology", buf.readCharSequence(
                    buf.readUnsignedByte(), StandardCharsets.US_ASCII).toString());
            position.set("networkBand", buf.readCharSequence(
                    buf.readUnsignedByte(), StandardCharsets.US_ASCII).toString());
            buf.readCharSequence(buf.readUnsignedByte(), StandardCharsets.US_ASCII); // imsi
            position.set(Position.KEY_ICCID, buf.readCharSequence(
                    buf.readUnsignedByte(), StandardCharsets.US_ASCII).toString());

            return position;

        } else if ((type == MSG_DRIVER_BEHAVIOR_1 || type == MSG_DRIVER_BEHAVIOR_2) && header == 0x2626) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            switch (buf.readUnsignedByte()) {
                case 0:
                case 4:
                    position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
                    break;
                case 1:
                case 3:
                case 5:
                    position.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
                    break;
                case 2:
                    if (type == MSG_DRIVER_BEHAVIOR_1) {
                        position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
                    } else {
                        position.set(Position.KEY_ALARM, Position.ALARM_CORNERING);
                    }
                    break;
                default:
                    break;
            }

            position.setTime(readDate(buf));

            if (type == MSG_DRIVER_BEHAVIOR_2) {
                int status = buf.readUnsignedByte();
                position.setValid(!BitUtil.check(status, 7));
                buf.skipBytes(5); // acceleration
            } else {
                position.setValid(true);
            }

            position.setAltitude(buf.readFloatLE());
            position.setLongitude(buf.readFloatLE());
            position.setLatitude(buf.readFloatLE());
            position.setSpeed(UnitsConverter.knotsFromKph(BcdUtil.readInteger(buf, 4) * 0.1));
            position.setCourse(buf.readUnsignedShort());

            position.set(Position.KEY_RPM, buf.readUnsignedShort());

            return position;

        } else if (type == MSG_BLE) {

            return decodeBle(channel, deviceSession, buf, type, index, imei);

        } else if (type == MSG_COMMAND) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            buf.readUnsignedByte(); // protocol number

            position.set(Position.KEY_RESULT, buf.toString(StandardCharsets.UTF_16LE));

            return position;

        }

        return null;
    }

    private double decodeBleTemp(ByteBuf buf) {
        int value = buf.readUnsignedShort();
        return (BitUtil.check(value, 15) ? -BitUtil.to(value, 15) : BitUtil.to(value, 15)) * 0.01;
    }

    private Position decodeBle(
            Channel channel, DeviceSession deviceSession, ByteBuf buf, int type, int index, ByteBuf imei) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, readDate(buf));

        position.set(Position.KEY_IGNITION, buf.readUnsignedByte() > 0);

        int i = 1;
        while (buf.isReadable()) {
            switch (buf.readUnsignedShort()) {
                case 0x01:
                    position.set("tag" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("tag" + i + "Battery", buf.readUnsignedByte() * 0.01 + 1.22);
                    position.set("tag" + i + "TirePressure", buf.readUnsignedByte() * 1.527 * 2);
                    position.set("tag" + i + "TireTemp", buf.readUnsignedByte() - 55);
                    position.set("tag" + i + "TireStatus", buf.readUnsignedByte());
                    break;
                case 0x02:
                    position.set("tag" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("tag" + i + "Battery", BcdUtil.readInteger(buf, 2) * 0.1);
                    switch (buf.readUnsignedByte()) {
                        case 0:
                            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                            break;
                        case 1:
                            position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                            break;
                        default:
                            break;
                    }
                    buf.readUnsignedByte(); // status
                    buf.skipBytes(16); // location
                    break;
                case 0x03:
                    position.set(Position.KEY_DRIVER_UNIQUE_ID, ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("tag" + i + "Battery", BcdUtil.readInteger(buf, 2) * 0.1);
                    if (buf.readUnsignedByte() == 1) {
                        position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                    }
                    buf.readUnsignedByte(); // status
                    buf.skipBytes(16); // location
                    break;
                case 0x04:
                    position.set("tag" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("tag" + i + "Battery", buf.readUnsignedByte() * 0.01 + 2);
                    buf.readUnsignedByte(); // battery level
                    position.set("tag" + i + "Temp", decodeBleTemp(buf));
                    position.set("tag" + i + "Humidity", buf.readUnsignedShort() * 0.01);
                    position.set("tag" + i + "LightSensor", buf.readUnsignedShort());
                    position.set("tag" + i + "Rssi", buf.readUnsignedByte() - 128);
                    break;
                case 0x05:
                    position.set("tag" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("tag" + i + "Battery", buf.readUnsignedByte() * 0.01 + 2);
                    buf.readUnsignedByte(); // battery level
                    position.set("tag" + i + "Temp", decodeBleTemp(buf));
                    position.set("tag" + i + "Door", buf.readUnsignedByte() > 0);
                    position.set("tag" + i + "Rssi", buf.readUnsignedByte() - 128);
                    break;
                case 0x06:
                    position.set("tag" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(6)));
                    position.set("tag" + i + "Battery", buf.readUnsignedByte() * 0.01 + 2);
                    position.set("tag" + i + "Output", buf.readUnsignedByte() > 0);
                    position.set("tag" + i + "Rssi", buf.readUnsignedByte() - 128);
                    break;
                default:
                    break;
            }
            i += 1;
        }

        sendResponse(channel, header, type, index, imei, 0);

        return position;
    }

    private Position decodePosition(
            Channel channel, DeviceSession deviceSession, ByteBuf buf, int type, int index, ByteBuf imei) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_INDEX, index);

        if (header != 0x2727) {

            buf.readUnsignedShort(); // acc on interval
            buf.readUnsignedShort(); // acc off interval
            buf.readUnsignedByte(); // angle compensation
            buf.readUnsignedShort(); // distance compensation

            position.set(Position.KEY_RSSI, BitUtil.to(buf.readUnsignedShort(), 7));

        }

        int status = buf.readUnsignedByte();
        position.set(Position.KEY_SATELLITES, BitUtil.to(status, 5));

        if (header != 0x2727) {

            buf.readUnsignedByte(); // gsensor manager status
            buf.readUnsignedByte(); // other flags
            buf.readUnsignedByte(); // heartbeat
            buf.readUnsignedByte(); // relay status
            buf.readUnsignedShort(); // drag alarm setting

            int io = buf.readUnsignedShort();
            position.set(Position.KEY_IGNITION, BitUtil.check(io, 14));
            position.set("ac", BitUtil.check(io, 13));
            position.set(Position.PREFIX_IN + 3, BitUtil.check(io, 12));
            position.set(Position.PREFIX_IN + 4, BitUtil.check(io, 11));

            if (type == MSG_GPS_2 || type == MSG_ALARM_2) {
                position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
                buf.readUnsignedByte(); // reserved
            } else {
                position.set(Position.PREFIX_OUT + 1, BitUtil.check(io, 7));
                position.set(Position.PREFIX_OUT + 2, BitUtil.check(io, 8));
                position.set(Position.PREFIX_OUT + 3, BitUtil.check(io, 9));
            }

            if (header != 0x2626) {
                int adcCount = type == MSG_GPS_2 || type == MSG_ALARM_2 ? 5 : 2;
                for (int i = 1; i <= adcCount; i++) {
                    String value = ByteBufUtil.hexDump(buf.readSlice(2));
                    if (!value.equals("ffff")) {
                        position.set(Position.PREFIX_ADC + i, Integer.parseInt(value) * 0.01);
                    }
                }
            }

        }

        int alarm = buf.readUnsignedByte();
        position.set(Position.KEY_ALARM, header != 0x2727 ? decodeAlarm1(alarm) : decodeAlarm2(alarm));

        if (header != 0x2727) {

            buf.readUnsignedByte(); // reserved

            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

            int battery = BcdUtil.readInteger(buf, 2);
            position.set(Position.KEY_BATTERY_LEVEL, battery > 0 ? battery : 100);

        }

        if (BitUtil.check(status, 6)) {

            position.setValid(true);
            position.setTime(readDate(buf));
            position.setAltitude(buf.readFloatLE());
            position.setLongitude(buf.readFloatLE());
            position.setLatitude(buf.readFloatLE());
            position.setSpeed(UnitsConverter.knotsFromKph(BcdUtil.readInteger(buf, 4) * 0.1));
            position.setCourse(buf.readUnsignedShort());

        } else {

            getLastLocation(position, readDate(buf));

            int mcc = buf.readUnsignedShortLE();
            int mnc = buf.readUnsignedShortLE();

            if (mcc != 0xffff && mnc != 0xffff) {
                Network network = new Network();
                for (int i = 0; i < 3; i++) {
                    network.addCellTower(CellTower.from(
                            mcc, mnc, buf.readUnsignedShortLE(), buf.readUnsignedShortLE()));
                }
                position.setNetwork(network);
            }

        }

        if (header == 0x2727) {

            byte[] accelerationBytes = new byte[5];
            buf.readBytes(accelerationBytes);
            long acceleration = new BigInteger(accelerationBytes).longValue();
            double accelerationZ = BitUtil.between(acceleration, 8, 15) + BitUtil.between(acceleration, 4, 8) * 0.1;
            if (!BitUtil.check(acceleration, 15)) {
                accelerationZ = -accelerationZ;
            }
            double accelerationY = BitUtil.between(acceleration, 20, 27) + BitUtil.between(acceleration, 16, 20) * 0.1;
            if (!BitUtil.check(acceleration, 27)) {
                accelerationY = -accelerationY;
            }
            double accelerationX = BitUtil.between(acceleration, 28, 32) + BitUtil.between(acceleration, 32, 39) * 0.1;
            if (!BitUtil.check(acceleration, 39)) {
                accelerationX = -accelerationX;
            }
            position.set(Position.KEY_G_SENSOR, "[" + accelerationX + "," + accelerationY + "," + accelerationZ + "]");

            int battery = BcdUtil.readInteger(buf, 2);
            position.set(Position.KEY_BATTERY_LEVEL, battery > 0 ? battery : 100);
            position.set(Position.KEY_DEVICE_TEMP, (int) buf.readByte());
            position.set("lightSensor", BcdUtil.readInteger(buf, 2) * 0.1);
            position.set(Position.KEY_BATTERY, BcdUtil.readInteger(buf, 2) * 0.1);
            position.set("solarPanel", BcdUtil.readInteger(buf, 2) * 0.1);
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

            int inputStatus = buf.readUnsignedShort();
            position.set(Position.KEY_IGNITION, BitUtil.check(inputStatus, 2));
            position.set(Position.KEY_RSSI, BitUtil.between(inputStatus, 4, 11));

            buf.readUnsignedShort(); // ignition on upload interval
            buf.readUnsignedInt(); // ignition off upload interval
            buf.readUnsignedByte(); // angle upload interval
            buf.readUnsignedShort(); // distance upload interval
            buf.readUnsignedByte(); // heartbeat

        } else {

            if (buf.readableBytes() >= 2) {
                position.set(Position.KEY_POWER, BcdUtil.readInteger(buf, 4) * 0.01);
            }
            if (buf.readableBytes() >= 19) {
                position.set(Position.KEY_OBD_SPEED, BcdUtil.readInteger(buf, 4) * 0.01);
                position.set(Position.KEY_FUEL_USED, buf.readUnsignedInt() * 0.001);
                position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedInt() * 0.001);
                position.set(Position.KEY_RPM, buf.readUnsignedShort());
                int value;
                value = buf.readUnsignedByte();
                if (value != 0xff) {
                    position.set("airInput", value);
                }
                if (value != 0xff) {
                    position.set("airPressure", value);
                }
                if (value != 0xff) {
                    position.set(Position.KEY_COOLANT_TEMP, value - 40);
                }
                if (value != 0xff) {
                    position.set("airTemp", value - 40);
                }
                if (value != 0xff) {
                    position.set(Position.KEY_ENGINE_LOAD, value);
                }
                if (value != 0xff) {
                    position.set(Position.KEY_THROTTLE, value);
                }
                if (value != 0xff) {
                    position.set(Position.KEY_FUEL_LEVEL, value);
                }
            }
        }

        if (type == MSG_ALARM || type == MSG_ALARM_2) {
            sendResponse(channel, header, type, index, imei, alarm);
        }

        return position;
    }

}
