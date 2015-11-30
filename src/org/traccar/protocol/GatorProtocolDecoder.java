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
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class GatorProtocolDecoder extends BaseProtocolDecoder {

    public GatorProtocolDecoder(GatorProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_HEARTBEAT = 0x21;
    public static final int MSG_POSITION_DATA = 0x80;
    public static final int MSG_ROLLCALL_RESPONSE = 0x81;
    public static final int MSG_ALARM_DATA = 0x82;
    public static final int MSG_TERMINAL_STATUS = 0x83;
    public static final int MSG_MESSAGE = 0x84;
    public static final int MSG_TERMINAL_ANSWER = 0x85;
    public static final int MSG_BLIND_AREA = 0x8E;
    public static final int MSG_PICTURE_FRAME = 0x54;
    public static final int MSG_CAMERA_RESPONSE = 0x56;
    public static final int MSG_PICTURE_DATA = 0x57;

    public static String decodeId(int b1, int b2, int b3, int b4) {

        int d1 = 30 + ((b1 >> 7) << 3) + ((b2 >> 7) << 2) + ((b3 >> 7) << 1) + (b4 >> 7);
        int d2 = b1 & 0x7f;
        int d3 = b2 & 0x7f;
        int d4 = b3 & 0x7f;
        int d5 = b4 & 0x7f;

        return String.format("%02d%02d%02d%02d%02d", d1, d2, d3, d4, d5);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        int type = buf.readUnsignedByte();
        buf.readUnsignedShort(); // length

        String id = decodeId(
                buf.readUnsignedByte(), buf.readUnsignedByte(),
                buf.readUnsignedByte(), buf.readUnsignedByte());

        if (type == MSG_POSITION_DATA || type == MSG_ROLLCALL_RESPONSE
                || type == MSG_ALARM_DATA || type == MSG_BLIND_AREA) {

            Position position = new Position();
            position.setProtocol(getProtocolName());

            if (!identify("1" + id, channel, remoteAddress, false) && !identify(id, channel, remoteAddress)) {
                return null;
            }
            position.setDeviceId(getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setYear(ChannelBufferTools.readHexInteger(buf, 2))
                    .setMonth(ChannelBufferTools.readHexInteger(buf, 2))
                    .setDay(ChannelBufferTools.readHexInteger(buf, 2))
                    .setHour(ChannelBufferTools.readHexInteger(buf, 2))
                    .setMinute(ChannelBufferTools.readHexInteger(buf, 2))
                    .setSecond(ChannelBufferTools.readHexInteger(buf, 2));
            position.setTime(dateBuilder.getDate());

            position.setLatitude(ChannelBufferTools.readCoordinate(buf));
            position.setLongitude(ChannelBufferTools.readCoordinate(buf));
            position.setSpeed(UnitsConverter.knotsFromKph(ChannelBufferTools.readHexInteger(buf, 4)));
            position.setCourse(ChannelBufferTools.readHexInteger(buf, 4));

            int flags = buf.readUnsignedByte();
            position.setValid((flags & 0x80) != 0);
            position.set(Event.KEY_SATELLITES, flags & 0x0f);

            position.set(Event.KEY_STATUS, buf.readUnsignedByte());
            position.set("key", buf.readUnsignedByte());
            position.set("oil", buf.readUnsignedShort() / 10.0);
            position.set(Event.KEY_POWER, buf.readUnsignedByte() + buf.readUnsignedByte() / 100.0);
            position.set(Event.KEY_ODOMETER, buf.readUnsignedInt());

            return position;
        }

        return null;
    }

}
