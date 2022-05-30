/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.ObdDecoder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
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

    public CastelProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final short MSG_SC_LOGIN = 0x1001;
    public static final short MSG_SC_LOGIN_RESPONSE = (short) 0x9001;
    public static final short MSG_SC_LOGOUT = 0x1002;
    public static final short MSG_SC_HEARTBEAT = 0x1003;
    public static final short MSG_SC_HEARTBEAT_RESPONSE = (short) 0x9003;
    public static final short MSG_SC_GPS = 0x4001;
    public static final short MSG_SC_PID_DATA = 0x4002;
    public static final short MSG_SC_G_SENSOR = 0x4003;
    public static final short MSG_SC_SUPPORTED_PID = 0x4004;
    public static final short MSG_SC_OBD_DATA = 0x4005;
    public static final short MSG_SC_DTCS_PASSENGER = 0x4006;
    public static final short MSG_SC_DTCS_COMMERCIAL = 0x400B;
    public static final short MSG_SC_ALARM = 0x4007;
    public static final short MSG_SC_CELL = 0x4008;
    public static final short MSG_SC_GPS_SLEEP = 0x4009;
    public static final short MSG_SC_FUEL = 0x400E;
    public static final short MSG_SC_AGPS_REQUEST = 0x5101;
    public static final short MSG_SC_QUERY_RESPONSE = (short) 0xA002;
    public static final short MSG_SC_CURRENT_LOCATION = (short) 0xB001;

    public static final short MSG_CC_LOGIN = 0x4001;
    public static final short MSG_CC_LOGIN_RESPONSE = (short) 0x8001;
    public static final short MSG_CC_HEARTBEAT = 0x4206;
    public static final short MSG_CC_PETROL_CONTROL = 0x4583;
    public static final short MSG_CC_HEARTBEAT_RESPONSE = (short) 0x8206;

    private Position readPosition(DeviceSession deviceSession, ByteBuf buf) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        position.setTime(dateBuilder.getDate());

        double lat = buf.readUnsignedIntLE() / 3600000.0;
        double lon = buf.readUnsignedIntLE() / 3600000.0;
        position.setSpeed(UnitsConverter.knotsFromCps(buf.readUnsignedShortLE()));
        position.setCourse(buf.readUnsignedShortLE() * 0.1);

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

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        return position;
    }

    private void decodeObd(Position position, ByteBuf buf, boolean groups) {

        int count = buf.readUnsignedByte();

        int[] pids = new int[count];
        for (int i = 0; i < count; i++) {
            pids[i] = buf.readUnsignedShortLE() & 0xff;
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
                    value = buf.readUnsignedShortLE();
                    break;
                case 4:
                    value = buf.readIntLE();
                    break;
                default:
                    value = 0;
                    break;
            }
            position.add(ObdDecoder.decodeData(pids[i], value, false));
        }
    }

    private void decodeStat(Position position, ByteBuf buf) {

        buf.readUnsignedIntLE(); // ACC ON time
        buf.readUnsignedIntLE(); // UTC time
        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
        position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedIntLE());
        position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedIntLE());
        buf.readUnsignedShortLE(); // current fuel consumption

        long state = buf.readUnsignedIntLE();
        position.set(Position.KEY_ALARM, BitUtil.check(state, 4) ? Position.ALARM_ACCELERATION : null);
        position.set(Position.KEY_ALARM, BitUtil.check(state, 5) ? Position.ALARM_BRAKING : null);
        position.set(Position.KEY_ALARM, BitUtil.check(state, 6) ? Position.ALARM_IDLE : null);
        position.set(Position.KEY_IGNITION, BitUtil.check(state, 2 * 8 + 2));
        position.set(Position.KEY_STATUS, state);

        buf.skipBytes(8);
    }

    private void sendResponse(
            Channel channel, SocketAddress remoteAddress,
            int version, ByteBuf id, short type, ByteBuf content) {

        if (channel != null) {
            int length = 2 + 2 + 1 + id.readableBytes() + 2 + 2 + 2;
            if (content != null) {
                length += content.readableBytes();
            }

            ByteBuf response = Unpooled.buffer(length);
            response.writeByte('@'); response.writeByte('@');
            response.writeShortLE(length);
            response.writeByte(version);
            response.writeBytes(id);
            response.writeShort(type);
            if (content != null) {
                response.writeBytes(content);
                content.release();
            }
            response.writeShortLE(
                    Checksum.crc16(Checksum.CRC16_X25, response.nioBuffer(0, response.writerIndex())));
            response.writeByte(0x0D); response.writeByte(0x0A);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private void sendResponse(
            Channel channel, SocketAddress remoteAddress, ByteBuf id, short type) {

        if (channel != null) {
            int length = 2 + 2 + id.readableBytes() + 2 + 4 + 8 + 2 + 2;

            ByteBuf response = Unpooled.buffer(length);
            response.writeByte('@'); response.writeByte('@');
            response.writeShortLE(length);
            response.writeBytes(id);
            response.writeShort(type);
            response.writeIntLE(0);
            for (int i = 0; i < 8; i++) {
                response.writeByte(0xff);
            }
            response.writeShortLE(
                    Checksum.crc16(Checksum.CRC16_X25, response.nioBuffer(0, response.writerIndex())));
            response.writeByte(0x0D); response.writeByte(0x0A);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private void decodeAlarm(Position position, int alarm) {
        switch (alarm) {
            case 0x01:
                position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
                break;
            case 0x02:
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_POWER);
                break;
            case 0x03:
                position.set(Position.KEY_ALARM, Position.ALARM_TEMPERATURE);
                break;
            case 0x04:
                position.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
                break;
            case 0x05:
                position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
                break;
            case 0x06:
                position.set(Position.KEY_ALARM, Position.ALARM_IDLE);
                break;
            case 0x07:
                position.set(Position.KEY_ALARM, Position.ALARM_TOW);
                break;
            case 0x08:
                position.set(Position.KEY_ALARM, Position.ALARM_HIGH_RPM);
                break;
            case 0x09:
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_ON);
                break;
            case 0x0B:
                position.set(Position.KEY_ALARM, Position.ALARM_LANE_CHANGE);
                break;
            case 0x0C:
                position.set(Position.KEY_ALARM, Position.ALARM_CORNERING);
                break;
            case 0x0E:
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_OFF);
                break;
            case 0x16:
                position.set(Position.KEY_IGNITION, true);
                break;
            case 0x17:
                position.set(Position.KEY_IGNITION, false);
                break;
            default:
                break;
        }
    }

    private Object decodeSc(
            Channel channel, SocketAddress remoteAddress, ByteBuf buf,
            int version, ByteBuf id, short type, DeviceSession deviceSession) {

        Position position;
        int count;

        switch (type) {

            case MSG_SC_HEARTBEAT:
                sendResponse(channel, remoteAddress, version, id, MSG_SC_HEARTBEAT_RESPONSE, null);
                return null;

            case MSG_SC_LOGIN:
            case MSG_SC_LOGOUT:
            case MSG_SC_GPS:
            case MSG_SC_ALARM:
            case MSG_SC_CURRENT_LOCATION:
            case MSG_SC_FUEL:
                if (type == MSG_SC_LOGIN) {
                    ByteBuf response = Unpooled.buffer(10);
                    response.writeIntLE(0xFFFFFFFF);
                    response.writeShortLE(0);
                    response.writeIntLE((int) (System.currentTimeMillis() / 1000));
                    sendResponse(channel, remoteAddress, version, id, MSG_SC_LOGIN_RESPONSE, response);
                }

                if (type == MSG_SC_GPS) {
                    buf.readUnsignedByte(); // historical
                } else if (type == MSG_SC_ALARM) {
                    buf.readUnsignedIntLE(); // alarm
                } else if (type == MSG_SC_CURRENT_LOCATION) {
                    buf.readUnsignedShortLE();
                }

                buf.readUnsignedIntLE(); // ACC ON time
                buf.readUnsignedIntLE(); // UTC time
                long odometer = buf.readUnsignedIntLE();
                long tripOdometer = buf.readUnsignedIntLE();
                long fuelConsumption = buf.readUnsignedIntLE();
                buf.readUnsignedShortLE(); // current fuel consumption
                long status = buf.readUnsignedIntLE();
                buf.skipBytes(8);

                count = buf.readUnsignedByte();

                List<Position> positions = new LinkedList<>();

                for (int i = 0; i < count; i++) {
                    position = readPosition(deviceSession, buf);
                    position.set(Position.KEY_ODOMETER, odometer);
                    position.set(Position.KEY_ODOMETER_TRIP, tripOdometer);
                    position.set(Position.KEY_FUEL_CONSUMPTION, fuelConsumption);
                    position.set(Position.KEY_STATUS, status);
                    positions.add(position);
                }

                if (type == MSG_SC_ALARM) {
                    int alarmCount = buf.readUnsignedByte();
                    for (int i = 0; i < alarmCount; i++) {
                        if (buf.readUnsignedByte() != 0) {
                            int alarm = buf.readUnsignedByte();
                            for (Position p : positions) {
                                decodeAlarm(p, alarm);
                            }
                            buf.readUnsignedShortLE(); // description
                            buf.readUnsignedShortLE(); // threshold
                        }
                    }
                } else if (type == MSG_SC_FUEL) {
                    for (Position p : positions) {
                        p.set(Position.PREFIX_ADC + 1, buf.readUnsignedShortLE());
                    }
                }

                return positions.isEmpty() ? null : positions;

            case MSG_SC_GPS_SLEEP:
                buf.readUnsignedIntLE(); // device time
                return readPosition(deviceSession, buf);

            case MSG_SC_AGPS_REQUEST:
                return readPosition(deviceSession, buf);

            case MSG_SC_PID_DATA:
                position = createPosition(deviceSession);

                decodeStat(position, buf);

                buf.readUnsignedShortLE(); // sample rate
                decodeObd(position, buf, true);

                return position;

            case MSG_SC_G_SENSOR:
                position = createPosition(deviceSession);

                decodeStat(position, buf);

                buf.readUnsignedShortLE(); // sample rate

                count = buf.readUnsignedByte();

                StringBuilder data = new StringBuilder("[");
                for (int i = 0; i < count; i++) {
                    if (i > 0) {
                        data.append(",");
                    }
                    data.append("[");
                    data.append(buf.readShortLE() * 0.015625);
                    data.append(",");
                    data.append(buf.readShortLE() * 0.015625);
                    data.append(",");
                    data.append(buf.readShortLE() * 0.015625);
                    data.append("]");
                }
                data.append("]");

                position.set(Position.KEY_G_SENSOR, data.toString());

                return position;

            case MSG_SC_DTCS_PASSENGER:
                position = createPosition(deviceSession);

                decodeStat(position, buf);

                buf.readUnsignedByte(); // flag
                position.add(ObdDecoder.decodeCodes(ByteBufUtil.hexDump(buf.readSlice(buf.readUnsignedByte()))));

                return position;

            case MSG_SC_OBD_DATA:
                position = createPosition(deviceSession);

                decodeStat(position, buf);

                buf.readUnsignedByte(); // flag
                decodeObd(position, buf, false);

                return position;

            case MSG_SC_CELL:
                position = createPosition(deviceSession);

                decodeStat(position, buf);

                position.setNetwork(new Network(
                        CellTower.fromLacCid(getConfig(), buf.readUnsignedShortLE(), buf.readUnsignedShortLE())));

                return position;

            case MSG_SC_QUERY_RESPONSE:
                position = createPosition(deviceSession);

                buf.readUnsignedShortLE(); // index
                buf.readUnsignedByte(); // response count
                buf.readUnsignedByte(); // response index

                int failureCount = buf.readUnsignedByte();
                for (int i = 0; i < failureCount; i++) {
                    buf.readUnsignedShortLE(); // tag
                }

                int successCount = buf.readUnsignedByte();
                for (int i = 0; i < successCount; i++) {
                    buf.readUnsignedShortLE(); // tag
                    position.set(Position.KEY_RESULT,
                            buf.readSlice(buf.readUnsignedShortLE()).toString(StandardCharsets.US_ASCII));
                }

                return position;

            default:
                return null;

        }
    }

    private Object decodeCc(
            Channel channel, SocketAddress remoteAddress, ByteBuf buf,
            int version, ByteBuf id, short type, DeviceSession deviceSession) {

        if (type == MSG_CC_HEARTBEAT) {

            sendResponse(channel, remoteAddress, version, id, MSG_CC_HEARTBEAT_RESPONSE, null);

            buf.readUnsignedByte(); // 0x01 for history
            int count = buf.readUnsignedByte();

            List<Position> positions = new LinkedList<>();

            for (int i = 0; i < count; i++) {
                Position position = readPosition(deviceSession, buf);

                position.set(Position.KEY_STATUS, buf.readUnsignedIntLE());
                position.set(Position.KEY_BATTERY, buf.readUnsignedByte());
                position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());

                buf.readUnsignedByte(); // geo-fencing id
                buf.readUnsignedByte(); // geo-fencing flags
                buf.readUnsignedByte(); // additional flags

                position.setNetwork(new Network(
                        CellTower.fromLacCid(getConfig(), buf.readUnsignedShortLE(), buf.readUnsignedShortLE())));

                positions.add(position);
            }

            return positions;

        } else if (type == MSG_CC_LOGIN) {

            sendResponse(channel, remoteAddress, version, id, MSG_CC_LOGIN_RESPONSE, null);

            Position position = readPosition(deviceSession, buf);

            position.set(Position.KEY_STATUS, buf.readUnsignedIntLE());
            position.set(Position.KEY_BATTERY, buf.readUnsignedByte());
            position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());

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

    private Object decodeMpip(
            Channel channel, SocketAddress remoteAddress, ByteBuf buf,
            int version, ByteBuf id, short type, DeviceSession deviceSession) {

        if (type == 0x4001) {

            sendResponse(channel, remoteAddress, version, id, (short) type, null);

            return readPosition(deviceSession, buf);

        } else if (type == 0x2001) {

            sendResponse(channel, remoteAddress, id, (short) 0x1001);

            buf.readUnsignedIntLE(); // index
            buf.readUnsignedIntLE(); // unix time
            buf.readUnsignedByte();

            return readPosition(deviceSession, buf);

        } else if (type == 0x4201 || type == 0x4202 || type == 0x4206) {

            return readPosition(deviceSession, buf);

        } else if (type == 0x4204) {

            List<Position> positions = new LinkedList<>();

            for (int i = 0; i < 8; i++) {
                Position position = readPosition(deviceSession, buf);
                buf.skipBytes(31);
                positions.add(position);
            }

            return positions;

        }

        return null;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int header = buf.readUnsignedShortLE();
        buf.readUnsignedShortLE(); // length

        int version = -1;
        if (header == 0x4040) {
            version = buf.readUnsignedByte();
        }

        ByteBuf id = buf.readSlice(20);
        short type = buf.readShort();

        DeviceSession deviceSession = getDeviceSession(
                channel, remoteAddress, id.toString(StandardCharsets.US_ASCII).trim());
        if (deviceSession == null) {
            return null;
        }

        switch (version) {
            case -1:
                return decodeMpip(channel, remoteAddress, buf, version, id, type, deviceSession);
            case 3:
            case 4:
                return decodeSc(channel, remoteAddress, buf, version, id, type, deviceSession);
            default:
                return decodeCc(channel, remoteAddress, buf, version, id, type, deviceSession);
        }
    }

}
