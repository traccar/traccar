/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class OrionProtocolDecoder extends BaseProtocolDecoder {

    public OrionProtocolDecoder(OrionProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_USERLOG = 0;
    public static final int MSG_SYSLOG = 3;

    private static void sendResponse(Channel channel, ChannelBuffer buf) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.directBuffer(4);
            response.writeByte('*');
            response.writeShort(buf.getUnsignedShort(buf.writerIndex() - 2));
            response.writeByte(buf.getUnsignedByte(buf.writerIndex() - 3));
            channel.write(response);
        }
    }

    private static double convertCoordinate(int raw) {
        int degrees = raw / 1000000;
        double minutes = (raw % 1000000) / 10000.0;
        return degrees + minutes / 60;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        int type = buf.readUnsignedByte() & 0x0f;

        if (type == MSG_USERLOG) {

            int header = buf.readUnsignedByte();

            if ((header & 0x40) != 0) {
                sendResponse(channel, buf);
            }

            if (!identify(String.valueOf(buf.readUnsignedInt()), channel)) {
                return null;
            }

            List<Position> positions = new LinkedList<>();

            for (int i = 0; i < (header & 0x0f); i++) {

                // Create new position
                Position position = new Position();
                position.setDeviceId(getDeviceId());
                position.setProtocol(getProtocolName());

                position.set(Event.KEY_EVENT, buf.readUnsignedByte());
                buf.readUnsignedByte(); // length
                position.set(Event.KEY_FLAGS, buf.readUnsignedShort());

                // Location
                position.setLatitude(convertCoordinate(buf.readInt()));
                position.setLongitude(convertCoordinate(buf.readInt()));
                position.setAltitude(buf.readShort() / 10.0);
                position.setCourse(buf.readUnsignedShort());
                position.setSpeed(buf.readUnsignedShort() * 0.0539957);

                // Date and time
                Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                time.clear();
                time.set(Calendar.YEAR, 2000 + buf.readUnsignedByte());
                time.set(Calendar.MONTH, buf.readUnsignedByte() - 1);
                time.set(Calendar.DAY_OF_MONTH, buf.readUnsignedByte());
                time.set(Calendar.HOUR_OF_DAY, buf.readUnsignedByte());
                time.set(Calendar.MINUTE, buf.readUnsignedByte());
                time.set(Calendar.SECOND, buf.readUnsignedByte());
                position.setTime(time.getTime());

                // Accuracy
                int satellites = buf.readUnsignedByte();
                position.set(Event.KEY_SATELLITES, satellites);
                position.setValid(satellites >= 3);
                positions.add(position);
            }

            return positions;
        }

        return null;
    }

}
