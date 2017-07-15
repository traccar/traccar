/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class Gl200BinaryProtocolDecoder extends BaseProtocolDecoder {

    public Gl200BinaryProtocolDecoder(Gl200Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        String header = buf.readBytes(4).toString(StandardCharsets.US_ASCII);

        if (header.equals("+EVT")) {

            Position position = new Position();
            position.setProtocol(getProtocolName());

            buf.readUnsignedByte(); // message type

            buf.readUnsignedInt(); // mask

            buf.readUnsignedShort(); // length
            buf.readUnsignedByte(); // device type
            buf.readUnsignedShort(); // protocol version

            position.set(Position.KEY_VERSION_FW, String.valueOf(buf.readUnsignedShort()));

            DeviceSession deviceSession = getDeviceSession(
                    channel, remoteAddress, String.format("%015d", buf.readLong()));
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());

            position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
            position.set(Position.KEY_POWER, buf.readUnsignedShort());

            buf.readUnsignedByte(); // motion status

            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            position.set(Position.KEY_INDEX, buf.readUnsignedByte());

            int hdop = buf.readUnsignedByte();
            position.setValid(hdop > 0);
            position.set(Position.KEY_HDOP, hdop);

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedMedium()));
            position.setCourse(buf.readUnsignedShort());
            position.setAltitude(buf.readShort());
            position.setLongitude(buf.readInt() * 0.000001);
            position.setLatitude(buf.readInt() * 0.000001);

            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(buf.readUnsignedShort(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
            position.setTime(dateBuilder.getDate());

            position.setNetwork(new Network(CellTower.from(
                    buf.readUnsignedShort(), buf.readUnsignedShort(),
                    buf.readUnsignedShort(), buf.readUnsignedShort())));

            buf.readUnsignedByte(); // reserved

            return position;

        }

        return null;
    }

}
