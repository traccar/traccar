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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class NavisetProtocolDecoder extends BaseProtocolDecoder {

    public NavisetProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_HEADER = 0b00;
    public static final int MSG_DATA = 0b01;
    public static final int MSG_RESPONSE = 0b10;
    public static final int MSG_RESERVE = 0b11;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0x01);
            response.writeShortLE(buf.getUnsignedShortLE(buf.writerIndex() - 2));
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }

        int length = buf.readUnsignedShortLE();
        int type = BitUtil.between(length, 14, 16);
        buf.readUnsignedShortLE(); // device number

        if (type == MSG_HEADER) {

            getDeviceSession(channel, remoteAddress, buf.readCharSequence(15, StandardCharsets.US_ASCII).toString());

        } else if (type == MSG_DATA) {

            List<Position> positions = new LinkedList<>();
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            int blockMask = buf.readUnsignedByte();

            while (buf.readableBytes() > 2) {

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                position.set(Position.KEY_INDEX, buf.readUnsignedShortLE());
                position.set(Position.KEY_STATUS, buf.readUnsignedByte());
                position.setValid(true);
                position.setTime(new Date(buf.readUnsignedIntLE() * 1000));
                position.setLatitude(buf.readUnsignedIntLE() * 0.000001);
                position.setLongitude(buf.readUnsignedIntLE() * 0.000001);
                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE() * 0.1));

                if (BitUtil.check(blockMask, 0)) {
                    int dataMask = buf.readUnsignedByte();
                    if (BitUtil.check(dataMask, 0)) {
                        int satellites = buf.readUnsignedByte();
                        position.setValid(BitUtil.check(satellites, 7));
                        position.set(Position.KEY_SATELLITES, BitUtil.to(satellites, 7));
                    }
                    if (BitUtil.check(dataMask, 1)) {
                        position.setCourse(buf.readUnsignedShortLE() * 0.1);
                    }
                    if (BitUtil.check(dataMask, 2)) {
                        position.setAltitude(buf.readShortLE());
                    }
                    if (BitUtil.check(dataMask, 3)) {
                        position.set(Position.KEY_HDOP, buf.readUnsignedByte() * 0.1);
                    }
                    if (BitUtil.check(dataMask, 4)) {
                        position.set(Position.KEY_POWER, buf.readUnsignedShortLE() * 0.001);
                        position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.001);
                    }
                    if (BitUtil.check(dataMask, 5)) {
                        position.set(Position.KEY_INPUT, buf.readUnsignedByte());
                        position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
                    }
                    if (BitUtil.check(dataMask, 6)) {
                        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
                    }
                    if (BitUtil.check(dataMask, 7)) {
                        buf.skipBytes(6); // accelerometer
                    }
                }

                if (BitUtil.check(blockMask, 1)) {
                    int dataMask = buf.readUnsignedByte();
                    for (int i = 0; i < 8; i++) {
                        if (BitUtil.check(dataMask, i)) {
                            position.set(Position.PREFIX_ADC + (i + 1), buf.readUnsignedShortLE());
                        }
                    }
                }

                if (BitUtil.check(blockMask, 2)) {
                    int dataMask = buf.readUnsignedByte();
                    if (BitUtil.check(dataMask, 0)) {
                        position.set(Position.KEY_DEVICE_TEMP, (int) buf.readByte());
                    }
                    if (BitUtil.check(dataMask, 1)) {
                        buf.skipBytes(6); // key code
                    }
                    if (BitUtil.check(dataMask, 2)) {
                        position.set(Position.PREFIX_TEMP + 1, (int) buf.readByte());
                        position.set(Position.PREFIX_TEMP + 2, (int) buf.readByte());
                    }
                    if (BitUtil.check(dataMask, 3)) {
                        position.set(Position.PREFIX_TEMP + 3, (int) buf.readByte());
                        position.set(Position.PREFIX_TEMP + 4, (int) buf.readByte());
                    }
                    if (BitUtil.check(dataMask, 4)) {
                        position.set(Position.PREFIX_TEMP + 5, (int) buf.readByte());
                        position.set(Position.PREFIX_TEMP + 6, (int) buf.readByte());
                        position.set(Position.PREFIX_TEMP + 7, (int) buf.readByte());
                        position.set(Position.PREFIX_TEMP + 8, (int) buf.readByte());
                    }
                    if (BitUtil.check(dataMask, 5)) {
                        position.set(Position.KEY_HOURS, buf.readUnsignedShortLE() / 60.0);
                    }
                    if (BitUtil.check(dataMask, 6)) {
                        buf.readUnsignedByte(); // extra status
                    }
                    if (BitUtil.check(dataMask, 7)) {
                        buf.readUnsignedByte(); // geofence
                    }
                }

                if (BitUtil.check(blockMask, 3)) {
                    int dataMask = buf.readUnsignedByte();
                    if (BitUtil.check(dataMask, 0)) {
                        position.set("fuel1", buf.readUnsignedShortLE());
                    }
                    if (BitUtil.check(dataMask, 1)) {
                        position.set("fuel2", buf.readUnsignedShortLE());
                    }
                    if (BitUtil.check(dataMask, 2)) {
                        position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedShortLE());
                    }
                    if (BitUtil.check(dataMask, 3)) {
                        buf.skipBytes(18);
                    }
                    if (BitUtil.check(dataMask, 4)) {
                        buf.readUnsignedByte(); // fuel 1 temperature
                    }
                    if (BitUtil.check(dataMask, 5)) {
                        buf.readUnsignedByte(); // fuel 2 temperature
                    }
                    if (BitUtil.check(dataMask, 6)) {
                        buf.readUnsignedShortLE(); // fuel 1 frequency
                    }
                    if (BitUtil.check(dataMask, 7)) {
                        buf.readUnsignedShortLE(); // fuel 2 frequency
                    }
                }

                if (BitUtil.check(blockMask, 4)) {
                    int dataMask = buf.readUnsignedByte();
                    if (BitUtil.check(dataMask, 0)) {
                        buf.readUnsignedByte(); // fuel level (percentage)
                        position.set(Position.KEY_RPM, buf.readUnsignedShortLE());
                        position.set(Position.KEY_COOLANT_TEMP, (int) buf.readByte());
                    }
                    if (BitUtil.check(dataMask, 1)) {
                        buf.readUnsignedIntLE(); // fuel consumption
                    }
                    if (BitUtil.check(dataMask, 2)) {
                        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
                    }
                    for (int i = 3; i < 8; i++) {
                        if (BitUtil.check(dataMask, i)) {
                            buf.readUnsignedShortLE(); // axle weight
                        }
                    }
                }

                if (BitUtil.check(blockMask, 4)) {
                    int dataMask = buf.readUnsignedByte();
                    if (BitUtil.check(dataMask, 0)) {
                        buf.readUnsignedByte(); // speed
                    }
                    if (BitUtil.check(dataMask, 1)) {
                        buf.readUnsignedMediumLE(); // prefix S
                    }
                    if (BitUtil.check(dataMask, 2)) {
                        buf.readUnsignedIntLE(); // prefix P
                    }
                    if (BitUtil.check(dataMask, 3)) {
                        buf.readUnsignedIntLE(); // prefix A or B
                    }
                    if (BitUtil.check(dataMask, 4)) {
                        buf.readUnsignedShortLE(); // prefix R
                    }
                    if (BitUtil.check(dataMask, 5)) {
                        buf.skipBytes(26);
                    }
                    if (BitUtil.check(dataMask, 6)) {
                        buf.readUnsignedIntLE(); // reserved
                    }
                    if (BitUtil.check(dataMask, 7)) {
                        buf.readUnsignedIntLE(); // reserved
                    }
                }

                positions.add(position);
            }

            return positions;
        }

        return null;
    }

}
