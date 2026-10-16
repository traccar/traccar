/*
 * Copyright 2014 - 2026 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.socket.DatagramChannel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.BufferUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DataConverter;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class KhdProtocolDecoder extends BaseProtocolDecoder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyMMddHHmmss").withZone(ZoneOffset.UTC);

    public KhdProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private String[] readIdentifiers(ByteBuf buf) {
        String[] identifiers = new String[2];

        identifiers[0] = ByteBufUtil.hexDump(buf, buf.readerIndex(), 4);

        int b1 = buf.readUnsignedByte();
        int b2 = buf.readUnsignedByte() - 0x80;
        int b3 = buf.readUnsignedByte() - 0x80;
        int b4 = buf.readUnsignedByte();
        identifiers[1] = String.format("%02d%02d%02d%02d", b1, b2, b3, b4);

        return identifiers;
    }

    public static final int MSG_LOGIN = 0xB1;
    public static final int MSG_CONFIRMATION = 0x21;
    public static final int MSG_ON_DEMAND = 0x81;
    public static final int MSG_POSITION_UPLOAD = 0x80;
    public static final int MSG_POSITION_REUPLOAD = 0x8E;
    public static final int MSG_ALARM = 0x82;
    public static final int MSG_ADMIN_NUMBER = 0x83;
    public static final int MSG_SEND_TEXT = 0x84;
    public static final int MSG_REPLY = 0x85;
    public static final int MSG_SMS_ALARM_SWITCH = 0x86;
    public static final int MSG_PERIPHERAL = 0xA3;
    public static final int MSG_TIME_APPLY = 0xD6;
    public static final int MSG_TIME_RESPONSE = 0xD7;
    public static final int MSG_PARAM_APPLY = 0xD8;

    private void sendResponse(Channel channel, SocketAddress remoteAddress, ByteBuf buf, int type) {
        ByteBuf response = Unpooled.buffer();
        response.writeByte(0x29);
        response.writeByte(0x29); // header
        if (type == MSG_TIME_APPLY) {
            response.writeByte(MSG_TIME_RESPONSE);
            response.writeShort(13); // size
            response.writeBytes(buf, buf.readerIndex(), 4); // device address
            response.writeBytes(DataConverter.parseHex(DATE_FORMAT.format(Instant.now())));
            response.writeByte(0); // reserved
        } else {
            response.writeByte(MSG_CONFIRMATION);
            response.writeShort(5); // size
            response.writeByte(buf.getByte(buf.writerIndex() - 2));
            response.writeByte(type);
            response.writeByte(buf.writerIndex() > 9 ? buf.getByte(9) : 0); // 10th byte
        }
        response.writeByte(Checksum.xor(response.nioBuffer()));
        response.writeByte(0x0D); // ending

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private void decodeAlarmStatus(Position position, byte[] status) {
        if (BitUtil.check(status[0], 4)) {
            position.addAlarm(Position.ALARM_LOW_POWER);
        } else if (BitUtil.check(status[0], 6)) {
            position.addAlarm(Position.ALARM_GEOFENCE_EXIT);
        } else if (BitUtil.check(status[0], 7)) {
            position.addAlarm(Position.ALARM_GEOFENCE_ENTER);
        } else if (BitUtil.check(status[1], 0)) {
            position.addAlarm(Position.ALARM_SOS);
        } else if (BitUtil.check(status[1], 1)) {
            position.addAlarm(Position.ALARM_OVERSPEED);
        } else if (BitUtil.check(status[1], 3)) {
            position.addAlarm(Position.ALARM_POWER_CUT);
        } else if (BitUtil.check(status[1], 6)) {
            position.addAlarm(Position.ALARM_TOW);
        } else if (BitUtil.check(status[1], 7)) {
            position.addAlarm(Position.ALARM_DOOR);
        } else if (BitUtil.check(status[2], 2)) {
            position.addAlarm(Position.ALARM_TEMPERATURE);
        } else if (BitUtil.check(status[2], 4)) {
            position.addAlarm(Position.ALARM_TAMPERING);
        } else if (BitUtil.check(status[2], 6)) {
            position.addAlarm(Position.ALARM_FATIGUE_DRIVING);
        } else if (BitUtil.check(status[2], 7)) {
            position.addAlarm(Position.ALARM_IDLE);
        } else if (BitUtil.check(status[6], 3)) {
            position.addAlarm(Position.ALARM_VIBRATION);
        } else if (BitUtil.check(status[6], 4)) {
            position.addAlarm(Position.ALARM_BRAKING);
        } else if (BitUtil.check(status[6], 5)) {
            position.addAlarm(Position.ALARM_ACCELERATION);
        } else if (BitUtil.check(status[6], 6)) {
            position.addAlarm(Position.ALARM_CORNERING);
        } else if (BitUtil.check(status[6], 7)) {
            position.addAlarm(Position.ALARM_ACCIDENT);
        }
    }

    private void decodeExtendedData(Position position, ByteBuf buf) {
        int endIndex = buf.writerIndex() - 2; // checksum and ending
        Network network = new Network();
        while (buf.readerIndex() < endIndex) {
            int length = buf.readUnsignedShort();
            int type = buf.readUnsignedShort();
            int dataEnd = buf.readerIndex() + length - 2;
            switch (type) {
                case 0x0004 -> position.set(Position.PREFIX_ADC + 1, BcdUtil.readInteger(buf, 6) / 100000.0);
                case 0x0008 -> position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedShort() * 100 / 1500);
                case 0x0024 -> {
                    String[] values = BufferUtil.readString(buf, length - 2).split(";");
                    network.addCellTower(CellTower.from(
                            Integer.parseInt(values[0]), Integer.parseInt(values[1]),
                            Integer.parseInt(values[2]), Long.parseLong(values[3])));
                }
                case 0x0088 -> position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                case 0x0089 -> {
                    if (!BitUtil.check(buf.readUnsignedInt(), 12)) {
                        position.addAlarm(Position.ALARM_TAMPERING);
                    }
                }
                case 0x00A3 -> position.set("debug", BufferUtil.readString(buf, length - 2).trim());
                case 0x00A5 -> position.set("model", String.format("%08X", buf.readUnsignedInt()));
                case 0x00A8 -> position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                case 0x00A9 -> {
                    int mcc = buf.readUnsignedShort();
                    int mnc = buf.readUnsignedByte();
                    int count = buf.readUnsignedByte();
                    for (int i = 0; i < count; i++) {
                        network.addCellTower(CellTower.from(
                                mcc, mnc, buf.readUnsignedShort(),
                                buf.readUnsignedShort(), buf.readUnsignedByte()));
                    }
                }
                case 0x00AA -> {
                    position.set(Position.KEY_IGNITION, buf.readUnsignedByte() == 1);
                    position.set(Position.KEY_G_SENSOR, buf.readUnsignedByte() == 1);
                    buf.skipBytes(12); // reporting intervals and mode durations
                    String[] values = BufferUtil.readString(buf, length - 16).split(";");
                    int mcc = Integer.parseInt(values[0]);
                    int mnc = Integer.parseInt(values[1]);
                    int count = Integer.parseInt(values[2]);
                    for (int i = 0; i < count; i++) {
                        network.addCellTower(CellTower.from(
                                mcc, mnc, Integer.parseInt(values[3 + i * 2]),
                                Long.parseLong(values[4 + i * 2])));
                    }
                }
                case 0x00B3 -> {
                    boolean bluetooth = buf.readUnsignedByte() == 0;
                    int alarm = buf.readUnsignedShort();
                    buf.readUnsignedShort(); // status
                    if (bluetooth && !BitUtil.check(alarm, 1)) {
                        position.addAlarm(Position.ALARM_TAMPERING);
                    }
                }
                case 0x00B9 -> {
                    int count = buf.readUnsignedByte();
                    String[] values = BufferUtil.readString(buf, length - 3).split(",");
                    for (int i = 0; i < count; i++) {
                        network.addWifiAccessPoint(WifiAccessPoint.from(
                                values[i * 2].trim(), Integer.parseInt(values[i * 2 + 1].trim())));
                    }
                }
                default -> {
                }
            }
            buf.readerIndex(dataEnd);
        }
        if (network.getCellTowers() != null || network.getWifiAccessPoints() != null) {
            position.setNetwork(network);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // header
        int type = buf.readUnsignedByte();
        buf.readUnsignedShort(); // size

        if (type == MSG_LOGIN || type == MSG_ADMIN_NUMBER || type == MSG_SEND_TEXT
                || type == MSG_SMS_ALARM_SWITCH || type == MSG_POSITION_REUPLOAD || type == MSG_TIME_APPLY
                || channel instanceof DatagramChannel && (type == MSG_POSITION_UPLOAD || type == MSG_REPLY)) {
            sendResponse(channel, remoteAddress, buf, type);
        }

        if (type == MSG_PARAM_APPLY) {
            getDeviceSession(channel, remoteAddress, readIdentifiers(buf));
            return null;
        }

        if (type == MSG_ON_DEMAND || type == MSG_POSITION_UPLOAD || type == MSG_POSITION_REUPLOAD
                || type == MSG_ALARM || type == MSG_REPLY || type == MSG_PERIPHERAL || type == MSG_TIME_APPLY) {

            Position position = new Position(getProtocolName());

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, readIdentifiers(buf));
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setYear(BcdUtil.readInteger(buf, 2))
                    .setMonth(BcdUtil.readInteger(buf, 2))
                    .setDay(BcdUtil.readInteger(buf, 2))
                    .setHour(BcdUtil.readInteger(buf, 2))
                    .setMinute(BcdUtil.readInteger(buf, 2))
                    .setSecond(BcdUtil.readInteger(buf, 2));
            position.setTime(dateBuilder.getDate());

            position.setLatitude(BcdUtil.readCoordinate(buf));
            position.setLongitude(BcdUtil.readCoordinate(buf));
            position.setSpeed(UnitsConverter.knotsFromKph(BcdUtil.readInteger(buf, 4)));
            position.setCourse(BcdUtil.readInteger(buf, 4));
            position.setValid((buf.readUnsignedByte() & 0x80) != 0);

            if (type != MSG_ALARM) {

                int odometer = buf.readUnsignedMedium();
                if (BitUtil.to(odometer, 16) > 0) {
                    position.set(Position.KEY_ODOMETER, odometer);
                } else if (odometer > 0) {
                    position.set(Position.KEY_FUEL, BitUtil.from(odometer, 16));
                }

                long status = buf.readUnsignedInt();
                position.set(Position.KEY_IGNITION, !BitUtil.check(status, 7 + 3 * 8));
                position.set(Position.KEY_STATUS, status);

                buf.readUnsignedShort();
                buf.readUnsignedByte();
                buf.readUnsignedByte();
                buf.readUnsignedByte();
                buf.readUnsignedByte();
                buf.readUnsignedByte();

                int command = buf.readUnsignedByte();
                if (command != 0 && command != MSG_CONFIRMATION) {
                    position.set(Position.KEY_RESULT, String.valueOf(command));
                }

                if (type == MSG_PERIPHERAL) {

                    buf.readUnsignedShort(); // data length

                    int dataType = buf.readUnsignedByte();
                    int dataLength = buf.readUnsignedByte();

                    switch (dataType) {
                        case 0x01 -> position.set(Position.KEY_FUEL,
                                buf.readUnsignedByte() * 100 + buf.readUnsignedByte());
                        case 0x02 -> position.set(Position.PREFIX_TEMP + 1,
                                buf.readUnsignedByte() * 100 + buf.readUnsignedByte());
                        case 0x05 -> {
                            int sign = buf.readUnsignedByte();
                            switch (sign) {
                                case 1:
                                    position.set("sign", true);
                                    break;
                                case 2:
                                    position.set("sign", false);
                                    break;
                                default:
                                    break;
                            }
                            position.set(Position.KEY_DRIVER_UNIQUE_ID, BufferUtil.readString(buf, dataLength - 1));
                        }
                        case 0x18 -> {
                            for (int i = 1; i <= 4; i++) {
                                double value = buf.readUnsignedShort();
                                if (value > 0x0000 && value < 0xFFFF) {
                                    position.set("fuel" + i, value / 0xFFFE);
                                }
                            }
                        }
                        case 0x20 -> position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                        case 0x23 -> {
                            Network network = new Network();
                            int count = buf.readUnsignedByte();
                            for (int i = 0; i < count; i++) {
                                network.addCellTower(CellTower.from(
                                        buf.readUnsignedShort(), buf.readUnsignedByte(),
                                        buf.readUnsignedShort(), buf.readUnsignedShort(), buf.readUnsignedByte()));
                            }
                            if (count > 0) {
                                position.setNetwork(network);
                            }
                        }
                    }

                }

            } else {

                buf.readUnsignedByte(); // overloaded state
                buf.readUnsignedByte(); // logging status

                byte[] alarmStatus = new byte[8];
                buf.readBytes(alarmStatus);

                decodeAlarmStatus(position, alarmStatus);

            }

            if (type == MSG_POSITION_UPLOAD || type == MSG_REPLY || type == MSG_TIME_APPLY) {
                decodeExtendedData(position, buf);
            }

            return position;

        }

        return null;
    }

}
