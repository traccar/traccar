/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class ObdDongleProtocolDecoder extends BaseProtocolDecoder {

    public ObdDongleProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_TYPE_CONNECT = 0x01;
    public static final int MSG_TYPE_CONNACK = 0x02;
    public static final int MSG_TYPE_PUBLISH = 0x03;
    public static final int MSG_TYPE_PUBACK = 0x04;
    public static final int MSG_TYPE_PINGREQ = 0x0C;
    public static final int MSG_TYPE_PINGRESP = 0x0D;
    public static final int MSG_TYPE_DISCONNECT = 0x0E;

    private static void sendResponse(Channel channel, int type, int index, String imei, ByteBuf content) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeShort(0x5555); // header
            response.writeShort(index);
            response.writeBytes(imei.getBytes(StandardCharsets.US_ASCII));
            response.writeByte(type);
            response.writeShort(content.readableBytes());
            response.writeBytes(content);
            content.release();
            response.writeByte(0); // checksum
            response.writeShort(0xAAAA);
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // header
        int index = buf.readUnsignedShort();

        String imei = buf.readSlice(15).toString(StandardCharsets.US_ASCII);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        int type = buf.readUnsignedByte();

        buf.readUnsignedShort(); // data length

        if (type == MSG_TYPE_CONNECT) {

            ByteBuf response = Unpooled.buffer();
            response.writeByte(1);
            response.writeShort(0);
            response.writeInt(0);
            sendResponse(channel, MSG_TYPE_CONNACK, index, imei, response);

        } else if (type == MSG_TYPE_PUBLISH) {

            int typeMajor = buf.readUnsignedByte();
            int typeMinor = buf.readUnsignedByte();

            buf.readUnsignedByte(); // event id

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setTime(new Date(buf.readUnsignedInt() * 1000));

            int flags = buf.readUnsignedByte();

            position.setValid(!BitUtil.check(flags, 6));

            position.set(Position.KEY_SATELLITES, BitUtil.to(flags, 4));

            double longitude = ((BitUtil.to(buf.readUnsignedShort(), 1) << 24) + buf.readUnsignedMedium()) * 0.00001;
            position.setLongitude(BitUtil.check(flags, 5) ? longitude : -longitude);

            double latitude = buf.readUnsignedMedium() * 0.00001;
            position.setLatitude(BitUtil.check(flags, 4) ? latitude : -latitude);

            int speedCourse = buf.readUnsignedMedium();
            position.setSpeed(UnitsConverter.knotsFromMph(BitUtil.from(speedCourse, 10) * 0.1));
            position.setCourse(BitUtil.to(speedCourse, 10));

            ByteBuf response = Unpooled.buffer();
            response.writeByte(typeMajor);
            response.writeByte(typeMinor);
            sendResponse(channel, MSG_TYPE_PUBACK, index, imei, response);

            return position;

        }

        return null;
    }

}
