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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.Date;

public class Xexun3ProtocolDecoder extends BaseProtocolDecoder {

    public Xexun3ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_DATA = 0x20;
    public static final int MSG_COMMAND = 0x21;

    private void sendResponse(Channel channel, int type, int index, ByteBuf imei) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0xFC);
            response.writeShort(12); // length
            response.writeByte(0x03); // version
            response.writeByte(type);
            response.writeByte(index);
            response.writeBytes(imei, imei.readerIndex(), 8);
            response.writeByte(0); // result
            response.writeShort(Checksum.crc16(
                    Checksum.CRC16_CCITT_FALSE, response.nioBuffer(3, 12)));
            response.writeByte(0xCF);
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    private String decodeAlarm(int value) {
        return switch (value) {
            case 1 -> Position.ALARM_SOS;
            case 2 -> Position.ALARM_LOW_BATTERY;
            case 4 -> Position.ALARM_REMOVING;
            case 5 -> Position.ALARM_MOVEMENT;
            case 7 -> Position.ALARM_FALL_DOWN;
            case 8 -> Position.ALARM_ACCIDENT;
            case 11 -> Position.ALARM_POWER_RESTORED;
            case 12 -> Position.ALARM_POWER_CUT;
            case 13 -> Position.ALARM_POWER_ON;
            case 14 -> Position.ALARM_POWER_OFF;
            case 15, 16 -> Position.ALARM_DOOR;
            default -> null;
        };
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // header
        int length = buf.readUnsignedShort();
        buf.readUnsignedByte(); // version
        int type = buf.readUnsignedByte();
        int index = buf.readUnsignedByte();

        ByteBuf imei = buf.readSlice(8);
        DeviceSession deviceSession = getDeviceSession(
                channel, remoteAddress, ByteBufUtil.hexDump(imei).substring(1));
        if (deviceSession == null) {
            return null;
        }

        if (type != MSG_COMMAND) {
            sendResponse(channel, type, index, imei);
        }

        if (type != MSG_DATA) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        Network network = new Network();
        boolean hasLocation = false;

        int bodyEnd = buf.readerIndex() + length - 11;
        while (buf.readerIndex() < bodyEnd) {
            int subType = buf.readUnsignedByte();
            int subLength = buf.readUnsignedByte();
            int subEnd = buf.readerIndex() + subLength;

            switch (subType) {
                case 0x64 -> {
                    position.setTime(new Date(buf.readUnsignedInt() * 1000));
                    position.setValid(true);
                    position.setLatitude(buf.readDouble());
                    position.setLongitude(buf.readDouble());
                    position.setAltitude(buf.readFloat());
                    buf.readUnsignedByte(); // ephemeris
                    position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                    buf.readUnsignedByte(); // signal
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
                    hasLocation = true;
                }
                case 0x65 -> {
                    position.setDeviceTime(new Date(buf.readUnsignedInt() * 1000));
                    int count = buf.readUnsignedByte();
                    for (int i = 0; i < count; i++) {
                        String mac = ByteBufUtil.hexDump(buf.readSlice(6)).replaceAll("(..)", "$1:");
                        network.addWifiAccessPoint(WifiAccessPoint.from(
                                mac.substring(0, mac.length() - 1), buf.readByte()));
                    }
                }
                case 0x66 -> {
                    position.setDeviceTime(new Date(buf.readUnsignedInt() * 1000));
                    network.addCellTower(CellTower.from(
                            buf.readUnsignedShort(),
                            buf.readUnsignedShort(),
                            buf.readInt(),
                            buf.readUnsignedInt(),
                            buf.readUnsignedByte()));
                }
                case 0x69 -> {
                    position.setDeviceTime(new Date(buf.readUnsignedInt() * 1000));
                    int alarmId = buf.readUnsignedByte();
                    int dataLength = buf.readUnsignedByte();
                    position.addAlarm(decodeAlarm(alarmId));
                    if (alarmId == 17 && dataLength > 0) {
                        position.set(Position.KEY_ICCID, ByteBufUtil.hexDump(buf.readSlice(dataLength)));
                    }
                }
                case 0x6A -> {
                    position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                    buf.readUnsignedShort(); // network duration
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    buf.readUnsignedByte(); // track sequence
                    position.set(Position.KEY_MOTION, buf.readUnsignedByte() > 0);
                    position.set(Position.KEY_CHARGE, buf.readUnsignedByte() > 0);
                    int steps = buf.readUnsignedShort();
                    if (steps != 0xFFFF) {
                        position.set(Position.KEY_STEPS, steps);
                    }
                    int temperature = buf.readByte();
                    if (temperature != -1) {
                        position.set("temperature", temperature);
                    }
                    buf.readUnsignedByte(); // respiratory rate
                    int heartRate = buf.readUnsignedByte();
                    if (heartRate != 0xFF) {
                        position.set(Position.KEY_HEART_RATE, heartRate);
                    }
                    buf.readUnsignedByte(); // blood pressure systolic
                    buf.readUnsignedByte(); // blood pressure diastolic
                    buf.readUnsignedByte(); // blood oxygen
                    int fuel = buf.readUnsignedByte();
                    if (fuel != 0xFF) {
                        position.set(Position.KEY_FUEL_LEVEL, fuel);
                    }
                }
                default -> {}
            }

            buf.readerIndex(subEnd);
        }

        if (network.getCellTowers() != null || network.getWifiAccessPoints() != null) {
            position.setNetwork(network);
        }

        if (position.getDeviceTime() == null) {
            return null;
        }

        if (!hasLocation) {
            getLastLocation(position, position.getDeviceTime());
        }

        return position;
    }

}
