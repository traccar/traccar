/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;
import java.net.SocketAddress;
import java.util.Date;

public class BwsProtocolDecoder extends BaseProtocolDecoder {

    public BwsProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // device type

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, ByteBufUtil.hexDump(buf.readSlice(4)));
        if (deviceSession == null) {
            return null;
        }

        buf.readUnsignedByte(); // message type
        buf.readUnsignedByte(); // index

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(new Date(buf.readUnsignedInt() * 1000));
        position.setLatitude(buf.readInt() / 360000.0);
        position.setLongitude(buf.readInt() / 360000.0);

        int flags = buf.readUnsignedByte();
        position.setValid(BitUtil.check(flags, 7));
        position.set(Position.KEY_SATELLITES, BitUtil.to(flags, 7));

        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
        position.setCourse(buf.readUnsignedShort() / 10.0);

        position.set(Position.KEY_POWER, buf.readUnsignedShort() / 10.0);
        position.set(Position.KEY_BATTERY, buf.readUnsignedByte() / 10.0);
        position.set(Position.PREFIX_IO + 1, buf.readUnsignedShort());
        position.set(Position.KEY_ODOMETER, buf.readUnsignedMedium() * 100);
        position.set(Position.KEY_HOURS, buf.readUnsignedMedium() * 60000L);
        position.set(Position.KEY_DEVICE_TEMP, buf.readUnsignedByte());
        position.set(Position.KEY_RSSI, buf.readUnsignedByte());

        buf.readUnsignedByte(); // checksum

        return position;
    }

}
