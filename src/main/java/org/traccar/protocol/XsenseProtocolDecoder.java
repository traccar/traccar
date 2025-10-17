/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class XsenseProtocolDecoder extends BaseProtocolDecoder {

    public XsenseProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    // Message type constants from legacy MessageType.java
    public static final int M_SYSTEM_LOG = 0x01;
    public static final int M_ALERT = 0x02;
    public static final int M_UPDATE_INTERVAL_TIME_RESULT = 0x03;
    public static final int M_ENGINE_CONTROL_RESULT = 0x04;
    public static final int M_PING_REPLY = 0x05;
    public static final int M_EXTEND_POSITION_REPORT = 0x10;
    public static final int M_BATCH_POSITION_REPORT = 0x11;
    public static final int M_BATCH_OFFLINE_POSITION_REPORT = 0x12;
    public static final int M_PING_REPLY_ENHIO = 0x25;
    public static final int M_BATCH_ONLINE_POSITION_REPORT_ENHIO = 0x30;
    public static final int M_BATCH_OFFLINE_POSITION_REPORT_ENHIO = 0x31;
    public static final int M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO = 0x40;
    public static final int M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO = 0x41;

    // XOR keys for each message type (from legacy MessageType.java)
    private byte getXorKey(int messageType) {
        return switch (messageType) {
            case M_SYSTEM_LOG -> (byte) 0x53;
            case M_ALERT -> (byte) 0x54;
            case M_UPDATE_INTERVAL_TIME_RESULT -> (byte) 0x55;
            case M_ENGINE_CONTROL_RESULT -> (byte) 0x56;
            case M_PING_REPLY -> (byte) 0x57;
            case M_EXTEND_POSITION_REPORT -> (byte) 0x58;
            case M_BATCH_POSITION_REPORT -> (byte) 0x59;
            case M_BATCH_OFFLINE_POSITION_REPORT -> (byte) 0x5A;
            case M_PING_REPLY_ENHIO -> (byte) 0x5B;
            case M_BATCH_ONLINE_POSITION_REPORT_ENHIO -> (byte) 0x5C;
            case M_BATCH_OFFLINE_POSITION_REPORT_ENHIO -> (byte) 0x5D;
            case M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO -> (byte) 0x5E;
            case M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO -> (byte) 0x5F;
            default -> (byte) 0x00;
        };
    }

    private boolean isPositionReport(int messageType) {
        return messageType == M_EXTEND_POSITION_REPORT
                || messageType == M_BATCH_POSITION_REPORT
                || messageType == M_BATCH_OFFLINE_POSITION_REPORT
                || messageType == M_BATCH_ONLINE_POSITION_REPORT_ENHIO
                || messageType == M_BATCH_OFFLINE_POSITION_REPORT_ENHIO
                || messageType == M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO
                || messageType == M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.readableBytes() < 10) {
            return null;
        }

        int messageType = buf.readUnsignedByte();
        byte xorKey = getXorKey(messageType);

        // Create a copy for decoding
        ByteBuf decoded = Unpooled.buffer(buf.readableBytes());
        decoded.writeByte(messageType);

        // XOR decode from position 1 onwards
        while (buf.isReadable()) {
            decoded.writeByte(buf.readByte() ^ xorKey);
        }

        // Validate CRC16/CCITT
        int dataLength = decoded.readableBytes() - 2;
        if (dataLength < 8) {
            decoded.release();
            return null;
        }

        ByteBuf dataForCrc = decoded.slice(0, dataLength);
        int receivedCrc = decoded.getUnsignedShortLE(dataLength);
        int calculatedCrc = Checksum.crc16(Checksum.CRC16_CCITT_FALSE, dataForCrc.nioBuffer());

        if (receivedCrc != calculatedCrc) {
            decoded.release();
            return null;
        }

        // Parse packet structure: Type(1) | Size(2) | Ver(1) | TID(3) | Seq(1) | Data(N)
        decoded.readerIndex(1); // Skip type already read
        decoded.readUnsignedShortLE(); // payload size
        decoded.readUnsignedByte(); // version

        byte[] tidBytes = new byte[3];
        decoded.readBytes(tidBytes);
        String terminalId = bytesToHex(tidBytes);

        decoded.readUnsignedByte(); // sequence

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, terminalId);
        if (deviceSession == null) {
            decoded.release();
            return null;
        }

        // Handle position reports
        if (isPositionReport(messageType)) {
            List<Position> positions = decodePositions(deviceSession, decoded, messageType);
            decoded.release();
            return positions.isEmpty() ? null : positions;
        }

        decoded.release();
        return null;
    }

    private List<Position> decodePositions(DeviceSession deviceSession, ByteBuf buf, int messageType) {
        List<Position> positions = new ArrayList<>();

        while (buf.readableBytes() >= 20) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            // Parse extended position record (based on ExtendPosition.java)
            // Date/Time: YYYYMMDDHHMMSS (6 bytes BCD-encoded)
            int year = 2000 + decodeBcd(buf.readByte());
            int month = decodeBcd(buf.readByte());
            int day = decodeBcd(buf.readByte());
            int hour = decodeBcd(buf.readByte());
            int minute = decodeBcd(buf.readByte());
            int second = decodeBcd(buf.readByte());

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.clear();
            calendar.set(year, month - 1, day, hour, minute, second);
            position.setTime(calendar.getTime());

            // Latitude (4 bytes) - stored as integer representing degrees * 1000000
            int latRaw = buf.readIntLE();
            position.setLatitude(latRaw / 1000000.0);

            // Longitude (4 bytes)
            int lonRaw = buf.readIntLE();
            position.setLongitude(lonRaw / 1000000.0);

            // Speed (2 bytes) in km/h
            int speed = buf.readUnsignedShortLE();
            position.setSpeed(UnitsConverter.knotsFromKph(speed));

            // Course (2 bytes) in degrees
            int course = buf.readUnsignedShortLE();
            position.setCourse(course);

            // Status flags (1 byte)
            int status = buf.readUnsignedByte();
            position.setValid((status & 0x01) != 0);

            // Satellites (1 byte)
            if (buf.readableBytes() > 0) {
                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            }

            // Digital inputs (if available)
            if (buf.readableBytes() >= 2) {
                int inputs = buf.readUnsignedShortLE();
                position.set(Position.KEY_INPUT, inputs);
            }

            positions.add(position);
        }

        return positions;
    }

    private int decodeBcd(byte b) {
        int high = (b >> 4) & 0x0F;
        int low = b & 0x0F;
        return high * 10 + low;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

}
