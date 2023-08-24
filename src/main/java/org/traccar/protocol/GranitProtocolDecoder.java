/*
 * Copyright 2016 - 2020 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Checksum;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GranitProtocolDecoder extends BaseProtocolDecoder {

    private static final int HEADER_LENGTH = 6;

    public GranitProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static void appendChecksum(ByteBuf buffer, int length) {
        buffer.writeByte('*');
        int checksum = Checksum.xor(buffer.nioBuffer(0, length)) & 0xFF;
        String checksumString = String.format("%02X", checksum);
        buffer.writeBytes(checksumString.getBytes(StandardCharsets.US_ASCII));
        buffer.writeByte('\r'); buffer.writeByte('\n');
    }

    private static void sendResponseCurrent(Channel channel, int deviceId, long time) {
        ByteBuf response = Unpooled.buffer();
        response.writeBytes("BB+UGRC~".getBytes(StandardCharsets.US_ASCII));
        response.writeShortLE(6); // length
        response.writeInt((int) time);
        response.writeShortLE(deviceId);
        appendChecksum(response, 16);
        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
    }

    private static void sendResponseArchive(Channel channel, int deviceId, int packNum) {
        ByteBuf response = Unpooled.buffer();
        response.writeBytes("BB+ARCF~".getBytes(StandardCharsets.US_ASCII));
        response.writeShortLE(4); // length
        response.writeShortLE(packNum);
        response.writeShortLE(deviceId);
        appendChecksum(response, 14);
        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
    }

    private void decodeStructure(ByteBuf buf, Position position) {
        short flags = buf.readUnsignedByte();
        position.setValid(BitUtil.check(flags, 7));
        if (BitUtil.check(flags, 1)) {
            position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
        }

        short satDel = buf.readUnsignedByte();
        position.set(Position.KEY_SATELLITES, BitUtil.from(satDel, 4));

        int pdop = BitUtil.to(satDel, 4);
        position.set(Position.KEY_PDOP, pdop);

        int lonDegrees = buf.readUnsignedByte();
        int latDegrees = buf.readUnsignedByte();
        int lonMinutes = buf.readUnsignedShortLE();
        int latMinutes = buf.readUnsignedShortLE();

        double latitude = latDegrees + latMinutes / 60000.0;
        double longitude = lonDegrees + lonMinutes / 60000.0;

        if (position.getValid()) {
            if (!BitUtil.check(flags, 4)) {
                latitude = -latitude;
            }
            if (!BitUtil.check(flags, 5)) {
                longitude = -longitude;
            }
        }

        position.setLongitude(longitude);
        position.setLatitude(latitude);

        position.setSpeed(buf.readUnsignedByte());

        int course = buf.readUnsignedByte();
        if (BitUtil.check(flags, 6)) {
            course = course | 0x100;
        }
        position.setCourse(course);

        position.set(Position.KEY_DISTANCE, buf.readShortLE());

        int analogIn1 = buf.readUnsignedByte();
        int analogIn2 = buf.readUnsignedByte();
        int analogIn3 = buf.readUnsignedByte();
        int analogIn4 = buf.readUnsignedByte();

        int analogInHi = buf.readUnsignedByte();

        analogIn1 = analogInHi << 8 & 0x300 | analogIn1;
        analogIn2 = analogInHi << 6 & 0x300 | analogIn2;
        analogIn3 = analogInHi << 4 & 0x300 | analogIn3;
        analogIn4 = analogInHi << 2 & 0x300 | analogIn4;

        position.set(Position.PREFIX_ADC + 1, analogIn1);
        position.set(Position.PREFIX_ADC + 2, analogIn2);
        position.set(Position.PREFIX_ADC + 3, analogIn3);
        position.set(Position.PREFIX_ADC + 4, analogIn4);

        position.setAltitude(buf.readUnsignedByte() * 10);

        int output = buf.readUnsignedByte();
        for (int i = 0; i < 8; i++) {
            position.set(Position.PREFIX_IO + (i + 1), BitUtil.check(output, i));
        }
        buf.readUnsignedByte(); // status message buffer
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int indexTilde = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '~');

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);

        if (deviceSession != null && indexTilde == -1) {
            String bufString = buf.toString(StandardCharsets.US_ASCII);
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setTime(new Date());
            getLastLocation(position, new Date());
            position.setValid(false);
            position.set(Position.KEY_RESULT, bufString);
            return position;
        }

        if (buf.readableBytes() < HEADER_LENGTH) {
            return null;
        }
        String header = buf.readSlice(HEADER_LENGTH).toString(StandardCharsets.US_ASCII);

        if (header.equals("+RRCB~")) {

            buf.skipBytes(2); // binary length 26
            int deviceId = buf.readUnsignedShortLE();
            deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(deviceId));
            if (deviceSession == null) {
                return null;
            }
            long unixTime = buf.readUnsignedIntLE();
            if (channel != null) {
                sendResponseCurrent(channel, deviceId, unixTime);
            }
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setTime(new Date(unixTime * 1000));

            decodeStructure(buf, position);
            return position;

        } else if (header.equals("+DDAT~")) {

            buf.skipBytes(2); // binary length
            int deviceId = buf.readUnsignedShortLE();
            deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(deviceId));
            if (deviceSession == null) {
                return null;
            }
            byte format = buf.readByte();
            if (format != 4) {
                return null;
            }
            byte nblocks = buf.readByte();
            int packNum = buf.readUnsignedShortLE();
            if (channel != null) {
                sendResponseArchive(channel, deviceId, packNum);
            }
            List<Position> positions = new ArrayList<>();
            while (nblocks > 0) {
                nblocks--;
                long unixTime = buf.readUnsignedIntLE();
                int timeIncrement = buf.getUnsignedShortLE(buf.readerIndex() + 120);
                for (int i = 0; i < 6; i++) {
                    if (buf.getUnsignedByte(buf.readerIndex()) != 0xFE) {
                        Position position = new Position(getProtocolName());
                        position.setDeviceId(deviceSession.getDeviceId());
                        position.setTime(new Date((unixTime + i * timeIncrement) * 1000));
                        decodeStructure(buf, position);
                        position.set(Position.KEY_ARCHIVE, true);
                        positions.add(position);
                    } else {
                        buf.skipBytes(20); // skip filled 0xFE structure
                    }
                }
                buf.skipBytes(2); // increment
            }
            return positions;

        }

        return null;
    }

}
