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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class NavisetProtocolDecoder extends BaseProtocolDecoder {

    public NavisetProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_HEADER = 0b00;
    public static final int MSG_DATA = 0b01;
    public static final int MSG_RESPONSE = 0b10;
    public static final int MSG_RESERVE = 0b11;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0x01);
            response.writeShortLE(buf.getUnsignedShortLE(buf.writerIndex() - 2));
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }

        int length = buf.readUnsignedShortLE();
        BitUtil.between(length, 12, 14); // version
        int type = BitUtil.between(length, 14, 16);
        buf.readUnsignedShortLE(); // device number

        if (type == MSG_HEADER) {

            getDeviceSession(channel, remoteAddress, buf.readCharSequence(15, StandardCharsets.US_ASCII).toString());

        } else if (type == MSG_DATA) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            buf.readUnsignedByte(); // mask
            position.set(Position.KEY_INDEX, buf.readUnsignedShortLE());
            position.set(Position.KEY_STATUS, buf.readUnsignedByte());

            position.setValid(true);
            position.setTime(new Date(buf.readUnsignedIntLE() * 1000));
            position.setLatitude(buf.readUnsignedIntLE() * 0.000001);
            position.setLongitude(buf.readUnsignedIntLE() * 0.000001);
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE() * 0.1));

            // additional data

            return position;
        }

        return null;
    }

}
