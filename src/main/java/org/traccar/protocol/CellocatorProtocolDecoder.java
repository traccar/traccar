/*
 * Copyright 2013 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class CellocatorProtocolDecoder extends BaseProtocolDecoder {

    public CellocatorProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    static final int MSG_CLIENT_STATUS = 0;
    static final int MSG_CLIENT_PROGRAMMING = 3;
    static final int MSG_CLIENT_SERIAL_LOG = 7;
    static final int MSG_CLIENT_SERIAL = 8;
    static final int MSG_CLIENT_MODULAR = 9;
    static final int MSG_CLIENT_MODULAR_EXT = 11;

    public static final int MSG_SERVER_ACKNOWLEDGE = 4;

    public static ByteBuf encodeContent(int type, int uniqueId, int packetNumber, ByteBuf content) {

        ByteBuf buf = Unpooled.buffer();
        buf.writeByte('M');
        buf.writeByte('C');
        buf.writeByte('G');
        buf.writeByte('P');
        buf.writeByte(type);
        buf.writeIntLE(uniqueId);
        buf.writeByte(packetNumber);
        buf.writeIntLE(0); // authentication code
        buf.writeBytes(content);

        byte checksum = 0;
        for (int i = 4; i < buf.writerIndex(); i++) {
            checksum += buf.getByte(i);
        }
        buf.writeByte(checksum);

        return buf;
    }

    private void sendResponse(Channel channel, SocketAddress remoteAddress, long deviceId, byte packetNumber) {
        if (channel != null) {
            ByteBuf content = Unpooled.buffer();
            content.writeByte(0);
            content.writeByte(packetNumber);
            content.writeZero(11);

            ByteBuf reply = encodeContent(MSG_SERVER_ACKNOWLEDGE, (int) deviceId, 0, content);
            channel.writeAndFlush(new NetworkMessage(reply, remoteAddress));
        }
    }

    private void sendModuleResponse(Channel channel, SocketAddress remoteAddress, long deviceId, byte packetNumber) {
        if (channel != null) {
            ByteBuf content = Unpooled.buffer();
            content.writeByte(0x80);
            content.writeShortLE(10); // modules length
            content.writeIntLE(0); // reserved
            content.writeByte(9); // ack module type
            content.writeShortLE(3); // module length
            content.writeByte(0); // ack
            content.writeShortLE(0); // reserved

            ByteBuf reply = encodeContent(MSG_CLIENT_MODULAR_EXT, (int) deviceId, packetNumber, content);
            channel.writeAndFlush(new NetworkMessage(reply, remoteAddress));
        }
    }

    private String decodeAlarm(short reason) {
        switch (reason) {
            case 70:
                return Position.ALARM_SOS;
            case 80:
                return Position.ALARM_POWER_CUT;
            case 81:
                return Position.ALARM_LOW_POWER;
            default:
                return null;
        }
    }

    private Position decodeStatus(ByteBuf buf, DeviceSession deviceSession, boolean alternative) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_VERSION_HW, buf.readUnsignedByte());
        position.set(Position.KEY_VERSION_FW, buf.readUnsignedByte());
        buf.readUnsignedByte(); // protocol version

        position.set(Position.KEY_STATUS, buf.readUnsignedByte() & 0x0f);

        buf.readUnsignedByte(); // operator / configuration flags
        buf.readUnsignedByte(); // reason data
        position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedByte()));

        position.set("mode", buf.readUnsignedByte());

        long input = buf.readUnsignedIntLE();
        position.set(Position.KEY_DOOR, BitUtil.check(input, 3 * 8));
        position.set(Position.KEY_IGNITION, BitUtil.check(input, 2 * 8 + 7));
        position.set(Position.KEY_CHARGE, BitUtil.check(input, 7));
        position.set(Position.KEY_INPUT, input);

        if (alternative) {
            buf.readUnsignedByte(); // input
            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShortLE());
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShortLE());
        } else {
            buf.readUnsignedByte(); // operator
            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedByte());
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedByte());
            position.set(Position.PREFIX_ADC + 3, buf.readUnsignedByte());
            position.set(Position.PREFIX_ADC + 4, buf.readUnsignedByte());
        }

        position.set(Position.KEY_ODOMETER, buf.readUnsignedMediumLE());

        buf.skipBytes(6); // multi-purpose data
        buf.readUnsignedShortLE(); // fix time
        buf.readUnsignedByte(); // location status
        buf.readUnsignedByte(); // mode 1
        buf.readUnsignedByte(); // mode 2

        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());

        position.setValid(true);

        if (alternative) {
            position.setLongitude(buf.readIntLE() / 10000000.0);
            position.setLatitude(buf.readIntLE() / 10000000.0);
        } else {
            position.setLongitude(buf.readIntLE() / Math.PI * 180 / 100000000);
            position.setLatitude(buf.readIntLE() / Math.PI * 180 / 100000000);
        }

        position.setAltitude(buf.readIntLE() * 0.01);

        if (alternative) {
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedIntLE()));
            position.setCourse(buf.readUnsignedShortLE() / 1000.0);
        } else {
            position.setSpeed(UnitsConverter.knotsFromMps(buf.readUnsignedIntLE() * 0.01));
            position.setCourse(buf.readUnsignedShortLE() / Math.PI * 180.0 / 1000.0);
        }

        DateBuilder dateBuilder = new DateBuilder()
                .setTimeReverse(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setDateReverse(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedShortLE());
        position.setTime(dateBuilder.getDate());

        return position;
    }

    private Position decodeModular(ByteBuf buf, DeviceSession deviceSession) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        buf.readUnsignedByte(); // packet control
        buf.readUnsignedShortLE(); // length
        buf.readUnsignedShortLE(); // reserved
        buf.readUnsignedShortLE(); // reserved

        while (buf.readableBytes() > 3) {

            int moduleType = buf.readUnsignedByte();
            int endIndex = buf.readUnsignedShortLE() + buf.readerIndex();

            switch (moduleType) {
                case 2:
                    buf.readUnsignedShortLE(); // operator id
                    buf.readUnsignedIntLE(); // pl signature
                    int count = buf.readUnsignedByte();
                    for (int i = 0; i < count; i++) {
                        int id = buf.readUnsignedShortLE();
                        buf.readUnsignedByte(); // variable length
                        position.set(Position.PREFIX_IO + id, buf.readUnsignedIntLE());
                    }
                    break;
                case 6:
                    buf.readUnsignedByte(); // hdop
                    buf.readUnsignedByte(); // mode 1
                    buf.readUnsignedByte(); // mode 2
                    buf.readUnsignedByte(); // satellites
                    position.setLongitude(buf.readIntLE() / Math.PI * 180 / 100000000);
                    position.setLatitude(buf.readIntLE() / Math.PI * 180 / 100000000);
                    position.setAltitude(buf.readIntLE() * 0.01);
                    position.setSpeed(UnitsConverter.knotsFromMps(buf.readUnsignedByte() * 0.01));
                    position.setCourse(buf.readUnsignedShortLE() / Math.PI * 180.0 / 1000.0);
                    break;
                case 7:
                    buf.readUnsignedByte(); // valid
                    DateBuilder dateBuilder = new DateBuilder()
                            .setTimeReverse(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                            .setDateReverse(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
                    position.setTime(dateBuilder.getDate());
                    break;
                default:
                    break;
            }

            buf.readerIndex(endIndex);

        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        boolean alternative = buf.getByte(buf.readerIndex() + 3) != 'P';

        buf.skipBytes(4); // system code
        int type = buf.readUnsignedByte();

        long deviceUniqueId = buf.readUnsignedIntLE();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(deviceUniqueId));
        if (deviceSession == null) {
            return null;
        }

        if (type != MSG_CLIENT_SERIAL) {
            buf.readUnsignedShortLE(); // communication control
        }
        byte packetNumber = buf.readByte();

        if (type == MSG_CLIENT_MODULAR_EXT) {
            sendModuleResponse(channel, remoteAddress, deviceUniqueId, packetNumber);
        } else {
            sendResponse(channel, remoteAddress, deviceUniqueId, packetNumber);
        }

        if (type == MSG_CLIENT_STATUS) {
            return decodeStatus(buf, deviceSession, alternative);
        } else if (type == MSG_CLIENT_MODULAR_EXT) {
            return decodeModular(buf, deviceSession);
        }

        return null;
    }

}
