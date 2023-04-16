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
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

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
            LOGGER.info("============  RFID data ====== ");
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

            String RFID_Data = "";
            // read the RFID data from the buffer until the 0x0D is found
            while (buf.readableBytes() > 0) {
                int _byte = buf.readUnsignedByte();
                if (_byte == 0x0D) {
                    break;
                }
                // append the byte as a character to the RFID data
                RFID_Data += (char) _byte;
            }

            LOGGER.info("============  RFID data ========= " + RFID_Data);

            position.set(Position.KEY_DRIVER_UNIQUE_ID ,RFID_Data);

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

            return position;
        }

        return null;
    }

}
