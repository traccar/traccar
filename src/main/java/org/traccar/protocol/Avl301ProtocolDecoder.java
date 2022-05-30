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
import org.traccar.helper.DateBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class Avl301ProtocolDecoder extends BaseProtocolDecoder {

    public Avl301ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private String readImei(ByteBuf buf) {
        int b = buf.readUnsignedByte();
        StringBuilder imei = new StringBuilder();
        imei.append(b & 0x0F);
        for (int i = 0; i < 7; i++) {
            b = buf.readUnsignedByte();
            imei.append((b & 0xF0) >> 4);
            imei.append(b & 0x0F);
        }
        return imei.toString();
    }

    public static final int MSG_LOGIN = 'L';
    public static final int MSG_STATUS = 'H';
    public static final int MSG_GPS_LBS_STATUS = '$';

    private void sendResponse(Channel channel, int type) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer(5);
            response.writeByte('$');
            response.writeByte(type);
            response.writeByte('#');
            response.writeByte('\r'); response.writeByte('\n');
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(1); // header
        int type = buf.readUnsignedByte();
        buf.readUnsignedByte(); // length

        if (type == MSG_LOGIN) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, readImei(buf));
            if (deviceSession == null) {
                sendResponse(channel, type);
            }

        } else if (type == MSG_STATUS) {

            sendResponse(channel, type);

        } else if (type == MSG_GPS_LBS_STATUS) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
            position.setTime(dateBuilder.getDate());

            int gpsLength = buf.readUnsignedByte(); // gps len and sat
            position.set(Position.KEY_SATELLITES, gpsLength & 0xf);

            position.set(Position.KEY_SATELLITES_VISIBLE, buf.readUnsignedByte()); // satellites

            double latitude = buf.readUnsignedInt() / 600000.0;
            double longitude = buf.readUnsignedInt() / 600000.0;
            position.setSpeed(buf.readUnsignedByte());

            int union = buf.readUnsignedShort(); // course and flags
            position.setCourse(union & 0x03FF);
            position.setValid((union & 0x1000) != 0);
            if ((union & 0x0400) != 0) {
                latitude = -latitude;
            }
            if ((union & 0x0800) != 0) {
                longitude = -longitude;
            }

            position.setLatitude(latitude);
            position.setLongitude(longitude);

            if ((union & 0x4000) != 0) {
                position.set("acc", (union & 0x8000) != 0);
            }

            position.setNetwork(new Network(
                    CellTower.fromLacCid(getConfig(), buf.readUnsignedShort(), buf.readUnsignedMedium())));

            position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
            int flags = buf.readUnsignedByte();
            position.set("acc", (flags & 0x2) != 0);

            // parse other flags

            position.set(Position.KEY_POWER, buf.readUnsignedByte());
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());

            return position;
        }

        return null;
    }

}
