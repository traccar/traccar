/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class NiotProtocolDecoder extends BaseProtocolDecoder {

    public NiotProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_RESPONSE = 0x21;
    public static final int MSG_POSITION_DATA = 0x80;

    private void sendResponse(Channel channel, SocketAddress remoteAddress, int type, int checksum) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeShort(0x5858); // header
            response.writeByte(MSG_RESPONSE);
            response.writeShort(5); // length
            response.writeByte(checksum);
            response.writeByte(type);
            response.writeByte(0); // subtype
            response.writeByte(Checksum.xor(response.nioBuffer(2, response.writerIndex())));
            response.writeByte(0x0D);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private double readCoordinate(ByteBuf buf) {
        long value = buf.readUnsignedInt();
        double result = BitUtil.to(value, 31) / 1800000.0;
        return BitUtil.check(value, 31) ? -result : result;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // header
        int type = buf.readUnsignedByte();
        buf.readUnsignedShort(); // length

        String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);

        sendResponse(channel, remoteAddress, type, buf.getByte(buf.writerIndex() - 2));

        if (type == MSG_POSITION_DATA) {

            Position position = new Position(getProtocolName());

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setYear(BcdUtil.readInteger(buf, 2))
                    .setMonth(BcdUtil.readInteger(buf, 2))
                    .setDay(BcdUtil.readInteger(buf, 2))
                    .setHour(BcdUtil.readInteger(buf, 2))
                    .setMinute(BcdUtil.readInteger(buf, 2))
                    .setSecond(BcdUtil.readInteger(buf, 2));
            position.setTime(dateBuilder.getDate());

            position.setLatitude(readCoordinate(buf));
            position.setLongitude(readCoordinate(buf));
            BcdUtil.readInteger(buf, 4); // reserved
            position.setCourse(BcdUtil.readInteger(buf, 4));

            int statusX = buf.readUnsignedByte();
            position.setValid(BitUtil.check(statusX, 7));
            switch (BitUtil.between(statusX, 3, 5)) {
                case 0b10:
                    position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
                    break;
                case 0b01:
                    position.set(Position.KEY_ALARM, Position.ALARM_LOW_POWER);
                    break;
                default:
                    break;
            }

            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

            int statusA = buf.readUnsignedByte();
            position.set(Position.KEY_IGNITION, !BitUtil.check(statusA, 7));
            if (!BitUtil.check(statusA, 6)) {
                position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
            }

            buf.readUnsignedByte(); // statusB
            buf.readUnsignedByte(); // statusC
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            position.set(Position.KEY_BATTERY, buf.readUnsignedByte() * 0.1);
            position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.1);
            buf.readUnsignedByte(); // speed limit
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
            buf.readUnsignedByte(); // sensor speed
            buf.readUnsignedByte(); // reserved
            buf.readUnsignedByte(); // reserved

            while (buf.readableBytes() > 4) {
                int extendedLength = buf.readUnsignedShort();
                int extendedType = buf.readUnsignedShort();
                switch (extendedType) {
                    case 0x0001:
                        position.set(Position.KEY_ICCID,
                                buf.readCharSequence(20, StandardCharsets.US_ASCII).toString());
                        break;
                    case 0x0002:
                        int statusD = buf.readUnsignedByte();
                        position.set(Position.KEY_ALARM, BitUtil.check(statusD, 5) ? Position.ALARM_REMOVING : null);
                        position.set(Position.KEY_ALARM, BitUtil.check(statusD, 4) ? Position.ALARM_TAMPERING : null);
                        buf.readUnsignedByte(); // run mode
                        buf.readUnsignedByte(); // reserved
                        break;
                    default:
                        buf.skipBytes(extendedLength - 2);
                        break;
                }

            }

            return position;
        }

        return null;
    }

}
