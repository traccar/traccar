/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class IterProtocolDecoder extends BaseProtocolDecoder {

    public IterProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 0x01;
    public static final int MSG_HEARTBEAT = 0x03;
    public static final int MSG_LOCATION = 0x12;
    public static final int MSG_WARNING = 0x14;
    public static final int MSG_REPORT = 0x15;
    public static final int MSG_PARAM_SET = 0x1B;
    public static final int MSG_IDLE_TIME = 0x3B;
    public static final int MSG_INSTRUCTION = 0x80;

    private void sendResponse(
            Channel channel, SocketAddress remoteAddress, int type, int sequence, ByteBuf content) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0x28);
            response.writeByte(0x28);
            response.writeByte(type);
            response.writeShort(2 + (content != null ? content.readableBytes() : 0));
            response.writeShort(sequence);
            if (content != null) {
                response.writeBytes(content);
                content.release();
            }
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private Position readPosition(DeviceSession deviceSession, ByteBuf buf) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(new Date(buf.readUnsignedInt() * 1000));

        int mask = buf.readUnsignedByte();
        int mcc = 0;
        int mnc = 0;

        if (BitUtil.check(mask, 0)) {
            position.setLatitude(buf.readInt() / 1800000.0);
            position.setLongitude(buf.readInt() / 1800000.0);
            position.setAltitude(buf.readShort());
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
            position.setCourse(buf.readUnsignedShort());
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            position.setValid(true);
        }

        if (BitUtil.check(mask, 1)) {
            mcc = buf.readUnsignedShort();
            mnc = buf.readUnsignedShort();
            Network network = new Network(CellTower.from(
                    mcc, mnc, buf.readUnsignedShort(), buf.readUnsignedInt(),
                    buf.readUnsignedByte() - 110));
            for (int i = 0; i < 3; i++) {
                if (BitUtil.check(mask, 2 + i)) {
                    network.addCellTower(CellTower.from(
                            mcc, mnc, buf.readUnsignedShort(), buf.readUnsignedInt(),
                            buf.readUnsignedByte() - 110));
                }
            }
            position.setNetwork(network);
        }

        return position;
    }

    private void readStatus(Position position, int status) {
        position.set(Position.KEY_STATUS, status);
        if (BitUtil.check(status, 1)) {
            position.set(Position.KEY_IGNITION, BitUtil.check(status, 2));
        }
        if (BitUtil.check(status, 3)) {
            position.set(Position.KEY_MOTION, BitUtil.check(status, 9));
        }
        if (BitUtil.check(status, 7)) {
            position.set(Position.KEY_CHARGE, BitUtil.check(status, 8));
        }
        position.set(Position.KEY_INPUT, BitUtil.check(status, 15) ? 1 : 0);
    }

    private String decodeWarningType(int type) {
        switch (type) {
            case 0x01:
                return Position.ALARM_POWER_CUT;
            case 0x02:
                return Position.ALARM_SOS;
            case 0x04:
                return Position.ALARM_MOVEMENT;
            case 0x05:
                return Position.ALARM_TOW;
            case 0x08:
            case 0x09:
                return Position.ALARM_GPS_ANTENNA_CUT;
            case 0x24:
                return Position.ALARM_POWER_RESTORED;
            case 0x80:
            case 0x82:
                return Position.ALARM_OVERSPEED;
            case 0x83:
                return Position.ALARM_GEOFENCE_ENTER;
            case 0x84:
                return Position.ALARM_GEOFENCE_EXIT;
            case 0x85:
                return Position.ALARM_VIBRATION;
            case 0x87:
                return Position.ALARM_LOW_BATTERY;
            default:
                return Position.ALARM_GENERAL;
        }
    }

    private void readLocationTail(Position position, ByteBuf buf) {
        readStatus(position, buf.readUnsignedShortLE());
        if (buf.readableBytes() >= 2) {
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
        }
        if (buf.readableBytes() >= 2) {
            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort() / 100.0);
        }
        if (buf.readableBytes() >= 2) {
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort() / 100.0);
        }
        if (buf.readableBytes() >= 4) {
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
        }
        if (buf.readableBytes() >= 2 + 2) {
            buf.skipBytes(2 + 2); // GSM and GPS counters
        }
        if (buf.readableBytes() >= 16) {
            buf.skipBytes(16); // reserved
        }
        if (buf.readableBytes() >= 4) {
            position.set(Position.KEY_HOURS, buf.readUnsignedInt() * 1000);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // mark
        int type = buf.readUnsignedByte();
        buf.readUnsignedShort(); // size
        int sequence = buf.readUnsignedShort();

        if (type == MSG_LOGIN) {

            String imei = ByteBufUtil.hexDump(buf.readSlice(8)).replaceFirst("^0", "");
            buf.skipBytes(1 + 1 + 2 + 2); // language, timezone, system and app versions
            buf.skipBytes(2 + 2 + 2 + 2); // param-set version, sizes and checksum

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
            if (deviceSession != null) {
                ByteBuf content = Unpooled.buffer();
                content.writeInt((int) (System.currentTimeMillis() / 1000));
                content.writeShort(1); // protocol version
                content.writeByte(0); // param-set action
                sendResponse(channel, remoteAddress, type, sequence, content);
            }

            return null;

        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        if (type == MSG_HEARTBEAT) {

            sendResponse(channel, remoteAddress, type, sequence, null);

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            getLastLocation(position, null);
            readStatus(position, buf.readUnsignedShortLE());
            return position;

        }

        if (type == MSG_LOCATION || type == MSG_WARNING || type == MSG_REPORT || type == MSG_IDLE_TIME) {

            sendResponse(channel, remoteAddress, type, sequence, null);

            Position position = readPosition(deviceSession, buf);

            if (type == MSG_WARNING) {
                position.addAlarm(decodeWarningType(buf.readUnsignedByte()));
                readStatus(position, buf.readUnsignedShortLE());
                if (buf.readableBytes() >= 4) {
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                }
                if (buf.readableBytes() >= 4) {
                    position.set(Position.KEY_HOURS, buf.readUnsignedInt() * 1000);
                }
                if (buf.readableBytes() >= 2) {
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
                }
                if (buf.readableBytes() >= 2) {
                    position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort() / 100.0);
                }
                if (buf.readableBytes() >= 2) {
                    position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort() / 100.0);
                }

            } else if (type == MSG_REPORT) {
                int report = buf.readUnsignedByte();
                switch (report) {
                    case 0x01:
                        position.set(Position.KEY_IGNITION, true);
                        break;
                    case 0x02:
                        position.set(Position.KEY_IGNITION, false);
                        break;
                    default:
                        position.set(Position.KEY_EVENT, report);
                        break;
                }
                readStatus(position, buf.readUnsignedShortLE());
                if (buf.readableBytes() >= 4) {
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                }
                if (buf.readableBytes() >= 4) {
                    position.set(Position.KEY_HOURS, buf.readUnsignedInt() * 1000);
                }
                if (buf.readableBytes() >= 2) {
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
                }
                if (buf.readableBytes() >= 2) {
                    position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort() / 100.0);
                }
                if (buf.readableBytes() >= 2) {
                    position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort() / 100.0);
                }

            } else if (type == MSG_IDLE_TIME) {
                position.set("idle", true);
                readStatus(position, buf.readUnsignedShortLE());
                if (buf.readableBytes() >= 2) {
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
                }
                if (buf.readableBytes() >= 2) {
                    position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort() / 100.0);
                }
                if (buf.readableBytes() >= 2) {
                    position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort() / 100.0);
                }
                if (buf.readableBytes() >= 2) {
                    position.set("idleTime", buf.readUnsignedShort());
                }

            } else {
                readLocationTail(position, buf);
            }

            return position;

        }

        if (type == MSG_PARAM_SET) {
            sendResponse(channel, remoteAddress, type, sequence, null);
            return null;
        }

        if (type == MSG_INSTRUCTION) {

            buf.readUnsignedByte(); // instruction type
            long uid = buf.readUnsignedInt();

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            getLastLocation(position, null);

            position.set("instructionUid", uid);
            position.set(Position.KEY_RESULT, buf.toString(StandardCharsets.US_ASCII));
            return position;

        }

        return null;
    }

}
