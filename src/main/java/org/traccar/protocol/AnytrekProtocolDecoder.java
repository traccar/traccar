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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class AnytrekProtocolDecoder extends BaseProtocolDecoder {

    public AnytrekProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, int type) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeShort(0x7878);
            response.writeShortLE(1 + 1 + 2 + 1 + 2); // length
            response.writeByte(type);
            response.writeByte(0); // error
            response.writeShortLE(0); // report interval
            response.writeByte(0); // clear alarm
            response.writeShortLE(0); // checksum
            response.writeByte('\r');
            response.writeByte('\n');
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // header
        buf.readUnsignedShortLE(); // size
        int type = buf.readUnsignedByte();

        String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(2);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_VERSION_FW, buf.readUnsignedShortLE());
        position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.01);
        position.set(Position.KEY_RSSI, buf.readUnsignedByte());

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        position.setTime(dateBuilder.getDate());

        position.set(Position.KEY_SATELLITES, BitUtil.to(buf.readUnsignedByte(), 4));

        double latitude = buf.readUnsignedIntLE() / 1800000.0;
        double longitude = buf.readUnsignedIntLE() / 1800000.0;
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));

        int flags = buf.readUnsignedShortLE();
        position.setCourse(BitUtil.to(flags, 10));
        position.setValid(BitUtil.check(flags, 12));

        if (!BitUtil.check(flags, 10)) {
            latitude = -latitude;
        }
        if (BitUtil.check(flags, 11)) {
            longitude = -longitude;
        }

        position.setLatitude(latitude);
        position.setLongitude(longitude);

        buf.readUnsignedIntLE(); // info index
        buf.readUnsignedIntLE(); // setting index

        flags = buf.readUnsignedByte();
        position.set(Position.KEY_CHARGE, BitUtil.check(flags, 0));
        position.set(Position.KEY_IGNITION, BitUtil.check(flags, 1));
        position.addAlarm(BitUtil.check(flags, 4) ? Position.ALARM_GENERAL : null);

        buf.readUnsignedShortLE(); // charge current

        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());

        sendResponse(channel, remoteAddress, type);

        return position;
    }

}
