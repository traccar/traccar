/*
 * Copyright 2019 - 2020 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;

public class PstProtocolDecoder extends BaseProtocolDecoder {

    public PstProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_ACK = 0x00;
    public static final int MSG_STATUS = 0x05;
    public static final int MSG_COMMAND = 0x06;

    private Date readDate(ByteBuf buf) {
        long value = buf.readUnsignedInt();
        return new DateBuilder()
                .setYear((int) BitUtil.between(value, 26, 32))
                .setMonth((int) BitUtil.between(value, 22, 26))
                .setDay((int) BitUtil.between(value, 17, 22))
                .setHour((int) BitUtil.between(value, 12, 17))
                .setMinute((int) BitUtil.between(value, 6, 12))
                .setSecond((int) BitUtil.between(value, 0, 6)).getDate();
    }

    private double readCoordinate(ByteBuf buf) {
        long value = buf.readUnsignedInt();
        int sign = BitUtil.check(value, 31) ? -1 : 1;
        value = BitUtil.to(value, 31);
        return sign * (BitUtil.from(value, 16) + BitUtil.to(value, 16) / 10000.0) / 60;
    }

    private void sendResponse(
            Channel channel, SocketAddress remoteAddress, long id, int version, long index, int type) {
        if (channel != null) {

            ByteBuf response = Unpooled.buffer();
            response.writeInt((int) id);
            response.writeByte(version);
            response.writeInt((int) index);
            response.writeByte(MSG_ACK);
            response.writeByte(type);
            response.writeShort(Checksum.crc16(Checksum.CRC16_XMODEM, response.nioBuffer()));

            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));

        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        long id = buf.readUnsignedInt();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(id));
        if (deviceSession == null) {
            return null;
        }

        int version = buf.readUnsignedByte();
        long index = buf.readUnsignedInt();

        int type = buf.readUnsignedByte();

        sendResponse(channel, remoteAddress, id, version, index, type);

        if (type == MSG_STATUS) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setDeviceTime(readDate(buf));

            int status = buf.readUnsignedByte();
            position.set(Position.KEY_BLOCKED, BitUtil.check(status, 4));
            position.set(Position.KEY_IGNITION, BitUtil.check(status, 7));
            position.set(Position.KEY_STATUS, status);

            int count = buf.readUnsignedByte();
            for (int i = 0; i < count; i++) {

                int tag = buf.readUnsignedByte();
                int length = buf.readUnsignedByte();

                switch (tag) {
                    case 0x09:
                        buf.readUnsignedByte(); // sensor count
                        buf.readUnsignedByte(); // sensor logic
                        buf.readUnsignedByte(); // sensor status
                        break;
                    case 0x0D:
                        int battery = buf.readUnsignedByte();
                        if (battery <= 20) {
                            position.set(Position.KEY_BATTERY_LEVEL, battery * 5);
                        }
                        break;
                    case 0x10:
                        position.setValid(true);
                        position.setFixTime(readDate(buf));
                        position.setLatitude(readCoordinate(buf));
                        position.setLongitude(readCoordinate(buf));
                        position.setSpeed(buf.readUnsignedByte());
                        position.setCourse(buf.readUnsignedByte() * 2);
                        position.setAltitude(buf.readShort());
                        buf.readUnsignedInt(); // gps condition
                        break;
                    default:
                        buf.skipBytes(length);
                        break;
                }
            }

            return position.getFixTime() != null ? position : null;
        }

        return null;
    }

}
