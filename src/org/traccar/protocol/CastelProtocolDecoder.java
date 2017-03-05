/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.ObdDecoder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CastelProtocolDecoder extends BaseProtocolDecoder {

    private static final Map<Integer, Integer> PID_LENGTH_MAP = new HashMap<>();

    static {
        int[] l1 = {
            0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0b, 0x0d,
            0x0e, 0x0f, 0x11, 0x12, 0x13, 0x1c, 0x1d, 0x1e, 0x2c,
            0x2d, 0x2e, 0x2f, 0x30, 0x33, 0x43, 0x45, 0x46,
            0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x51, 0x52,
            0x5a
        };
        int[] l2 = {
            0x02, 0x03, 0x0a, 0x0c, 0x10, 0x14, 0x15, 0x16,
            0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1f, 0x21, 0x22,
            0x23, 0x31, 0x32, 0x3c, 0x3d, 0x3e, 0x3f, 0x42,
            0x44, 0x4d, 0x4e, 0x50, 0x53, 0x54, 0x55, 0x56,
            0x57, 0x58, 0x59
        };
        int[] l4 = {
            0x00, 0x01, 0x20, 0x24, 0x25, 0x26, 0x27, 0x28,
            0x29, 0x2a, 0x2b, 0x34, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x3a, 0x3b, 0x40, 0x41, 0x4f
        };
        for (int i : l1) {
            PID_LENGTH_MAP.put(i, 1);
        }
        for (int i : l2) {
            PID_LENGTH_MAP.put(i, 2);
        }
        for (int i : l4) {
            PID_LENGTH_MAP.put(i, 4);
        }
    }

    public CastelProtocolDecoder(CastelProtocol protocol) {
        super(protocol);
    }

    public static final short MSG_SC_LOGIN = 0x1001;
    public static final short MSG_SC_LOGIN_RESPONSE = (short) 0x9001;
    public static final short MSG_SC_LOGOUT = 0x1002;
    public static final short MSG_SC_HEARTBEAT = 0x1003;
    public static final short MSG_SC_HEARTBEAT_RESPONSE = (short) 0x9003;
    public static final short MSG_SC_GPS = 0x4001;
    public static final short MSG_SC_PID_DATA = 0x4002;
    public static final short MSG_SC_SUPPORTED_PID = 0x4004;
    public static final short MSG_SC_OBD_DATA = 0x4005;
    public static final short MSG_SC_DTCS_PASSENGER = 0x4006;
    public static final short MSG_SC_DTCS_COMMERCIAL = 0x400B;
    public static final short MSG_SC_ALARM = 0x4007;
    public static final short MSG_SC_CELL = 0x4008;
    public static final short MSG_SC_GPS_SLEEP = 0x4009;
    public static final short MSG_SC_AGPS_REQUEST = 0x5101;
    public static final short MSG_SC_CURRENT_LOCATION = (short) 0xB001;

    public static final short MSG_CC_LOGIN = 0x4001;
    public static final short MSG_CC_LOGIN_RESPONSE = (short) 0x8001;
    public static final short MSG_CC_HEARTBEAT = 0x4206;
    public static final short MSG_CC_HEARTBEAT_RESPONSE = (short) 0x8206;

    private Position readPosition(DeviceSession deviceSession, ChannelBuffer buf) {

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        position.setTime(dateBuilder.getDate());

        double lat = buf.readUnsignedInt() / 3600000.0;
        double lon = buf.readUnsignedInt() / 3600000.0;
        position.setSpeed(UnitsConverter.knotsFromCps(buf.readUnsignedShort()));
        position.setCourse(buf.readUnsignedShort() * 0.1);

        int flags = buf.readUnsignedByte();
        if ((flags & 0x02) == 0) {
            lat = -lat;
        }
        if ((flags & 0x01) == 0) {
            lon = -lon;
        }
        position.setLatitude(lat);
        position.setLongitude(lon);
        position.setValid((flags & 0x0C) > 0);
        position.set(Position.KEY_SATELLITES, flags >> 4);

        return position;
    }

    private Position createPosition(DeviceSession deviceSession) {

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        return position;
    }

    private void decodeObd(Position position, ChannelBuffer buf, boolean groups) {

        int count = buf.readUnsignedByte();

        int[] pids = new int[count];
        for (int i = 0; i < count; i++) {
            pids[i] = buf.readUnsignedShort() & 0xff;
        }

        if (groups) {
            buf.readUnsignedByte(); // group count
            buf.readUnsignedByte(); // group size
        }

        for (int i = 0; i < count; i++) {
            int value;
            switch (PID_LENGTH_MAP.get(pids[i])) {
                case 1:
                    value = buf.readUnsignedByte();
                    break;
                case 2:
                    value = buf.readUnsignedShort();
                    break;
                case 4:
                    value = buf.readInt();
                    break;
                default:
                    value = 0;
                    break;
            }
            position.add(ObdDecoder.decodeData(pids[i], value, false));
        }
    }

    private void decodeStat(Position position, ChannelBuffer buf) {

        buf.readUnsignedInt(); // ACC ON time
        buf.readUnsignedInt(); // UTC time
        position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
        position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedInt());
        position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedInt());
        buf.readUnsignedShort(); // current fuel consumption
        position.set(Position.KEY_STATUS, buf.readUnsignedInt());
        buf.skipBytes(8);
    }

    private void sendResponse(
            Channel channel, SocketAddress remoteAddress,
            int version, ChannelBuffer id, short type, ChannelBuffer content) {

        if (channel != null) {
            int length = 2 + 2 + 1 + id.readableBytes() + 2 + 2 + 2;
            if (content != null) {
                length += content.readableBytes();
            }

            ChannelBuffer response = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, length);
            response.writeByte('@'); response.writeByte('@');
            response.writeShort(length);
            response.writeByte(version);
            response.writeBytes(id);
            response.writeShort(ChannelBuffers.swapShort(type));
            if (content != null) {
                response.writeBytes(content);
            }
            response.writeShort(
                    Checksum.crc16(Checksum.CRC16_X25, response.toByteBuffer(0, response.writerIndex())));
            response.writeByte(0x0D); response.writeByte(0x0A);
            channel.write(response, remoteAddress);
        }
    }

    private void sendResponse(
            Channel channel, SocketAddress remoteAddress, ChannelBuffer id, short type) {

        if (channel != null) {
            int length = 2 + 2 + id.readableBytes() + 2 + 4 + 8 + 2 + 2;

            ChannelBuffer response = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, length);
            response.writeByte('@'); response.writeByte('@');
            response.writeShort(length);
            response.writeBytes(id);
            response.writeShort(ChannelBuffers.swapShort(type));
            response.writeInt(0);
            for (int i = 0; i < 8; i++) {
                response.writeByte(0xff);
            }
            response.writeShort(
                    Checksum.crc16(Checksum.CRC16_X25, response.toByteBuffer(0, response.writerIndex())));
            response.writeByte(0x0D); response.writeByte(0x0A);
            channel.write(response, remoteAddress);
        }
    }

    private Object decodeSc(
            Channel channel, SocketAddress remoteAddress, ChannelBuffer buf,
            int version, ChannelBuffer id, int type, DeviceSession deviceSession) {

        if (type == MSG_SC_HEARTBEAT) {

            sendResponse(channel, remoteAddress, version, id, MSG_SC_HEARTBEAT_RESPONSE, null);

        } else if (type == MSG_SC_LOGIN || type == MSG_SC_LOGOUT || type == MSG_SC_GPS
                || type == MSG_SC_ALARM || type == MSG_SC_CURRENT_LOCATION) {

            if (type == MSG_SC_LOGIN) {
                ChannelBuffer response = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 10);
                response.writeInt(0xFFFFFFFF);
                response.writeShort(0);
                response.writeInt((int) (System.currentTimeMillis() / 1000));
                sendResponse(channel, remoteAddress, version, id, MSG_SC_LOGIN_RESPONSE, response);
            }

            if (type == MSG_SC_GPS) {
                buf.readUnsignedByte(); // historical
            } else if (type == MSG_SC_ALARM) {
                buf.readUnsignedInt(); // alarm
            } else if (type == MSG_SC_CURRENT_LOCATION) {
                buf.readUnsignedShort();
            }

            buf.readUnsignedInt(); // ACC ON time
            buf.readUnsignedInt(); // UTC time
            long odometer = buf.readUnsignedInt();
            long tripOdometer = buf.readUnsignedInt();
            long fuelConsumption = buf.readUnsignedInt();
            buf.readUnsignedShort(); // current fuel consumption
            long status = buf.readUnsignedInt();
            buf.skipBytes(8);

            int count = buf.readUnsignedByte();

            List<Position> positions = new LinkedList<>();

            for (int i = 0; i < count; i++) {
                Position position = readPosition(deviceSession, buf);
                position.set(Position.KEY_ODOMETER, odometer);
                position.set(Position.KEY_ODOMETER_TRIP, tripOdometer);
                position.set(Position.KEY_FUEL_CONSUMPTION, fuelConsumption);
                position.set(Position.KEY_STATUS, status);
                positions.add(position);
            }

            if (!positions.isEmpty()) {
                return positions;
            }

        } else if (type == MSG_SC_GPS_SLEEP) {

            buf.readUnsignedInt(); // device time

            return readPosition(deviceSession, buf);

        } else if (type == MSG_SC_AGPS_REQUEST) {

            return readPosition(deviceSession, buf);

        } else if (type == MSG_SC_PID_DATA) {

            Position position = createPosition(deviceSession);

            decodeStat(position, buf);

            buf.readUnsignedShort(); // sample rate
            decodeObd(position, buf, true);

            return position;

        } else if (type == MSG_SC_DTCS_PASSENGER) {

            Position position = createPosition(deviceSession);

            decodeStat(position, buf);

            buf.readUnsignedByte(); // flag
            position.add(ObdDecoder.decodeCodes(ChannelBuffers.hexDump(buf.readBytes(buf.readUnsignedByte()))));

            return position;

        } else if (type == MSG_SC_OBD_DATA) {

            Position position = createPosition(deviceSession);

            decodeStat(position, buf);

            buf.readUnsignedByte(); // flag
            decodeObd(position, buf, false);

            return position;

        } else if (type == MSG_SC_CELL) {

            Position position = createPosition(deviceSession);

            decodeStat(position, buf);

            position.setNetwork(new Network(
                    CellTower.fromLacCid(buf.readUnsignedShort(), buf.readUnsignedShort())));

            return position;

        }

        return null;
    }


    private Object decodeCc(
            Channel channel, SocketAddress remoteAddress, ChannelBuffer buf,
            int version, ChannelBuffer id, int type, DeviceSession deviceSession) {

        if (type == MSG_CC_HEARTBEAT) {

            sendResponse(channel, remoteAddress, version, id, MSG_CC_HEARTBEAT_RESPONSE, null);

            buf.readUnsignedByte(); // 0x01 for history
            int count = buf.readUnsignedByte();

            List<Position> positions = new LinkedList<>();

            for (int i = 0; i < count; i++) {
                Position position = readPosition(deviceSession, buf);

                position.set(Position.KEY_STATUS, buf.readUnsignedInt());
                position.set(Position.KEY_BATTERY, buf.readUnsignedByte());
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

                buf.readUnsignedByte(); // geo-fencing id
                buf.readUnsignedByte(); // geo-fencing flags
                buf.readUnsignedByte(); // additional flags

                position.setNetwork(new Network(
                        CellTower.fromLacCid(buf.readUnsignedShort(), buf.readUnsignedShort())));

                positions.add(position);
            }

            return positions;

        } else if (type == MSG_CC_LOGIN) {

            sendResponse(channel, remoteAddress, version, id, MSG_CC_LOGIN_RESPONSE, null);

            Position position = readPosition(deviceSession, buf);

            position.set(Position.KEY_STATUS, buf.readUnsignedInt());
            position.set(Position.KEY_BATTERY, buf.readUnsignedByte());
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

            buf.readUnsignedByte(); // geo-fencing id
            buf.readUnsignedByte(); // geo-fencing flags
            buf.readUnsignedByte(); // additional flags

            // GSM_CELL_CODE
            // STR_Z - firmware version
            // STR_Z - hardware version

            return position;

        }

        return null;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        int header = buf.readUnsignedShort();
        buf.readUnsignedShort(); // length

        int version = -1;
        if (header == 0x4040) {
            version = buf.readUnsignedByte();
        }

        ChannelBuffer id = buf.readBytes(20);
        int type = ChannelBuffers.swapShort(buf.readShort());

        DeviceSession deviceSession = getDeviceSession(
                channel, remoteAddress, id.toString(StandardCharsets.US_ASCII).trim());
        if (deviceSession == null) {
            return null;
        }

        if (version == -1) {

            if (type == 0x2001) {

                sendResponse(channel, remoteAddress, id, (short) 0x1001);

                buf.readUnsignedInt(); // index
                buf.readUnsignedInt(); // unix time
                buf.readUnsignedByte();

                return readPosition(deviceSession, buf);

            }

        } else if (version == 4) {

            return decodeSc(channel, remoteAddress, buf, version, id, type, deviceSession);

        } else {

            return decodeCc(channel, remoteAddress, buf, version, id, type, deviceSession);

        }

        return null;
    }

}
