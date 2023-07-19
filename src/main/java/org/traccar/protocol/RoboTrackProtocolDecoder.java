/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class RoboTrackProtocolDecoder extends BaseProtocolDecoder {

    public RoboTrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_ID = 0x00;
    public static final int MSG_ACK = 0x80;
    public static final int MSG_GPS = 0x03;
    public static final int MSG_GSM = 0x04;
    public static final int MSG_IMAGE_START = 0x06;
    public static final int MSG_IMAGE_DATA = 0x07;
    public static final int MSG_IMAGE_END = 0x08;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int type = buf.readUnsignedByte();

        if (type == MSG_ID) {

            buf.skipBytes(16); // name

            String imei = buf.readSlice(15).toString(StandardCharsets.US_ASCII);

            if (getDeviceSession(channel, remoteAddress, imei) != null && channel != null) {
                ByteBuf response = Unpooled.buffer();
                response.writeByte(MSG_ACK);
                response.writeByte(0x01); // success
                response.writeByte(0x66); // checksum
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }

        } else if (type == MSG_GPS || type == MSG_GSM) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setDeviceTime(new Date(buf.readUnsignedIntLE() * 1000));

            if (type == MSG_GPS) {

                position.setValid(true);
                position.setFixTime(position.getDeviceTime());
                position.setLatitude(buf.readIntLE() * 0.000001);
                position.setLongitude(buf.readIntLE() * 0.000001);
                position.setSpeed(UnitsConverter.knotsFromKph(buf.readByte()));

            } else {

                getLastLocation(position, position.getDeviceTime());

                position.setNetwork(new Network(CellTower.from(
                        buf.readUnsignedShortLE(), buf.readUnsignedShortLE(),
                        buf.readUnsignedShortLE(), buf.readUnsignedShortLE())));

                buf.readUnsignedByte(); // reserved

            }

            int value = buf.readUnsignedByte();

            position.set(Position.KEY_SATELLITES, BitUtil.to(value, 4));
            position.set(Position.KEY_RSSI, BitUtil.between(value, 4, 7));
            position.set(Position.KEY_MOTION, BitUtil.check(value, 7));

            value = buf.readUnsignedByte();

            position.set(Position.KEY_CHARGE, BitUtil.check(value, 0));

            for (int i = 1; i <= 4; i++) {
                position.set(Position.PREFIX_IN + i, BitUtil.check(value, i));
            }

            position.set(Position.KEY_BATTERY_LEVEL, BitUtil.from(value, 5) * 100 / 7);
            position.set(Position.KEY_DEVICE_TEMP, buf.readByte());

            for (int i = 1; i <= 3; i++) {
                position.set(Position.PREFIX_ADC + i, buf.readUnsignedShortLE());
            }

            return position;

        }

        return null;
    }

}
