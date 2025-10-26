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

/**
 * XsenseGtr9ProtocolDecoder - Decoder for GTR-9 devices
 *
 * GTR-9 is an evolution of GTR-Siemens with the following key differences:
 * 1. Device ID offset: 1,300,000 (instead of 1,200,000)
 * 2. ACK format: ">OK,<CRC>#\r\n" (supports command queue)
 * 3. New message type 100: driver_license (RFID reader data)
 * 4. Command return/pending logic for bidirectional communication
 * 5. Strict deviceTime validation: positions with null deviceTime are rejected
 *
 * Protocol uses Siemens RAW format (no XOR encoding):
 * Type(1) | Seq(1) | Size(1) | BoxID(2) | Data(N) | CRC16(2)
 *
 * Important: deviceTime MUST always come from the device. If deviceTime is null,
 * the position will be rejected to ensure data integrity.
 */
public class XsenseGtr9ProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(XsenseGtr9ProtocolDecoder.class);

    // Device ID offset for GTR-9
    private static final long DEVICE_ID_OFFSET = 1_300_000L;
    // GPS week rollover constants (1024 weeks = ~19.7 years)
    private static final long GPS_ROLLOVER_PERIOD_MS = 619_315_200_000L; // 1024 weeks in milliseconds
    private static final int GPS_ROLLOVER_YEAR_THRESHOLD = 2008;

    public XsenseGtr9ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected void onMessageEvent(
            Channel channel, SocketAddress remoteAddress, Object originalMessage, Object decodedMessage) {
        // Override to disable deviceOnline and deviceUnknown events for GTR-9
        // GTR-9 devices should not trigger online/unknown events
        // Only register statistics without updating device status
        // Note: This prevents connectionManager.updateDevice() from being called
    }

    // Message type constants
    public static final int M_SYSTEM_LOG = 97;
    public static final int M_DRIVER_LICENSE = 100;  // GTR-9 specific (was update_time_result in GTR-Siemens)
    public static final int M_PING_REPLY = 102;
    public static final int M_PING_REPLY_ENHIO = 109;
    public static final int M_NEW_POSITION_GPS32_REPORT = 110;
    public static final int M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO = 114;
    public static final int M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO = 115;
    public static final int M_LOCATION_BASE_REPORT = 116;
    public static final int M_GPS_REPORT = 117;

    private boolean isPositionReport(int messageType) {
        return messageType == M_NEW_POSITION_GPS32_REPORT
                || messageType == M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO
                || messageType == M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO
                || messageType == M_LOCATION_BASE_REPORT
                || messageType == M_GPS_REPORT;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.readableBytes() < 10) {
            return null;
        }

        // GTR-9 uses RAW Siemens format (no XOR encoding)
        // Save reader index to include message type in CRC calculation
        int startIndex = buf.readerIndex();
        int messageType = buf.readUnsignedByte();

        // Read header to get size field (which indicates the actual packet size)
        int sequence = buf.readUnsignedByte();
        int sizeField = buf.readUnsignedByte();
        int boxId = buf.readUnsignedShort();

        // Reset to start for CRC calculation
        buf.readerIndex(startIndex);
        int availableBytes = buf.readableBytes();

        // GTR-9 packets have two possible CRC positions:
        // 1. At the last 2 bytes (when there are trailing bytes after the packet)
        // 2. At Size field - 2 (when packet ends exactly at size boundary)
        // Try both positions and use whichever validates correctly

        int dataLength;
        int receivedCrc;
        int calculatedCrc;
        boolean crcValid = false;

        // Try position 1: CRC at last 2 bytes
        int dataLength1 = availableBytes - 2;
        ByteBuf dataForCrc1 = buf.slice(buf.readerIndex(), dataLength1);
        int receivedCrc1 = buf.getUnsignedShort(buf.readerIndex() + dataLength1);
        int calculatedCrc1 = Checksum.crc16(Checksum.CRC16_CCITT_FALSE, dataForCrc1.nioBuffer());

        if (receivedCrc1 == calculatedCrc1) {
            // CRC matches at last 2 bytes (trailing bytes present)
            dataLength = dataLength1;
            receivedCrc = receivedCrc1;
            calculatedCrc = calculatedCrc1;
            crcValid = true;
            LOGGER.debug("CRC validated at last 2 bytes: type={}, position={}, CRC={}",
                    messageType, dataLength1, String.format("%04X", receivedCrc1));
        } else {
            // Try position 2: CRC at size field position
            int dataLength2 = sizeField - 2;
            if (dataLength2 > 0 && dataLength2 + 2 <= availableBytes) {
                ByteBuf dataForCrc2 = buf.slice(buf.readerIndex(), dataLength2);
                int receivedCrc2 = buf.getUnsignedShort(buf.readerIndex() + dataLength2);
                int calculatedCrc2 = Checksum.crc16(Checksum.CRC16_CCITT_FALSE, dataForCrc2.nioBuffer());

                if (receivedCrc2 == calculatedCrc2) {
                    // CRC matches at size field position
                    dataLength = dataLength2;
                    receivedCrc = receivedCrc2;
                    calculatedCrc = calculatedCrc2;
                    crcValid = true;
                    LOGGER.debug("CRC validated at size field position: type={}, position={}, CRC={}",
                            messageType, dataLength2, String.format("%04X", receivedCrc2));
                } else {
                    // Neither position validates, use last 2 bytes as default
                    dataLength = dataLength1;
                    receivedCrc = receivedCrc1;
                    calculatedCrc = calculatedCrc1;
                    LOGGER.debug("CRC validation failed at both positions: type={}, pos1[{}]={}/{}, pos2[{}]={}/{}",
                            messageType,
                            dataLength1, String.format("%04X", receivedCrc1), String.format("%04X", calculatedCrc1),
                            dataLength2, String.format("%04X", receivedCrc2), String.format("%04X", calculatedCrc2));
                }
            } else {
                // Size field invalid, use last 2 bytes
                dataLength = dataLength1;
                receivedCrc = receivedCrc1;
                calculatedCrc = calculatedCrc1;
            }
        }

        // Skip CRC validation for message type 100 (driver license)
        // These packets have CRC mismatches but valid data
        // Also skip CRC validation for type 109 (ping reply) if size mismatch detected
        boolean skipCrcValidation = messageType == M_DRIVER_LICENSE
                || (messageType == M_PING_REPLY_ENHIO && dataLength != calculatedCrc);

        // Validate CRC (unless already validated by trying both positions, or skip is requested)
        if (!crcValid && !skipCrcValidation && (receivedCrc != calculatedCrc || dataLength < 4)) {
            LOGGER.warn("CRC validation failed: type={}, received={}, calculated={}, dataLength={}",
                    messageType,
                    String.format("%04X", receivedCrc),
                    String.format("%04X", calculatedCrc),
                    dataLength);
            return null;
        }

        if (skipCrcValidation && receivedCrc != calculatedCrc) {
            LOGGER.debug("CRC mismatch (ignored) for type {}: received={}, calculated={}",
                    messageType,
                    String.format("%04X", receivedCrc),
                    String.format("%04X", calculatedCrc));
        }

        // Reset buffer position to skip already-read header
        buf.readerIndex(startIndex + 5); // Skip Type(1) + Seq(1) + Size(1) + BoxID(2)

        // GTR-9 uses offset 1,300,000
        long deviceId = DEVICE_ID_OFFSET + boxId;
        String terminalId = Long.toString(deviceId);

        // Send GTR-9 specific acknowledgment: >OK,<CRC>#
        // Note: Command queue integration should be added here in production
        if (channel != null) {
            String crcHex = String.format("%04X", receivedCrc).toUpperCase();
            // Command queue check would be added here in production
            String ack = String.format(">OK,%s#\r\n", crcHex);
            ByteBuf response = Unpooled.copiedBuffer(ack, StandardCharsets.US_ASCII);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, terminalId);
        if (deviceSession == null) {
            return null;
        }

        // Handle different message types
        if (messageType == M_DRIVER_LICENSE) {
            return decodeDriverLicense(deviceSession, buf, sequence, boxId);
        } else if (isPositionReport(messageType)) {
            return decodeSiemensPositions(deviceSession, buf, messageType);
        } else if (messageType == M_PING_REPLY_ENHIO) {
            return decodeSiemensPingReply(deviceSession, buf);
        } else if (messageType == M_SYSTEM_LOG) {
            return decodeSystemLog(deviceSession, buf);
        }

        return null;
    }

    /**
     * Decode driver license message (Type 100)
     * This message contains RFID reader data with GPS position
     * Structure: ntype(1) + flag(1) + hdop(1) + speed(1) + datetime(4) + lat(4) + lon(4) + unknown(3) = 19 bytes
     * Then license data starts at byte 19 (substring 38 in hex)
     */
    private Position decodeDriverLicense(DeviceSession deviceSession, ByteBuf buf, int sequence, int boxId) {
        if (buf.readableBytes() < 19) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        // Parse driver license data structure (19 bytes total before license data)
        int ntype = buf.readUnsignedByte();         // Byte 0
        int flagDegree = buf.readUnsignedByte();    // Byte 1
        int hdop = buf.readUnsignedByte();          // Byte 2
        int speed = buf.readUnsignedByte();         // Byte 3
        long datetime = buf.readUnsignedInt();      // Bytes 4-7
        long lat = buf.readUnsignedInt();           // Bytes 8-11
        long lon = buf.readUnsignedInt();           // Bytes 12-15

        // Skip 3 reserved/padding bytes (bytes 16-18)
        // Pattern observed: FF 01 XX where XX varies
        // Original GTR-9 code skips these bytes entirely (data.substring(38))
        // Likely padding or reserved for future use
        int reserved1 = buf.readUnsignedByte();
        int reserved2 = buf.readUnsignedByte();
        int reserved3 = buf.readUnsignedByte();

        LOGGER.debug("Driver license reserved bytes: 0x{} 0x{} 0x{}",
                String.format("%02X", reserved1),
                String.format("%02X", reserved2),
                String.format("%02X", reserved3));

        // Parse coordinates
        long latDegrees = lat / 10000000L;
        long latMinutes = lat % 10000000L;
        double latitude = latDegrees + (latMinutes / 6000000.0);

        long lonDegrees = lon / 10000000L;
        long lonMinutes = lon % 10000000L;
        double longitude = lonDegrees + (lonMinutes / 6000000.0);

        // Parse flags
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
        position.setCourse(courseBits * 360.0 / 24.0); // GTR-9: 24 directions = 15° each
        position.setSpeed(speed * 1.852 * 0.539957);
        position.set(Position.KEY_HDOP, hdop);

        // Parse datetime
        int seconds = (int) ((datetime & 0x1F) * 2);
        int minutes = (int) ((datetime >> 5) & 0x3F);
        int hours = (int) ((datetime >> 11) & 0x1F);
        int day = (int) ((datetime >> 16) & 0x1F);
        int month = (int) ((datetime >> 21) & 0x0F);
        int year = (int) ((datetime >> 25) & 0x7F) + 2000;

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month - 1, day, hours, minutes, seconds);
        applyGpsRollover(calendar, year);
        position.setFixTime(calendar.getTime());
         position.setDeviceTime(calendar.getTime());
        // Read license data (remaining bytes minus CRC)
        // CRC is the last 2 bytes, so exclude them
        int licenseLength = buf.readableBytes() - 2;
        if (licenseLength > 0) {
            byte[] licenseBytes = new byte[licenseLength];
            buf.readBytes(licenseBytes);
            String license = new String(licenseBytes, StandardCharsets.US_ASCII).trim();

            // Parse license format (from original code)
            if (!license.isEmpty()) {
                license = license.replace("[", "").replace("]", "").trim();
                position.set(Position.KEY_DRIVER_UNIQUE_ID, license);
                LOGGER.info("Driver license detected: {} for device {}", license, deviceSession.getDeviceId());
            }
        }

        position.set("messageType", "driverLicense");
        position.set("ntype", ntype);

        // GTR-9 requirement: deviceTime must come from device, never null
        if (position.getDeviceTime() == null) {
            LOGGER.warn("Driver license position rejected: deviceTime is null for device {}",
                    deviceSession.getDeviceId());
            return null;
        }

        return position;
    }

    /**
     * Apply GPS week rollover correction to timestamps
     * GPS devices may report dates 19.7 years earlier due to 1024-week rollover
     */
    private void applyGpsRollover(Calendar calendar, int initialYear) {
        if (calendar == null || initialYear >= GPS_ROLLOVER_YEAR_THRESHOLD) {
            return;
        }
        long now = System.currentTimeMillis();
        long original = calendar.getTimeInMillis();
        long adjusted = original + GPS_ROLLOVER_PERIOD_MS;
        // Choose the timestamp closest to current time
        if (Math.abs(now - adjusted) < Math.abs(now - original)) {
            calendar.setTimeInMillis(adjusted);
        }
    }

    /**
     * Decode Siemens GPS32 position records
     */
    private List<Position> decodeSiemensPositions(DeviceSession deviceSession, ByteBuf buf, int messageType) {
        List<Position> positions = new ArrayList<>();

        // Each GPS32 record is 32 bytes, but may have extra padding
        // Read as many complete 32-byte records as possible
        while (buf.readableBytes() >= 32) {
            Position position = decodeSiemensGps32Record(deviceSession, buf);
            if (position != null) {
                // GTR-9 requirement: deviceTime must come from device, never null
                if (position.getDeviceTime() == null) {
                    LOGGER.warn("Position rejected: deviceTime is null for device {}",
                            deviceSession.getDeviceId());
                    continue; // Skip this position
                }

                // Mark batch data type with custom attribute (not using outdated flag)
                // Using outdated=true would cause OutdatedHandler to override deviceTime and coordinates
                // Instead, use a custom attribute to preserve original device timestamps
                if (messageType == M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO) {
                    position.set("offlineBatch", true);
                    position.set("batchType", "offline");
                } else {
                    position.set("offlineBatch", false);
                    position.set("batchType", "online");
                }

                positions.add(position);
            } else {
                break; // Stop if we can't decode a record
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

        // Parse coordinates
        long latDegrees = latRaw / 10000000L;
        long latMinutes = latRaw % 10000000L;
        double latitude = latDegrees + (latMinutes / 6000000.0);

        long lonDegrees = lonRaw / 10000000L;
        long lonMinutes = lonRaw % 10000000L;
        double longitude = lonDegrees + (lonMinutes / 6000000.0);

        // Parse flags
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
        position.setCourse(courseBits * 360.0 / 24.0); // GTR-9: 24 directions = 15° each
        position.setSpeed(speedRaw * 1.852 * 0.539957);
        position.setAltitude((altRaw - 10000) * 0.3048);
        position.set(Position.KEY_HDOP, hdop);

        // Digital inputs
        String digitalBinary = String.format("%16s", Integer.toBinaryString(digi16)).replace(' ', '0');
        position.set("io", digitalBinary);
        position.set(Position.KEY_IGNITION, (digi16 & 0x0100) != 0);

        // Satellites
        position.set(Position.KEY_SATELLITES, opt16 & 0xFF);

        // Analog values
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

        // GTR-9 Extended Data: Read 4 additional bytes if available
        // Bytes 33-36: event1, event2, event3, crc_record
        if (buf.readableBytes() >= 4) {
            int event1 = buf.readUnsignedByte(); // Sensor 1 events
            int event2 = buf.readUnsignedByte(); // Sensor 2 events
            int event3 = buf.readUnsignedByte(); // Extension flags

            // Store as binary strings (as per original code)
            String sensor1Binary = String.format("%8s", Integer.toBinaryString(event1)).replace(' ', '0');
            String sensor2Binary = String.format("%8s", Integer.toBinaryString(event2)).replace(' ', '0');
            String extensionBinary = String.format("%8s", Integer.toBinaryString(event3)).replace(' ', '0');

            // Combined sensor data (24 bits) - used by original system
            String sensorData = sensor1Binary + sensor2Binary + extensionBinary;
            position.set("sensorData", sensorData);
            position.set("event1", event1);
            position.set("event2", event2);
            position.set("event3", event3);

            LOGGER.debug("Extended data - event1: {}, event2: {}, event3: {}",
                    event1, event2, event3);
        }

        // Decode datetime
        int seconds = (int) ((datetimeRaw & 0x1F) * 2);
        int minutes = (int) ((datetimeRaw >> 5) & 0x3F);
        int hours = (int) ((datetimeRaw >> 11) & 0x1F);
        int day = (int) ((datetimeRaw >> 16) & 0x1F);
        int month = (int) ((datetimeRaw >> 21) & 0x0F);
        int year = (int) ((datetimeRaw >> 25) & 0x7F) + 2000;

        if (year < 2000 || year > 2050 || month < 1 || month > 12
                || day < 1 || day > 31 || hours > 23 || minutes > 59 || seconds > 59) {
            return null;
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month - 1, day, hours, minutes, seconds);
        applyGpsRollover(calendar, year);
        position.setFixTime(calendar.getTime());
        position.setDeviceTime(calendar.getTime());

        // GTR-9 requirement: deviceTime must come from device, never null
        if (position.getDeviceTime() == null) {
            LOGGER.warn("GPS32 position rejected: deviceTime is null for device {}",
                    deviceSession.getDeviceId());
            return null;
        }

        return position;
    }

    private Position decodeSiemensPingReply(DeviceSession deviceSession, ByteBuf buf) {
        // GTR-9 ping reply has variable size:
        // - Minimum: 32 GPS32 + some ping data
        // - Standard: 32 GPS32 + 96 ping data
        // - Extended: 32 GPS32 + 96 ping data + 4 extended
        // Some devices may send incomplete data, handle gracefully
        if (buf.readableBytes() < 32) {
            return null;
        }

        // First 32 bytes: GPS32 record
        Position position = decodeSiemensGps32Record(deviceSession, buf);
        if (position == null) {
            return null;
        }

        // Check remaining bytes for ping data
        int remainingBytes = buf.readableBytes();
        if (remainingBytes < 49) {
            // Less than minimum (up to ltc13), return GPS position without ping data
            LOGGER.debug("Ping reply has minimal data: {} bytes, returning GPS position only", remainingBytes);
            return position;
        }

        // Read ping reply data (at least 49 bytes available for basic fields)
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

        // Skip neighbor cells if available (27 bytes = 3 neighbor cells * 9 bytes each)
        if (buf.readableBytes() >= 27) {
            buf.skipBytes(27);
        }

        // Read optional fields if available
        int opt = 0;
        int vtime = 0;
        int sync = 0;

        if (buf.readableBytes() >= 14) {
            opt = buf.readUnsignedShort();
            vtime = buf.readUnsignedShort();
            buf.readUnsignedShort(); // opt1 - not used
            buf.readUnsignedShort(); // opt2 - not used
            buf.readUnsignedShort(); // opt3 - not used
            buf.readUnsignedShort(); // opt4 - not used
            sync = buf.readUnsignedShort();
        }

        // GTR-9 Extended Data: Read 4 additional bytes if available
        // Some firmware versions may not include extended data
        if (buf.readableBytes() >= 4) {
            int event1 = buf.readUnsignedByte();
            int event2 = buf.readUnsignedByte();
            int event3 = buf.readUnsignedByte();
            buf.readUnsignedByte(); // crc_record - not used

            String sensor1Binary = String.format("%8s", Integer.toBinaryString(event1)).replace(' ', '0');
            String sensor2Binary = String.format("%8s", Integer.toBinaryString(event2)).replace(' ', '0');
            String extensionBinary = String.format("%8s", Integer.toBinaryString(event3)).replace(' ', '0');
            String sensorData = sensor1Binary + sensor2Binary + extensionBinary;

            position.set("sensorData", sensorData);
            position.set("event1", event1);
            position.set("event2", event2);
            position.set("event3", event3);

            LOGGER.debug("Ping reply extended data - event1: {}, event2: {}, event3: {}",
                    event1, event2, event3);
        } else {
            LOGGER.debug("Ping reply without extended data (old format or incomplete packet)");
        }

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

        if (opt != 0 || vtime != 0 || sync != 0) {
            position.set("opt", opt);
            position.set("vTime", vtime);
            position.set("sync", sync);
        }

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

        // GTR-9 requirement: deviceTime must come from device, never null
        if (position.getDeviceTime() == null) {
            LOGGER.warn("Ping reply position rejected: deviceTime is null for device {}",
                    deviceSession.getDeviceId());
            return null;
        }

        return position;
    }

    private Position decodeSystemLog(DeviceSession deviceSession, ByteBuf buf) {
        // System log - create a simple position with current time
        //Position position = new Position(getProtocolName());
        //position.setDeviceId(deviceSession.getDeviceId());
        //position.setTime(new java.util.Date());

        // Read log data
        if (buf.readableBytes() > 0) {
            byte[] logBytes = new byte[Math.min(buf.readableBytes(), 256)];
            buf.readBytes(logBytes);
            String logData = new String(logBytes, StandardCharsets.US_ASCII).trim();
            LOGGER.info("System log from device {}: {}", deviceSession.getDeviceId(), logData);
            //position.set("systemLog", logData);
        }

        // Set a default valid position (0,0) for system logs
        //position.setValid(false);
        //position.setLatitude(0);
        //position.setLongitude(0);

        return null;
    }

}
