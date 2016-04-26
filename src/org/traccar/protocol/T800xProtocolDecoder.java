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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.ByteOrder;

public class T800xProtocolDecoder extends BaseProtocolDecoder {

    public T800xProtocolDecoder(T800xProtocol protocol) {
        super(protocol);
    }

    private static final int MSG_LOGIN = 0x01;
    private static final int MSG_GPS = 0x02;
    private static final int MSG_HEARTBEAT = 0x03;
    private static final int MSG_ALARM = 0x04;

    private static float readSwappedFloat(ChannelBuffer buf) {
        byte[] bytes = new byte[4];
        buf.readBytes(bytes);
        return ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, bytes).readFloat();
    }

    private void sendResponse(Channel channel, int type, ChannelBuffer imei) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.directBuffer(15);
            response.writeByte(0x23);
            response.writeByte(0x23); // header
            response.writeByte(type);
            response.writeShort(response.capacity()); // length
            response.writeShort(0x0001); // index
            response.writeBytes(imei);
            channel.write(response);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2);
        int type = buf.readUnsignedByte();
        buf.readUnsignedShort(); // length
        int index = buf.readUnsignedShort();
        ChannelBuffer imei = buf.readBytes(8);

        if (!identify(ChannelBuffers.hexDump(imei).substring(1), channel, remoteAddress)) {
            return null;
        }

        if (type == MSG_LOGIN || type == MSG_ALARM || type == MSG_HEARTBEAT) {
            sendResponse(channel, type, imei);
        }

        if (type == MSG_GPS || type == MSG_ALARM) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());

            position.set(Event.KEY_INDEX, index);

            buf.readUnsignedShort(); // acc on interval
            buf.readUnsignedShort(); // acc off interval
            buf.readUnsignedByte(); // angle compensation
            buf.readUnsignedShort(); // distance compensation
            buf.readUnsignedShort(); // speed alarm

            int locationStatus = buf.readUnsignedByte();

            buf.readUnsignedByte(); // gsensor manager status
            buf.readUnsignedByte(); // other flags
            buf.readUnsignedByte(); // heartbeat
            buf.readUnsignedByte(); // relay status
            buf.readUnsignedShort(); // drag alarm setting

            int io = buf.readUnsignedShort();
            position.set(Event.KEY_IGNITION, BitUtil.check(io, 14));
            position.set("ac", BitUtil.check(io, 13));

            position.set(Event.PREFIX_ADC + 1, buf.readUnsignedShort());
            position.set(Event.PREFIX_ADC + 2, buf.readUnsignedShort());

            position.set(Event.KEY_ALARM, buf.readUnsignedByte());

            buf.readUnsignedByte(); // reserved

            position.set(Event.KEY_ODOMETER, buf.readUnsignedInt());

            int battery = BcdUtil.readInteger(buf, 2);
            if (battery == 0) {
                battery = 100;
            }
            position.set(Event.KEY_BATTERY, battery);

            DateBuilder dateBuilder = new DateBuilder()
                    .setYear(BcdUtil.readInteger(buf, 2))
                    .setMonth(BcdUtil.readInteger(buf, 2))
                    .setDay(BcdUtil.readInteger(buf, 2))
                    .setHour(BcdUtil.readInteger(buf, 2))
                    .setMinute(BcdUtil.readInteger(buf, 2))
                    .setSecond(BcdUtil.readInteger(buf, 2));

            if (BitUtil.check(locationStatus, 6)) {

                position.setValid(!BitUtil.check(locationStatus, 7));
                position.setTime(dateBuilder.getDate());
                position.setAltitude(readSwappedFloat(buf));
                position.setLongitude(readSwappedFloat(buf));
                position.setLatitude(readSwappedFloat(buf));
                position.setSpeed(UnitsConverter.knotsFromKph(
                        BcdUtil.readInteger(buf, 4) * 0.1));
                position.setCourse(buf.readUnsignedShort());

            } else {

                getLastLocation(position, dateBuilder.getDate());

                byte[] array = new byte[16];
                buf.readBytes(array);
                ChannelBuffer swapped = ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, array);

                position.set(Event.KEY_MCC, swapped.readUnsignedShort());
                position.set(Event.KEY_MNC, swapped.readUnsignedShort());
                position.set(Event.KEY_LAC, swapped.readUnsignedShort());
                position.set(Event.KEY_CID, swapped.readUnsignedShort());

                // two more cell towers

            }

            return position;

        }

        return null;
    }

}
