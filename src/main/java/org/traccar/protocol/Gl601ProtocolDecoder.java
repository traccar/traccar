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
import java.util.LinkedList;
import java.util.List;

public class Gl601ProtocolDecoder extends BaseProtocolDecoder {

    public Gl601ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private int readVariableInt(ByteBuf buf) {
        if (BitUtil.check(buf.getUnsignedByte(buf.readerIndex()), 7)) {
            return buf.readUnsignedShort();
        } else {
            return buf.readUnsignedByte();
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // header
        buf.readUnsignedByte(); // reserved
        buf.readUnsignedShort(); // length

        int flags = buf.readUnsignedByte();
        int count = BitUtil.check(flags, 7) ? buf.readUnsignedShort() : 1;

        String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        buf.readUnsignedShort(); // device type
        buf.readUnsignedShort(); // protocol version
        buf.readUnsignedByte(); // custom version

        buf.skipBytes(buf.readUnsignedByte()); // reserved

        List<Position> positions = new LinkedList<>();
        for (int i = 0; i < count; i++) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            int recordEndIndex = buf.readerIndex() + readVariableInt(buf);

            position.setDeviceTime(new Date(buf.readUnsignedInt() * 1000));

            buf.readUnsignedShort(); // record index
            buf.readUnsignedByte(); // record id
            position.set(Position.KEY_EVENT, buf.readUnsignedByte());

            while (buf.readerIndex() < recordEndIndex) {

                int dataId = readVariableInt(buf);
                int dataEndIndex = readVariableInt(buf) + buf.readerIndex();

                switch (dataId) {
                    case 81, 82 -> {
                        int state = buf.readUnsignedByte();
                        position.setValid(BitUtil.between(state, 2, 4) == 0b10);
                        position.setLongitude(buf.readInt() / 1000000.0);
                        position.setLatitude(buf.readInt() / 1000000.0);
                        position.setFixTime(new Date(buf.readUnsignedInt() * 1000));
                        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort() / 10.0));
                        if (dataId == 82) {
                            position.set(Position.KEY_HDOP, buf.readUnsignedByte() / 10.0);
                            position.setCourse(buf.readUnsignedShort());
                            position.setAltitude(buf.readMedium() / 10.0);
                            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                        }
                    }
                    case 97 -> {
                        buf.readUnsignedByte(); // battery status
                        position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
                        position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                        buf.readUnsignedByte(); // charging
                    }
                }

                buf.readerIndex(dataEndIndex);

            }

            if (position.getFixTime() == null) {
                getLastLocation(position, position.getDeviceTime());
            }

            positions.add(position);

        }

        return positions.isEmpty() ? null : positions;
    }

}
