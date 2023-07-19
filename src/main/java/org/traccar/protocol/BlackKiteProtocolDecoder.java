/*
 * Copyright 2013 - 2018 Anton Tananaev (anton@traccar.org)
 * Copyright 2015 Vijay Kumar (vijaykumar@zilogic.com)
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
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BlackKiteProtocolDecoder extends BaseProtocolDecoder {

    public BlackKiteProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final int TAG_IMEI = 0x03;
    private static final int TAG_DATE = 0x20;
    private static final int TAG_COORDINATES = 0x30;
    private static final int TAG_SPEED_COURSE = 0x33;
    private static final int TAG_ALTITUDE = 0x34;
    private static final int TAG_STATUS = 0x40;
    private static final int TAG_DIGITAL_OUTPUTS = 0x45;
    private static final int TAG_DIGITAL_INPUTS = 0x46;
    private static final int TAG_INPUT_VOLTAGE1 = 0x50;
    private static final int TAG_INPUT_VOLTAGE2 = 0x51;
    private static final int TAG_INPUT_VOLTAGE3 = 0x52;
    private static final int TAG_INPUT_VOLTAGE4 = 0x53;
    private static final int TAG_XT1 = 0x60;
    private static final int TAG_XT2 = 0x61;
    private static final int TAG_XT3 = 0x62;

    private void sendResponse(Channel channel, int checksum) {
        if (channel != null) {
            ByteBuf reply = Unpooled.buffer(3);
            reply.writeByte(0x02);
            reply.writeShortLE((short) checksum);
            channel.writeAndFlush(new NetworkMessage(reply, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // header
        int length = (buf.readUnsignedShortLE() & 0x7fff) + 3;

        List<Position> positions = new LinkedList<>();
        Set<Integer> tags = new HashSet<>();
        boolean hasLocation = false;
        Position position = new Position(getProtocolName());

        while (buf.readerIndex() < length) {

            // Check if new message started
            int tag = buf.readUnsignedByte();
            if (tags.contains(tag)) {
                if (hasLocation && position.getFixTime() != null) {
                    positions.add(position);
                }
                tags.clear();
                hasLocation = false;
                position = new Position(getProtocolName());
            }
            tags.add(tag);

            switch (tag) {

                case TAG_IMEI:
                    getDeviceSession(channel, remoteAddress, buf.readSlice(15).toString(StandardCharsets.US_ASCII));
                    break;

                case TAG_DATE:
                    position.setTime(new Date(buf.readUnsignedIntLE() * 1000));
                    break;

                case TAG_COORDINATES:
                    hasLocation = true;
                    position.setValid((buf.readUnsignedByte() & 0xf0) == 0x00);
                    position.setLatitude(buf.readIntLE() / 1000000.0);
                    position.setLongitude(buf.readIntLE() / 1000000.0);
                    break;

                case TAG_SPEED_COURSE:
                    position.setSpeed(buf.readUnsignedShortLE() * 0.0539957);
                    position.setCourse(buf.readUnsignedShortLE() * 0.1);
                    break;

                case TAG_ALTITUDE:
                    position.setAltitude(buf.readShortLE());
                    break;

                case TAG_STATUS:
                    int status = buf.readUnsignedShortLE();
                    position.set(Position.KEY_IGNITION, BitUtil.check(status, 9));
                    if (BitUtil.check(status, 15)) {
                        position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
                    }
                    position.set(Position.KEY_CHARGE, BitUtil.check(status, 2));
                    break;

                case TAG_DIGITAL_INPUTS:
                    int input = buf.readUnsignedShortLE();
                    for (int i = 0; i < 16; i++) {
                        position.set(Position.PREFIX_IO + (i + 1), BitUtil.check(input, i));
                    }
                    break;

                case TAG_DIGITAL_OUTPUTS:
                    int output = buf.readUnsignedShortLE();
                    for (int i = 0; i < 16; i++) {
                        position.set(Position.PREFIX_IO + (i + 17), BitUtil.check(output, i));
                    }
                    break;

                case TAG_INPUT_VOLTAGE1:
                    position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShortLE() / 1000.0);
                    break;

                case TAG_INPUT_VOLTAGE2:
                    position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShortLE() / 1000.0);
                    break;

                case TAG_INPUT_VOLTAGE3:
                    position.set(Position.PREFIX_ADC + 3, buf.readUnsignedShortLE() / 1000.0);
                    break;

                case TAG_INPUT_VOLTAGE4:
                    position.set(Position.PREFIX_ADC + 4, buf.readUnsignedShortLE() / 1000.0);
                    break;

                case TAG_XT1:
                case TAG_XT2:
                case TAG_XT3:
                    buf.skipBytes(16);
                    break;

                default:
                    break;

            }
        }

        if (hasLocation && position.getFixTime() != null) {
            positions.add(position);
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        sendResponse(channel, buf.readUnsignedShortLE());

        for (Position p : positions) {
            p.setDeviceId(deviceSession.getDeviceId());
        }

        if (positions.isEmpty()) {
            return null;
        }

        return positions;
    }

}
