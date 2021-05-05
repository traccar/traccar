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

import java.net.SocketAddress;
import java.time.Instant;
import java.util.Date;

import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class Mavlink2ProtocolDecoder extends BaseProtocolDecoder {

    public Mavlink2ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        if (buf.readUnsignedByte() != 0xFD) { //Packet start marker
            return null;
        }
        buf.readUnsignedByte(); // Payload length
        buf.readUnsignedByte(); // Incompatibility Flags
        buf.readUnsignedByte(); // Compatibility Flags
        buf.readUnsignedByte(); // Packet sequence number
        int senderSystemId = buf.readUnsignedByte(); // System ID (sender)
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, Integer.toString(senderSystemId));
        buf.readUnsignedByte(); // Component ID (sender)
        if (deviceSession == null) {
            return null;
        }
        int messageId = buf.readUnsignedMediumLE(); // Message ID (low, middle, high bytes)
        if (messageId == 33) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            position.setValid(true);
            position.setTime(Date.from(Instant.now()));
            position.set("timeBootms", buf.readUnsignedIntLE()); // Timestamp (time since system boot).
            position.setLatitude(buf.readIntLE() / 10000000.0);
            position.setLongitude(buf.readIntLE() / 10000000.0);
            position.setAltitude(buf.readIntLE() / 1000.0); // Altitude (MSL).
            position.set("relativeAltitude", buf.readIntLE() / 1000.0); // Altitude above ground
            int groundSpeedX = buf.readShortLE();
            int groundSpeedY = buf.readShortLE();
            buf.readShortLE(); // Ground Z Speed
            double speed = Math.sqrt(Math.pow(groundSpeedX, 2) + Math.pow(groundSpeedY, 2));
            position.setSpeed(UnitsConverter.knotsFromCps(speed));
            position.setCourse(buf.readUnsignedShortLE() / 100.0);
            return position;
        }
        return null;
    }

}
