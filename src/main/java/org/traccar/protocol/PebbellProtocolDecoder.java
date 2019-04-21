/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
import java.nio.charset.StandardCharsets;

public class PebbellProtocolDecoder extends BaseProtocolDecoder {

    public PebbellProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_DATA = 0x01;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // header
        buf.readUnsignedByte(); // properties
        buf.readUnsignedShortLE(); // length
        buf.readUnsignedShortLE(); // checksum
        buf.readUnsignedShortLE(); // index
        int type = buf.readUnsignedByte();

        if (type == MSG_DATA) {

            Position position = new Position(getProtocolName());

            while (buf.isReadable()) {
                int endIndex = buf.readUnsignedByte() + buf.readerIndex();
                int key = buf.readUnsignedByte();
                switch (key) {
                    case 0x01:
                        DeviceSession deviceSession = getDeviceSession(
                                channel, remoteAddress, buf.readBytes(15).toString(StandardCharsets.US_ASCII));
                        if (deviceSession == null) {
                            return null;
                        }
                        position.setDeviceId(deviceSession.getDeviceId());
                        break;
                    case 0x20:
                        position.setLatitude(buf.readIntLE() * 0.0000001);
                        position.setLongitude(buf.readIntLE() * 0.0000001);
                        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE()));
                        position.setCourse(buf.readUnsignedShortLE());
                        position.setAltitude(buf.readShortLE());
                        position.setValid(buf.readUnsignedShortLE() > 0);
                        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
                        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                        break;
                    default:
                        break;
                }
                buf.readerIndex(endIndex);
            }

            if (!position.getAttributes().containsKey(Position.KEY_SATELLITES)) {
                getLastLocation(position, null);
            }

            return position.getDeviceId() > 0 ? position : null;

        }

        return null;
    }

}
