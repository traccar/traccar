/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DateBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

public class UuxProtocolDecoder extends BaseProtocolDecoder {

    public UuxProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_IMMOBILIZER = 0x9E;
    public static final int MSG_ACK = 0xD0;
    public static final int MSG_NACK = 0xF0;

    private void sendResponse(Channel channel, int productCode, int protocolVersion, int type) {
        if (channel != null && BitUtil.check(protocolVersion, 7)) {
            ByteBuf response = Unpooled.buffer();
            response.writeShort(productCode);
            response.writeByte(BitUtil.to(protocolVersion, 7));
            response.writeByte(1); // length
            response.writeByte(type);
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int productCode = buf.readUnsignedShort();
        int protocolVersion = buf.readUnsignedByte();
        buf.readUnsignedByte(); // length
        int type = buf.readUnsignedByte();

        String vehicleId = buf.readCharSequence(10, StandardCharsets.US_ASCII).toString();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, vehicleId);
        if (deviceSession == null) {
            sendResponse(channel, productCode, protocolVersion, MSG_NACK);
            return null;
        }

        if (type == MSG_IMMOBILIZER) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(Calendar.getInstance().get(Calendar.YEAR), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());

            getLastLocation(position, dateBuilder.getDate());

            position.set("companyId", buf.readCharSequence(6, StandardCharsets.US_ASCII).toString());
            position.set("tripId", buf.readUnsignedShort());

            return position;

        }

        sendResponse(channel, productCode, protocolVersion, MSG_ACK);

        return null;
    }

}
