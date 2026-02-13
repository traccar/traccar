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

public class BlueProtocolDecoder extends BaseProtocolDecoder {

    public BlueProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private double readCoordinate(ByteBuf buf, boolean negative) {

        int value = buf.readUnsignedShort();
        int degrees = value / 100;
        double minutes = value % 100 + buf.readUnsignedShort() * 0.0001;
        double coordinate = degrees + minutes / 60;
        return negative ? -coordinate : coordinate;
    }

    private void sendResponse(Channel channel, int deviceIndex) {
        if (channel != null) {

            ByteBuf response = Unpooled.buffer();
            response.writeByte(0xaa);
            response.writeShort(2 + 1 + 1 + 6 + 1);
            response.writeByte(0x86); // version
            response.writeByte(0);

            response.writeByte(6); // data length
            response.writeByte(0xa4); // type
            response.writeByte(0); // server index
            response.writeByte(deviceIndex);
            response.writeByte(0);
            response.writeByte(0);

            response.writeByte(Checksum.xor(response.nioBuffer(1, response.writerIndex() - 1)));

            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    private String decodeAlarm(int value) {
        return switch (value) {
            case 1 -> Position.ALARM_SOS;
            case 8 -> Position.ALARM_OVERSPEED;
            case 19 -> Position.ALARM_LOW_POWER;
            default -> null;
        };
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // header
        buf.readUnsignedShort(); // length
        buf.readUnsignedByte(); // version
        buf.readUnsignedByte();

        String id = String.valueOf(buf.readUnsignedInt());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        while (buf.readableBytes() > 1) {

            int frameEnd = buf.readerIndex() + buf.readUnsignedByte();

            int type = buf.readUnsignedByte();
            int index = buf.readUnsignedByte();
            buf.readUnsignedByte();
            buf.readUnsignedByte(); // flags

            if (type == 0x01) {

                buf.readUnsignedByte(); // reserved
                int flags = buf.readUnsignedByte();

                position.setValid(BitUtil.check(flags, 7));
                position.setLatitude(readCoordinate(buf, BitUtil.check(flags, 6)));
                position.setLongitude(readCoordinate(buf, BitUtil.check(flags, 5)));
                position.setSpeed(buf.readUnsignedShort() + buf.readUnsignedShort() * 0.001);
                position.setCourse(buf.readUnsignedShort() + buf.readUnsignedByte() * 0.01);

                DateBuilder dateBuilder = new DateBuilder()
                        .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                        .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
                position.setTime(dateBuilder.getDate());

                buf.readUnsignedShort(); // lac
                buf.readUnsignedShort(); // cid

            } else if (type == 0x12) {

                int status;

                status = buf.readUnsignedByte(); // status 1
                position.addAlarm(BitUtil.check(status, 1) ? Position.ALARM_VIBRATION : null);

                buf.readUnsignedByte(); // status 2
                buf.readUnsignedByte(); // status 3

                status = buf.readUnsignedByte(); // status 4
                int ignition = BitUtil.between(status, 2, 4);
                if (ignition == 0b01) {
                    position.set(Position.KEY_IGNITION, false);
                }
                if (ignition == 0b10) {
                    position.set(Position.KEY_IGNITION, true);
                }

                buf.readUnsignedByte(); // status 5
                buf.readUnsignedByte(); // status 6

                position.set(Position.KEY_STATUS, buf.readUnsignedShort());

            } else if (type == 0x81) {

                position.addAlarm(decodeAlarm(buf.readUnsignedByte()));

            } else if (type == 0x84) {

                sendResponse(channel, index);

            }

            buf.readerIndex(frameEnd);
        }

        return position.getFixTime() != null ? position : null;
    }

}
