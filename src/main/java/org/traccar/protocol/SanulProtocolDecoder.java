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
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class SanulProtocolDecoder extends BaseProtocolDecoder {

    public static final int MSG_LOGIN = 0;
    public static final int MSG_LOCATION = 1;
    public static final int MSG_RESPONSE = 5;

    public SanulProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, int type) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0xaa); // header
            response.writeShortLE(0x85da); // reserved
            response.writeShortLE(15); // length
            response.writeByte(1); // edition
            response.writeShortLE(MSG_RESPONSE);
            response.writeShortLE(type);
            response.writeIntLE(0); // command id
            response.writeByte(0); // status
            response.writeByte(0); // result length
            response.writeIntLE(0x20000); // result data ?
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // header
        buf.readUnsignedShortLE(); // reserved
        buf.readUnsignedShortLE(); // length
        buf.readUnsignedByte(); // edition

        int type = buf.readUnsignedShortLE();

        buf.readUnsignedIntLE(); // command id

        sendResponse(channel, type);

        if (type == MSG_LOGIN) {

            getDeviceSession(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII).trim());

        } else if (type == MSG_LOCATION) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            return position;

        }

        return null;
    }

}
