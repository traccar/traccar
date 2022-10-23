/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;

public class ContinentalProtocolDecoder extends BaseProtocolDecoder {

    public ContinentalProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_KEEPALIVE = 0x00;
    public static final int MSG_STATUS = 0x02;
    public static final int MSG_ACK = 0x06;
    public static final int MSG_NACK = 0x15;

    private double readCoordinate(ByteBuf buf, boolean extended) {
        long value = buf.readUnsignedInt();
        if (extended ? (value & 0x08000000) != 0 : (value & 0x00800000) != 0) {
            value |= extended ? 0xF0000000 : 0xFF000000;
        }
        return (int) value / (extended ? 360000.0 : 3600.0);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // header
        buf.readUnsignedShort(); // length
        buf.readUnsignedByte(); // software version

        long serialNumber = buf.readUnsignedInt();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(serialNumber));
        if (deviceSession == null) {
            return null;
        }

        buf.readUnsignedByte(); // product

        int type = buf.readUnsignedByte();

        if (type == MSG_STATUS) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setFixTime(new Date(buf.readUnsignedInt() * 1000L));

            boolean extended = buf.getUnsignedByte(buf.readerIndex()) != 0;
            position.setLatitude(readCoordinate(buf, extended));
            position.setLongitude(readCoordinate(buf, extended));

            position.setCourse(buf.readUnsignedShort());
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

            position.setValid(buf.readUnsignedByte() > 0);

            position.setDeviceTime(new Date(buf.readUnsignedInt() * 1000L));

            position.set(Position.KEY_EVENT, buf.readUnsignedShort());

            int input = buf.readUnsignedShort();
            position.set(Position.KEY_IGNITION, BitUtil.check(input, 0));
            position.set(Position.KEY_INPUT, input);

            position.set(Position.KEY_OUTPUT, buf.readUnsignedShort());
            position.set(Position.KEY_BATTERY, buf.readUnsignedByte());
            position.set(Position.KEY_DEVICE_TEMP, buf.readByte());

            buf.readUnsignedShort(); // reserved

            if (buf.readableBytes() > 4) {
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
            }

            if (buf.readableBytes() > 4) {
                position.set(Position.KEY_HOURS, UnitsConverter.msFromHours(buf.readUnsignedInt()));
            }

            return position;

        }

        return null;
    }

}
