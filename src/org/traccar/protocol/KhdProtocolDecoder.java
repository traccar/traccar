/*
 * Copyright 2014 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class KhdProtocolDecoder extends BaseProtocolDecoder {

    public KhdProtocolDecoder(KhdProtocol protocol) {
        super(protocol);
    }

    private String readSerialNumber(ChannelBuffer buf) {
        int b1 = buf.readUnsignedByte();
        int b2 = buf.readUnsignedByte();
        if (b2 > 0x80) {
            b2 -= 0x80;
        }
        int b3 = buf.readUnsignedByte();
        if (b3 > 0x80) {
            b3 -= 0x80;
        }
        int b4 = buf.readUnsignedByte();
        String serialNumber = String.format("%02d%02d%02d%02d", b1, b2, b3, b4);
        return String.valueOf(Long.parseLong(serialNumber));
    }

    public static final int MSG_LOGIN = 0xB1;
    public static final int MSG_CONFIRMATION = 0x21;
    public static final int MSG_ON_DEMAND = 0x81;
    public static final int MSG_POSITION_UPLOAD = 0x80;
    public static final int MSG_POSITION_REUPLOAD = 0x8E;
    public static final int MSG_ALARM = 0x82;
    public static final int MSG_REPLY = 0x85;
    public static final int MSG_PERIPHERAL = 0xA3;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        int type = buf.readUnsignedByte();
        buf.readUnsignedShort(); // size

        if (type == MSG_ON_DEMAND || type == MSG_POSITION_UPLOAD || type == MSG_POSITION_REUPLOAD
                || type == MSG_ALARM || type == MSG_REPLY || type == MSG_PERIPHERAL) {

            Position position = new Position();
            position.setProtocol(getProtocolName());

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, readSerialNumber(buf));
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

            position.setLatitude(BcdUtil.readCoordinate(buf));
            position.setLongitude(BcdUtil.readCoordinate(buf));
            position.setSpeed(UnitsConverter.knotsFromKph(BcdUtil.readInteger(buf, 4)));
            position.setCourse(BcdUtil.readInteger(buf, 4));
            position.setValid((buf.readUnsignedByte() & 0x80) != 0);

            if (type != MSG_ALARM) {

                position.set(Position.KEY_ODOMETER, buf.readUnsignedMedium());
                position.set(Position.KEY_STATUS, buf.readUnsignedInt());
                position.set(Position.KEY_HDOP, buf.readUnsignedByte());
                position.set(Position.KEY_VDOP, buf.readUnsignedByte());
                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());

                buf.skipBytes(5); // other location data

                if (type == MSG_PERIPHERAL) {

                    buf.readUnsignedShort(); // data length

                    int dataType = buf.readUnsignedByte();

                    buf.readUnsignedByte(); // content length

                    switch (dataType) {
                        case 0x01:
                            position.set(Position.KEY_FUEL_LEVEL,
                                    buf.readUnsignedByte() * 100 + buf.readUnsignedByte());
                            break;
                        case 0x02:
                            position.set(Position.PREFIX_TEMP + 1,
                                    buf.readUnsignedByte() * 100 + buf.readUnsignedByte());
                            break;
                        default:
                            break;
                    }

                }

            }

            return position;

        } else if (type == MSG_LOGIN && channel != null) {

            buf.skipBytes(4); // serial number
            buf.readByte(); // reserved

            ChannelBuffer response = ChannelBuffers.dynamicBuffer();
            response.writeByte(0x29); response.writeByte(0x29); // header
            response.writeByte(MSG_CONFIRMATION);
            response.writeShort(5); // size
            response.writeByte(buf.readUnsignedByte());
            response.writeByte(type);
            response.writeByte(0); // reserved
            response.writeByte(Checksum.xor(response.toByteBuffer()));
            response.writeByte(0x0D); // ending
            channel.write(response);

        }

        return null;
    }

}
