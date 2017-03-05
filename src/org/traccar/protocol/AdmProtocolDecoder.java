/*
 * Copyright 2012 Anton Tananaev (anton@traccar.org)
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
import org.traccar.DeviceSession;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
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

        DeviceSession deviceSession;
        if (type == MSG_IMEI) {
            deviceSession = getDeviceSession(
                    channel, remoteAddress, buf.readBytes(15).toString(StandardCharsets.US_ASCII));
        } else {
            deviceSession = getDeviceSession(channel, remoteAddress);
        }

        if (deviceSession == null) {
            return null;
        }

        if (BitUtil.to(type, 2) == 0) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.set(Position.KEY_VERSION_FW, buf.readUnsignedByte());
            buf.readUnsignedShort(); // index

            position.set(Position.KEY_STATUS, buf.readUnsignedShort());

            position.setValid(true);
            position.setLatitude(buf.readFloat());
            position.setLongitude(buf.readFloat());
            position.setCourse(buf.readUnsignedShort() * 0.1);
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort() * 0.1));

            position.set(Position.KEY_ACCELERATION, buf.readUnsignedByte());

            position.setAltitude(buf.readUnsignedShort());

            position.set(Position.KEY_HDOP, buf.readUnsignedByte() * 0.1);
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte() & 0x0f);

            position.setTime(new Date(buf.readUnsignedInt() * 1000));

            position.set(Position.KEY_POWER, buf.readUnsignedShort());
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort());

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
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
            }

            return position;
        }

        return null;
    }

}
