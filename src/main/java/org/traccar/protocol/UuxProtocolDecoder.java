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
import org.traccar.session.DeviceSession;
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

    public static final int MSG_GENERAL = 0x90;
    public static final int MSG_IMMOBILIZER = 0x9E;
    public static final int MSG_ACK = 0xD0;
    public static final int MSG_NACK = 0xF0;
    public static final int MSG_KEEPALIVE = 0xFF;

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

    private int readInt(ByteBuf buf, int length) {
        return Integer.parseInt(buf.readCharSequence(length, StandardCharsets.US_ASCII).toString());
    }

    private double readDouble(ByteBuf buf, int length) {
        return Double.parseDouble(buf.readCharSequence(length, StandardCharsets.US_ASCII).toString());
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int productCode = buf.readUnsignedShort();
        int protocolVersion = buf.readUnsignedByte();
        buf.readUnsignedByte(); // length
        int type = buf.readUnsignedByte();

        if (type == MSG_KEEPALIVE) {
            return null;
        }

        String vehicleId = buf.readCharSequence(10, StandardCharsets.US_ASCII).toString();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, vehicleId);
        if (deviceSession == null) {
            sendResponse(channel, productCode, protocolVersion, MSG_NACK);
            return null;
        }

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(Calendar.getInstance().get(Calendar.YEAR), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());

        if (type == MSG_GENERAL) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setTime(dateBuilder.getDate());

            buf.skipBytes(10); // reason
            buf.readUnsignedShort(); // flags

            buf.readUnsignedByte(); // position status
            position.setValid(true);

            position.set(Position.KEY_SATELLITES, readInt(buf, 2));

            double latitude = readInt(buf, 2);
            latitude += readDouble(buf, 7) / 60;
            position.setLatitude(buf.readUnsignedByte() == 'S' ? -latitude : latitude);

            double longitude = readInt(buf, 3);
            longitude += readDouble(buf, 7) / 60;
            position.setLongitude(buf.readUnsignedByte() == 'W' ? -longitude : longitude);

            position.setSpeed(readInt(buf, 3));
            position.setCourse(readInt(buf, 3));
            readInt(buf, 3); // alternative speed

            position.set(Position.KEY_ODOMETER, buf.readUnsignedByte() * 10000 + buf.readUnsignedByte() * 256
                    + buf.readUnsignedByte() + buf.readUnsignedByte() * 0.1);
            position.set(Position.KEY_HOURS, buf.readUnsignedInt());
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());

            position.set("companyId", buf.readCharSequence(6, StandardCharsets.US_ASCII).toString());

            buf.skipBytes(10); // reason data

            position.set("tripId", buf.readUnsignedShort());

            return position;

        } else if (type == MSG_IMMOBILIZER) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, dateBuilder.getDate());

            position.set("companyId", buf.readCharSequence(6, StandardCharsets.US_ASCII).toString());
            position.set("tripId", buf.readUnsignedShort());

            return position;

        }

        sendResponse(channel, productCode, protocolVersion, MSG_ACK);

        return null;
    }

}
