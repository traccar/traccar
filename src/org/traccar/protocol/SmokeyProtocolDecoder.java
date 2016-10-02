/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class SmokeyProtocolDecoder extends BaseProtocolDecoder {

    public SmokeyProtocolDecoder(SmokeyProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        buf.readUnsignedByte(); // protocol version

        String id = ChannelBuffers.hexDump(buf.readBytes(8));
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        int type = buf.readUnsignedByte();

        if (type == 0) {

            if (channel != null) {
                // TODO send ack
            }

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.set(Position.KEY_STATUS, buf.readUnsignedByte());

            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(2000, 1, 1).addSeconds(buf.readUnsignedInt());

            getLastLocation(position, dateBuilder.getDate());

            position.set(Position.KEY_INDEX, buf.readUnsignedByte());

            buf.readUnsignedShort(); // task / parameter number

            buf.readUnsignedShort(); // length

            // data

            return position;

        }

        return null;
    }

}
