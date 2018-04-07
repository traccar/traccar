/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class DmtProtocolDecoder extends BaseProtocolDecoder {

    public DmtProtocolDecoder(DmtProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_HELLO = 0x00;
    public static final int MSG_HELLO_RESPONSE = 0x01;
    public static final int MSG_DATA_RECORD = 0x04;
    public static final int MSG_COMMIT = 0x05;
    public static final int MSG_COMMIT_RESPONSE = 0x06;
    public static final int MSG_DATA_RECORD_64 = 0x10;

    public static final int MSG_CANNED_REQUEST_1 = 0x14;
    public static final int MSG_CANNED_RESPONSE_1 = 0x15;
    public static final int MSG_CANNED_REQUEST_2 = 0x22;
    public static final int MSG_CANNED_RESPONSE_2 = 0x23;

    private void sendResponse(Channel channel, int type, ChannelBuffer content) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 0);
            response.writeByte(0x02); response.writeByte(0x55); // header
            response.writeByte(type);
            response.writeShort(content != null ? content.readableBytes() : 0);
            if (content != null) {
                response.writeBytes(content);
            }
            channel.write(response);
        }
    }

    private List<Position> decodeFixed64(Channel channel, SocketAddress remoteAddress, ChannelBuffer buf) {

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        List<Position> positions = new LinkedList<>();

        while (buf.readableBytes() >= 64) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            buf.readByte(); // type

            position.set(Position.KEY_INDEX, buf.readUnsignedInt());

            long time = buf.readUnsignedInt();
            position.setTime(new DateBuilder()
                    .setYear((int) (2000 + (time & 0x3F)))
                    .setMonth((int) (time >> 6) & 0xF)
                    .setDay((int) (time >> 10) & 0x1F)
                    .setHour((int) (time >> 15) & 0x1F)
                    .setMinute((int) (time >> 20) & 0x3F)
                    .setSecond((int) (time >> 26) & 0x3F)
                    .getDate());

            position.setLongitude(buf.readInt() * 0.0000001);
            position.setLatitude(buf.readInt() * 0.0000001);
            position.setSpeed(UnitsConverter.knotsFromCps(buf.readUnsignedShort()));
            position.setCourse(buf.readUnsignedByte() * 2);
            position.setAltitude(buf.readShort());

            buf.readUnsignedShort(); // position accuracy
            buf.readUnsignedByte(); // speed accuracy

            position.set(Position.KEY_EVENT, buf.readUnsignedByte());

            position.setValid(BitUtil.check(buf.readByte(), 0));

            position.set(Position.KEY_INPUT, buf.readUnsignedInt());
            position.set(Position.KEY_OUTPUT, buf.readUnsignedShort());

            for (int i = 1; i <= 5; i++) {
                position.set(Position.PREFIX_ADC + i, buf.readShort());
            }

            position.set(Position.KEY_DEVICE_TEMP, buf.readByte());

            buf.readShort(); // accelerometer x
            buf.readShort(); // accelerometer y
            buf.readShort(); // accelerometer z

            buf.skipBytes(8); // device id

            position.set(Position.KEY_PDOP, buf.readUnsignedShort() * 0.01);

            buf.skipBytes(2); // reserved

            buf.readUnsignedShort(); // checksum

            positions.add(position);
        }

        return positions;
    }

    private List<Position> decodeStandard(Channel channel, SocketAddress remoteAddress, ChannelBuffer buf) {

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        List<Position> positions = new LinkedList<>();

        while (buf.readable()) {
            int recordEnd = buf.readerIndex() + buf.readUnsignedShort();

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.set(Position.KEY_INDEX, buf.readUnsignedInt());

            position.setDeviceTime(new Date(1356998400000L + buf.readUnsignedInt() * 1000)); // since 1 Jan 2013

            position.set(Position.KEY_EVENT, buf.readUnsignedByte());

            while (buf.readerIndex() < recordEnd) {

                int fieldId = buf.readUnsignedByte();
                int fieldLength = buf.readUnsignedByte();
                int fieldEnd = buf.readerIndex() + (fieldLength == 255 ? buf.readUnsignedShort() : fieldLength);

                if (fieldId == 0) {

                    position.setFixTime(new Date(1356998400000L + buf.readUnsignedInt() * 1000));
                    position.setLatitude(buf.readInt() * 0.0000001);
                    position.setLongitude(buf.readInt() * 0.0000001);
                    position.setAltitude(buf.readShort());
                    position.setSpeed(UnitsConverter.knotsFromCps(buf.readUnsignedShort()));

                    buf.readUnsignedByte(); // speed accuracy

                    position.setCourse(buf.readUnsignedByte() * 2);

                    position.set(Position.KEY_PDOP, buf.readUnsignedByte() * 0.1);

                    position.setAccuracy(buf.readUnsignedByte());
                    position.setValid(buf.readUnsignedByte() != 0);

                } else if (fieldId == 2) {

                    int input = buf.readInt();
                    int output = buf.readUnsignedShort();
                    int status = buf.readUnsignedShort();

                    position.set(Position.KEY_IGNITION, BitUtil.check(input, 0));

                    position.set(Position.KEY_INPUT, input);
                    position.set(Position.KEY_OUTPUT, output);
                    position.set(Position.KEY_STATUS, status);

                } else if (fieldId == 6) {

                    while (buf.readerIndex() < fieldEnd) {
                        switch (buf.readUnsignedByte()) {
                            case 1:
                                position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);
                                break;
                            case 2:
                                position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);
                                break;
                            case 3:
                                position.set(Position.KEY_DEVICE_TEMP, buf.readShort() * 0.01);
                                break;
                            case 4:
                                position.set(Position.KEY_RSSI, buf.readUnsignedShort());
                                break;
                            case 5:
                                position.set("solarPower", buf.readUnsignedShort() * 0.001);
                                break;
                            default:
                                break;
                        }
                    }

                }

                buf.readerIndex(fieldEnd);

            }

            if (position.getFixTime() == null) {
                getLastLocation(position, position.getDeviceTime());
            }

            positions.add(position);
        }

        return positions;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header

        int type = buf.readUnsignedByte();
        int length = buf.readUnsignedShort();

        if (type == MSG_HELLO) {

            buf.readUnsignedInt(); // device serial number

            DeviceSession deviceSession = getDeviceSession(
                    channel, remoteAddress, buf.readBytes(15).toString(StandardCharsets.US_ASCII));

            ChannelBuffer response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 0);
            if (length == 51) {
                response.writeByte(0); // reserved
                response.writeInt(0); // reserved
            } else {
                response.writeInt((int) ((System.currentTimeMillis() - 1356998400000L) / 1000));
                response.writeInt(deviceSession != null ? 0 : 1); // flags
            }

            sendResponse(channel, MSG_HELLO_RESPONSE, response);

        } else if (type == MSG_COMMIT) {

            ChannelBuffer response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 0);
            response.writeByte(1); // flags (success)
            sendResponse(channel, MSG_COMMIT_RESPONSE, response);

        } else if (type == MSG_CANNED_REQUEST_1) {

            ChannelBuffer response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 0);
            response.writeBytes(new byte[12]);
            sendResponse(channel, MSG_CANNED_RESPONSE_1, response);

        } else if (type == MSG_CANNED_REQUEST_2) {

            sendResponse(channel, MSG_CANNED_RESPONSE_2, null);

        } else if (type == MSG_DATA_RECORD_64) {

            return decodeFixed64(channel, remoteAddress, buf);

        } else if (type == MSG_DATA_RECORD) {

            return decodeStandard(channel, remoteAddress, buf);

        }

        return null;
    }

}
