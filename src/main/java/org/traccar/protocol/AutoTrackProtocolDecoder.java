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
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;

public class AutoTrackProtocolDecoder extends BaseProtocolDecoder {

    public AutoTrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN_REQUEST = 51;
    public static final int MSG_LOGIN_CONFIRM = 101;
    public static final int MSG_TELEMETRY_1 = 52;
    public static final int MSG_TELEMETRY_2 = 66;
    public static final int MSG_TELEMETRY_3 = 67;
    public static final int MSG_KEEP_ALIVE = 114;
    public static final int MSG_TELEMETRY_CONFIRM = 123;

    private Position decodeTelemetry(
            Channel channel, SocketAddress remoteAddress, DeviceSession deviceSession, ByteBuf buf) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(new Date(1009843200000L + buf.readUnsignedIntLE() * 1000)); // seconds since 2002
        position.setLatitude(buf.readIntLE() * 0.0000001);
        position.setLongitude(buf.readIntLE() * 0.0000001);

        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
        position.set(Position.KEY_FUEL_USED, buf.readUnsignedIntLE());

        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE()));
        buf.readUnsignedShortLE(); // max speed

        position.set(Position.KEY_INPUT, buf.readUnsignedShortLE());
        buf.readUnsignedIntLE(); // di 3 count
        buf.readUnsignedIntLE(); // di 4 count

        for (int i = 0; i < 5; i++) {
            position.set(Position.PREFIX_ADC + (i + 1), buf.readUnsignedShortLE());
        }

        position.setCourse(buf.readUnsignedShortLE());

        position.set(Position.KEY_STATUS, buf.readUnsignedShortLE());
        position.set(Position.KEY_EVENT, buf.readUnsignedShortLE());
        position.set(Position.KEY_DRIVER_UNIQUE_ID, buf.readLongLE());

        int index = buf.readUnsignedShortLE();

        buf.readUnsignedShortLE(); // checksum

        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeInt(0xF1F1F1F1); // sync
            response.writeByte(MSG_TELEMETRY_CONFIRM);
            response.writeShortLE(2); // length
            response.writeShortLE(index);
            response.writeShort(Checksum.crc16(Checksum.CRC16_XMODEM, response.nioBuffer()));
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(4); // sync
        int type = buf.readUnsignedByte();
        buf.readUnsignedShortLE(); // length

        DeviceSession deviceSession;
        switch (type) {
            case MSG_LOGIN_REQUEST -> {
                String imei = ByteBufUtil.hexDump(buf.readSlice(8));
                deviceSession = getDeviceSession(channel, remoteAddress, imei);
                if (deviceSession == null) {
                    return null;
                }
                int fuelConst = buf.readUnsignedShortLE();
                int tripConst = buf.readUnsignedShortLE();
                if (channel != null) {
                    ByteBuf response = Unpooled.buffer();
                    response.writeInt(0xF1F1F1F1); // sync
                    response.writeByte(MSG_LOGIN_CONFIRM);
                    response.writeShortLE(12); // length
                    response.writeBytes(ByteBufUtil.decodeHexDump(imei));
                    response.writeShortLE(fuelConst);
                    response.writeShortLE(tripConst);
                    response.writeShort(Checksum.crc16(Checksum.CRC16_XMODEM, response.nioBuffer()));
                    channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
                }
                return null;
            }
            case MSG_TELEMETRY_1, MSG_TELEMETRY_2, MSG_TELEMETRY_3 -> {
                deviceSession = getDeviceSession(channel, remoteAddress);
                if (deviceSession == null) {
                    return null;
                }
                return decodeTelemetry(channel, remoteAddress, deviceSession, buf);
            }
            default -> {
                return null;
            }
        }
    }

}
