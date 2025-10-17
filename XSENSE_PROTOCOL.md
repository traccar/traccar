# XSense Protocol Implementation

## Overview

The XSense protocol is a UDP-based tracking protocol that supports position reporting from XSense GPS tracking devices. This implementation decodes binary packets with XOR encryption and CRC16/CCITT validation.

## Features

- **Transport**: UDP (Datagram)
- **Port**: 5262 (default, configurable)
- **Encryption**: XOR cipher (type-specific keys)
- **Validation**: CRC16/CCITT checksum
- **Position Data**: GPS coordinates, speed, course, satellites, digital inputs

## Supported Message Types

The protocol supports various message types:

### Position Reports
- `0x10` - Extended Position Report
- `0x11` - Batch Position Report
- `0x12` - Batch Offline Position Report
- `0x30` - Batch Online Position Report (Enhanced I/O)
- `0x31` - Batch Offline Position Report (Enhanced I/O)
- `0x40` - Tini Batch Online Position Report (Enhanced I/O)
- `0x41` - Tini Batch Offline Position Report (Enhanced I/O)

### System Messages
- `0x01` - System Log
- `0x02` - Alert
- `0x03` - Update Interval Time Result
- `0x04` - Engine Control Result
- `0x05` - Ping Reply
- `0x25` - Ping Reply (Enhanced I/O)

## Configuration

The default port is **5262**. You can customize it in your `traccar.xml` configuration file:

```xml
<entry key='xsense.port'>5262</entry>
```

Change the port number as needed for your deployment.

## Protocol Structure

### Packet Format

```
|---------|------|-----|-----|---------|-------------|--------|----------|
| Type(1) | Size | Ver | TID | Seq No. | Data+Extend | CRC16  |
|         | (2)  | (1) | (3) |   (1)   |    (N+M)    | CCITT  |
|---------|------|-----|-----|---------|-------------|--------|----------|
```

- **Type**: Message type identifier (1 byte)
- **Size**: Payload size (2 bytes, little-endian)
- **Ver**: Protocol version (1 byte)
- **TID**: Terminal ID (3 bytes)
- **Seq No.**: Sequence number (1 byte)
- **Data**: Message payload (variable length)
- **Extend**: Extended data (variable length)
- **CRC16**: CRC16/CCITT checksum (2 bytes)

### Position Record Format

Each position record in the data payload contains:

- **DateTime**: 6 bytes BCD-encoded (YYMMDDHHMMSS)
- **Latitude**: 4 bytes little-endian integer (degrees × 1,000,000)
- **Longitude**: 4 bytes little-endian integer (degrees × 1,000,000)
- **Speed**: 2 bytes little-endian (km/h)
- **Course**: 2 bytes little-endian (degrees)
- **Status**: 1 byte (bit 0 = GPS valid)
- **Satellites**: 1 byte (optional)
- **Digital Inputs**: 2 bytes little-endian (optional)

## Decoding Process

1. **Extract Message Type**: Read first byte to determine message type
2. **XOR Decode**: Apply type-specific XOR key to bytes 1-N
3. **Validate CRC**: Compare received CRC16/CCITT with calculated value
4. **Parse Header**: Extract size, version, terminal ID, sequence
5. **Decode Positions**: Parse position records from payload
6. **Convert Coordinates**: Transform raw integers to decimal degrees
7. **Set Timestamps**: Convert BCD datetime to UTC timestamp

## Device Identification

Devices are identified by their 3-byte Terminal ID (TID), which is converted to a 6-character hexadecimal string (e.g., "A1B2C3").

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

## Legacy Reference

This implementation is based on the legacy XSense decoder found in:
```
tools/xsense/message/MessageDecode.java
tools/xsense/message/MessageType.java
tools/xsense/message/pack/ExtendPositionPack.java
tools/xsense/message/pack/ExtendPosition.java
```

## Notes

- All coordinates are stored as integers (degrees × 1,000,000) in the protocol
- DateTime fields use BCD encoding (2 digits per byte)
- Speed is transmitted in km/h and converted to knots for Traccar
- The protocol uses little-endian byte order for multi-byte integers
- CRC calculation covers all bytes except the final 2 CRC bytes

## Troubleshooting

### No Position Data Received

1. Verify UDP port 5262 is open and accessible
2. Check that devices are configured to send to correct IP:port
3. Review Traccar logs for decoding errors
4. Verify Terminal ID is registered in Traccar

### Invalid CRC Errors

1. Ensure correct XOR key is used for message type
2. Check for data corruption in transit
3. Verify CRC16/CCITT algorithm matches device implementation

### Time Offset Issues

1. Devices send datetime in local time or UTC (device-dependent)
2. Check device configuration for timezone settings
3. Review position timestamps in Traccar for accuracy
