/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class ThinkRaceProtocolDecoder extends BaseProtocolDecoder {

    public ThinkRaceProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 0x80;
    public static final int MSG_GPS = 0x90;

    private static double convertCoordinate(long raw, boolean negative) {
        long degrees = raw / 1000000;
        double minutes = (raw % 1000000) * 0.0001;
        double result = degrees + minutes / 60;
        if (negative) {
            result = -result;
        }
        return result;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // header
        ByteBuf id = buf.readSlice(12);
        buf.readUnsignedByte(); // separator
        int type = buf.readUnsignedByte();
        buf.readUnsignedShort(); // length

        if (type == MSG_LOGIN) {

            int command = buf.readUnsignedByte(); // 0x00 - heartbeat

            if (command == 0x01) {
                String imei = buf.toString(buf.readerIndex(), 15, StandardCharsets.US_ASCII);
                DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
                if (deviceSession != null && channel != null) {
                    ByteBuf response = Unpooled.buffer();
                    response.writeByte(0x48); response.writeByte(0x52); // header
                    response.writeBytes(id);
                    response.writeByte(0x2c); // separator
                    response.writeByte(type);
                    response.writeShort(0x0002); // length
                    response.writeShort(0x8000);
                    response.writeShort(0x0000); // checksum
                    channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
                }
            }

        } else if (type == MSG_GPS) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setTime(new Date(buf.readUnsignedInt() * 1000));

            int flags = buf.readUnsignedByte();

            position.setValid(true);
            position.setLatitude(convertCoordinate(buf.readUnsignedInt(), !BitUtil.check(flags, 0)));
            position.setLongitude(convertCoordinate(buf.readUnsignedInt(), !BitUtil.check(flags, 1)));

            position.setSpeed(buf.readUnsignedByte());
            position.setCourse(buf.readUnsignedByte());

            position.setNetwork(new Network(
                    CellTower.fromLacCid(getConfig(), buf.readUnsignedShort(), buf.readUnsignedShort())));

            return position;

        }

        return null;
    }

}
