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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Main;
import org.traccar.helper.*;
import org.traccar.model.Command;
import org.traccar.model.Event;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;


public class GatorProtocolDecoder extends BaseProtocolDecoder {

    public GatorProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

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

        if (type == MSG_RFID_DATA){
            Position position = new Position(getProtocolName());

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, "1" + id, id);
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());

            int _sub_signal = buf.readUnsignedByte();

            if (_sub_signal != 0x02){
                return null;
            }

            StringBuilder RFID_Data = new StringBuilder();
            // read the RFID data from the buffer until the 0x0D is found
            while (buf.readableBytes() > 0) {
                int _byte = buf.readUnsignedByte();
                if (_byte == 0x0D) {
                    break;
                }
                // append the byte as a character to the RFID data
                RFID_Data.append((char) _byte);
            }

            LOGGER.info("RFID ID:" + RFID_Data);

            position.set(Position.KEY_DRIVER_UNIQUE_ID , RFID_Data.toString());

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
        }
        else if (type == MSG_POSITION_DATA || type == MSG_ROLLCALL_RESPONSE
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
            position.set("key", buf.readUnsignedByte());

            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedByte() + buf.readUnsignedByte() * 0.01);
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedByte() + buf.readUnsignedByte() * 0.01);

            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

            // 2 byte - Temperature
            // 0x00 0x1E= 30℃
            // 0x01 0x1E= -30℃

            if (type == MSG_ALARM_DATA || type  == MSG_POSITION_DATA){
                int _temperature_sign_pos = buf.readUnsignedByte();
                int _temperature_pos = buf.readUnsignedByte();

                // 0xFF means there is no temperature sensor detected
                if (_temperature_pos != 0xFF){
                    if (_temperature_sign_pos == 0x00){

                        position.set(Position.PREFIX_TEMP + 1, _temperature_pos);
                    }
                    else if (_temperature_sign_pos == 0x01){
                        position.set(Position.PREFIX_TEMP + 1, -_temperature_pos);
                    }
                }
            }
            if (type == MSG_ALARM_DATA){
                // Alarm Data - 2 Bytes
                int _alarm_data_b1 = buf.readUnsignedByte();
                int _alarm_data_b2 = buf.readUnsignedByte();

                // Bitmask and Set Position Keys
                if ((_alarm_data_b1 & 0b00100000) >= 1) position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE_EXIT);
                if ((_alarm_data_b1 & 0b00000001) >= 1) position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE_ENTER);

                // Bitmask and Set Position Keys
                if ((_alarm_data_b2 & 0b10000000) >= 1) position.set(Position.KEY_ALARM, Position.ALARM_DOOR);
                if ((_alarm_data_b2 & 0b00100000) >= 1) position.set(Position.KEY_ALARM, Position.ALARM_VIBRATION);
                if ((_alarm_data_b2 & 0b00001000) >= 1) position.set(Position.KEY_ALARM, Position.ALARM_POWER_OFF);
                if ((_alarm_data_b2 & 0b00000100) >= 1) position.set(Position.KEY_ALARM, Position.ALARM_PARKING);
                if ((_alarm_data_b2 & 0b00000010) >= 1) position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
                if ((_alarm_data_b2 & 0b00000001) >= 1) position.set(Position.KEY_ALARM, Position.ALARM_SOS);
            }
            else if (type == MSG_POSITION_DATA){
                // Note: M588FS Returned 120D after Temperature => Following Part is not used
                // May be Some Other Devices Would Use The Following Part of this Protocol

//                // 1 Byte - Rotation Sensor Status
//                // 0x00 = Stopped
//                // 0x01 = Stirring
//                // 0x02 = Unloading
//                buf.readUnsignedByte();
//
//                // 1 Byte - Reserved Byte
//                buf.readUnsignedByte();
//
//                // 1 Byte - Harsh Alarm Byte
//                // 0x01 = Harsh Acceleration
//                // 0x02 = Harsh Breaking
//                // 0x04 = Harsh Cornering
//                // 0x10 = Collision
//                // 0x20 = Rollover
//                int _harsh_alarm = buf.readUnsignedByte();
//
//                if (_harsh_alarm == 0x01){
//                    position.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
//                }
//                else if (_harsh_alarm == 0x02){
//                    position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
//                }
//                else if (_harsh_alarm == 0x04){
//                    position.set(Position.KEY_ALARM, Position.ALARM_CORNERING);
//                }
//                else if (_harsh_alarm == 0x10){
//                    position.set(Position.KEY_ALARM, Position.ALARM_ACCIDENT);
//                }
//                else if (_harsh_alarm == 0x20){
//                    position.set(Position.KEY_ALARM, Position.ALARM_ACCIDENT);
//                }
//
//                // 1 Byte - Sub Signal
//                // 0x03 - Temperature Data 2, 3, 4, Weight Sensor Value -> 8 Byte
//                // 0x06 - Packet Length, Temperature, Humidity
//
//                int _sub_signal = buf.readUnsignedByte();
//
//                if (_sub_signal == 0x03){
//                    for (int _index = 0; _index < 3; _index++){
//                        int _temperature_sign_sub = buf.readUnsignedByte();
//
//                        if (_temperature_sign_sub == 0x00){
//                            position.set(Position.PREFIX_TEMP + (_index + 2), buf.readUnsignedByte());
//                        }
//                        else if (_temperature_sign_sub == 0x01){
//                            position.set(Position.PREFIX_TEMP + (_index + 2), -buf.readUnsignedByte());
//                        }
//                    }
//
//                    // Read two bytes of Weight Sensor Value
//                    position.set(Position.KEY_AXLE_WEIGHT, buf.readUnsignedShort());
//                }
//                else if (_sub_signal == 0x06){
//                    // 1 Byte - Packet Length
//                    if (buf.readUnsignedByte() == 0x05) {
//
//                        // 3 Byte - Temperature
//                        int _temperature_sign_sub = buf.readUnsignedByte();
//
//                        if (_temperature_sign_sub == 0x00) {
//                            position.set(Position.PREFIX_TEMP + 5, buf.readUnsignedByte() + buf.readUnsignedByte() * 0.01);
//                        } else if (_temperature_sign_sub == 0x01) {
//                            position.set(Position.PREFIX_TEMP + 5, -(buf.readUnsignedByte() + buf.readUnsignedByte() * 0.01));
//                        }
//
//                        // 2 Byte - Humidity
//                        // No Key for Humidity
//                    }
//                }
            }

            return position;
        }
        else if (type == MSG_TERMINAL_ANSWER){
            Position position = new Position(getProtocolName());

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, "1" + id, id);
            if (deviceSession == null) {
                return null;
            }

            position.setDeviceId(deviceSession.getDeviceId());
            position.setTime(new Date());

            // Get Last Position and Set it to Current Position
            getLastLocation(position, null);

            // Main Order
            int _main_order = buf.readUnsignedByte();

            // Slave Order
//            int _slave_order = buf.readUnsignedByte();
            buf.readUnsignedByte();

            // Success (0x01) or Fail (0x00)
            int _success = buf.readUnsignedByte();

            // Reserved - 2 Bytes
            buf.readUnsignedShort();

            position.set(Position.KEY_EVENT, Event.TYPE_COMMAND_RESULT);
            position.setValid(false);

            // If Main Order -> 0x38 = Start Engine, 0x39 = Stop Engine
            if (_success == 0x01){
                if (_main_order == 0x38){
                    position.set(Position.KEY_COMMAND, Command.TYPE_ENGINE_RESUME);
                    position.set(Position.KEY_RESULT, "Engine Started");
                }
                else if (_main_order == 0x39){
                    position.set(Position.KEY_COMMAND, Command.TYPE_ENGINE_STOP);
                    position.set(Position.KEY_RESULT, "Engine Stopped");
                }
            }
            else {
                position.set(Position.KEY_RESULT, "Command Failed");
            }

            return position;
        }

        return null;
    }

}

/* Logs
*
24248000253c0d0733230414195650008307570765682900000000c0470100000c0f0000473000ff710d
2424210005718000d50d
24248000253c0d0733230414195720008307570765682900000000c0470100000c0f0000473600ff060d
2424210005068000a20d
24248000253c0d0733230414195750008307570765682900000000c0470100000c0f0000473a00ff7a0d
24242100057a8000de0d
24248000253c0d0733230414195820008307570765683000000000c0470100000c0f0000473d00ff1b0d
24242100051b8000bf0d
24248000253c0d0733230414195850008307570765682900000000c0470100000c0f0000474000ff0f0d
24242100050f8000ab0d
24248000253c0d0733230414195920008307580765682900000000c0470100000c0f0000474200ff730d
2424210005738000d70d
24248000253c0d0733230414195950008307580765683100000000c0470100000c0f0000474800ff110d
24248000253c0d0733230414200020008307590765683200000000c0470100000c0f0000474b00ff000d
2424210005008000a40d
24248000253c0d0733230414200050008307590765683200000000c0470100000c0f0000474d00ff760d
24248000253c0d0733230414200120008307590765683200000000c0470100000c0f0000475100ff1b0d
24242100051b8000bf0d
24248000253c0d0733230414200135008307590765683200000000c0470000000c0d0000475300ff0f0d
24242100050f8000ab0d
24248200273c0d0733230414200145008307590765683200000000c04700000000000000475300ff0008760d
2424210005768200d00d
24248000253c0d0733230414200149008307590765683200000000c04701000000000000475300ff730d
2424210005738000d70d
24247200203c0d07330242313631394143430d2a4123041420015600830758076568329f0d
24242100059f7200c90d
24247200203c0d07330231394143324630430d2a412304142002010083075807656831c90d
2424210005c972009f0d
24248000253c0d0733230414200219008307580765683200000000c0470100000c0d0000475500ff260d
2424210005268000820d
24248200273c0d0733230414200220008307580765683200000000c0470100000c0d0000475500ff00011e0d
24242100051e8200b80d
24248000253c0d0733230414200249008307580765683200000000c0470100000c0f0000475700ff760d
2424210005768000d20d
* */

