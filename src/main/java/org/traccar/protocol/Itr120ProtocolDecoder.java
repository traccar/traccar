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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.Date;

public class Itr120ProtocolDecoder extends BaseProtocolDecoder {

    private static final int PID_LOGIN = 0x01;
    private static final int PID_HEARTBEAT = 0x03;
    private static final int PID_LOCATION = 0x12;
    private static final int PID_WARNING = 0x14;
    private static final int PID_REPORT = 0x15;
    private static final int PID_IDLE_TIME = 0x3B;
    private static final int PID_INSTRUCTION = 0x80;

    public Itr120ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static String decodeBcd(ByteBuf buf, int digits) {
        StringBuilder result = new StringBuilder(digits);
        for (int i = 0; i < digits / 2; i++) {
            int b = buf.readUnsignedByte();
            result.append((char) ('0' + (b >>> 4)));
            result.append((char) ('0' + (b & 0x0f)));
        }
        if (digits % 2 != 0) {
            int b = buf.readUnsignedByte();
            result.append((char) ('0' + (b >>> 4)));
        }
        int index = 0;
        while (index < result.length() - 1 && result.charAt(index) == '0') {
            index++;
        }
        return result.substring(index);
    }

    private void writeAck(Channel channel, SocketAddress remoteAddress, int pid, int sequence) {
        if (channel == null) {
            return;
        }

        ByteBuf response = Unpooled.buffer();
        response.writeByte(0x28);
        response.writeByte(0x28);
        response.writeByte(pid);
        response.writeShort(2);
        response.writeShort(sequence);
        channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
    }

    private void writeLoginResponse(Channel channel, SocketAddress remoteAddress, int sequence) {
        if (channel == null) {
            return;
        }

        ByteBuf response = Unpooled.buffer();
        response.writeByte(0x28);
        response.writeByte(0x28);
        response.writeByte(PID_LOGIN);
        response.writeShort(9);
        response.writeShort(sequence);
        response.writeInt((int) (System.currentTimeMillis() / 1000L));
        response.writeShort(0x0001);
        response.writeByte(0x03);
        channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
    }

    private void readBattery(ByteBuf buf, Position position) {
        if (buf.readableBytes() >= 2) {
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
        }
    }

    private Position parsePosition(ByteBuf buf, long deviceId) {
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceId);

        long timestamp = buf.readUnsignedInt();
        position.setTime(new Date(timestamp * 1000L));

        int mask = buf.readUnsignedByte();
        boolean gps = BitUtil.check(mask, 0);
        if (gps) {
            position.setLatitude(buf.readInt() / 1800000.0);
            position.setLongitude(buf.readInt() / 1800000.0);
            position.setAltitude((double) buf.readShort());
            position.setSpeed(convertSpeed(buf.readUnsignedShort(), "kmh"));
            position.setCourse((double) buf.readUnsignedShort());
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        }

        if (BitUtil.check(mask, 1)) {
            buf.skipBytes(11);
        }
        if (BitUtil.check(mask, 2)) {
            buf.skipBytes(7);
        }
        if (BitUtil.check(mask, 3)) {
            buf.skipBytes(7);
        }
        if (BitUtil.check(mask, 4)) {
            buf.skipBytes(7);
        }

        position.setValid(gps);
        return position;
    }

    private void applyStatus(Position position, int status) {
        position.set(Position.KEY_STATUS, status);
        position.setValid(BitUtil.check(status, 0));
        if (BitUtil.check(status, 5)) {
            position.set(Position.KEY_OUTPUT, !BitUtil.check(status, 6));
        }
        if (BitUtil.check(status, 3)) {
            position.set(Position.KEY_MOTION, BitUtil.check(status, 9));
        }
        if (BitUtil.check(status, 7)) {
            position.set(Position.KEY_CHARGE, BitUtil.check(status, 8));
        }
    }

    private void readLocationTail(ByteBuf buf, Position position) {
        int status = buf.readUnsignedShortLE();
        applyStatus(position, status);

        readBattery(buf, position);
        if (buf.readableBytes() >= 2) {
            position.set("adc1", buf.readUnsignedShort() / 100.0);
        }
        if (buf.readableBytes() >= 2) {
            position.set("adc2", buf.readUnsignedShort() / 100.0);
        }
        if (buf.readableBytes() >= 4) {
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
        }
        if (buf.readableBytes() >= 2) {
            buf.readUnsignedShort(); // GSM counter
        }
        if (buf.readableBytes() >= 2) {
            buf.readUnsignedShort(); // GPS counter
        }
        if (buf.readableBytes() >= 16) {
            buf.skipBytes(16); // RFU
        }
        if (buf.readableBytes() >= 4) {
            position.set(Position.KEY_HOURS, buf.readUnsignedInt() * 1000L);
        }
        if (buf.readableBytes() >= 2) {
            buf.skipBytes(2); // RFU
        }
    }

    private void readEventTail(ByteBuf buf, Position position) {
        int status = buf.readUnsignedShortLE();
        applyStatus(position, status);

        if (buf.readableBytes() >= 4) {
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
        }
        if (buf.readableBytes() >= 4) {
            position.set(Position.KEY_HOURS, buf.readUnsignedInt() * 1000L);
        }
        readBattery(buf, position);
        if (buf.readableBytes() >= 2) {
            position.set("adc1", buf.readUnsignedShort() / 100.0);
        }
        if (buf.readableBytes() >= 2) {
            position.set("adc2", buf.readUnsignedShort() / 100.0);
        }
    }

    private void readReportTail(ByteBuf buf, Position position) {
        int status = buf.readUnsignedShortLE();
        applyStatus(position, status);

        if (buf.readableBytes() >= 4) {
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
        }
        if (buf.readableBytes() >= 4) {
            position.set(Position.KEY_HOURS, buf.readUnsignedInt() * 1000L);
        }
        readBattery(buf, position);
        if (buf.readableBytes() >= 2) {
            position.set("adc1", buf.readUnsignedShort() / 100.0);
        }
        if (buf.readableBytes() >= 2) {
            position.set("adc2", buf.readUnsignedShort() / 100.0);
        }
    }

    private void readIdleTimeTail(ByteBuf buf, Position position) {
        int status = buf.readUnsignedShortLE();
        applyStatus(position, status);

        readBattery(buf, position);
        if (buf.readableBytes() >= 2) {
            position.set("adc1", buf.readUnsignedShort() / 100.0);
        }
        if (buf.readableBytes() >= 2) {
            position.set("adc2", buf.readUnsignedShort() / 100.0);
        }
        if (buf.readableBytes() >= 2) {
            position.set("idleTime", buf.readUnsignedShort());
        }
    }

    static Boolean decodeIgnition(int status) {
        return BitUtil.check(status, 1) ? BitUtil.check(status, 2) : null;
    }

    private int readStatus(ByteBuf buf) {
        return buf.readUnsignedShortLE();
    }

    private Position parseLocation(ByteBuf buf, long deviceId) {
        Position position = parsePosition(buf, deviceId);
        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        int header = buf.readUnsignedShort();
        if (header != 0x2828) {
            return null;
        }

        int pid = buf.readUnsignedByte();
        int size = buf.readUnsignedShort();
        int sequence = buf.readUnsignedShort();

        if (pid == PID_LOGIN) {
            String imei = decodeBcd(buf, 16);
            if (buf.isReadable()) {
                buf.skipBytes(buf.readableBytes());
            }
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
            if (deviceSession != null) {
                writeLoginResponse(channel, remoteAddress, sequence);
            }
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        long deviceId = deviceSession.getDeviceId();

        return switch (pid) {
            case PID_HEARTBEAT -> {
                if (buf.readableBytes() >= 2) {
                    readStatus(buf);
                }
                writeAck(channel, remoteAddress, pid, sequence);
                yield null;
            }
            case PID_LOCATION -> {
                Position position = parseLocation(buf, deviceId);
                readLocationTail(buf, position);
                writeAck(channel, remoteAddress, pid, sequence);
                yield position;
            }
            case PID_WARNING -> {
                Position position = parseLocation(buf, deviceId);
                if (buf.isReadable()) {
                    int warning = buf.readUnsignedByte();
                    switch (warning) {
                        case 0x01, 0x24, 0x02, 0x05, 0x81, 0x82, 0x83, 0x84, 0x85 ->
                                position.set(Position.KEY_EVENT, warning);
                        default -> position.set(Position.KEY_EVENT, warning);
                    }
                }
                readEventTail(buf, position);
                writeAck(channel, remoteAddress, pid, sequence);
                yield position;
            }
            case PID_REPORT -> {
                Position position = parseLocation(buf, deviceId);
                int report = buf.isReadable() ? buf.readUnsignedByte() : -1;
                readReportTail(buf, position);
                switch (report) {
                    case 0x01 -> {
                        position.set(Position.KEY_IGNITION, true);
                    }
                    case 0x02 -> {
                        position.set(Position.KEY_IGNITION, false);
                    }
                    case 0x03 -> position.set(Position.KEY_EVENT, report);
                    case -1 -> {
                    }
                    default -> position.set(Position.KEY_EVENT, report);
                }
                writeAck(channel, remoteAddress, pid, sequence);
                yield position;
            }
            case PID_IDLE_TIME -> {
                Position position = parseLocation(buf, deviceId);
                readIdleTimeTail(buf, position);
                position.set(Position.KEY_IGNITION, true);
                writeAck(channel, remoteAddress, pid, sequence);
                yield position;
            }
            case PID_INSTRUCTION -> {
                if (buf.readableBytes() >= 1) {
                    buf.readUnsignedByte(); // type
                }
                if (buf.readableBytes() >= 4) {
                    buf.readUnsignedInt(); // uid
                }
                if (buf.isReadable()) {
                    buf.skipBytes(buf.readableBytes());
                }
                writeAck(channel, remoteAddress, pid, sequence);
                yield null;
            }
            default -> {
                writeAck(channel, remoteAddress, pid, sequence);
                yield null;
            }
        };
    }

}
