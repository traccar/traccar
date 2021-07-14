/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class ThinkPowerProtocolDecoder extends BaseProtocolDecoder {

    public ThinkPowerProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN_REQUEST = 0x01;
    public static final int MSG_LOGIN_RESPONSE = 0x02;
    public static final int MSG_HEARTBEAT_REQUEST = 0x03;
    public static final int MSG_HEARTBEAT_RESPONSE = 0x04;
    public static final int MSG_RECORD_REPORT = 0x05;
    public static final int MSG_RECORD_RESPONSE = 0x06;

    private void sendResponse(Channel channel, int type, int index, ByteBuf content) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(type);
            response.writeByte(index);
            if (content != null) {
                response.writeShort(content.readableBytes());
                response.writeBytes(content);
                content.release();
            } else {
                response.writeShort(0);
            }
            response.writeShort(Checksum.crc16(Checksum.CRC16_CCITT_FALSE, response.nioBuffer()));
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    private void decodeValue(Position position, int type, ByteBuf buf) {
        switch (type) {
            case 0x01:
                position.setValid(true);
                position.setLatitude(buf.readInt() * 0.0000001);
                position.setLongitude(buf.readInt() * 0.0000001);
                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort() * 0.1));
                position.setCourse(buf.readUnsignedShort() * 0.01);
                break;
            case 0x02:
                position.setValid(buf.readUnsignedByte() > 0);
                break;
            case 0x03:
                buf.skipBytes(3); // geofence
                break;
            case 0x06:
            case 0x07:
            case 0x08:
                buf.skipBytes(2); // g-sensor x/y/z
                break;
            case 0x09:
                buf.readUnsignedByte(); // collision alarm
                break;
            case 0x0A:
                buf.readUnsignedByte(); // drop alarm
                break;
            case 0x10:
                if (buf.readUnsignedByte() > 0) {
                    position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                }
                break;
            case 0x12:
                position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.1);
                break;
            case 0x13:
                if (buf.readUnsignedByte() > 0) {
                    position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                }
                break;
            case 0x16:
                buf.readUnsignedShort(); // temperature
                break;
            case 0x17:
                buf.readUnsignedByte(); // humidity
                break;
            case 0x18:
                buf.readUnsignedShort(); // high temperature
                break;
            case 0x19:
                buf.readUnsignedByte(); // high humidity
                break;
            case 0x50:
                if (buf.readUnsignedByte() > 0) {
                    position.set(Position.KEY_ALARM, Position.ALARM_REMOVING);
                }
                break;
            case 0x51:
                if (buf.readUnsignedByte() > 0) {
                    position.set(Position.KEY_ALARM, Position.ALARM_TAMPERING);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int type = buf.readUnsignedByte();
        int index = buf.readUnsignedByte();
        buf.readUnsignedShort(); // length

        if (type == MSG_LOGIN_REQUEST) {

            buf.readUnsignedByte(); // protocol major version
            buf.readUnsignedByte(); // protocol minor version

            String id = buf.readCharSequence(buf.readUnsignedByte(), StandardCharsets.US_ASCII).toString();
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);

            ByteBuf content = Unpooled.buffer();
            content.writeByte(deviceSession != null ? 0 : 4);
            sendResponse(channel, MSG_LOGIN_RESPONSE, index, content);

        } else if (type == MSG_HEARTBEAT_REQUEST) {

            sendResponse(channel, MSG_HEARTBEAT_RESPONSE, index, null);

        } else if (type == MSG_RECORD_REPORT) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            buf.readUnsignedByte(); // count

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setTime(new Date(buf.readUnsignedInt() * 1000));

            while (buf.readableBytes() > 2) {
                decodeValue(position, buf.readUnsignedByte(), buf);
            }

            sendResponse(channel, MSG_RECORD_RESPONSE, index, null);

            return position;

        }

        return null;
    }

}
