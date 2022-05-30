/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class RecodaProtocolDecoder extends BaseProtocolDecoder {

    public RecodaProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_HEARTBEAT = 0x00001001;
    public static final int MSG_REQUEST_RESPONSE = 0x20000001;
    public static final int MSG_SIGNAL_LINK_REGISTRATION = 0x20001001;
    public static final int MSG_EVENT_NOTICE = 0x20002001;
    public static final int MSG_GPS_DATA = 0x20001011;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int type = buf.readIntLE();
        buf.readUnsignedIntLE(); // length

        if (type != MSG_HEARTBEAT) {
            buf.readUnsignedShortLE(); // version
            buf.readUnsignedShortLE(); // index
        }

        if (type == MSG_SIGNAL_LINK_REGISTRATION) {

            getDeviceSession(channel, remoteAddress, buf.readSlice(12).toString(StandardCharsets.US_ASCII));

        } else if (type == MSG_GPS_DATA) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setTime(new Date(buf.readLongLE()));

            int flags = buf.readUnsignedByte();

            if (BitUtil.check(flags, 0)) {

                buf.readUnsignedShortLE(); // declination

                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE()));

                position.setLongitude(buf.readUnsignedByte() + buf.readUnsignedByte() / 60.0);
                position.setLatitude(buf.readUnsignedByte() + buf.readUnsignedByte() / 60.0);

                position.setLongitude(position.getLongitude() + buf.readUnsignedIntLE() / 3600.0);
                position.setLatitude(position.getLatitude() + buf.readUnsignedIntLE() / 3600.0);

                int status = buf.readUnsignedByte();

                position.setValid(BitUtil.check(status, 0));
                if (BitUtil.check(status, 1)) {
                    position.setLongitude(-position.getLongitude());
                }
                if (!BitUtil.check(status, 2)) {
                    position.setLatitude(-position.getLatitude());
                }

            } else {

                getLastLocation(position, position.getDeviceTime());

            }

            return position;

        }

        return null;
    }

}
