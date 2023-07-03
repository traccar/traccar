/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class T622IridiumProtocolDecoder extends BaseProtocolDecoder {

    public T622IridiumProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // protocol revision
        buf.readUnsignedShort(); // length
        buf.readUnsignedByte(); // header indicator
        buf.readUnsignedShort(); // header length
        buf.readUnsignedInt(); // reference

        String imei = buf.readCharSequence(15, StandardCharsets.US_ASCII).toString();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        buf.readUnsignedByte(); // session status
        buf.readUnsignedShort(); // originator index
        buf.readUnsignedShort(); // transfer index
        buf.readUnsignedInt(); // session time
        buf.readUnsignedByte(); // payload indicator
        buf.readUnsignedShort(); // payload length

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_EVENT, buf.readUnsignedByte());

        position.setLatitude(buf.readIntLE() / 1000000.0);
        position.setLongitude(buf.readIntLE() / 1000000.0);
        position.setTime(new Date((buf.readUnsignedIntLE() + 946713600) * 1000));
        position.setValid(buf.readUnsignedByte() > 0);

        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_RSSI, buf.readUnsignedByte());

        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE()));
        position.setCourse(buf.readUnsignedShortLE());

        position.set(Position.KEY_HDOP, buf.readUnsignedByte() * 0.1);

        position.setAltitude(buf.readShortLE());

        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
        position.set(Position.KEY_HOURS, buf.readUnsignedIntLE() * 1000);
        position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
        position.set(Position.KEY_INPUT, buf.readUnsignedByte());
        position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.01);
        position.set(Position.KEY_POWER, buf.readUnsignedShortLE() * 0.01);

        buf.readUnsignedByte(); // geofence

        return position;
    }

}
