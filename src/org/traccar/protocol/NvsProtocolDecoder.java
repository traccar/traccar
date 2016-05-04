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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class NvsProtocolDecoder extends BaseProtocolDecoder {

    public NvsProtocolDecoder(NvsProtocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, String response) {
        if (channel != null) {
            channel.write(ChannelBuffers.copiedBuffer(response, StandardCharsets.US_ASCII));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;


        if (buf.getUnsignedByte(buf.readerIndex()) == 0) {

            buf.readUnsignedShort(); // length

            String imei = buf.toString(buf.readerIndex(), 15, StandardCharsets.US_ASCII);

            if (!identify(imei, channel, remoteAddress)) {
                sendResponse(channel, "NO01");
            } else {
                sendResponse(channel, "OK");
            }

        } else if (hasDeviceId()) {

            List<Position> positions = new LinkedList<>();

            buf.skipBytes(4); // marker
            buf.readUnsignedShort(); // length
            buf.readLong(); // imei
            buf.readUnsignedByte(); // codec
            int count = buf.readUnsignedByte();

            for (int i = 0; i < count; i++) {
                Position position = new Position();
                position.setProtocol(getProtocolName());
                position.setDeviceId(getDeviceId());

                position.setTime(new Date(buf.readUnsignedInt() * 1000));

                position.set("reason", buf.readUnsignedByte());

                position.setLongitude(buf.readInt() / 10000000.0);
                position.setLatitude(buf.readInt() / 10000000.0);
                position.setAltitude(buf.readShort());
                position.setCourse(buf.readUnsignedShort());

                position.set(Event.KEY_SATELLITES, buf.readUnsignedByte());

                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
                position.setValid(buf.readUnsignedByte() != 0);

                buf.readUnsignedByte(); // used systems

                buf.readUnsignedByte(); // cause element id

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

            return positions;

        }

        return null;
    }

}
