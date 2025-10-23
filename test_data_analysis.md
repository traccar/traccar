# Howen Protocol Test Data Analysis - Detailed Breakdown

## Raw Hex Data:
```
48014110600000002436423842343536372d32334336333237422d41393938334336342d373334383333363600190a141309300f000101190a141309302a1500000d000600645ba604000d2b51080007ffffffff000000000200004000001f000101000100000000
```

## Complete Packet Structure:

### 1. Protocol Header (8 bytes):
```
48 01 41 10 60 00 00 00
```
- `48` (0x48): Protocol identifier 'H'
- `01`: Protocol version/type
- `41 10`: Message ID = 0x1041 (little-endian) = STATUS message
- `60 00`: Payload length = 0x0060 = 96 bytes
- `00 00`: Sequence number/reserved

### 2. Device ID (36 bytes):
```
24 36 42 38 42 34 35 36 37 2d 32 33 43 36 33 32 37 42 2d 41 39 39 38 33 43 36 34 2d 37 33 34 38 33 33 36 36 00
```
ASCII String: `$6B8B4567-23C6327B-A9983C64-73483366\0`
- Format: UUID-like device identifier
- NULL terminated (00 at end)

### 3. Device Timestamp (6 bytes):
```
19 0a 14 13 09 30
```
Parsed:
- Year: 0x19 = 25 → 2025
- Month: 0x0a = 10 (October)
- Day: 0x14 = 20
- Hour: 0x13 = 19 (7:00 PM)
- Minute: 0x09 = 9
- Second: 0x30 = 48

**Device Time: 2025-10-20 19:09:48**

### 4. Content Bitmask (2 bytes):
```
0f 00
```
Binary: `0000 0000 0000 1111`

Active bits:
- ✅ Bit 0: Location data
- ✅ Bit 1: G-Sensor data
- ✅ Bit 2: Basic status (4 bytes)
- ✅ Bit 3: Module status (8 bytes)
- ❌ Bit 5: Storage status (not present)
- ❌ Bit 7: Temperature status (not present)
- ❌ Bit 8: Mobile status (not present)
- ❌ Bit 9: Alarm status (not present)

### 5. Location Data (Present - Bit 0):

#### Location Timestamp (6 bytes):
```
01 01 19 0a 14 13 09 30
```
Wait, let me re-parse this...

Actually, after the content mask, the data should be:
```
0f 00 01 01 19 0a 14 13 09 30 2a 15 00 00 0d 00 06 00 64 5b a6 04 00 0d 2b 51 08 00 07 ff ff ff ff 00 00 00 00 02 00 00 40 00 00 1f 00 01 01 00 01 00 00 00 00
```

Let me break this down correctly:

### Data Sections After Content Mask (0f 00):

#### A. Basic Status (Bit 2) - 4 bytes:
```
01 01 19 0a
```
Wait, this doesn't look right. Let me reconsider the structure...

## Corrected Analysis:

After content mask `0f 00`, if Bit 0 (Location) is set, we need to read location first.

Let me look at the test code pattern and re-analyze:

### Content Mask: 0x000f
- Bit 0 (Location): YES
- Bit 1 (G-Sensor): YES  
- Bit 2 (Basic Status): YES
- Bit 3 (Module Status): YES

### Reading Order (based on code):
1. Location (if bit 0)
2. G-Sensor (if bit 1) 
3. Basic Status (if bit 2) - 4 bytes
4. Module Status (if bit 3) - 8 bytes
5. Mobile Status (if bit 8)
6. Storage Status (if bit 5)
7. Alarm Status (if bit 9)
8. Temperature Status (if bit 7)

### Remaining Data to Parse:
```
01 01 19 0a 14 13 09 30 2a 15 00 00 0d 00 06 00 64 5b a6 04 00 0d 2b 51 08 00 07 ff ff ff ff 00 00 00 00 02 00 00 40 00 00 1f 00 01 01 00 01 00 00 00 00
```

Length: 58 bytes

#### Location Data:
Looking at the existing test, location format seems to be:
- First we need to understand if there's a timestamp before location or not

Let me check the actual decoder implementation to understand the exact format...

## Summary of What We Know:
- Message Type: 0x1041 (Status)
- Device ID: $6B8B4567-23C6327B-A9983C64-73483366
- Device Time: 2025-10-20 19:09:48
- Content Mask: 0x000F (Location + G-Sensor + Basic Status + Module Status)
- Remaining payload: 58 bytes to be parsed

**Next Step:** Need to trace through decoder to understand exact byte-by-byte parsing.
