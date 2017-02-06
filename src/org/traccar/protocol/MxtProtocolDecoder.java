/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.ByteOrder;

public class MxtProtocolDecoder extends BaseProtocolDecoder {

    public MxtProtocolDecoder(MxtProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_ACK = 0x02;
    public static final int MSG_NACK = 0x03;
    public static final int MSG_POSITION = 0x31;

    private static void sendResponse(Channel channel, int device, long id, int crc) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 0);
            response.writeByte(0x01); // header
            response.writeByte(device);
            response.writeByte(MSG_ACK);
            response.writeInt((int) id);
            response.writeShort(crc);
            response.writeShort(Checksum.crc16(
                    Checksum.CRC16_XMODEM, response.toByteBuffer(1, response.readableBytes() - 1)));
            response.writeByte(0x04); // ending
            channel.write(response);

            ChannelBuffer encoded = ChannelBuffers.dynamicBuffer();
            while (response.readable()) {
                int b = response.readByte();
                if (response.readerIndex() != 1 && response.readableBytes() != 0
                        && (b == 0x01 || b == 0x04 || b == 0x10 || b == 0x11 || b == 0x13)) {
                    encoded.writeByte(0x10);
                    b += 0x20;
                }
                response.writeByte(b);
            }
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.readUnsignedByte(); // start
        int device = buf.readUnsignedByte(); // device descriptor
        int type = buf.readUnsignedByte();

        long id = buf.readUnsignedInt();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(id));
        if (deviceSession == null) {
            return null;
        }

        if (type == MSG_POSITION) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            buf.readUnsignedByte(); // protocol
            int infoGroups = buf.readUnsignedByte();

            position.set(Position.KEY_INDEX, buf.readUnsignedShort());

            DateBuilder dateBuilder = new DateBuilder().setDate(2000, 1, 1);

            long date = buf.readUnsignedInt();

            long days = BitUtil.from(date, 6 + 6 + 5);
            long hours = BitUtil.between(date, 6 + 6, 6 + 6 + 5);
            long minutes = BitUtil.between(date, 6, 6 + 6);
            long seconds = BitUtil.to(date, 6);

            dateBuilder.addMillis((((days * 24 + hours) * 60 + minutes) * 60 + seconds) * 1000);

            position.setTime(dateBuilder.getDate());

            position.setValid(true);
            position.setLatitude(buf.readInt() / 1000000.0);
            position.setLongitude(buf.readInt() / 1000000.0);

            long flags = buf.readUnsignedInt();
            position.set(Position.KEY_IGNITION, BitUtil.check(flags, 0));
            if (BitUtil.check(flags, 1)) {
                position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
            }
            position.set(Position.KEY_INPUT, BitUtil.between(flags, 2, 7));
            position.set(Position.KEY_OUTPUT, BitUtil.between(flags, 7, 10));
            position.setCourse(BitUtil.between(flags, 10, 13) * 45);
            //position.setValid(BitUtil.check(flags, 15));
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
                buf.readUnsignedByte(); // GPS accuracy
                position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                buf.readUnsignedShort(); // time since boot
                buf.readUnsignedByte(); // input voltage
                position.set(Position.PREFIX_TEMP + 1, buf.readByte());
            }

            if (BitUtil.check(infoGroups, 3)) {
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
            }

            if (BitUtil.check(infoGroups, 4)) {
                position.set(Position.KEY_HOURS, buf.readUnsignedInt());
            }

            if (BitUtil.check(infoGroups, 5)) {
                buf.readUnsignedInt(); // reason
            }

            if (BitUtil.check(infoGroups, 6)) {
                position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.001);
                position.set(Position.KEY_BATTERY, buf.readUnsignedShort());
            }

            if (BitUtil.check(infoGroups, 7)) {
                position.set(Position.KEY_RFID, buf.readUnsignedInt());
            }

            buf.readerIndex(buf.writerIndex() - 3);
            sendResponse(channel, device, id, buf.readUnsignedShort());

            return position;
        }

        return null;
    }

}
