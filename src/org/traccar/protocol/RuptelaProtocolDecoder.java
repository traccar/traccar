/*
 * Copyright 2013 - 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class RuptelaProtocolDecoder extends BaseProtocolDecoder {

    public RuptelaProtocolDecoder(RuptelaProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_RECORDS = 1;
    public static final int MSG_EXTENDED_RECORDS = 68;
    public static final int MSG_SMS_VIA_GPRS = 108;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.readUnsignedShort(); // data length

        String imei = String.format("%015d", buf.readLong());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        int type = buf.readUnsignedByte();

        if (type == MSG_RECORDS || type == MSG_EXTENDED_RECORDS) {
            List<Position> positions = new LinkedList<>();

            buf.readUnsignedByte(); // records left
            int count = buf.readUnsignedByte();

            for (int i = 0; i < count; i++) {
                Position position = new Position();
                position.setProtocol(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                position.setTime(new Date(buf.readUnsignedInt() * 1000));
                buf.readUnsignedByte(); // timestamp extension

                if (type == MSG_EXTENDED_RECORDS) {
                    buf.readUnsignedByte(); // record extension
                }

                buf.readUnsignedByte(); // priority (reserved)

                position.setValid(true);
                position.setLongitude(buf.readInt() / 10000000.0);
                position.setLatitude(buf.readInt() / 10000000.0);
                position.setAltitude(buf.readUnsignedShort() / 10.0);
                position.setCourse(buf.readUnsignedShort() / 100.0);

                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());

                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

                position.set(Position.KEY_HDOP, buf.readUnsignedByte() / 10.0);

                if (type == MSG_EXTENDED_RECORDS) {
                    buf.readUnsignedShort(); // event
                } else {
                    buf.readUnsignedByte(); // event
                }

                // Read 1 byte data
                int cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    position.set(Position.PREFIX_IO + id, buf.readUnsignedByte());
                }

                // Read 2 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    position.set(Position.PREFIX_IO + id, buf.readUnsignedShort());
                }

                // Read 4 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    position.set(Position.PREFIX_IO + id, buf.readUnsignedInt());
                }

                // Read 8 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    int id = type == MSG_EXTENDED_RECORDS ? buf.readUnsignedShort() : buf.readUnsignedByte();
                    position.set(Position.PREFIX_IO + id, buf.readLong());
                }

                positions.add(position);
            }

            if (channel != null) {
                byte[] response = {0x00, 0x02, 0x64, 0x01, 0x13, (byte) 0xbc};
                channel.write(ChannelBuffers.wrappedBuffer(response)); // acknowledgement
            }

            return positions;
        }

        return null;
    }

}
