/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the Li            // Decode packed datetime (from TiniPositionReport.java):
            // Device sends UTC time, no timezone conversion needed (Traccar uses UTC)
            // Bits 26-29: Year (4 bits) - TiniPositionReport logic:
            //   9: 2009 (special case for old data)
            //   0-8, 10-15: 2010-2025 (year_bits + 2010)
            // Bits 22-25: Month (1-12)
            // Bits 17-21: Day (1-31)
            // Bits 12-16: Hour (0-23)
            // Bits 6-11: Minute (0-59)
            // Bits 0-5: Second (0-59)
            int yearBits = (int) ((time32 >> 26) & 0x0F);
            int year = (yearBits == 9) ? 2009 : (yearBits + 2010);
            int month = (int) ((time32 >> 22) & 0x0F);
            int day = (int) ((time32 >> 17) & 0x1F);
            int hour = (int) ((time32 >> 12) & 0x1F);
            int minute = (int) ((time32 >> 6) & 0x3F);
            int second = (int) (time32 & 0x3F);    http://www.apache.org/licenses/LICENSE-2.0
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
import org.traccar.helper.Checksum;
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

    // Message type constants from legacy MessageType.java (decimal values!)
    public static final int M_SYSTEM_LOG = 97;
    public static final int M_ALERT = 99;
    public static final int M_UPDATE_INTERVAL_TIME_RESULT = 100;
    public static final int M_ENGINE_CONTROL_RESULT = 101;
    public static final int M_PING_REPLY = 102;
    public static final int M_EXTEND_POSITION_REPORT = 103;
    public static final int M_BATCH_POSITION_REPORT = 104;
    public static final int M_BATCH_OFFLINE_POSITION_REPORT = 105;
    public static final int M_POSITION_REPORT_ENHIO = 106;
    public static final int M_BATCH_ONLINE_POSITION_REPORT_ENHIO = 107;
    public static final int M_BATCH_OFFLINE_POSITION_REPORT_ENHIO = 108;
    public static final int M_PING_REPLY_ENHIO = 109;
    public static final int M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO = 114;
    public static final int M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO = 115;

    // XOR keys for each message type (from legacy MessageType.java)
    private byte getXorKey(int messageType) {
        return switch (messageType) {
            case M_SYSTEM_LOG -> (byte) 0x39;
            case M_ALERT -> (byte) 0x25;
            case M_UPDATE_INTERVAL_TIME_RESULT -> (byte) 0x56;
            case M_ENGINE_CONTROL_RESULT -> (byte) 0x72;
            case M_PING_REPLY -> (byte) 0x29;
            case M_EXTEND_POSITION_REPORT -> (byte) 0x33;
            case M_BATCH_POSITION_REPORT -> (byte) 0x73;
            case M_BATCH_OFFLINE_POSITION_REPORT -> (byte) 0xE7;
            case M_POSITION_REPORT_ENHIO -> (byte) 0x66;
            case M_BATCH_ONLINE_POSITION_REPORT_ENHIO -> (byte) 0x7A;
            case M_BATCH_OFFLINE_POSITION_REPORT_ENHIO -> (byte) 0xDC;
            case M_PING_REPLY_ENHIO -> (byte) 0xB9;
            case M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO -> (byte) 0xAD;
            case M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO -> (byte) 0xD7;
            default -> (byte) 0x00;
        };
    }

    private boolean isPositionReport(int messageType) {
        return messageType == M_EXTEND_POSITION_REPORT
                || messageType == M_BATCH_POSITION_REPORT
                || messageType == M_BATCH_OFFLINE_POSITION_REPORT
                || messageType == M_POSITION_REPORT_ENHIO
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
        int receivedCrc = decoded.getUnsignedShort(dataLength);
        int calculatedCrc = Checksum.crc16(Checksum.CRC16_CCITT_FALSE, dataForCrc.nioBuffer());

        if (receivedCrc != calculatedCrc) {
            decoded.release();
            return null;
        }

        // Parse packet structure: Type(1) | Size(2) | Ver(1) | TID(3) | Seq(1) | Data(N)
        decoded.readerIndex(1); // Skip type already read
        decoded.readUnsignedShort(); // payload size (big-endian per legacy spec)
        decoded.readUnsignedByte(); // version

        byte[] tidBytes = new byte[3];
        decoded.readBytes(tidBytes);
        long terminalIdValue = Long.parseLong(bytesToHex(tidBytes), 16);
        String terminalId = Long.toString(terminalIdValue);

        decoded.readUnsignedByte(); // sequence

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, terminalId);
        if (deviceSession == null) {
            decoded.release();
            return null;
        }

        // Send acknowledgment response to device (same as legacy DataServer.java)
        if (channel != null) {
            ByteBuf response = Unpooled.copiedBuffer(
                    "\r\n>OK\r\n>OK\r\n>OK", java.nio.charset.StandardCharsets.US_ASCII);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
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

        // For ONLINE position reports, calculate how many position records by subtracting base station data
        int dataLength = buf.readableBytes();
        int positionDataLength = dataLength;

        // ONLINE modes have 44 bytes of base station data after position records
        if (messageType == M_BATCH_ONLINE_POSITION_REPORT_ENHIO
                || messageType == M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO) {
            if (dataLength >= 44) {
                positionDataLength = dataLength - 44;
            }
        }

        // Each record is 16 bytes (from TiniPositionReportPack.java):
        // latlong(7) + speed(1) + flagdegree(1) + digital(1) + analog(1) + enh(1) + time32(4)
        int recordCount = positionDataLength / 16;

        for (int i = 0; i < recordCount && buf.readableBytes() >= 16; i++) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            // Lat/Lon: 7 bytes as hex string, split into 3.5 bytes each
            byte[] latlongBytes = new byte[7];
            buf.readBytes(latlongBytes);
            String latlongHex = bytesToHex(latlongBytes);

            try {
                // First 7 hex chars = latitude, next 7 hex chars = longitude
                // Format is DDMM.MMMM (degrees + minutes) not decimal
                String latHex = latlongHex.substring(0, 7);
                String lonHex = latlongHex.substring(7, 14);
                position.setLatitude(parseLatitude(latHex));
                position.setLongitude(parseLongitude(lonHex));
            } catch (Exception e) {
                continue; // Skip invalid position
            }

            // Speed (1 byte) - km/h, convert to knots
            int speed = buf.readUnsignedByte();
            position.setSpeed(speed * 0.539957); // km/h to knots

            // Flag and Degree (1 byte) - contains GPS status (bit 6) and course (bits 0-5)
            int flagDegree = buf.readUnsignedByte();
            position.setValid((flagDegree & 0x40) != 0); // Bit 6 = GPS status
            int courseBits = flagDegree & 0x1F; // Lower 5 bits = course (0-31)
            position.setCourse(courseBits * 360.0 / 32.0); // Map to 0-360 degrees

            // Digital inputs (1 byte)
            int digital = buf.readUnsignedByte();
            String digitalBinary = String.format("%8s", Integer.toBinaryString(digital)).replace(' ', '0');
            position.set(Position.KEY_INPUT, digitalBinary);
            position.set(Position.KEY_IGNITION, (digital & 0x01) != 0);

            // Analog (1 byte) - will combine with top 2 bits of time32
            int analogByte = buf.readUnsignedByte();

            // Enh (1 byte) - enhanced I/O status
            int enh = buf.readUnsignedByte();
            position.set(Position.KEY_STATUS, enh);

            // Time (4 bytes) - packed as 32-bit integer, read as BIG-ENDIAN
            long time32 = buf.readUnsignedInt(); // Big-endian!

            // Analog value: combine analog byte with top 2 bits of time32 (bits 30-31)
            // From TiniPositionReport.getAnalog(): (analog_byte << 2) + top_2_bits
            int analogTop2Bits = (int) ((time32 >> 30) & 0x03);
            int analogValue = (analogByte << 2) + analogTop2Bits;
            position.set(Position.KEY_POWER, analogValue);

            // Decode packed datetime (from TiniPositionReport.java):
            // Device sends UTC time, no timezone conversion needed (Traccar uses UTC)
            // Note: Bits 30-31 are used for analog value, so only bits 0-29 for datetime
            // Bits 26-29: Year (0-15), special logic: 9=2019, else +2020 (compensate for device RTC -10 years)
            // Bits 22-25: Month (1-12)
            // Bits 17-21: Day (1-31)
            // Bits 12-16: Hour (0-23)
            // Bits 6-11: Minute (0-59)
            // Bits 0-5: Second (0-59)
            int yearBits = (int) ((time32 >> 26) & 0x0F);
            int year = (yearBits == 9) ? 2019 : (2020 + yearBits);  // +10 years from original logic
            int month = (int) ((time32 >> 22) & 0x0F);
            int day = (int) ((time32 >> 17) & 0x1F);
            int hour = (int) ((time32 >> 12) & 0x1F);
            int minute = (int) ((time32 >> 6) & 0x3F);
            int second = (int) (time32 & 0x3F);

            // Store as UTC (no timezone conversion)
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.clear();
            calendar.set(year, month - 1, day, hour, minute, second);
            position.setTime(calendar.getTime());

            // Only add valid positions with reasonable datetime
            // Valid range: 2019-2035 (+10 years from original 2009-2025), month 1-12, day 1-31
            if (year >= 2019 && year <= 2035 && month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                positions.add(position);
            }
        }

        // Parse base station data for ONLINE position reports (types 107, 114)
        // After all position records, if there's remaining data, it's base station info
        if ((messageType == M_BATCH_ONLINE_POSITION_REPORT_ENHIO
                || messageType == M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO)
                && buf.readableBytes() >= 44) {

            // Base station data: 44 bytes total
            // LTCell(2) + LAC(2) + CI(2) + Ta(1) + Tc(1) + LTbs(2) + BaseStation(32)
            int ltCell = buf.readUnsignedShortLE();
            int lac = buf.readUnsignedShortLE();
            int ci = buf.readUnsignedShortLE();
            int ta = buf.readUnsignedByte();
            int tc = buf.readUnsignedByte();
            int ltbs = buf.readUnsignedShortLE();

            byte[] baseStationBytes = new byte[32];
            buf.readBytes(baseStationBytes);
            String baseStation = new String(baseStationBytes, java.nio.charset.StandardCharsets.US_ASCII).trim();

            // Add base station data to the last position (most recent)
            if (!positions.isEmpty()) {
                Position lastPosition = positions.get(positions.size() - 1);
                lastPosition.setNetwork(new org.traccar.model.Network());

                // Create cell tower info
                org.traccar.model.CellTower cellTower = new org.traccar.model.CellTower();
                cellTower.setLocationAreaCode(lac);
                cellTower.setCellId((long) ci);

                lastPosition.getNetwork().addCellTower(cellTower);

                // Store additional base station data in position attributes
                lastPosition.set("cellTiming", ltCell);
                lastPosition.set("timingAdvance", ta);
                lastPosition.set("timingCorrection", tc);
                lastPosition.set("baseStationTiming", ltbs);
                lastPosition.set("baseStation", baseStation);
            }
        }

        return positions;
    }

    private double parseLatitude(String hex) {
        // hex = "013B2C4" (7 chars) represents DDMM.MMMM
        long value = Long.parseLong(hex, 16);
        String str = String.format("%08d", value); // Pad to 8 digits
        // Format: DDMM.MMMM -> degrees + minutes/60
        double degrees = Double.parseDouble(str.substring(0, 2));
        double minutes = Double.parseDouble(str.substring(2, 4) + "." + str.substring(4));
        return degrees + (minutes / 60.0);
    }

    private double parseLongitude(String hex) {
        // hex = "0644F3A" (7 chars) represents DDDMM.MMMM
        long value = Long.parseLong(hex, 16);
        String str = String.format("%09d", value); // Pad to 9 digits
        // Format: DDDMM.MMMM -> degrees + minutes/60
        double degrees = Double.parseDouble(str.substring(0, 3));
        double minutes = Double.parseDouble(str.substring(3, 5) + "." + str.substring(5));
        return degrees + (minutes / 60.0);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

}
