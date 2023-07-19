/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class VnetProtocolDecoder extends BaseProtocolDecoder {

    public VnetProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 0x0000;
    public static final int MSG_LBS = 0x32;
    public static final int MSG_GPS = 0x33;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // header
        int type = BitUtil.to(buf.readUnsignedShortLE(), 15);
        buf.readUnsignedShortLE(); // length

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(BcdUtil.readInteger(buf, 2), BcdUtil.readInteger(buf, 2), BcdUtil.readInteger(buf, 2))
                .setTime(BcdUtil.readInteger(buf, 2), BcdUtil.readInteger(buf, 2), BcdUtil.readInteger(buf, 2));

        if (type == MSG_LOGIN) {

            String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(0, 15);
            getDeviceSession(channel, remoteAddress, imei);
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(
                        buf.retainedSlice(0, buf.writerIndex()), channel.remoteAddress()));
            }

        } else if (type == MSG_GPS) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            position.setTime(dateBuilder.getDate());

            int value;
            int degrees;

            value = BcdUtil.readInteger(buf, 8);
            degrees = value / 1000000;
            double lat = degrees + value % 1000000 * 0.0001 / 60;

            value = BcdUtil.readInteger(buf, 10);
            degrees = value / 10000000;
            double lon = degrees + value % 10000000 * 0.00001 / 60;

            int flags = buf.readUnsignedByte();
            position.setValid(BitUtil.check(flags, 0));
            position.setLatitude(BitUtil.check(flags, 1) ? lat : -lat);
            position.setLongitude(BitUtil.check(flags, 2) ? lon : -lon);

            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
            position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
            position.setCourse(buf.readUnsignedByte() * 2);

            return position;

        }

        return null;
    }

}
