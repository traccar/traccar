/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class CalAmpProtocolDecoder extends BaseProtocolDecoder {

    public CalAmpProtocolDecoder(String protocol) {
        super(protocol);
    }

    private static final int MSG_NULL = 0;
    private static final int MSG_ACK = 1;
    private static final int MSG_EVENT_REPORT = 2;
    private static final int MSG_ID_REPORT = 3;
    private static final int MSG_USER_DATA = 4;
    private static final int MSG_APP_DATA = 5;
    private static final int MSG_CONFIG = 6;
    private static final int MSG_UNIT_REQUEST = 7;
    private static final int MSG_LOCATE_REPORT = 8;
    private static final int MSG_USER_DATA_ACC = 9;
    private static final int MSG_MINI_EVENT_REPORT = 10;
    private static final int MSG_MINI_USER_DATA = 11;

    private static final int SERVICE_UNACKNOWLEDGED = 0;
    private static final int SERVICE_ACKNOWLEDGED = 1;
    private static final int SERVICE_RESPONSE = 2;

    @Override
    public void handleUpstream(
            ChannelHandlerContext ctx, ChannelEvent evt)
            throws Exception {

        if (!(evt instanceof MessageEvent)) {
            ctx.sendUpstream(evt);
            return;
        }

        MessageEvent e = (MessageEvent) evt;
        Object decodedMessage = decode(ctx, e.getChannel(), e.getMessage(), e.getRemoteAddress());
        if (decodedMessage != null) {
            Channels.fireMessageReceived(ctx, decodedMessage, e.getRemoteAddress());
        }
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, int type, int index, int result) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.directBuffer(10);
            response.writeByte(SERVICE_RESPONSE);
            response.writeByte(MSG_ACK);
            response.writeShort(index);
            response.writeByte(type);
            response.writeByte(result);
            response.writeByte(0);
            response.writeMedium(0);
            channel.write(response, remoteAddress);
        }
    }

    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg, SocketAddress remoteAddress)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        // Check options header
        if ((buf.getByte(buf.readerIndex()) & 0x80) != 0) {

            int content = buf.readUnsignedByte();

            // Identifier
            if ((content & 0x01) != 0) {

                // Read identifier
                int length = buf.readUnsignedByte();
                long id = 0;
                for (int i = 0; i < length; i++) {
                    int b = buf.readUnsignedByte();
                    id = id * 10 + (b >> 4);
                    if ((b & 0xf) != 0xf) {
                        id = id * 10 + (b & 0xf);
                    }
                }

                identify(String.valueOf(id));
            }

            // Identifier type
            if ((content & 0x02) != 0) {
                buf.skipBytes(buf.readUnsignedByte());
            }

            // Authentication
            if ((content & 0x04) != 0) {
                buf.skipBytes(buf.readUnsignedByte());
            }

            // Routing
            if ((content & 0x08) != 0) {
                buf.skipBytes(buf.readUnsignedByte());
            }

            // Forwarding
            if ((content & 0x10) != 0) {
                buf.skipBytes(buf.readUnsignedByte());
            }

            // Responce redirection
            if ((content & 0x20) != 0) {
                buf.skipBytes(buf.readUnsignedByte());
            }

        }

        // Unidentified device
        if (!hasDeviceId()) {
            return null;
        }

        int service = buf.readUnsignedByte();
        int type = buf.readUnsignedByte();
        int index = buf.readUnsignedShort();

        // Send acknowledgement
        if (service == SERVICE_ACKNOWLEDGED) {
            sendResponse(channel, remoteAddress, type, index, 0);
        }

        if (type == MSG_EVENT_REPORT ||
            type == MSG_LOCATE_REPORT ||
            type == MSG_MINI_EVENT_REPORT) {

            // Create new position
            Position position = new Position();
            position.setDeviceId(getDeviceId());
            position.setProtocol(getProtocol());

            // Location data
            position.setTime(new Date(buf.readUnsignedInt() * 1000));
            if (type != MSG_MINI_EVENT_REPORT) {
                buf.readUnsignedInt(); // fix time
            }
            position.setLatitude(buf.readInt() * 0.0000001);
            position.setLongitude(buf.readInt() * 0.0000001);
            if (type != MSG_MINI_EVENT_REPORT) {
                position.setAltitude(buf.readInt() * 0.01);
                position.setSpeed(UnitsConverter.knotsFromCps(buf.readUnsignedInt()));
            }
            position.setCourse(buf.readShort());
            if (type == MSG_MINI_EVENT_REPORT) {
                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
            }

            // Fix status
            if (type == MSG_MINI_EVENT_REPORT) {
                position.set(Event.KEY_SATELLITES, buf.getUnsignedByte(buf.readerIndex()) & 0xf);
                position.setValid((buf.readUnsignedByte() & 0x20) == 0);
            } else {
                position.set(Event.KEY_SATELLITES, buf.readUnsignedByte());
                position.setValid((buf.readUnsignedByte() & 0x08) == 0);
            }

            if (type != MSG_MINI_EVENT_REPORT) {

                // Carrier
                position.set("carrier", buf.readUnsignedShort());

                // Cell signal
                position.set(Event.KEY_GSM, buf.readShort());

            }

            // Modem state
            position.set("modem", buf.readUnsignedByte());

            // HDOP
            if (type != MSG_MINI_EVENT_REPORT) {
                position.set(Event.KEY_HDOP, buf.readUnsignedByte());
            }

            // Inputs
            position.set(Event.KEY_INPUT, buf.readUnsignedByte());

            // Unit status
            if (type != MSG_MINI_EVENT_REPORT) {
                position.set(Event.KEY_STATUS, buf.readUnsignedByte());
            }

            // Event code and status
            if (type == MSG_EVENT_REPORT || type == MSG_MINI_EVENT_REPORT) {
                position.set(Event.KEY_EVENT, buf.readUnsignedByte() + " - " + buf.readUnsignedByte());
            }

            // Accumulators
            int accCount = buf.readUnsignedByte();
            int accType = accCount >> 6;
            accCount &= 0x3f;
            
            buf.readUnsignedByte(); // reserved

            if (accType == 1) {
                buf.readUnsignedInt(); // threshold
                buf.readUnsignedInt(); // mask
            }

            for (int i = 0; i < accCount; i++) {
                position.set("acc" + i, buf.readUnsignedInt());
            }
            return position;

        }

        return null;
    }

}
