# XSense Protocol Implementation

## Overview

The XSense protocol is a UDP-based tracking protocol that supports position reporting from XSense GPS tracking devices. This implementation decodes binary packets with XOR encryption and CRC16/CCITT validation.

## Features

- **Transport**: UDP (Datagram)
- **Port**: 5262 (default, configurable)
- **Encryption**: XOR cipher (type-specific keys)
- **Validation**: CRC16/CCITT checksum
- **Position Data**: GPS coordinates (DDMM.MMMM format), speed, course, digital I/O, analog inputs
- **Cell Tower**: LAC, Cell ID, and base station information (online mode only)
- **Timezone**: Automatic +7 hours adjustment (Bangkok time)

## Supported Message Types

The protocol supports various message types (decimal values):

### Position Reports
- `102` (0x66) - Extended Position Report
- `103` (0x67) - Batch Position Report
- `104` (0x68) - Batch Offline Position Report
- `112` (0x70) - Batch Online Position Report (Enhanced I/O)
- `113` (0x71) - Batch Offline Position Report (Enhanced I/O)
- **`114` (0x72) - Tini Batch Online Position Report (Enhanced I/O)** ← Most common
- `115` (0x73) - Tini Batch Offline Position Report (Enhanced I/O)

### System Messages
- `97` (0x61) - System Log
- `98` (0x62) - Alert
- `99` (0x63) - Update Interval Time Result
- `100` (0x64) - Engine Control Result
- `101` (0x65) - Ping Reply
- `108` (0x6C) - Ping Reply (Enhanced I/O)

## Configuration

The default port is **5262**. You can customize it in your `traccar.xml` configuration file:

```xml
<entry key='xsense.port'>5262</entry>
```

Change the port number as needed for your deployment.

## Protocol Structure

### Packet Format

```
|---------|------|-----|-----|---------|-----------------|----------|
| Type(1) | Size | Ver | TID | Seq No. | Position Data   | CRC16    |
|         | (2)  | (1) | (3) |   (1)   | (N×16) + BS(44) | CCITT(2) |
|---------|------|-----|-----|---------|-----------------|----------|
```

- **Type**: Message type identifier (1 byte, decimal)
- **Size**: Payload size (2 bytes, little-endian)
- **Ver**: Protocol version (1 byte)
- **TID**: Terminal ID (3 bytes, hex)
- **Seq No.**: Sequence number (1 byte)
- **Position Data**: N position records × 16 bytes each
- **Base Station**: 44 bytes (online mode only, types 112/114)
- **CRC16**: CRC16/CCITT checksum (2 bytes)

### Position Record Format (16 bytes each)

Each position record contains:

| Field | Size | Description |
|-------|------|-------------|
| LatLong | 7 bytes | Lat (3.5 bytes) + Lon (3.5 bytes) as hex string in DDMM.MMMM format |
| Speed | 1 byte | Speed × 1.943 = knots |
| FlagDegree | 1 byte | Bit 6: GPS valid, Bits 0-4: Course (0-31) × 360/32 = degrees |
| Digital | 1 byte | Digital inputs (Bit 0: Ignition) |
| Analog | 1 byte | Analog value upper 8 bits (combines with datetime bits 30-31) |
| Event | 1 byte | Event code |
| Time32 | 4 bytes | Packed datetime (little-endian) + analog bits |

### Packed DateTime Format (32-bit)

```
Bits 30-31: Analog (2 bits)
Bits 26-29: Year (0-15), special: 9=2019, else +2020
Bits 22-25: Month (1-12)
Bits 17-21: Day (1-31)
Bits 12-16: Hour (0-23)
Bits 6-11: Minute (0-59)
Bits 0-5: Second (0-59)
```

**Analog Value**: `(analog_byte << 2) + ((time32 >> 30) & 0x03)` = 10-bit value

### Base Station Data (44 bytes, online mode only)

| Field | Size | Description |
|-------|------|-------------|
| LTCell | 2 bytes | Cell timing (little-endian) |
| LAC | 2 bytes | Location Area Code (little-endian) |
| CI | 2 bytes | Cell ID (little-endian) |
| Ta | 1 byte | Timing Advance |
| Tc | 1 byte | Timing Correction |
| LTbs | 2 bytes | Base station timing (little-endian) |
| BaseStation | 32 bytes | Base station info string (ASCII) |

## Decoding Process

1. **Extract Message Type**: Read first byte (decimal value)
2. **XOR Decode**: Apply type-specific XOR key to bytes 1-N (Type byte stays plain)
3. **Validate CRC**: Compare received CRC16/CCITT with calculated value
4. **Parse Header**: Extract size, version, terminal ID, sequence
5. **Decode Positions**: Parse 16-byte position records
6. **Convert Coordinates**: Parse DDMM.MMMM hex string to decimal degrees
7. **Decode DateTime**: Extract year/month/day/hour/minute/second from packed bits
8. **Apply Timezone**: Add +7 hours (Bangkok time, 25,200,000 ms)
9. **Parse Base Station**: Extract cell tower data if online mode

### Coordinate Conversion Example

```
Hex: "013B2C4" (latitude)
→ Decimal: 1,293,380
→ Format to: "00013380"
→ Parse as DDMM.MMMM: DD=00, MM.MMMM=13.380
→ Decimal degrees: 0 + (13.380/60) = 0.223°
```

## XOR Encryption Keys

| Type | Key | Description |
|------|-----|-------------|
| 97 | 0x39 | System Log |
| 98 | 0x25 | Alert |
| 99 | 0x56 | Update Interval Time Result |
| 100 | 0x72 | Engine Control Result |
| 101 | 0x29 | Ping Reply |
| 102 | 0x33 | Extended Position Report |
| 103 | 0x73 | Batch Position Report |
| 104 | 0xE7 | Batch Offline Position Report |
| 108 | 0x66 | Ping Reply (Enhanced I/O) |
| 112 | 0x7A | Batch Online Position Report (Enhanced I/O) |
| 113 | 0xDC | Batch Offline Position Report (Enhanced I/O) |
| 114 | 0xAD | **Tini Batch Online Position Report (Enhanced I/O)** |
| 115 | 0xD7 | Tini Batch Offline Position Report (Enhanced I/O) |

## Device Identification

Devices are identified by their 3-byte Terminal ID (TID), which is converted to decimal (e.g., TID=0xC29A92 → Device ID=12753042).

## Testing

Unit tests are available in:
```
src/test/java/org/traccar/protocol/XsenseProtocolDecoderTest.java
```

To run tests:
```bash
./gradlew test --tests XsenseProtocolDecoderTest
```

## Implementation Files

- `XsenseProtocol.java` - Protocol definition and UDP server setup
- `XsenseProtocolDecoder.java` - Message decoder implementation
- `XsenseProtocolDecoderTest.java` - Unit tests
- `PortConfigSuffix.java` - Port 5262 registration

## Legacy Reference

This implementation is based on the legacy XSense decoder found in:
```
tools/xsense/boxmanager/UDPServer.java - UDP server entry point
tools/xsense/boxmanager/DataServer.java - Packet handler
tools/xsense/message/MessageProtocalManager.java - Protocol router
tools/xsense/message/MessageDecode.java - XOR + CRC validation
tools/xsense/boxmanager/ProtoManager.java - Message type routing
tools/xsense/message/pack/TiniPositionReportPack.java - Tini batch parser
tools/xsense/message/pack/PositionReport.java - Position data extractor
tools/xsense/message/MessageType.java - Message types + XOR keys
```

## Key Improvements from Legacy

1. **Lat/Lon Parsing**: Fixed DDMM.MMMM format conversion (was incorrectly dividing by 100,000)
2. **Speed Calculation**: Removed double conversion (legacy already multiplied by 1.943)
3. **Analog Value**: Added 2-bit combination with datetime bits 30-31 (10-bit total)
4. **Year Logic**: Implemented special case for year=9 → 2019
5. **Timezone**: Added +7 hours Bangkok timezone adjustment
6. **Base Station**: Added cell tower data parsing for online mode

## Notes

- Coordinates use **DDMM.MMMM format** (Degrees + Minutes), NOT simple decimal
- DateTime is **packed in 32-bit** with bit fields, NOT BCD encoding
- Speed is **already in knots** after multiplying by 1.943
- Analog is **10-bit value** combining analog byte + datetime bits
- **Timezone is +7 hours** (Bangkok) applied to all timestamps
- CRC calculation covers all bytes except the final 2 CRC bytes
- Base station data only present in **online mode** (types 112, 114)

## Troubleshooting

### No Position Data Received

1. Verify UDP port 5262 is open and accessible
2. Check that devices are configured to send to correct IP:port
3. Review Traccar logs for decoding errors
4. Verify Terminal ID is registered in Traccar

### Invalid CRC Errors

1. Ensure correct XOR key is used for message type
2. Check for data corruption in transit
3. Verify CRC16/CCITT algorithm (CRC16_CCITT_FALSE)

### Incorrect Coordinates

1. Verify DDMM.MMMM parsing (not simple division)
2. Check latitude padding to 8 digits, longitude to 9 digits
3. Ensure minutes are divided by 60

### Time Offset Issues

1. Protocol uses **Bangkok timezone (+7 hours)**
2. Year calculation: 9=2019, others=2020+yearBits
3. Check datetime bit extraction (26-29:year, 22-25:month, etc.)

### Base Station Not Parsed

1. Only available in **online mode** (types 112, 114)
2. Requires at least 44 bytes after position records
3. Check buf.readableBytes() >= 44
