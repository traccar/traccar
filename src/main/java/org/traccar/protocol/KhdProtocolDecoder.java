/*
 * Copyright 2014 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.DeviceSession;
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

public class KhdProtocolDecoder extends BaseProtocolDecoder {

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

    private void decodeAlarmStatus(Position position, byte[] status) {
        if (BitUtil.check(status[0], 4)) {
            position.set(Position.KEY_ALARM, Position.ALARM_LOW_POWER);
        } else if (BitUtil.check(status[0], 6)) {
            position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE_EXIT);
        } else if (BitUtil.check(status[0], 7)) {
            position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE_ENTER);
        } else if (BitUtil.check(status[1], 0)) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        } else if (BitUtil.check(status[1], 1)) {
            position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
        } else if (BitUtil.check(status[1], 3)) {
            position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
        } else if (BitUtil.check(status[1], 6)) {
            position.set(Position.KEY_ALARM, Position.ALARM_TOW);
        } else if (BitUtil.check(status[1], 7)) {
            position.set(Position.KEY_ALARM, Position.ALARM_DOOR);
        } else if (BitUtil.check(status[2], 2)) {
            position.set(Position.KEY_ALARM, Position.ALARM_TEMPERATURE);
        } else if (BitUtil.check(status[2], 4)) {
            position.set(Position.KEY_ALARM, Position.ALARM_TAMPERING);
        }  else if (BitUtil.check(status[2], 6)) {
            position.set(Position.KEY_ALARM, Position.ALARM_FATIGUE_DRIVING);
        } else if (BitUtil.check(status[2], 7)) {
            position.set(Position.KEY_ALARM, Position.ALARM_IDLE);
        } else if (BitUtil.check(status[6], 3)) {
            position.set(Position.KEY_ALARM, Position.ALARM_VIBRATION);
        } else if (BitUtil.check(status[6], 4)) {
            position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
        } else if (BitUtil.check(status[6], 5)) {
            position.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
        } else if (BitUtil.check(status[6], 6)) {
            position.set(Position.KEY_ALARM, Position.ALARM_CORNERING);
        } else if (BitUtil.check(status[6], 7)) {
            position.set(Position.KEY_ALARM, Position.ALARM_ACCIDENT);
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
                || type == MSG_SMS_ALARM_SWITCH || type == MSG_POSITION_REUPLOAD) {

            ByteBuf response = Unpooled.buffer();
            response.writeByte(0x29);
            response.writeByte(0x29); // header
            response.writeByte(MSG_CONFIRMATION);
            response.writeShort(5); // size
            response.writeByte(buf.getByte(buf.writerIndex() - 2));
            response.writeByte(type);
            response.writeByte(buf.writerIndex() > 9 ? buf.getByte(9) : 0); // 10th byte
            response.writeByte(Checksum.xor(response.nioBuffer()));
            response.writeByte(0x0D); // ending

            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }

        }

        if (type == MSG_ON_DEMAND || type == MSG_POSITION_UPLOAD || type == MSG_POSITION_REUPLOAD
                || type == MSG_ALARM || type == MSG_REPLY || type == MSG_PERIPHERAL) {

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

                position.set(Position.KEY_ODOMETER, buf.readUnsignedMedium());
                position.set(Position.KEY_STATUS, buf.readUnsignedInt());

                buf.readUnsignedShort();
                buf.readUnsignedByte();
                buf.readUnsignedByte();
                buf.readUnsignedByte();
                buf.readUnsignedByte();
                buf.readUnsignedByte();

                position.set(Position.KEY_RESULT, buf.readUnsignedByte());

                if (type == MSG_PERIPHERAL) {

                    buf.readUnsignedShort(); // data length

                    int dataType = buf.readUnsignedByte();

                    buf.readUnsignedByte(); // content length

                    switch (dataType) {
                        case 0x01:
                            position.set(Position.KEY_FUEL_LEVEL,
                                    buf.readUnsignedByte() * 100 + buf.readUnsignedByte());
                            break;
                        case 0x02:
                            position.set(Position.PREFIX_TEMP + 1,
                                    buf.readUnsignedByte() * 100 + buf.readUnsignedByte());
                            break;
                        case 0x18:
                            for (int i = 1; i <= 4; i++) {
                                double value = buf.readUnsignedShort();
                                if (value > 0x0000 && value < 0xFFFF) {
                                    position.set("fuel" + i, value / 0xFFFE);
                                }
                            }
                            break;
                        case 0x23:
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
                            break;
                        default:
                            break;
                    }

                }

            }  else {

                buf.readUnsignedByte(); // overloaded state
                buf.readUnsignedByte(); // logging status

                byte[] alarmStatus = new byte[8];
                buf.readBytes(alarmStatus);

                decodeAlarmStatus(position, alarmStatus);

            }

            return position;

        }

        return null;
    }

}
