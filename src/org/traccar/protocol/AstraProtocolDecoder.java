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
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

public class AstraProtocolDecoder extends BaseProtocolDecoder {

    public AstraProtocolDecoder(AstraProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_HEARTBEAT = 0x1A;
    public static final int MSG_DATA = 0x10;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.readUnsignedByte(); // protocol
        buf.readUnsignedShort(); // length

        String imei = String.format("%08d", buf.readUnsignedInt()) + String.format("%07d", buf.readUnsignedMedium());
        if (!identify(imei, channel, remoteAddress)) {
            return null;
        }

        List<Position> positions = new LinkedList<>();

        while (buf.readableBytes() > 2) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());

            buf.readUnsignedByte(); // index

            position.setLatitude(buf.readInt() * 0.000001);
            position.setLongitude(buf.readInt() * 0.000001);

            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(1980, 1, 6).addMillis(buf.readUnsignedInt() * 1000L);
            position.setTime(dateBuilder.getDate());

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte() * 2));
            position.setCourse(buf.readUnsignedByte() * 2);

            int reason = buf.readUnsignedMedium();
            int status = buf.readUnsignedShort();

            position.set(Event.PREFIX_IO + 1, buf.readUnsignedByte());
            position.set(Event.PREFIX_ADC + 1, buf.readUnsignedByte());
            position.set(Event.KEY_BATTERY, buf.readUnsignedByte());
            position.set(Event.KEY_POWER, buf.readUnsignedByte());

            buf.readUnsignedByte(); // max journey speed
            buf.skipBytes(6); // accelerometer
            buf.readUnsignedShort(); // journey distance
            buf.readUnsignedShort(); // journey idle time

            position.setAltitude(buf.readUnsignedByte() * 20);

            int quality = buf.readUnsignedByte();
            position.set(Event.KEY_SATELLITES, quality & 0xf);
            position.set(Event.KEY_GSM, quality >> 4);

            buf.readUnsignedByte(); // geofence events

            if (BitUtil.check(reason, 6) || BitUtil.check(reason, 7)) {

                position.set(Event.KEY_RFID, buf.readBytes(7).toString(Charset.defaultCharset()));
                position.set(Event.KEY_ODOMETER, buf.readUnsignedMedium());

                buf.readUnsignedShort(); // engine time

            }

            if (BitUtil.check(status, 6)) {

                // extra data

            }

            positions.add(position);

        }

        return positions;
    }

}
