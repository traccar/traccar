/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Date;

public class AdmProtocolDecoder extends BaseProtocolDecoder {

    public AdmProtocolDecoder(AdmProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_IMEI = 0x03;
    public static final int MSG_PHOTO = 0x0A;
    public static final int MSG_ADM5 = 0x01;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.readUnsignedShort(); // device id
        buf.readUnsignedByte(); // length

        int type = buf.readUnsignedByte();

        if (type == MSG_IMEI) {
            identify(buf.toString(buf.readerIndex(), 15, Charset.defaultCharset()), channel, remoteAddress);
        }

        if (hasDeviceId() && BitUtil.to(type, 2) == 0) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());

            buf.readUnsignedByte(); // firmware version
            buf.readUnsignedShort(); // index

            position.set(Event.KEY_STATUS, buf.readUnsignedShort());

            position.setValid(true);
            position.setLatitude(buf.readFloat());
            position.setLongitude(buf.readFloat());
            position.setCourse(buf.readUnsignedShort() * 0.1);
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort() * 0.1));

            buf.readUnsignedByte(); // acceleration

            position.setAltitude(buf.readUnsignedShort());

            position.set(Event.KEY_HDOP, buf.readUnsignedByte() * 0.1);
            position.set(Event.KEY_SATELLITES, buf.readUnsignedByte() & 0x0f);

            position.setTime(new Date(buf.readUnsignedInt() * 1000));

            position.set(Event.KEY_POWER, buf.readUnsignedShort());
            position.set(Event.KEY_BATTERY, buf.readUnsignedShort());

            if (BitUtil.check(type, 2)) {
                buf.skipBytes(4);
            }

            if (BitUtil.check(type, 3)) {
                buf.skipBytes(12);
            }

            if (BitUtil.check(type, 4)) {
                buf.skipBytes(8);
            }

            if (BitUtil.check(type, 5)) {
                buf.skipBytes(9);
            }

            if (BitUtil.check(type, 6)) {
                buf.skipBytes(buf.getUnsignedByte(buf.readerIndex()));
            }

            if (BitUtil.check(type, 7)) {
                position.set(Event.KEY_ODOMETER, buf.readUnsignedInt());
            }

            return position;
        }

        return null;
    }

}
