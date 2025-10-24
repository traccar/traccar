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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class XsenseProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(XsenseProtocolDecoder.class);

    public XsenseProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected void onMessageEvent(
            Channel channel, SocketAddress remoteAddress, Object originalMessage, Object decodedMessage) {
        // Override to disable deviceOnline and deviceUnknown events for GTR-3 and GTR-siemens devices
        // GTR-3 and GTR-siemens devices should not trigger online/unknown events
        // Only register statistics without updating device status
        // Note: This prevents connectionManager.updateDevice() from being called
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
    // Siemens GTR-Siemens message types
    public static final int M_NEW_POSITION_GPS32_REPORT = 110;
    public static final int M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO = 114;
    public static final int M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO = 115;
    public static final int M_LOCATION_BASE_REPORT = 116;
    public static final int M_GPS_REPORT = 117;

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
            case M_NEW_POSITION_GPS32_REPORT -> (byte) 0x00;
            case M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO -> (byte) 0xAD;
            case M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO -> (byte) 0xD7;
            case M_LOCATION_BASE_REPORT -> (byte) 0x00;
            case M_GPS_REPORT -> (byte) 0x00;
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
                || messageType == M_NEW_POSITION_GPS32_REPORT
                || messageType == M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO
                || messageType == M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO
                || messageType == M_LOCATION_BASE_REPORT
                || messageType == M_GPS_REPORT;
    }

    private boolean isSiemensDevice(int messageType) {
        // Siemens devices use message types 110, 116, 117
        // Note: Message types 109, 114, 115 can be either GTR-3 or Siemens
        // and need to be detected by payload size
        return messageType == M_NEW_POSITION_GPS32_REPORT
                || messageType == M_LOCATION_BASE_REPORT
                || messageType == M_GPS_REPORT;
    }

    private boolean isSiemensFormat(ByteBuf buf, int messageType) {
        // For message types 110, 116, 117: always Siemens
        if (isSiemensDevice(messageType)) {
            return true;
        }

        // For message types 109, 114, 115: detect by payload size
        // GTR-3 uses 16-byte TINI records for position, 82 bytes for ping reply
        // Siemens uses 32-byte GPS32 records for position, 128 bytes for ping reply
        // Note: M_PING_REPLY_ENHIO (109) format is detected by CRC validation in decode()
        // using isSiemensRaw flag, so no need to check here

        if (messageType == M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO
                || messageType == M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO) {
            int dataLength = buf.readableBytes();

            // For ONLINE reports, GTR-3 has 44-byte base station data at the end
            // So check if remaining data (after removing 44 bytes) is divisible by 16 or 32
            int testLength = dataLength;
            if (messageType == M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO && dataLength >= 44) {
                testLength = dataLength - 44;
            }

            // If divisible by 32 but not by 16 (or divisible by both but 32 is more likely), it's Siemens
            // If divisible by 16 but not by 32, it's GTR-3
            if (testLength % 32 == 0 || (testLength >= 2 && (testLength - 2) % 32 == 0)) {
                return true; // Siemens GPS32 format
            }
        }

        return false;
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

        // Try XOR decoding first
        ByteBuf decoded = Unpooled.buffer(buf.readableBytes());
        decoded.writeByte(messageType);

        byte[] payloadBytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), payloadBytes);

        for (byte b : payloadBytes) {
            decoded.writeByte(b ^ xorKey);
        }

        // Validate CRC16/CCITT for XOR decoded
        int dataLength = decoded.readableBytes() - 2;
        ByteBuf dataForCrc = decoded.slice(0, dataLength);
        int receivedCrc = decoded.getUnsignedShort(dataLength);
        int calculatedCrc = Checksum.crc16(Checksum.CRC16_CCITT_FALSE, dataForCrc.nioBuffer());

        boolean isSiemensRaw = false;
        boolean crcValid = (receivedCrc == calculatedCrc);

        // If XOR CRC fails and message type supports raw Siemens, try without XOR
        if (!crcValid && (messageType == M_PING_REPLY_ENHIO
                || messageType == M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO
                || messageType == M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO
                || isSiemensDevice(messageType))) {
            decoded.release();
            decoded = Unpooled.buffer(buf.readableBytes());
            decoded.writeByte(messageType);
            decoded.writeBytes(payloadBytes);

            dataLength = decoded.readableBytes() - 2;
            dataForCrc = decoded.slice(0, dataLength);
            receivedCrc = decoded.getUnsignedShort(dataLength);
            calculatedCrc = Checksum.crc16(Checksum.CRC16_CCITT_FALSE, dataForCrc.nioBuffer());
            crcValid = (receivedCrc == calculatedCrc);
            isSiemensRaw = crcValid;
        }

        if (!crcValid || dataLength < 8) {
            decoded.release();
            return null;
        }

        // Parse packet structure
        decoded.readerIndex(1); // Skip type already read

        String terminalId;
        String ackBoxId;
        int sequence;

        if (isSiemensRaw) {
            // Siemens raw format: Type(1) | Seq(1) | Size(1) | BoxID(2) | Data(N) | CRC(2)
            sequence = decoded.readUnsignedByte() & 0xFF;
            int size = decoded.readUnsignedByte() & 0xFF;
            int boxId = decoded.readUnsignedShort() & 0xFFFF;

            // Device ID = boxId + 1200000
            long deviceId = 1200000L + boxId;
            terminalId = Long.toString(deviceId);
            ackBoxId = String.format("%05d", boxId);
        } else {
            // GTR-3 format: Type(1) | Size(2) | Ver(1) | TID(3) | Seq(1) | Data(N) | CRC(2)
            decoded.readUnsignedShort(); // payload size
            decoded.readUnsignedByte(); // version

            byte[] tidBytes = new byte[3];
            decoded.readBytes(tidBytes);
            long terminalIdValue = Long.parseLong(bytesToHex(tidBytes), 16);
            terminalId = Long.toString(terminalIdValue);
            ackBoxId = terminalId;

            sequence = decoded.readUnsignedByte();
        }

        // Send acknowledgment
        if (channel != null) {
            String ack;
            if (isSiemensRaw || isSiemensFormat(decoded, messageType)) {
                // Siemens format: >OK,CRC,SEQ,BOXID,*
                String crcHex = String.format("%04X", receivedCrc).toUpperCase();
                ack = String.format(">OK,%s,%d,%s,*\r\n>OK\r\n",
                    crcHex, sequence, ackBoxId);
            } else {
                // GTR-3 format: simple >OK
                ack = "\r\n>OK\r\n>OK\r\n>OK";
            }
            ByteBuf response = Unpooled.copiedBuffer(ack, StandardCharsets.US_ASCII);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, terminalId);
        if (deviceSession == null) {
            decoded.release();
            return null;
        }

        // Handle position reports
        if (isPositionReport(messageType)) {
            List<Position> positions;
            if (isSiemensRaw || isSiemensFormat(decoded, messageType)) {
                // Siemens GTR-Siemens device (32-byte GPS32 format)
                positions = decodeSiemensPositions(deviceSession, decoded);
            } else {
                // GTR-3 device (16-byte TINI format)
                positions = decodePositions(deviceSession, decoded, messageType);
            }
            decoded.release();
            return positions.isEmpty() ? null : positions;
        } else if (messageType == M_PING_REPLY_ENHIO) {
            Position position;
            if (isSiemensRaw) {
                // Siemens GTR-Siemens ping reply (128 bytes)
                // For Siemens raw format, trim CRC from the end
                if (decoded.readableBytes() > 128) {
                    decoded.writerIndex(decoded.readerIndex() + 128);
                }
                position = decodeSiemensPingReply(deviceSession, decoded);
            } else {
                // GTR-3 ping reply (82 bytes)
                position = decodePingReply(deviceSession, decoded);
            }
            decoded.release();
            return position;
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

            // Mark batch data type with custom attribute (not using outdated flag)
            // Using outdated=true would cause OutdatedHandler to override deviceTime and coordinates
            // Instead, use a custom attribute to preserve original device timestamps
            if (messageType == M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO
                    || messageType == M_BATCH_OFFLINE_POSITION_REPORT_ENHIO) {
                position.set("offlineBatch", true);
                position.set("batchType", "offline");
            } else {
                position.set("offlineBatch", false);
                position.set("batchType", "online");
            }

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
            String enhBinary = String.format("%8s", Integer.toBinaryString(enh)).replace(' ', '0');
            position.set("io", digitalBinary + enhBinary);

            // Time (4 bytes) - packed as 32-bit integer, read as BIG-ENDIAN
            long time32 = buf.readUnsignedInt(); // Big-endian!

            // Analog value: combine analog byte with top 2 bits of time32 (bits 30-31)
            // From TiniPositionReport.getAnalog(): (analog_byte << 2) + top_2_bits
            int analogTop2Bits = (int) ((time32 >> 30) & 0x03);
            int analogValue = (analogByte << 2) + analogTop2Bits;
            position.set("adc1", analogValue);
            position.set("adc2", 0); // Reserved
            position.set("adc3", 0); // Reserved
            position.set("adc4", 0); // Reserved

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
            position.setFixTime(calendar.getTime());
            position.setDeviceTime(calendar.getTime());

            // Only add valid positions with reasonable datetime
            positions.add(position);

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

    private Position decodePingReply(DeviceSession deviceSession, ByteBuf buf) {
        // Expected payload length: 16 bytes for position fields + 66 bytes of extras
        if (buf.readableBytes() < 82) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        byte[] latlongBytes = new byte[7];
        buf.readBytes(latlongBytes);
        String latlongHex = bytesToHex(latlongBytes);

        try {
            String latHex = latlongHex.substring(0, 7);
            String lonHex = latlongHex.substring(7, 14);
            position.setLatitude(parseLatitude(latHex));
            position.setLongitude(parseLongitude(lonHex));
        } catch (Exception e) {
            return null;
        }

        int speed = buf.readUnsignedByte();
        position.setSpeed(speed * 0.539957);

        int flagDegree = buf.readUnsignedByte();
        position.setValid((flagDegree & 0x40) != 0);
        int courseBits = flagDegree & 0x1F;
        position.setCourse(courseBits * 360.0 / 32.0);

        int digital = buf.readUnsignedByte();
        String digitalBinary = String.format("%8s", Integer.toBinaryString(digital)).replace(' ', '0');
        position.set(Position.KEY_INPUT, digitalBinary);
        position.set(Position.KEY_IGNITION, (digital & 0x01) != 0);

        int analogByte = buf.readUnsignedByte();

        int enh = buf.readUnsignedByte();
        String enhBinary = String.format("%8s", Integer.toBinaryString(enh)).replace(' ', '0');
        position.set("io", digitalBinary + enhBinary);

        long time32 = buf.readUnsignedInt();
        int analogValue = (analogByte << 2) + (int) ((time32 >> 30) & 0x03);
        position.set("adc1", analogValue);
        position.set("adc2", 0); // Reserved
        position.set("adc3", 0); // Reserved
        position.set("adc4", 0); // Reserved

        int yearBits = (int) ((time32 >> 26) & 0x0F);
        int year = (yearBits == 9) ? 2019 : (2020 + yearBits);
        int month = (int) ((time32 >> 22) & 0x0F);
        int day = (int) ((time32 >> 17) & 0x1F);
        int hour = (int) ((time32 >> 12) & 0x1F);
        int minute = (int) ((time32 >> 6) & 0x3F);
        int second = (int) (time32 & 0x3F);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, second);
        position.setFixTime(calendar.getTime());
        position.setDeviceTime(calendar.getTime());

        byte[] phoneBytes = new byte[14];
        buf.readBytes(phoneBytes);
        String phone = new String(phoneBytes, StandardCharsets.US_ASCII).trim();
        if (!phone.isEmpty()) {
            position.set(Position.KEY_PHONE, phone);
        }

        long idSec = buf.readUnsignedInt();
        position.set("idSec", idSec);

        int sTime = buf.readUnsignedShort();
        position.set("sTime", sTime);

        buf.skipBytes(4); // reserved

        int ltCell = buf.readUnsignedShortLE();
        int lac = buf.readUnsignedShortLE();
        int ci = buf.readUnsignedShortLE();
        int ta = buf.readUnsignedByte();
        int tc = buf.readUnsignedByte();
        int ltbs = buf.readUnsignedShortLE();

        byte[] baseStationBytes = new byte[32];
        buf.readBytes(baseStationBytes);
        String baseStation = new String(baseStationBytes, StandardCharsets.US_ASCII).trim();

        if (!baseStation.isEmpty()) {
            position.set("baseStation", baseStation);
        }

        position.set("cellTiming", ltCell);
        position.set("timingAdvance", ta);
        position.set("timingCorrection", tc);
        position.set("baseStationTiming", ltbs);

        if (lac != 0 || ci != 0) {
            position.setNetwork(new org.traccar.model.Network());
            org.traccar.model.CellTower cellTower = new org.traccar.model.CellTower();
            cellTower.setLocationAreaCode(lac);
            cellTower.setCellId((long) ci);
            position.getNetwork().addCellTower(cellTower);
        }

        return position;
    }

    // ==================== Siemens GTR-Siemens Decoders ====================

    private List<Position> decodeSiemensPositions(DeviceSession deviceSession, ByteBuf buf) {
        List<Position> positions = new ArrayList<>();

        // Siemens uses 32-byte GPS32 records (not 16-byte TINI like GTR-3)
        int recordSize = 32;
        int recordCount = buf.readableBytes() / recordSize;

        for (int i = 0; i < recordCount && buf.readableBytes() >= recordSize; i++) {
            Position position = decodeSiemensGps32Record(deviceSession, buf);
            if (position != null) {
                positions.add(position);
            }
        }

        return positions;
    }

    private Position decodeSiemensGps32Record(DeviceSession deviceSession, ByteBuf buf) {
        if (buf.readableBytes() < 32) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        // Read 32-byte GPS32 record
        int recordType = buf.readUnsignedByte();
        int flagDegree = buf.readUnsignedByte();
        int hdop = buf.readUnsignedByte();
        int speedRaw = buf.readUnsignedByte();
        long datetimeRaw = buf.readUnsignedInt();
        long latRaw = buf.readUnsignedInt();
        long lonRaw = buf.readUnsignedInt();
        int digi16 = buf.readUnsignedShort();
        int opt16 = buf.readUnsignedShort();
        int altRaw = buf.readUnsignedShort();
        int ana01 = buf.readUnsignedMedium();
        int ana23 = buf.readUnsignedMedium();
        int ana45 = buf.readUnsignedMedium();
        int recordCrc = buf.readUnsignedByte();

        // Parse coordinates: dd*10000000 + mm.mmmmm
        // Result = degrees + minutes / 6000000
        long latDegrees = latRaw / 10000000L;
        long latMinutes = latRaw % 10000000L;
        double latitude = latDegrees + (latMinutes / 6000000.0);

        long lonDegrees = lonRaw / 10000000L;
        long lonMinutes = lonRaw % 10000000L;
        double longitude = lonDegrees + (lonMinutes / 6000000.0);

        // Parse flag_degree: bits 7=E/W, 6=N/S, 5=GPS valid, 4-0=course
        boolean east = (flagDegree & 0x80) != 0;
        boolean north = (flagDegree & 0x40) != 0;
        boolean gpsValid = (flagDegree & 0x20) != 0;
        int courseBits = flagDegree & 0x1F;

        if (!north) {
            latitude = -latitude;
        }
        if (!east) {
            longitude = -longitude;
        }

        position.setLatitude(latitude);
        position.setLongitude(longitude);
        position.setValid(gpsValid);
        position.setCourse(courseBits * 360.0 / 32.0);

        // Speed: raw * 1.852 km/h, convert to knots
        position.setSpeed(speedRaw * 1.852 * 0.539957);

        // Altitude: (raw - 10000) * 0.3048 meters
        position.setAltitude((altRaw - 10000) * 0.3048);

        // HDOP
        position.set(Position.KEY_HDOP, hdop);

        // Digital inputs (16-bit)
        String digitalBinary = String.format("%16s", Integer.toBinaryString(digi16)).replace(' ', '0');
        position.set("io", digitalBinary);
        position.set(Position.KEY_IGNITION, (digi16 & 0x0100) != 0); // bit 8

        // Satellites
        position.set(Position.KEY_SATELLITES, opt16 & 0xFF);
        position.set(Position.KEY_STATUS, opt16);

        // Analog values: 3 bytes each contain two 12-bit values
        int ana0 = (ana01 >> 12) & 0xFFF;
        int ana1 = ana01 & 0xFFF;
        int ana2 = (ana23 >> 12) & 0xFFF;
        int ana3 = ana23 & 0xFFF;
        int ana4 = (ana45 >> 12) & 0xFFF;
        int ana5 = ana45 & 0xFFF;

        position.set(Position.PREFIX_ADC + 1, ana0);
        position.set(Position.PREFIX_ADC + 2, ana1);
        position.set(Position.PREFIX_ADC + 3, ana2);
        position.set(Position.PREFIX_ADC + 4, ana3);
        position.set(Position.PREFIX_ADC + 5, ana4);
        position.set(Position.PREFIX_ADC + 6, ana5);
        position.set(Position.KEY_POWER, ana1);

        position.set("recordType", recordType);
        position.set("recordCrc", recordCrc);

        // Decode datetime: seconds/2(5), minutes(6), hours(5), day(5), month(4), year(7)+2000
        int seconds = (int) ((datetimeRaw & 0x1F) * 2);
        int minutes = (int) ((datetimeRaw >> 5) & 0x3F);
        int hours = (int) ((datetimeRaw >> 11) & 0x1F);
        int day = (int) ((datetimeRaw >> 16) & 0x1F);
        int month = (int) ((datetimeRaw >> 21) & 0x0F);
        int year = (int) ((datetimeRaw >> 25) & 0x7F) + 2000;

        if (year < 2000 || year > 2127 || month < 1 || month > 12
                || day < 1 || day > 31 || hours > 23 || minutes > 59 || seconds > 59) {
            return null;
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month - 1, day, hours, minutes, seconds);
        position.setFixTime(calendar.getTime());
        position.setDeviceTime(calendar.getTime());

        return position;
    }

    private Position decodeSiemensPingReply(DeviceSession deviceSession, ByteBuf buf) {
        if (buf.readableBytes() < 128) {
            return null;
        }

        // First 32 bytes: GPS32 record
        Position position = decodeSiemensGps32Record(deviceSession, buf);
        if (position == null) {
            return null;
        }

        // Remaining 96 bytes: additional ping reply data
        int offlinePtr = buf.readUnsignedShort();
        int idSec = buf.readUnsignedShort();
        int sTime = buf.readUnsignedShort();
        int mcc = buf.readUnsignedShort();
        int mnc = buf.readUnsignedShort();
        int ltCell = buf.readUnsignedShort();
        long cellId = buf.readUnsignedInt();
        int ta = buf.readUnsignedByte();
        int tc = buf.readUnsignedByte();
        int ltbs = buf.readUnsignedShort();

        byte[] baseStationBytes = new byte[32];
        buf.readBytes(baseStationBytes);
        String baseStation = new String(baseStationBytes, StandardCharsets.US_ASCII).trim();

        int rssi = buf.readUnsignedByte();
        int ltc13 = buf.readUnsignedShort();

        int mcc1 = buf.readUnsignedShort();
        int mnc1 = buf.readUnsignedShort();
        long cellId1 = buf.readUnsignedInt();
        int ta1 = buf.readUnsignedByte();

        int mcc2 = buf.readUnsignedShort();
        int mnc2 = buf.readUnsignedShort();
        long cellId2 = buf.readUnsignedInt();
        int ta2 = buf.readUnsignedByte();

        int mcc3 = buf.readUnsignedShort();
        int mnc3 = buf.readUnsignedShort();
        long cellId3 = buf.readUnsignedInt();
        int ta3 = buf.readUnsignedByte();

        int opt = buf.readUnsignedShort();
        int vtime = buf.readUnsignedShort();
        int opt1 = buf.readUnsignedShort();
        int opt2 = buf.readUnsignedShort();
        int opt3 = buf.readUnsignedShort();
        int opt4 = buf.readUnsignedShort();
        int sync = buf.readUnsignedShort();

        // Store additional fields
        position.set("offlinePointer", offlinePtr);
        position.set("idSec", idSec);
        position.set("sTime", sTime);
        position.set("cellTiming", ltCell);
        position.set("timingAdvance", ta);
        position.set("timingCorrection", tc);
        position.set("baseStationTiming", ltbs);

        if (!baseStation.isEmpty()) {
            position.set("baseStation", baseStation);
        }

        position.set(Position.KEY_RSSI, rssi);
        position.set("neighborTiming", ltc13);
        position.set("opt", opt);
        position.set("vTime", vtime);
        position.set("opt1", opt1);
        position.set("opt2", opt2);
        position.set("opt3", opt3);
        position.set("opt4", opt4);
        position.set("sync", sync);

        // Cell tower network
        if (mcc != 0 || cellId != 0) {
            org.traccar.model.Network network = new org.traccar.model.Network();
            org.traccar.model.CellTower primary = org.traccar.model.CellTower.from(mcc, mnc, ltCell, cellId);
            if (rssi != 0) {
                primary.setSignalStrength(rssi);
            }
            network.addCellTower(primary);
            position.setNetwork(network);
        }

        return position;
    }

    // ==================== GTR-3 Helper Methods ====================

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
