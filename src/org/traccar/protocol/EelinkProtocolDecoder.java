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
import java.util.Date;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class EelinkProtocolDecoder extends BaseProtocolDecoder {

    public EelinkProtocolDecoder(EelinkProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 0x01;
    public static final int MSG_GPS = 0x02;
    public static final int MSG_HEARTBEAT = 0x03;
    public static final int MSG_ALARM = 0x04;
    public static final int MSG_STATE = 0x05;
    public static final int MSG_SMS = 0x06;
    public static final int MSG_OBD = 0x07;
    public static final int MSG_INTERACTIVE = 0x80;
    public static final int MSG_DATA = 0x81;

    private void sendResponse(Channel channel, int type, int index) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.buffer(7);
            response.writeByte(0x67); response.writeByte(0x67); // header
            response.writeByte(type);
            response.writeShort(2); // length
            response.writeShort(index);
            channel.write(response);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        int type = buf.readUnsignedByte();
        buf.readShort(); // length
        int index = buf.readUnsignedShort();

        if (type != MSG_GPS && type != MSG_DATA) {
            sendResponse(channel, type, index);
        }

        if (type == MSG_LOGIN) {

            identify(ChannelBufferTools.readHexString(buf, 16).substring(1), channel);

        } else if (hasDeviceId()
                && (type == MSG_GPS || type == MSG_ALARM || type == MSG_STATE || type == MSG_SMS)) {

            // Create new position
            Position position = new Position();
            position.setDeviceId(getDeviceId());

            position.setProtocol(getProtocolName());
            position.set(Event.KEY_INDEX, index);

            // Location
            position.setTime(new Date(buf.readUnsignedInt() * 1000));
            position.setLatitude(buf.readInt() / 1800000.0);
            position.setLongitude(buf.readInt() / 1800000.0);
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
            position.setCourse(buf.readUnsignedShort());

            // Cell
            position.set(Event.KEY_CELL, ChannelBufferTools.readHexString(buf, 18));

            // Validity
            position.setValid((buf.readUnsignedByte() & 0x01) != 0);

            if (type == MSG_ALARM) {
                position.set(Event.KEY_ALARM, buf.readUnsignedByte());
            }

            if (type == MSG_STATE) {
                position.set(Event.KEY_STATUS, buf.readUnsignedByte());
            }
            return position;
        }

        return null;
    }

}
