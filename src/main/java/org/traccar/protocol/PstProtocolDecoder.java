/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;

public class PstProtocolDecoder extends BaseProtocolDecoder {

    public PstProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_STATUS = 0x05;

    private Date readDate(ByteBuf buf) {
        long value = buf.readUnsignedInt();
        return new DateBuilder()
                .setYear((int) BitUtil.between(value, 26, 32))
                .setMonth((int) BitUtil.between(value, 22, 26))
                .setDay((int) BitUtil.between(value, 17, 22))
                .setMonth((int) BitUtil.between(value, 12, 17))
                .setMonth((int) BitUtil.between(value, 6, 12))
                .setMonth((int) BitUtil.between(value, 0, 6)).getDate();
    }

    private double readCoordinate(ByteBuf buf) {
        long value = buf.readUnsignedInt();
        return (BitUtil.from(value, 16) + BitUtil.to(value, 16) * 0.00001) / 60;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        String id = String.valueOf(buf.readUnsignedInt());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        buf.readUnsignedByte(); // version
        buf.readUnsignedInt(); // index

        int type = buf.readUnsignedByte();

        if (type == MSG_STATUS) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setDeviceTime(readDate(buf));

            buf.readUnsignedByte();

            int count = buf.readUnsignedByte();
            for (int i = 0; i < count; i++) {

                int tag = buf.readUnsignedByte();
                int length = buf.readUnsignedByte();

                switch (tag) {
                    case 0x0D:
                        position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte() * 5);
                        break;
                    case 0x10:
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

            return position;
        }

        return null;
    }

}
