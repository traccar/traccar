/*
 * Copyright 2017 Ivan Muratov (binakot@gmail.com)
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
import org.traccar.helper.Checksum;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Arnavi4ProtocolDecoder extends BaseProtocolDecoder {

    private static final byte HEADER_START_SIGN = (byte) 0xff;
    private static final byte HEADER_VERSION_1 = 0x22;
    private static final byte HEADER_VERSION_2 = 0x23;

    private static final byte RECORD_PING = 0x00;
    private static final byte RECORD_DATA = 0x01;
    private static final byte RECORD_TEXT = 0x03;
    private static final byte RECORD_FILE = 0x04;
    private static final byte RECORD_BINARY = 0x06;

    private static final byte TAG_LATITUDE = 3;
    private static final byte TAG_LONGITUDE = 4;
    private static final byte TAG_COORD_PARAMS = 5;

    public Arnavi4ProtocolDecoder(Arnavi4Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, byte version, int index) {
        if (channel != null) {
            final ChannelBuffer response;
            if (version == HEADER_VERSION_1) {
                response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 4);
                response.writeByte(0x7b);
                response.writeByte(0x00);
                response.writeByte((byte) index);
                response.writeByte(0x7d);
            } else if (version == HEADER_VERSION_2) {
                response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 9);
                response.writeByte(0x7b);
                response.writeByte(0x04);
                response.writeByte(0x00);
                byte[] timeBytes = ByteBuffer.allocate(4).putInt((int) (System.currentTimeMillis() / 1000)).array();
                response.writeByte(Checksum.modulo256(timeBytes));
                response.writeBytes(timeBytes);
                response.writeByte(0x7d);
            } else {
                return; // Ignore unsupported header's versions
            }
            channel.write(response);
        }
    }

    private Position decodePosition(DeviceSession deviceSession, ChannelBuffer buf, int length, Date time) {

        final Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(time);

        int readBytes = 0;
        while (readBytes < length) {
            short tag = buf.readUnsignedByte();
            switch (tag) {
                case TAG_LATITUDE:
                    position.setLatitude(buf.readFloat());
                    position.setValid(true);
                    break;

                case TAG_LONGITUDE:
                    position.setLongitude(buf.readFloat());
                    position.setValid(true);
                    break;

                case TAG_COORD_PARAMS:
                    position.setCourse(buf.readUnsignedByte() * 2);
                    position.setAltitude(buf.readUnsignedByte() * 10);
                    byte satellites = buf.readByte();
                    position.set(Position.KEY_SATELLITES, satellites & 0x0F + (satellites >> 4) & 0x0F); // gps+glonass
                    position.setSpeed(buf.readUnsignedByte());
                    break;

                default:
                    buf.readBytes(4); // Skip unsupported tags
                    break;
            }

            readBytes += 5; // 1 byte tag + 4 bytes value
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        byte startSign = buf.readByte();

        if (startSign == HEADER_START_SIGN) {

            byte version = buf.readByte();

            String imei = String.valueOf(buf.readLong());
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);

            if (deviceSession != null) {
                sendResponse(channel, version, 0);
            }

            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        List<Position> positions = new LinkedList<>();

        int index = buf.readUnsignedByte();

        byte recordType = buf.readByte();
        while (buf.readableBytes() > 0) {
            switch (recordType) {
                case RECORD_PING:
                case RECORD_DATA:
                case RECORD_TEXT:
                case RECORD_FILE:
                case RECORD_BINARY:
                    int length = buf.readUnsignedShort();
                    Date time = new Date(buf.readUnsignedInt() * 1000);

                    if (recordType == RECORD_DATA) {
                        positions.add(decodePosition(deviceSession, buf, length, time));
                    } else {
                        buf.readBytes(length); // Skip other types of record
                    }

                    buf.readUnsignedByte(); // crc
                    break;

                default:
                    return null; // Ignore unsupported types of record
            }

            recordType = buf.readByte();
        }

        sendResponse(channel, HEADER_VERSION_1, index);

        return positions;
    }

}
