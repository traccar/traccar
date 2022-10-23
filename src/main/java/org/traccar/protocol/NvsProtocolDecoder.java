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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class NvsProtocolDecoder extends BaseProtocolDecoder {

    public NvsProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, String response) {
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    Unpooled.copiedBuffer(response, StandardCharsets.US_ASCII), remoteAddress));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;


        if (buf.getUnsignedByte(buf.readerIndex()) == 0) {

            buf.readUnsignedShort(); // length

            String imei = buf.toString(buf.readerIndex(), 15, StandardCharsets.US_ASCII);

            if (getDeviceSession(channel, remoteAddress, imei) != null) {
                sendResponse(channel, remoteAddress, "OK");
            } else {
                sendResponse(channel, remoteAddress, "NO01");
            }

        } else {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            List<Position> positions = new LinkedList<>();

            buf.skipBytes(4); // marker
            buf.readUnsignedShort(); // length
            buf.readLong(); // imei
            buf.readUnsignedByte(); // codec
            int count = buf.readUnsignedByte();

            for (int i = 0; i < count; i++) {
                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                position.setTime(new Date(buf.readUnsignedInt() * 1000));

                position.set("reason", buf.readUnsignedByte());

                position.setLongitude(buf.readInt() / 10000000.0);
                position.setLatitude(buf.readInt() / 10000000.0);
                position.setAltitude(buf.readShort());
                position.setCourse(buf.readUnsignedShort());

                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());

                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
                position.setValid(buf.readUnsignedByte() != 0);

                buf.readUnsignedByte(); // used systems

                buf.readUnsignedByte(); // cause element id

                // Read 1 byte data
                int cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    position.set(Position.PREFIX_IO + buf.readUnsignedByte(), buf.readUnsignedByte());
                }

                // Read 2 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    position.set(Position.PREFIX_IO + buf.readUnsignedByte(), buf.readUnsignedShort());
                }

                // Read 4 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    position.set(Position.PREFIX_IO + buf.readUnsignedByte(), buf.readUnsignedInt());
                }

                // Read 8 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    position.set(Position.PREFIX_IO + buf.readUnsignedByte(), buf.readLong());
                }

                positions.add(position);
            }

            return positions;

        }

        return null;
    }

}
