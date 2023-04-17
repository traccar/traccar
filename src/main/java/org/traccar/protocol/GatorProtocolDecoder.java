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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.Checksum;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Command;
import org.traccar.model.Event;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Date;


public class GatorProtocolDecoder extends BaseProtocolDecoder {

    public GatorProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_HEARTBEAT = 0x21;
    public static final int MSG_POSITION_DATA = 0x80;
    public static final int MSG_ROLLCALL_RESPONSE = 0x81;
    public static final int MSG_ALARM_DATA = 0x82;
    public static final int MSG_TERMINAL_STATUS = 0x83;
    public static final int MSG_MESSAGE = 0x84;
    public static final int MSG_TERMINAL_ANSWER = 0x85;
    public static final int MSG_BLIND_AREA = 0x8E;
    public static final int MSG_PICTURE_FRAME = 0x54;
    public static final int MSG_CAMERA_RESPONSE = 0x56;
    public static final int MSG_PICTURE_DATA = 0x57;
    public static final int MSG_RFID_DATA = 0x72;


    public static String decodeId(int b1, int b2, int b3, int b4) {

        int d1 = 30 + ((b1 >> 7) << 3) + ((b2 >> 7) << 2) + ((b3 >> 7) << 1) + (b4 >> 7);
        int d2 = b1 & 0x7f;
        int d3 = b2 & 0x7f;
        int d4 = b3 & 0x7f;
        int d5 = b4 & 0x7f;

        return String.format("%02d%02d%02d%02d%02d", d1, d2, d3, d4, d5);
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, int type, int checksum) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeShort(0x2424); // header
            response.writeByte(MSG_HEARTBEAT);
            response.writeShort(5); // length
            response.writeByte(checksum);
            response.writeByte(type);
            response.writeByte(0); // subtype
            response.writeByte(Checksum.xor(response.nioBuffer(2, response.writerIndex())));
            response.writeByte(0x0D);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // header
        int type = buf.readUnsignedByte();
        buf.readUnsignedShort(); // length

        String id = decodeId(
                buf.readUnsignedByte(), buf.readUnsignedByte(),
                buf.readUnsignedByte(), buf.readUnsignedByte());

        sendResponse(channel, remoteAddress, type, buf.getByte(buf.writerIndex() - 2));

        if (type == MSG_RFID_DATA) {
            Position position = new Position(getProtocolName());

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, "1" + id, id);
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());

            int dataType = buf.readUnsignedByte();

            // RFID Data Type - 0x02
            if (dataType != 0x02) {
                return null;
            }

            StringBuilder rfidData = new StringBuilder();
            int rfidDataEndIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) 0x0D);

            if (rfidDataEndIndex != -1) {
                int length = rfidDataEndIndex - buf.readerIndex();
                rfidData.append(buf.toString(buf.readerIndex(), length, Charset.defaultCharset()));

                buf.skipBytes(length + 1);
            } else {
                return null;
            }

            position.set(Position.KEY_DRIVER_UNIQUE_ID, rfidData.toString());

            // Reserved - Skip 1 Byte
            buf.readUnsignedByte();

            int flags = buf.readUnsignedByte();
            position.setValid((flags & 0x41) != 0);

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

            return position;
        } else if (type == MSG_POSITION_DATA || type == MSG_ROLLCALL_RESPONSE
                || type == MSG_ALARM_DATA || type == MSG_BLIND_AREA) {

            Position position = new Position(getProtocolName());

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, "1" + id, id);
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

            int flags = buf.readUnsignedByte();
            position.setValid((flags & 0x80) != 0);
            position.set(Position.KEY_SATELLITES, flags & 0x0f);

            position.set(Position.KEY_STATUS, buf.readUnsignedByte());
            position.set(Position.KEY_IGNITION, buf.readUnsignedByte() == 0x01);

            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedByte() + buf.readUnsignedByte() * 0.01);
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedByte() + buf.readUnsignedByte() * 0.01);

            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

            if (type == MSG_ALARM_DATA || type  == MSG_POSITION_DATA) {
                int temperatureSignPosition = buf.readUnsignedByte();
                int temperaturePosition = buf.readUnsignedByte();

                if (temperaturePosition != 0xFF) {
                    if (temperatureSignPosition == 0x00) {
                        position.set(Position.PREFIX_TEMP + 1, temperaturePosition);
                    } else if (temperatureSignPosition == 0x01) {
                        position.set(Position.PREFIX_TEMP + 1, -temperaturePosition);
                    }
                }
            }
            if (type == MSG_ALARM_DATA) {
                int alarmDataByte1 = buf.readUnsignedByte();
                int alarmDataByte2 = buf.readUnsignedByte();

                if ((alarmDataByte1 & 0b00100000) >= 1) {
                    position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE_EXIT);
                }
                if ((alarmDataByte1 & 0b00000001) >= 1) {
                    position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE_ENTER);
                }

                if ((alarmDataByte2 & 0b10000000) >= 1) {
                    position.set(Position.KEY_ALARM, Position.ALARM_DOOR);
                }
                if ((alarmDataByte2 & 0b00100000) >= 1) {
                    position.set(Position.KEY_ALARM, Position.ALARM_VIBRATION);
                }
                if ((alarmDataByte2 & 0b00001000) >= 1) {
                    position.set(Position.KEY_ALARM, Position.ALARM_POWER_OFF);
                }
                if ((alarmDataByte2 & 0b00000100) >= 1) {
                    position.set(Position.KEY_ALARM, Position.ALARM_PARKING);
                }
                if ((alarmDataByte2 & 0b00000010) >= 1) {
                    position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
                }
                if ((alarmDataByte2 & 0b00000001) >= 1) {
                    position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                }
            }

            return position;
        } else if (type == MSG_TERMINAL_ANSWER) {
            Position position = new Position(getProtocolName());

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, "1" + id, id);
            if (deviceSession == null) {
                return null;
            }

            position.setDeviceId(deviceSession.getDeviceId());
            position.setTime(new Date());

            getLastLocation(position, null);

            int mainType = buf.readUnsignedByte();

            // Sub Type - Skip 1 Byte
            buf.readUnsignedByte();

            int commandExecuted = buf.readUnsignedByte();

            // Reserved - Skip 2 Bytes
            buf.readUnsignedShort();

            position.set(Position.KEY_EVENT, Event.TYPE_COMMAND_RESULT);
            position.setValid(false);

            if (commandExecuted == 0x01) {
                if (mainType == 0x38) {
                    position.set(Position.KEY_COMMAND, Command.TYPE_ENGINE_RESUME);
                    position.set(Position.KEY_RESULT, "Engine Started");
                } else if (mainType == 0x39) {
                    position.set(Position.KEY_COMMAND, Command.TYPE_ENGINE_STOP);
                    position.set(Position.KEY_RESULT, "Engine Stopped");
                }
            } else {
                position.set(Position.KEY_RESULT, "Command Failed");
            }

            return position;
        }

        return null;
    }

}

