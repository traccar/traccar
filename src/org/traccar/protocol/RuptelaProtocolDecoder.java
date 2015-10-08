/*
 * Copyright 2013 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class RuptelaProtocolDecoder extends BaseProtocolDecoder {

    public RuptelaProtocolDecoder(RuptelaProtocol protocol) {
        super(protocol);
    }

    private static final int COMMAND_RECORDS = 0x01;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.readUnsignedShort(); // data length

        // Identify device
        String imei = String.format("%015d", buf.readLong());
        if (!identify(imei, channel)) {
            return null;
        }

        int type = buf.readUnsignedByte();

        if (type == COMMAND_RECORDS) {
            List<Position> positions = new LinkedList<>();

            buf.readUnsignedByte(); // records left
            int count = buf.readUnsignedByte();

            for (int i = 0; i < count; i++) {
                Position position = new Position();
                position.setProtocol(getProtocolName());
                position.setDeviceId(getDeviceId());

                // Time
                position.setTime(new Date(buf.readUnsignedInt() * 1000));
                buf.readUnsignedByte(); // timestamp extension

                buf.readUnsignedByte(); // priority (reserved)

                // Location
                position.setLongitude(buf.readInt() / 10000000.0);
                position.setLatitude(buf.readInt() / 10000000.0);
                position.setAltitude(buf.readUnsignedShort() / 10.0);
                position.setCourse(buf.readUnsignedShort() / 100.0);

                // Validity
                int satellites = buf.readUnsignedByte();
                position.set(Event.KEY_SATELLITES, satellites);
                position.setValid(satellites >= 3);

                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

                position.set(Event.KEY_HDOP, buf.readUnsignedByte() / 10.0);

                buf.readUnsignedByte();

                // Read 1 byte data
                int cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    position.set(Event.PREFIX_IO + buf.readUnsignedByte(), buf.readUnsignedByte());
                }

                // Read 2 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    position.set(Event.PREFIX_IO + buf.readUnsignedByte(), buf.readUnsignedShort());
                }

                // Read 4 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    position.set(Event.PREFIX_IO + buf.readUnsignedByte(), buf.readUnsignedInt());
                }

                // Read 8 byte data
                cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    position.set(Event.PREFIX_IO + buf.readUnsignedByte(), buf.readLong());
                }
                positions.add(position);
            }

            // Acknowledgement
            if (channel != null) {
                byte[] response = {0x00, 0x02, 0x64, 0x01, 0x13, (byte) 0xbc};
                channel.write(ChannelBuffers.wrappedBuffer(response));
            }

            return positions;
        }

        return null;
    }

}
