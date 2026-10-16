/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class TeltonikaIridiumProtocolDecoder extends BaseProtocolDecoder {

    public TeltonikaIridiumProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // protocol revision
        buf.readUnsignedShort(); // length

        DeviceSession deviceSession = null;
        List<Position> positions = new LinkedList<>();

        while (buf.isReadable()) {
            int type = buf.readUnsignedByte();
            int length = buf.readUnsignedShort();
            ByteBuf data = buf.readSlice(length);

            if (type == 0x01) {
                data.readUnsignedInt(); // reference
                String imei = data.readCharSequence(15, StandardCharsets.US_ASCII).toString();
                deviceSession = getDeviceSession(channel, remoteAddress, imei);
                if (deviceSession == null) {
                    return null;
                }
            } else if (type == 0x02) {
                while (data.isReadable()) {
                    Position position = new Position(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());

                    position.setTime(new Date(data.readUnsignedInt() * 1000));
                    position.setLongitude(data.readUnsignedMedium() * 360.0 / 0xffffff - 180);
                    position.setLatitude(data.readUnsignedMedium() * 180.0 / 0xffffff - 90);

                    int event = data.readUnsignedByte();
                    position.set(Position.KEY_EVENT, event);
                    if (event == 247) {
                        position.addAlarm(Position.ALARM_ACCIDENT);
                    }

                    int io = data.readUnsignedByte();
                    for (int i = 1; i <= 4; i++) {
                        position.set(Position.PREFIX_IN + i, BitUtil.check(io, 8 - i));
                        position.set(Position.PREFIX_OUT + i, BitUtil.check(io, 4 - i));
                    }
                    data.readUnsignedByte(); // reserved

                    int speed = data.readUnsignedByte();
                    if (speed != 0xff) {
                        position.setValid(true);
                        position.setSpeed(UnitsConverter.knotsFromKph(speed));
                    }

                    positions.add(position);
                }
            }
        }

        return positions.isEmpty() ? null : positions;
    }

}
