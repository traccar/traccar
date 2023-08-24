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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class MxtProtocolDecoder extends BaseProtocolDecoder {

    public MxtProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_ACK = 0x02;
    public static final int MSG_NACK = 0x03;
    public static final int MSG_POSITION = 0x31;

    private static void sendResponse(Channel channel, int device, long id, int crc) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(device);
            response.writeByte(MSG_ACK);
            response.writeIntLE((int) id);
            response.writeShortLE(crc);
            response.writeShortLE(Checksum.crc16(
                    Checksum.CRC16_XMODEM, response.nioBuffer()));

            ByteBuf encoded = Unpooled.buffer();
            encoded.writeByte(0x01); // header
            while (response.isReadable()) {
                int b = response.readByte();
                if (b == 0x01 || b == 0x04 || b == 0x10 || b == 0x11 || b == 0x13) {
                    encoded.writeByte(0x10);
                    b += 0x20;
                }
                encoded.writeByte(b);
            }
            response.release();
            encoded.writeByte(0x04); // ending
            channel.writeAndFlush(new NetworkMessage(encoded, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // start
        int device = buf.readUnsignedByte(); // device descriptor
        int type = buf.readUnsignedByte();

        long id = buf.readUnsignedIntLE();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(id));
        if (deviceSession == null) {
            return null;
        }

        if (type == MSG_POSITION) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            buf.readUnsignedByte(); // protocol
            int infoGroups = buf.readUnsignedByte();

            position.set(Position.KEY_INDEX, buf.readUnsignedShortLE());

            DateBuilder dateBuilder = new DateBuilder().setDate(2000, 1, 1);

            long date = buf.readUnsignedIntLE();

            long days = BitUtil.from(date, 6 + 6 + 5);
            if (days < 7 * 780) {
                days += 7 * 1024;
            }

            long hours = BitUtil.between(date, 6 + 6, 6 + 6 + 5);
            long minutes = BitUtil.between(date, 6, 6 + 6);
            long seconds = BitUtil.to(date, 6);

            dateBuilder.addMillis((((days * 24 + hours) * 60 + minutes) * 60 + seconds) * 1000);

            position.setTime(dateBuilder.getDate());

            position.setValid(true);
            position.setLatitude(buf.readIntLE() / 1000000.0);
            position.setLongitude(buf.readIntLE() / 1000000.0);

            long flags = buf.readUnsignedIntLE();
            position.set(Position.KEY_IGNITION, BitUtil.check(flags, 0));
            if (BitUtil.check(flags, 1)) {
                position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
            }
            position.set(Position.KEY_INPUT, BitUtil.between(flags, 2, 7));
            position.set(Position.KEY_OUTPUT, BitUtil.between(flags, 7, 10));
            position.setCourse(BitUtil.between(flags, 10, 13) * 45);
            // position.setValid(BitUtil.check(flags, 15));
            position.set(Position.KEY_CHARGE, BitUtil.check(flags, 20));

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));

            buf.readUnsignedByte(); // input mask

            if (BitUtil.check(infoGroups, 0)) {
                buf.skipBytes(8); // waypoints
            }

            if (BitUtil.check(infoGroups, 1)) {
                buf.skipBytes(8); // wireless accessory
            }

            if (BitUtil.check(infoGroups, 2)) {
                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                position.set(Position.KEY_HDOP, buf.readUnsignedByte());
                position.setAccuracy(buf.readUnsignedByte());
                position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                buf.readUnsignedShortLE(); // time since boot
                position.set(Position.KEY_POWER, buf.readUnsignedByte());
                position.set(Position.PREFIX_TEMP + 1, buf.readByte());
            }

            if (BitUtil.check(infoGroups, 3)) {
                position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
            }

            if (BitUtil.check(infoGroups, 4)) {
                position.set(Position.KEY_HOURS, UnitsConverter.msFromMinutes(buf.readUnsignedIntLE()));
            }

            if (BitUtil.check(infoGroups, 5)) {
                buf.readUnsignedIntLE(); // reason
            }

            if (BitUtil.check(infoGroups, 6)) {
                position.set(Position.KEY_POWER, buf.readUnsignedShortLE() * 0.001);
                position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE());
            }

            if (BitUtil.check(infoGroups, 7)) {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, String.valueOf(buf.readUnsignedIntLE()));
            }

            buf.readerIndex(buf.writerIndex() - 3);
            sendResponse(channel, device, id, buf.readUnsignedShortLE());

            return position;
        }

        return null;
    }

}
