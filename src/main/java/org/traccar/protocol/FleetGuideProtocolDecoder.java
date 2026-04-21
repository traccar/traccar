/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class FleetGuideProtocolDecoder extends BaseProtocolDecoder {

    public FleetGuideProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_EMPTY = 0;
    public static final int MSG_SYNC_REQ = 1;
    public static final int MSG_SYNC_ACK = 2;
    public static final int MSG_DATA_R_ACK = 3;
    public static final int MSG_DATA_N_ACK = 4;
    public static final int MSG_REP_R_ACK = 5;
    public static final int MSG_REP_N_ACK = 6;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // signature
        int options = buf.readUnsignedShortLE();
        int length = BitUtil.to(options, 11);

        DeviceSession deviceSession;
        Long deviceId;
        if (BitUtil.check(options, 11)) {
            deviceId = buf.readUnsignedIntLE();
            deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(deviceId));
        } else {
            deviceId = null;
            deviceSession = getDeviceSession(channel, remoteAddress);
        }
        if (deviceSession == null) {
            return null;
        }

        int type;
        Integer index;
        if (BitUtil.check(options, 12)) {
            int value = buf.readUnsignedByte();
            type = BitUtil.to(value, 4);
            index = BitUtil.from(value, 4);
        } else {
            type = 0;
            index = null;
        }

        if (type != MSG_DATA_N_ACK && type != MSG_REP_N_ACK) {
            Integer responseType;
            if (type == MSG_SYNC_REQ) {
                responseType = MSG_SYNC_ACK;
            } else {
                responseType = null;
            }
            sendResponse(channel, remoteAddress, deviceId, responseType, index);
        }

        if (BitUtil.check(options, 13)) {
            buf.readUnsignedShortLE(); // acknowledgement
        }

        ByteBuf data;
        if (BitUtil.check(options, 14)) {
            data = decompress(buf.readSlice(length));
        } else {
            data = buf.readRetainedSlice(length);
        }

        List<Position> positions = new LinkedList<>();

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        while (data.isReadable()) {

            int recordHeader = data.readUnsignedShortLE();
            int recordLength = BitUtil.to(recordHeader, 10);
            int recordType = BitUtil.from(recordHeader, 10);
            int recordEndIndex = data.readerIndex() + recordLength;

            if (recordType == 0 && position.getDeviceTime() != null) {
                processPosition(positions, position);
                position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());
            }

            switch (recordType) {
                case 0:
                    position.setTime(new Date((data.readUnsignedIntLE() + 1262304000) * 1000)); // since 2010-01-01
                    break;
                case 1:
                    position.setLatitude(data.readUnsignedIntLE() * 90.0 / 0xFFFFFFFFL);
                    position.setLongitude(data.readUnsignedIntLE() * 180.0 / 0xFFFFFFFFL);
                    int speed = data.readUnsignedShortLE();
                    position.setSpeed(UnitsConverter.knotsFromKph(BitUtil.to(speed, 14) * 0.1));
                    if (BitUtil.check(speed, 14)) {
                        position.setLatitude(-position.getLatitude());
                    }
                    if (BitUtil.check(speed, 15)) {
                        position.setLongitude(-position.getLongitude());
                    }
                    int course = data.readUnsignedShortLE();
                    position.setSpeed(BitUtil.to(course, 9));
                    int motion = BitUtil.between(course, 9, 11);
                    if (motion > 0) {
                        position.set(Position.KEY_MOTION, motion == 1);
                    }
                    position.set(Position.KEY_SATELLITES, BitUtil.from(course, 11));
                    int altitude = data.readUnsignedShortLE();
                    position.setAltitude(BitUtil.to(altitude, 14));
                    if (BitUtil.check(altitude, 14)) {
                        position.setAltitude(-position.getAltitude());
                    }
                    break;
                case 3:
                    int powerLow = data.readUnsignedByte();
                    int powerFlags = data.readUnsignedByte();
                    int batteryHigh = data.readUnsignedByte();
                    position.set(Position.KEY_POWER, (powerLow + (BitUtil.to(powerFlags, 5) << 8)) * 0.01);
                    position.set(Position.KEY_IGNITION, BitUtil.check(powerFlags, 5));
                    position.set(Position.KEY_BATTERY, (BitUtil.from(powerFlags, 6) + (batteryHigh << 2)) * 0.01);
                    if (recordLength >= 4) {
                        int extraFlags = data.readUnsignedByte();
                        if (BitUtil.check(extraFlags, 0)) {
                            position.addAlarm(Position.ALARM_LOW_POWER);
                        }
                        if (BitUtil.check(extraFlags, 1)) {
                            position.addAlarm(Position.ALARM_LOW_BATTERY);
                        }
                    }
                    break;
                case 6:
                    position.set(Position.KEY_INPUT, data.readUnsignedByte());
                    break;
                case 7:
                    position.set(Position.KEY_OUTPUT, data.readUnsignedByte());
                    break;
                case 8:
                    int adcMask = data.readUnsignedByte();
                    for (int i = 0; i < 8; i++) {
                        if (BitUtil.check(adcMask, i)) {
                            position.set(Position.PREFIX_ADC + (i + 1), data.readUnsignedShortLE());
                        }
                    }
                    break;
                case 11:
                    int fuelMask = data.readUnsignedByte();
                    for (int i = 1; i < 8; i++) {
                        if (BitUtil.check(fuelMask, i)) {
                            position.set("fuel" + i, data.readUnsignedShortLE());
                        }
                    }
                    break;
                case 12:
                    int fuelTempMask = data.readUnsignedByte();
                    for (int i = 1; i < 8; i++) {
                        if (BitUtil.check(fuelTempMask, i)) {
                            position.set("fuelTemp" + i, (int) data.readByte());
                        }
                    }
                    break;
                case 13:
                    int tempMask = data.readUnsignedByte();
                    for (int i = 0; i < 8; i++) {
                        if (BitUtil.check(tempMask, i)) {
                            position.set(Position.PREFIX_TEMP + (i + 1), data.readShortLE() * 0.01);
                        }
                    }
                    break;
                case 18:
                    int sensorIndex = data.readUnsignedByte();
                    switch (recordLength - 1) {
                        case 1 -> position.set("sensor" + sensorIndex, data.readUnsignedByte());
                        case 2 -> position.set("sensor" + sensorIndex, data.readUnsignedShortLE());
                        case 4 -> position.set("sensor" + sensorIndex, data.readUnsignedIntLE());
                    }
                    break;
                default:
                    break;
            }

            data.readerIndex(recordEndIndex);

        }

        processPosition(positions, position);

        data.release();

        return positions.isEmpty() ? null : positions;
    }

    private void processPosition(List<Position> positions, Position position) {
        if (!position.getAttributes().isEmpty()) {
            if (position.getFixTime() == null) {
                position.setTime(new Date());
            }
            if (!position.getAttributes().containsKey(Position.KEY_SATELLITES)) {
                getLastLocation(position, null);
            }
            positions.add(position);
        }
    }


    private void sendResponse(
            Channel channel, SocketAddress remoteAddress, Long deviceId, Integer type, Integer index) {
        if (channel != null) {

            ByteBuf response = Unpooled.buffer();
            response.writeByte(0x53); // signature

            int options = 0;
            if (deviceId != null) {
                options |= 1 << 11;
            }
            if (type != null) {
                options |= 1 << 12;
            }
            if (index != null) {
                options |= 1 << 13;
            }
            response.writeShortLE(options);

            if (deviceId != null) {
                response.writeIntLE(deviceId.intValue());
            }
            if (type != null) {
                response.writeByte(type);
            }
            if (index != null) {
                int mask = (1 << (index + 1)) - 1;
                response.writeShortLE(mask);
            }
            response.writeShortLE(Checksum.crc16(
                    Checksum.CRC16_CCITT_FALSE, response.nioBuffer(1, response.writerIndex() - 1)));

            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private int readVarSize(ByteBuf buf) {
        int b;
        int y = 0;
        do {
            b = buf.readUnsignedByte();
            y = (y << 7) | (b & 0x0000007f);
        } while ((b & 0x00000080) > 0);

        return y;
    }

    private ByteBuf decompress(ByteBuf in) {

        ByteBuf out = Unpooled.buffer();

        if (in.readableBytes() < 1) {
            return out;
        }

        int marker = in.readUnsignedByte();

        do {
            int symbol = in.readUnsignedByte();
            if (symbol == marker) {
                if (in.getUnsignedByte(in.readerIndex()) == 0) {
                    out.writeByte(marker);
                    in.skipBytes(1);
                } else {
                    int length = readVarSize(in);
                    int offset = readVarSize(in);

                    for (int i = 0; i < length; i++) {
                        out.writeByte(out.getUnsignedByte(out.writerIndex() - offset));
                    }
                }
            } else {
                out.writeByte(symbol);
            }
        } while (in.isReadable());

        return out;
    }

}
