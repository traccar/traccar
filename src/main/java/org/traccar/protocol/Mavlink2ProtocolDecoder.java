/*
 * Copyright 2021 Chipeng Li (chplee@gmail.com)
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
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;

public class Mavlink2ProtocolDecoder extends BaseProtocolDecoder {

    public Mavlink2ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        if (buf.readUnsignedByte() != 0xFD) {
            return null;
        }

        buf.readUnsignedByte(); // length
        buf.readUnsignedByte(); // incompatibility flags
        buf.readUnsignedByte(); // compatibility flags
        buf.readUnsignedByte(); // index

        int senderSystemId = buf.readUnsignedByte();
        buf.readUnsignedByte(); // component id
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, Integer.toString(senderSystemId));
        if (deviceSession == null) {
            return null;
        }

        int type = buf.readUnsignedMediumLE();
        if (type == 33) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.set("timeBoot", buf.readUnsignedIntLE()); // time since system boot

            position.setValid(true);
            position.setTime(new Date());
            position.setLatitude(buf.readIntLE() / 10000000.0);
            position.setLongitude(buf.readIntLE() / 10000000.0);
            position.setAltitude(buf.readIntLE() / 1000.0);
            position.set("relativeAltitude", buf.readIntLE() / 1000.0);

            int groundSpeedX = buf.readShortLE();
            int groundSpeedY = buf.readShortLE();
            buf.readShortLE(); // ground speed z
            double speed = Math.sqrt(Math.pow(groundSpeedX, 2) + Math.pow(groundSpeedY, 2));
            position.setSpeed(UnitsConverter.knotsFromCps(speed));

            position.setCourse(buf.readUnsignedShortLE() / 100.0);

            return position;

        }

        return null;
    }

}
