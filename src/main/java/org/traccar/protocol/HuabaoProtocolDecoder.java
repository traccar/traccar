/*
 * Copyright 2015 - 2023 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.WifiAccessPoint;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public class HuabaoProtocolDecoder extends BaseProtocolDecoder {

    public HuabaoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_GENERAL_RESPONSE = 0x8001;
    public static final int MSG_GENERAL_RESPONSE_2 = 0x4401;
    public static final int MSG_HEARTBEAT = 0x0002;
    public static final int MSG_HEARTBEAT_2 = 0x0506;
    public static final int MSG_TERMINAL_REGISTER = 0x0100;
    public static final int MSG_TERMINAL_REGISTER_RESPONSE = 0x8100;
    public static final int MSG_TERMINAL_CONTROL = 0x8105;
    public static final int MSG_TERMINAL_AUTH = 0x0102;
    public static final int MSG_LOCATION_REPORT = 0x0200;
    public static final int MSG_LOCATION_BATCH_2 = 0x0210;
    public static final int MSG_ACCELERATION = 0x2070;
    public static final int MSG_LOCATION_REPORT_2 = 0x5501;
    public static final int MSG_LOCATION_REPORT_BLIND = 0x5502;
    public static final int MSG_LOCATION_BATCH = 0x0704;
    public static final int MSG_OIL_CONTROL = 0XA006;
    public static final int MSG_TIME_SYNC_REQUEST = 0x0109;
    public static final int MSG_TIME_SYNC_RESPONSE = 0x8109;
    public static final int MSG_PHOTO = 0x8888;
    public static final int MSG_TRANSPARENT = 0x0900;

    public static final int RESULT_SUCCESS = 0;

    public static ByteBuf formatMessage(int type, ByteBuf id, boolean shortIndex, ByteBuf data) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x7e);
        buf.writeShort(type);
        buf.writeShort(data.readableBytes());
        buf.writeBytes(id);
        if (shortIndex) {
            buf.writeByte(1);
        } else {
            buf.writeShort(0);
        }
        buf.writeBytes(data);
        data.release();
        buf.writeByte(Checksum.xor(buf.nioBuffer(1, buf.readableBytes() - 1)));
        buf.writeByte(0x7e);
        return buf;
    }

    private void sendGeneralResponse(
            Channel channel, SocketAddress remoteAddress, ByteBuf id, int type, int index) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeShort(index);
            response.writeShort(type);
            response.writeByte(RESULT_SUCCESS);
            channel.writeAndFlush(new NetworkMessage(
                    formatMessage(MSG_GENERAL_RESPONSE, id, false, response), remoteAddress));
        }
    }

    private void sendGeneralResponse2(
            Channel channel, SocketAddress remoteAddress, ByteBuf id, int type) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeShort(type);
            response.writeByte(RESULT_SUCCESS);
            channel.writeAndFlush(new NetworkMessage(
                    formatMessage(MSG_GENERAL_RESPONSE_2, id, true, response), remoteAddress));
        }
    }

    private String decodeAlarm(long value) {
        if (BitUtil.check(value, 0)) {
            return Position.ALARM_SOS;
        }
        if (BitUtil.check(value, 1)) {
            return Position.ALARM_OVERSPEED;
        }
        if (BitUtil.check(value, 5)) {
            return Position.ALARM_GPS_ANTENNA_CUT;
        }
        if (BitUtil.check(value, 4) || BitUtil.check(value, 9)
                || BitUtil.check(value, 10) || BitUtil.check(value, 11)) {
            return Position.ALARM_FAULT;
        }
        if (BitUtil.check(value, 7) || BitUtil.check(value, 18)) {
            return Position.ALARM_LOW_BATTERY;
        }
        if (BitUtil.check(value, 8)) {
            return Position.ALARM_POWER_OFF;
        }
        if (BitUtil.check(value, 15)) {
            return Position.ALARM_VIBRATION;
        }
        if (BitUtil.check(value, 16) || BitUtil.check(value, 17)) {
            return Position.ALARM_TAMPERING;
        }
        if (BitUtil.check(value, 20)) {
            return Position.ALARM_GEOFENCE;
        }
        if (BitUtil.check(value, 28)) {
            return Position.ALARM_MOVEMENT;
        }
        if (BitUtil.check(value, 29) || BitUtil.check(value, 30)) {
            return Position.ALARM_ACCIDENT;
        }
        return null;
    }

    private int readSignedWord(ByteBuf buf) {
        int value = buf.readUnsignedShort();
        return BitUtil.check(value, 15) ? -BitUtil.to(value, 15) : BitUtil.to(value, 15);
    }

    private Date readDate(ByteBuf buf, TimeZone timeZone) {
        DateBuilder dateBuilder = new DateBuilder(timeZone)
                .setYear(BcdUtil.readInteger(buf, 2))
                .setMonth(BcdUtil.readInteger(buf, 2))
                .setDay(BcdUtil.readInteger(buf, 2))
                .setHour(BcdUtil.readInteger(buf, 2))
                .setMinute(BcdUtil.readInteger(buf, 2))
                .setSecond(BcdUtil.readInteger(buf, 2));
        return dateBuilder.getDate();
    }

    private String decodeId(ByteBuf id) {
        String serial = ByteBufUtil.hexDump(id);
        if (serial.matches("[0-9]+")) {
            return serial;
        } else {
            long imei = id.getUnsignedShort(0);
            imei = (imei << 32) + id.getUnsignedInt(2);
            return String.valueOf(imei) + Checksum.luhn(imei);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getByte(buf.readerIndex()) == '(') {
            String sentence = buf.toString(StandardCharsets.US_ASCII);
            if (sentence.contains("BASE,2")) {
                DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                String response = sentence.replace("TIME", dateFormat.format(new Date()));
                if (channel != null) {
                    channel.writeAndFlush(new NetworkMessage(
                            Unpooled.copiedBuffer(response, StandardCharsets.US_ASCII), remoteAddress));
                }
                return null;
            } else {
                return decodeResult(channel, remoteAddress, sentence);
            }
        }

        buf.readUnsignedByte(); // start marker
        int type = buf.readUnsignedShort();
        int attribute = buf.readUnsignedShort();
        ByteBuf id = buf.readSlice(6); // phone number
        int index;
        if (type == MSG_LOCATION_REPORT_2 || type == MSG_LOCATION_REPORT_BLIND) {
            index = buf.readUnsignedByte();
        } else {
            index = buf.readUnsignedShort();
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, decodeId(id));
        if (deviceSession == null) {
            return null;
        }

        if (!deviceSession.contains(DeviceSession.KEY_TIMEZONE)) {
            deviceSession.set(DeviceSession.KEY_TIMEZONE, getTimeZone(deviceSession.getDeviceId(), "GMT+8"));
        }

        if (type == MSG_TERMINAL_REGISTER) {

            if (channel != null) {
                ByteBuf response = Unpooled.buffer();
                response.writeShort(index);
                response.writeByte(RESULT_SUCCESS);
                response.writeBytes(decodeId(id).getBytes(StandardCharsets.US_ASCII));
                channel.writeAndFlush(new NetworkMessage(
                        formatMessage(MSG_TERMINAL_REGISTER_RESPONSE, id, false, response), remoteAddress));
            }

        } else if (type == MSG_TERMINAL_AUTH || type == MSG_HEARTBEAT || type == MSG_HEARTBEAT_2 || type == MSG_PHOTO) {

            sendGeneralResponse(channel, remoteAddress, id, type, index);

        } else if (type == MSG_LOCATION_REPORT) {

            sendGeneralResponse(channel, remoteAddress, id, type, index);

            return decodeLocation(deviceSession, buf);

        } else if (type == MSG_LOCATION_REPORT_2 || type == MSG_LOCATION_REPORT_BLIND) {

            if (BitUtil.check(attribute, 15)) {
                sendGeneralResponse2(channel, remoteAddress, id, type);
            }

            return decodeLocation2(deviceSession, buf, type);

        } else if (type == MSG_LOCATION_BATCH || type == MSG_LOCATION_BATCH_2) {

            sendGeneralResponse(channel, remoteAddress, id, type, index);

            return decodeLocationBatch(deviceSession, buf, type);

        } else if (type == MSG_TIME_SYNC_REQUEST) {

            if (channel != null) {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                ByteBuf response = Unpooled.buffer();
                response.writeShort(calendar.get(Calendar.YEAR));
                response.writeByte(calendar.get(Calendar.MONTH) + 1);
                response.writeByte(calendar.get(Calendar.DAY_OF_MONTH));
                response.writeByte(calendar.get(Calendar.HOUR_OF_DAY));
                response.writeByte(calendar.get(Calendar.MINUTE));
                response.writeByte(calendar.get(Calendar.SECOND));
                channel.writeAndFlush(new NetworkMessage(
                        formatMessage(MSG_TERMINAL_REGISTER_RESPONSE, id, false, response), remoteAddress));
            }

        } else if (type == MSG_ACCELERATION) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            StringBuilder data = new StringBuilder("[");
            while (buf.readableBytes() > 2) {
                buf.skipBytes(6); // time
                if (data.length() > 1) {
                    data.append(",");
                }
                data.append("[");
                data.append(readSignedWord(buf));
                data.append(",");
                data.append(readSignedWord(buf));
                data.append(",");
                data.append(readSignedWord(buf));
                data.append("]");
            }
            data.append("]");

            position.set(Position.KEY_G_SENSOR, data.toString());

            return position;

        } else if (type == MSG_TRANSPARENT) {

            sendGeneralResponse(channel, remoteAddress, id, type, index);

            return decodeTransparent(deviceSession, buf);

        }

        return null;
    }

    private Position decodeResult(Channel channel, SocketAddress remoteAddress, String sentence) {
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession != null) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            getLastLocation(position, null);
            position.set(Position.KEY_RESULT, sentence);
            return position;
        }
        return null;
    }

    private void decodeExtension(Position position, ByteBuf buf, int endIndex) {
        while (buf.readerIndex() < endIndex) {
            int type = buf.readUnsignedByte();
            int length = buf.readUnsignedByte();
            switch (type) {
                case 0x01 -> position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 100L);
                case 0x02 -> position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedShort() * 0.1);
                case 0x03 -> position.set(Position.KEY_OBD_SPEED, buf.readUnsignedShort() * 0.1);
                case 0x56 -> {
                    buf.readUnsignedByte(); // power level
                    position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                }
                case 0x61 -> position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);
                case 0x69 -> position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.01);
                case 0x80 -> position.set(Position.KEY_OBD_SPEED, buf.readUnsignedByte());
                case 0x81 -> position.set(Position.KEY_RPM, buf.readUnsignedShort());
                case 0x82 -> position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.1);
                case 0x83 -> position.set(Position.KEY_ENGINE_LOAD, buf.readUnsignedByte());
                case 0x84 -> position.set(Position.KEY_COOLANT_TEMP, buf.readUnsignedByte() - 40);
                case 0x85 -> position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedShort());
                case 0x86 -> position.set("intakeTemp", buf.readUnsignedByte() - 40);
                case 0x87 -> position.set("intakeFlow", buf.readUnsignedShort());
                case 0x88 -> position.set("intakePressure", buf.readUnsignedByte());
                case 0x89 -> position.set(Position.KEY_THROTTLE, buf.readUnsignedByte());
                case 0x8B -> {
                    position.set(Position.KEY_VIN, buf.readCharSequence(17, StandardCharsets.US_ASCII).toString());
                }
                case 0x8C -> position.set(Position.KEY_OBD_ODOMETER, buf.readUnsignedInt() * 100L);
                case 0x8D -> position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedShort() * 1000L);
                case 0x8E -> position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedByte());
                case 0xA0 -> {
                    String codes = buf.readCharSequence(length, StandardCharsets.US_ASCII).toString();
                    position.set(Position.KEY_DTCS, codes.replace(',', ' '));
                }
                case 0xCC -> {
                    position.set(Position.KEY_ICCID, buf.readCharSequence(20, StandardCharsets.US_ASCII).toString());
                }
                default -> buf.skipBytes(length);
            }
        }
    }

    private void decodeCoordinates(Position position, DeviceSession deviceSession, ByteBuf buf) {

        int status = buf.readInt();

        String model = getDeviceModel(deviceSession);

        position.set(Position.KEY_IGNITION, BitUtil.check(status, 0));
        if ("G1C Pro".equals(model)) {
            position.set(Position.KEY_MOTION, BitUtil.check(status, 4));
        }
        position.set(Position.KEY_BLOCKED, BitUtil.check(status, 10));
        position.set(Position.KEY_CHARGE, BitUtil.check(status, 26));

        position.setValid(BitUtil.check(status, 1));

        double lat = buf.readUnsignedInt() * 0.000001;
        double lon = buf.readUnsignedInt() * 0.000001;

        if (BitUtil.check(status, 2)) {
            position.setLatitude(-lat);
        } else {
            position.setLatitude(lat);
        }

        if (BitUtil.check(status, 3)) {
            position.setLongitude(-lon);
        } else {
            position.setLongitude(lon);
        }
    }

    private double decodeCustomDouble(ByteBuf buf) {
        int b1 = buf.readByte();
        int b2 = buf.readUnsignedByte();
        int sign = b1 != 0 ? b1 / Math.abs(b1) : 1;
        return sign * (Math.abs(b1) + b2 / 255.0);
    }

    private Position decodeLocation(DeviceSession deviceSession, ByteBuf buf) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.addAlarm(decodeAlarm(buf.readUnsignedInt()));

        decodeCoordinates(position, deviceSession, buf);

        position.setAltitude(buf.readShort());
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort() * 0.1));
        position.setCourse(buf.readUnsignedShort());
        position.setTime(readDate(buf, deviceSession.get(DeviceSession.KEY_TIMEZONE)));

        if (buf.readableBytes() == 20) {

            buf.skipBytes(4); // remaining battery and mileage
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 1000);
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.1);
            buf.readUnsignedInt(); // area id
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());
            buf.skipBytes(3); // reserved

            return position;

        }
        Network network = new Network();

        while (buf.readableBytes() > 2) {

            int subtype = buf.readUnsignedByte();
            int length = buf.readUnsignedByte();
            int endIndex = buf.readerIndex() + length;
            String stringValue;
            switch (subtype) {
                case 0x01:
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 100);
                    break;
                case 0x02:
                    position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedShort() * 0.1);
                    break;
                case 0x25:
                    position.set(Position.KEY_INPUT, buf.readUnsignedInt());
                    break;
                case 0x2B:
                case 0xA7:
                    position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
                    position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());
                    break;
                case 0x30:
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    break;
                case 0x31:
                    position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                    break;
                case 0x33:
                    stringValue = buf.readCharSequence(length, StandardCharsets.US_ASCII).toString();
                    if (stringValue.startsWith("*M00")) {
                        String lockStatus = stringValue.substring(8, 8 + 7);
                        position.set(Position.KEY_BATTERY, Integer.parseInt(lockStatus.substring(2, 5)) * 0.01);
                    }
                    break;
                case 0x51:
                    if (length == 2 || length == 16) {
                        for (int i = 1; i <= length / 2; i++) {
                            int value = buf.readUnsignedShort();
                            if (value != 0xffff) {
                                if (BitUtil.check(value, 15)) {
                                    position.set(Position.PREFIX_TEMP + i, -BitUtil.to(value, 15) / 10.0);
                                } else {
                                    position.set(Position.PREFIX_TEMP + i, value / 10.0);
                                }
                            }
                        }
                    }
                    break;
                case 0x56:
                    position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte() * 10);
                    buf.readUnsignedByte(); // reserved
                    break;
                case 0x57:
                    int alarm = buf.readUnsignedShort();
                    position.addAlarm(BitUtil.check(alarm, 8) ? Position.ALARM_ACCELERATION : null);
                    position.addAlarm(BitUtil.check(alarm, 9) ? Position.ALARM_BRAKING : null);
                    position.addAlarm(BitUtil.check(alarm, 10) ? Position.ALARM_CORNERING : null);
                    buf.readUnsignedShort(); // external switch state
                    buf.skipBytes(4); // reserved
                    break;
                case 0x60:
                    int event = buf.readUnsignedShort();
                    position.set(Position.KEY_EVENT, event);
                    if (event >= 0x0061 && event <= 0x0066) {
                        buf.skipBytes(6); // lock id
                        stringValue = buf.readCharSequence(8, StandardCharsets.US_ASCII).toString();
                        position.set(Position.KEY_DRIVER_UNIQUE_ID, stringValue);
                    }
                    break;
                case 0x63:
                    for (int i = 1; i <= length / 11; i++) {
                        position.set("lock" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(6)));
                        position.set("lock" + i + "Battery", buf.readUnsignedShort() * 0.001);
                        position.set("lock" + i + "Seal", buf.readUnsignedByte() == 0x31);
                        buf.readUnsignedByte(); // physical state
                        buf.readUnsignedByte(); // rssi
                    }
                    break;
                case 0x64:
                    buf.readUnsignedInt(); // alarm serial number
                    buf.readUnsignedByte(); // alarm status
                    position.set("adasAlarm", buf.readUnsignedByte());
                    break;
                case 0x65:
                    buf.readUnsignedInt(); // alarm serial number
                    buf.readUnsignedByte(); // alarm status
                    position.set("dmsAlarm", buf.readUnsignedByte());
                    break;
                case 0x70:
                    buf.readUnsignedInt(); // alarm serial number
                    buf.readUnsignedByte(); // alarm status
                    switch (buf.readUnsignedByte()) {
                        case 0x01 -> position.addAlarm(Position.ALARM_ACCELERATION);
                        case 0x02 -> position.addAlarm(Position.ALARM_BRAKING);
                        case 0x03 -> position.addAlarm(Position.ALARM_CORNERING);
                        case 0x16 -> position.addAlarm(Position.ALARM_ACCIDENT);
                    }
                    break;
                case 0x69:
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.01);
                    break;
                case 0x80:
                    buf.readUnsignedByte(); // content
                    endIndex = buf.writerIndex() - 2;
                    decodeExtension(position, buf, endIndex);
                    break;
                case 0x91:
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.1);
                    position.set(Position.KEY_RPM, buf.readUnsignedShort());
                    position.set(Position.KEY_OBD_SPEED, buf.readUnsignedByte());
                    position.set(Position.KEY_THROTTLE, buf.readUnsignedByte() * 100 / 255);
                    position.set(Position.KEY_ENGINE_LOAD, buf.readUnsignedByte() * 100 / 255);
                    position.set(Position.KEY_COOLANT_TEMP, buf.readUnsignedByte() - 40);
                    buf.readUnsignedShort();
                    position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedShort() * 0.01);
                    buf.readUnsignedShort();
                    buf.readUnsignedInt();
                    buf.readUnsignedShort();
                    position.set(Position.KEY_FUEL_USED, buf.readUnsignedShort() * 0.01);
                    break;
                case 0x94:
                    if (length > 0) {
                        stringValue = buf.readCharSequence(length, StandardCharsets.US_ASCII).toString();
                        position.set(Position.KEY_VIN, stringValue);
                    }
                    break;
                case 0xAC:
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                    break;
                case 0xBC:
                    stringValue = buf.readCharSequence(length, StandardCharsets.US_ASCII).toString();
                    position.set("driver", stringValue.trim());
                    break;
                case 0xBD:
                    stringValue = buf.readCharSequence(length, StandardCharsets.US_ASCII).toString();
                    position.set(Position.KEY_DRIVER_UNIQUE_ID, stringValue);
                    break;
                case 0xD0:
                    long userStatus = buf.readUnsignedInt();
                    if (BitUtil.check(userStatus, 3)) {
                        position.addAlarm(Position.ALARM_VIBRATION);
                    }
                    break;
                case 0xD3:
                    position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.1);
                    break;
                case 0xD4:
                case 0xE1:
                    if (length == 1) {
                        position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                    } else {
                        position.set(Position.KEY_DRIVER_UNIQUE_ID, String.valueOf(buf.readUnsignedInt()));
                    }
                    break;
                case 0xD5:
                    if (length == 2) {
                        position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.01);
                    } else {
                        int count = buf.readUnsignedByte();
                        for (int i = 1; i <= count; i++) {
                            position.set("lock" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(5)));
                            position.set("lock" + i + "Card", ByteBufUtil.hexDump(buf.readSlice(5)));
                            position.set("lock" + i + "Battery", buf.readUnsignedByte());
                            int status = buf.readUnsignedShort();
                            position.set("lock" + i + "Locked", !BitUtil.check(status, 5));
                        }
                    }
                    break;
                case 0xDA:
                    buf.readUnsignedShort(); // string cut count
                    int deviceStatus = buf.readUnsignedByte();
                    position.set("string", BitUtil.check(deviceStatus, 0));
                    position.set(Position.KEY_MOTION, BitUtil.check(deviceStatus, 2));
                    position.set("cover", BitUtil.check(deviceStatus, 3));
                    break;
                case 0xE2:
                    position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedInt() * 0.1);
                    break;
                case 0xE3:
                    buf.readUnsignedByte(); // reserved
                    position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 100.0);
                    break;
                case 0xE6:
                    while (buf.readerIndex() < endIndex) {
                        int sensorIndex = buf.readUnsignedByte();
                        buf.skipBytes(6); // mac
                        position.set(Position.PREFIX_TEMP + sensorIndex, decodeCustomDouble(buf));
                        position.set("humidity" + sensorIndex, decodeCustomDouble(buf));
                    }
                    break;
                case 0xEB:
                    if (buf.getUnsignedShort(buf.readerIndex()) > 200) {
                        int mcc = buf.readUnsignedShort();
                        int mnc = buf.readUnsignedByte();
                        while (buf.readerIndex() < endIndex) {
                            network.addCellTower(CellTower.from(
                                    mcc, mnc, buf.readUnsignedShort(), buf.readUnsignedShort(),
                                    buf.readUnsignedByte()));
                        }
                    } else {
                        while (buf.readerIndex() < endIndex) {
                            int extendedLength = buf.readUnsignedShort();
                            int extendedType = buf.readUnsignedShort();
                            switch (extendedType) {
                                case 0x0001:
                                    position.set("fuel1", buf.readUnsignedShort() * 0.1);
                                    buf.readUnsignedByte(); // unused
                                    break;
                                case 0x0023:
                                    position.set("fuel2", Double.parseDouble(
                                            buf.readCharSequence(6, StandardCharsets.US_ASCII).toString()));
                                    break;
                                case 0x00B2:
                                    position.set(Position.KEY_ICCID, ByteBufUtil.hexDump(
                                            buf.readSlice(10)).replaceAll("f", ""));
                                    break;
                                case 0x00CE:
                                    position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);
                                    break;
                                case 0x00D8:
                                    network.addCellTower(CellTower.from(
                                            buf.readUnsignedShort(), buf.readUnsignedByte(),
                                            buf.readUnsignedShort(), buf.readUnsignedInt()));
                                    break;
                                case 0x00A8:
                                case 0x00E1:
                                    position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                                    break;
                                default:
                                    buf.skipBytes(extendedLength - 2);
                                    break;
                            }
                        }
                    }
                    break;
                case 0xED:
                    stringValue = buf.readCharSequence(length, StandardCharsets.US_ASCII).toString();
                    position.set(Position.KEY_CARD, stringValue.trim());
                    break;
                case 0xEE:
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.001);
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);
                    position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                    break;
                case 0xF1:
                    position.set(Position.KEY_POWER, buf.readUnsignedInt() * 0.001);
                    break;
                case 0xF3:
                    while (buf.readerIndex() < endIndex) {
                        int extendedType = buf.readUnsignedShort();
                        int extendedLength = buf.readUnsignedByte();
                        switch (extendedType) {
                            case 0x0002 -> position.set(Position.KEY_OBD_SPEED, buf.readUnsignedShort() * 0.1);
                            case 0x0003 -> position.set(Position.KEY_RPM, buf.readUnsignedShort());
                            case 0x0004 -> position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.001);
                            case 0x0005 -> position.set(Position.KEY_OBD_ODOMETER, buf.readUnsignedInt() * 100);
                            case 0x0007 -> position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedShort() * 0.1);
                            case 0x0008 -> position.set(Position.KEY_ENGINE_LOAD, buf.readUnsignedShort() * 0.1);
                            case 0x0009 -> position.set(Position.KEY_COOLANT_TEMP, buf.readUnsignedShort() - 40);
                            case 0x000B -> position.set("intakePressure", buf.readUnsignedShort());
                            case 0x000C -> position.set("intakeTemp", buf.readUnsignedShort() - 40);
                            case 0x000D -> position.set("intakeFlow", buf.readUnsignedShort());
                            case 0x000E -> position.set(Position.KEY_THROTTLE, buf.readUnsignedShort() * 100 / 255);
                            case 0x0050 -> {
                                position.set(Position.KEY_VIN, buf.readSlice(17).toString(StandardCharsets.US_ASCII));
                            }
                            case 0x0100 -> position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedShort() * 0.1);
                            case 0x0102 -> position.set("tripFuel", buf.readUnsignedShort() * 0.1);
                            case 0x0112 -> position.set("hardAccelerationCount", buf.readUnsignedShort());
                            case 0x0113 -> position.set("hardDecelerationCount", buf.readUnsignedShort());
                            case 0x0114 -> position.set("hardCorneringCount", buf.readUnsignedShort());
                            default -> buf.skipBytes(extendedLength);
                        }
                    }
                    break;
                case 0xF4:
                    while (buf.readerIndex() < endIndex) {
                        String mac = ByteBufUtil.hexDump(buf.readSlice(6)).replaceAll("(..)", "$1:");
                        network.addWifiAccessPoint(WifiAccessPoint.from(
                                mac.substring(0, mac.length() - 1), buf.readByte()));
                    }
                    break;
                case 0xF6:
                    buf.readUnsignedByte(); // data type
                    int fieldMask = buf.readUnsignedByte();
                    if (BitUtil.check(fieldMask, 0)) {
                        buf.readUnsignedShort(); // light
                    }
                    if (BitUtil.check(fieldMask, 1)) {
                        position.set(Position.PREFIX_TEMP + 1, buf.readShort() * 0.1);
                    }
                    break;
                case 0xF7:
                    position.set(Position.KEY_BATTERY, buf.readUnsignedInt() * 0.001);
                    if (length >= 5) {
                        short batteryStatus = buf.readUnsignedByte();
                        if (batteryStatus == 2 || batteryStatus == 3) {
                            position.set(Position.KEY_CHARGE, true);
                        }
                    }
                    if (length >= 6) {
                        position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                    }
                    break;
                case 0xFE:
                    if (length == 1) {
                        position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                    } else if (length == 2) {
                        position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.1);
                    } else {
                        int mark = buf.readUnsignedByte();
                        if (mark == 0x7C) {
                            while (buf.readerIndex() < endIndex) {
                                int extendedType = buf.readUnsignedByte();
                                int extendedLength = buf.readUnsignedByte();
                                if (extendedType == 0x01) {
                                    long alarms = buf.readUnsignedInt();
                                    if (BitUtil.check(alarms, 0)) {
                                        position.addAlarm(Position.ALARM_ACCELERATION);
                                    }
                                    if (BitUtil.check(alarms, 1)) {
                                        position.addAlarm(Position.ALARM_BRAKING);
                                    }
                                    if (BitUtil.check(alarms, 2)) {
                                        position.addAlarm(Position.ALARM_CORNERING);
                                    }
                                    if (BitUtil.check(alarms, 3)) {
                                        position.addAlarm(Position.ALARM_ACCIDENT);
                                    }
                                    if (BitUtil.check(alarms, 4)) {
                                        position.addAlarm(Position.ALARM_TAMPERING);
                                    }
                                } else {
                                    buf.skipBytes(extendedLength);
                                }
                            }
                        }
                        position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                    }
                    break;
                default:
                    break;
            }
            buf.readerIndex(endIndex);
        }

        if (network.getCellTowers() != null || network.getWifiAccessPoints() != null) {
            position.setNetwork(network);
        }
        return position;
    }

    private Position decodeLocation2(DeviceSession deviceSession, ByteBuf buf, int type) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        Jt600ProtocolDecoder.decodeBinaryLocation(buf, position);
        position.setValid(type != MSG_LOCATION_REPORT_BLIND);

        position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 1000L);

        int battery = buf.readUnsignedByte();
        if (battery <= 100) {
            position.set(Position.KEY_BATTERY_LEVEL, battery);
        } else if (battery == 0xAA || battery == 0xAB) {
            position.set(Position.KEY_CHARGE, true);
        }

        long cid = buf.readUnsignedInt();
        int lac = buf.readUnsignedShort();
        if (cid > 0 && lac > 0) {
            position.setNetwork(new Network(CellTower.fromCidLac(getConfig(), cid, lac)));
        }

        int product = buf.readUnsignedByte();
        int status = buf.readUnsignedShort();
        int alarm = buf.readUnsignedShort();

        if (product == 1 || product == 2) {
            if (BitUtil.check(alarm, 0)) {
                position.addAlarm(Position.ALARM_LOW_POWER);
            }
        } else if (product == 3) {
            position.set(Position.KEY_BLOCKED, BitUtil.check(status, 5));
            if (BitUtil.check(alarm, 0)) {
                position.addAlarm(Position.ALARM_OVERSPEED);
            }
            if (BitUtil.check(alarm, 1)) {
                position.addAlarm(Position.ALARM_LOW_POWER);
            }
            if (BitUtil.check(alarm, 2)) {
                position.addAlarm(Position.ALARM_VIBRATION);
            }
            if (BitUtil.check(alarm, 3)) {
                position.addAlarm(Position.ALARM_LOW_BATTERY);
            }
            if (BitUtil.check(alarm, 5)) {
                position.addAlarm(Position.ALARM_GEOFENCE_ENTER);
            }
            if (BitUtil.check(alarm, 6)) {
                position.addAlarm(Position.ALARM_GEOFENCE_EXIT);
            }
        }

        position.set(Position.KEY_STATUS, status);

        while (buf.readableBytes() > 2) {
            int id = buf.readUnsignedByte();
            int length = buf.readUnsignedByte();
            switch (id) {
                case 0x02:
                    position.setAltitude(buf.readShort());
                    break;
                case 0x10:
                    position.set("wakeSource", buf.readUnsignedByte());
                    break;
                case 0x0A:
                    if (length == 3) {
                        buf.readUnsignedShort(); // mcc
                        buf.readUnsignedByte(); // mnc
                    } else {
                        buf.skipBytes(length);
                    }
                    break;
                case 0x0B:
                    position.set("lockCommand", buf.readUnsignedByte());
                    if (length >= 5 && length <= 6) {
                        position.set("lockCard", buf.readUnsignedInt());
                    } else if (length >= 7) {
                        position.set("lockPassword", buf.readCharSequence(6, StandardCharsets.US_ASCII).toString());
                    }
                    if (length % 2 == 0) {
                        position.set("unlockResult", buf.readUnsignedByte());
                    }
                    break;
                case 0x0C:
                    int x = buf.readUnsignedShort();
                    if (x > 0x8000) {
                        x -= 0x10000;
                    }
                    int y = buf.readUnsignedShort();
                    if (y > 0x8000) {
                        y -= 0x10000;
                    }
                    int z = buf.readUnsignedShort();
                    if (z > 0x8000) {
                        z -= 0x10000;
                    }
                    position.set("tilt", String.format("[%d,%d,%d]", x, y, z));
                    break;
                case 0xFC:
                    position.set(Position.KEY_GEOFENCE, buf.readUnsignedByte());
                    break;
                default:
                    buf.skipBytes(length);
                    break;
            }
        }

        return position;
    }

    private List<Position> decodeLocationBatch(DeviceSession deviceSession, ByteBuf buf, int type) {

        List<Position> positions = new LinkedList<>();

        int locationType = 0;
        if (type == MSG_LOCATION_BATCH) {
            buf.readUnsignedShort(); // count
            locationType = buf.readUnsignedByte();
        }

        while (buf.readableBytes() > 2) {
            int length = type == MSG_LOCATION_BATCH_2 ? buf.readUnsignedByte() : buf.readUnsignedShort();
            ByteBuf fragment = buf.readSlice(length);
            Position position = decodeLocation(deviceSession, fragment);
            if (locationType > 0) {
                position.set(Position.KEY_ARCHIVE, true);
            }
            positions.add(position);
        }

        return positions;
    }

    private Position decodeTransparent(DeviceSession deviceSession, ByteBuf buf) {

        int type = buf.readUnsignedByte();

        if (type == 0xF0) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            Date time = readDate(buf, deviceSession.get(DeviceSession.KEY_TIMEZONE));

            if (buf.readUnsignedByte() > 0) {
                position.set(Position.KEY_ARCHIVE, true);
            }

            buf.readUnsignedByte(); // vehicle type

            int count;
            int subtype = buf.readUnsignedByte();
            switch (subtype) {
                case 0x01:
                    count = buf.readUnsignedByte();
                    for (int i = 0; i < count; i++) {
                        int id = buf.readUnsignedShort();
                        int length = buf.readUnsignedByte();
                        switch (id) {
                            case 0x0102, 0x0528, 0x0546 -> {
                                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 100);
                            }
                            case 0x0103 -> position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedInt() * 0.01);
                            case 0x0111 -> position.set("fuelTemp", buf.readUnsignedByte() - 40);
                            case 0x012E -> position.set("oilLevel", buf.readUnsignedShort() * 0.1);
                            case 0x052A -> position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedShort() * 0.01);
                            case 0x0105, 0x052C -> position.set(Position.KEY_FUEL_USED, buf.readUnsignedInt() * 0.01);
                            case 0x014A, 0x0537, 0x0538, 0x0539 -> {
                                position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedShort() * 0.01);
                            }
                            case 0x052B -> position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedByte());
                            case 0x052D -> position.set(Position.KEY_COOLANT_TEMP, buf.readUnsignedByte() - 40);
                            case 0x052E -> position.set("airTemp", buf.readUnsignedByte() - 40);
                            case 0x0530 -> position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.001);
                            case 0x0535 -> position.set(Position.KEY_OBD_SPEED, buf.readUnsignedShort() * 0.1);
                            case 0x0536 -> position.set(Position.KEY_RPM, buf.readUnsignedShort());
                            case 0x053D -> position.set("intakePressure", buf.readUnsignedShort() * 0.1);
                            case 0x0544 -> position.set("liquidLevel", buf.readUnsignedByte());
                            case 0x0547, 0x0548 -> position.set(Position.KEY_THROTTLE, buf.readUnsignedByte());
                            default -> {
                                switch (length) {
                                    case 1 -> position.set(Position.PREFIX_IO + id, buf.readUnsignedByte());
                                    case 2 -> position.set(Position.PREFIX_IO + id, buf.readUnsignedShort());
                                    case 4 -> position.set(Position.PREFIX_IO + id, buf.readUnsignedInt());
                                    default -> buf.skipBytes(length);
                                }
                            }
                        }
                    }
                    getLastLocation(position, time);
                    decodeCoordinates(position, deviceSession, buf);
                    position.setTime(time);
                    break;
                case 0x02:
                    List<String> codes = new LinkedList<>();
                    count = buf.readUnsignedShort();
                    for (int i = 0; i < count; i++) {
                        buf.readUnsignedInt(); // system id
                        int codeCount = buf.readUnsignedShort();
                        for (int j = 0; j < codeCount; j++) {
                            buf.readUnsignedInt(); // dtc
                            buf.readUnsignedInt(); // status
                            codes.add(buf.readCharSequence(
                                    buf.readUnsignedShort(), StandardCharsets.US_ASCII).toString().trim());
                        }
                    }
                    position.set(Position.KEY_DTCS, String.join(" ", codes));
                    getLastLocation(position, time);
                    decodeCoordinates(position, deviceSession, buf);
                    position.setTime(time);
                    break;
                case 0x03:
                    count = buf.readUnsignedByte();
                    for (int i = 0; i < count; i++) {
                        int id = buf.readUnsignedByte();
                        int length = buf.readUnsignedByte();
                        switch (id) {
                            case 0x01:
                                position.addAlarm(Position.ALARM_POWER_RESTORED);
                                break;
                            case 0x02:
                                position.addAlarm(Position.ALARM_POWER_CUT);
                                break;
                            case 0x1A:
                                position.addAlarm(Position.ALARM_ACCELERATION);
                                break;
                            case 0x1B:
                                position.addAlarm(Position.ALARM_BRAKING);
                                break;
                            case 0x1C:
                                position.addAlarm(Position.ALARM_CORNERING);
                                break;
                            case 0x1D:
                            case 0x1E:
                            case 0x1F:
                                position.addAlarm(Position.ALARM_LANE_CHANGE);
                                break;
                            case 0x23:
                                position.addAlarm(Position.ALARM_FATIGUE_DRIVING);
                                break;
                            case 0x26:
                            case 0x27:
                            case 0x28:
                                position.addAlarm(Position.ALARM_ACCIDENT);
                                break;
                            case 0x31:
                            case 0x32:
                                position.addAlarm(Position.ALARM_DOOR);
                                break;
                            default:
                                break;
                        }
                        buf.skipBytes(length);
                    }
                    getLastLocation(position, time);
                    decodeCoordinates(position, deviceSession, buf);
                    position.setTime(time);
                    break;
                case 0x0B:
                    if (buf.readUnsignedByte() > 0) {
                        position.set(Position.KEY_VIN, buf.readCharSequence(17, StandardCharsets.US_ASCII).toString());
                    }
                    getLastLocation(position, time);
                    break;
                case 0x15:
                    int event = buf.readInt();
                    switch (event) {
                        case 51 -> position.addAlarm(Position.ALARM_ACCELERATION);
                        case 52 -> position.addAlarm(Position.ALARM_BRAKING);
                        case 53 -> position.addAlarm(Position.ALARM_CORNERING);
                        case 54 -> position.addAlarm(Position.ALARM_LANE_CHANGE);
                        case 56 -> position.addAlarm(Position.ALARM_ACCIDENT);
                        default -> position.set(Position.KEY_EVENT, event);
                    }
                    getLastLocation(position, time);
                    break;
                default:
                    return null;
            }

            return position;

        } else if (type == 0xFF) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setValid(true);
            position.setTime(readDate(buf, deviceSession.get(DeviceSession.KEY_TIMEZONE)));
            position.setLatitude(buf.readInt() * 0.000001);
            position.setLongitude(buf.readInt() * 0.000001);
            position.setAltitude(buf.readShort());
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort() * 0.1));
            position.setCourse(buf.readUnsignedShort());

            // TODO more positions and g sensor data

            return position;

        }

        return null;
    }

}
