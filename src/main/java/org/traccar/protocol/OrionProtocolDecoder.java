/*
 * Copyright 2014 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;

public class OrionProtocolDecoder extends BaseProtocolDecoder {

    public OrionProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_USERLOG = 0;
    public static final int MSG_SYSLOG = 3;

    private static void sendResponse(Channel channel, ByteBuf buf) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer(4);
            response.writeByte('*');
            response.writeShort(buf.getUnsignedShort(buf.writerIndex() - 2));
            response.writeByte(buf.getUnsignedByte(buf.writerIndex() - 3));
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    private static double convertCoordinate(int raw) {
        int degrees = raw / 1000000;
        double minutes = (raw % 1000000) / 10000.0;
        return degrees + minutes / 60;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // header
        int type = buf.readUnsignedByte() & 0x0f;

        if (type == MSG_USERLOG) {

            int header = buf.readUnsignedByte();

            if ((header & 0x40) != 0) {
                sendResponse(channel, buf);
            }

            DeviceSession deviceSession = getDeviceSession(
                    channel, remoteAddress, String.valueOf(buf.readUnsignedInt()));
            if (deviceSession == null) {
                return null;
            }

            List<Position> positions = new LinkedList<>();

            for (int i = 0; i < (header & 0x0f); i++) {

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                position.set(Position.KEY_EVENT, buf.readUnsignedByte());
                buf.readUnsignedByte(); // length
                position.set(Position.KEY_FLAGS, buf.readUnsignedShortLE());

                position.setLatitude(convertCoordinate(buf.readIntLE()));
                position.setLongitude(convertCoordinate(buf.readIntLE()));
                position.setAltitude(buf.readShortLE() / 10.0);
                position.setCourse(buf.readUnsignedShortLE());
                position.setSpeed(buf.readUnsignedShortLE() * 0.0539957);

                DateBuilder dateBuilder = new DateBuilder()
                        .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                        .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
                position.setTime(dateBuilder.getDate());

                int satellites = buf.readUnsignedByte();
                position.setValid(satellites >= 3);
                position.set(Position.KEY_SATELLITES, satellites);

                positions.add(position);
            }

            return positions;
        }

        return null;
    }

}
