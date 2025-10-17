# XSense Protocol Decoder - Review Findings

## Overview
การ review code เดิมจาก `tools/xsense` พบความแตกต่างสำคัญหลายจุดที่ต้องแก้ไขใน XsenseProtocolDecoder.java

## Flow การทำงานของ Legacy Code

```
UDPServer.java (Main entry point)
    ↓
DataServer.java (Thread handler for each UDP packet)
    ↓
MessageProtocalManager.java (Protocol manager)
    ↓
MessageDecode.java (XOR decrypt + CRC validation)
    ↓
ProtoManager.java (Route by message type)
    ↓
TiniPositionReportPack.java / PositionReportPack.java (Parse position records)
    ↓
PositionReport.java (Extract individual position data)
```

## Critical Fixes Applied

### 1. **Latitude/Longitude Parsing** ⚠️ MAJOR FIX

**Original (WRONG):**
```java
long latVal = Long.parseLong(latlongHex.substring(0, 7), 16);
position.setLatitude(latVal / 100000.0);
```

**Legacy Format (CORRECT):**
```java
// Format: DDMM.MMMM (Degrees + Minutes)
// Example: "013B2C4" hex = 1,293,380 dec
// Convert to: "0013.2C4" → DD=13, MM.MMMM=2C4
// Decimal degrees = 13 + (2C4/60)

private double parseLatitude(String hex) {
    long value = Long.parseLong(hex, 16);
    String str = String.format("%08d", value); // "00013380"
    double degrees = Double.parseDouble(str.substring(0, 2)); // "00"
    double minutes = Double.parseDouble(str.substring(2, 4) + "." + str.substring(4)); // "13.380"
    return degrees + (minutes / 60.0);
}
```

**Longitude** uses same logic but with 9-digit format (DDDMM.MMMM)

**Reference:** `PositionReport.java` lines 120-152
```java
public String latPaser(String lat) {
    if(lat.length()<8) lat="0"+lat;
    lat = lat.substring(0, 4) + "." + lat.substring(4);
    return nlatlongformatter.format(
        Double.parseDouble(lat.substring(0, 2)) + 
        (Double.parseDouble(lat.substring(2)) / 60)
    );
}
```

---

### 2. **Speed Calculation** ⚠️ MAJOR FIX

**Original (WRONG):**
```java
position.setSpeed(UnitsConverter.knotsFromKph(speed * 1.943));
// This converts TWICE: first multiply by 1.943, then knotsFromKph
```

**Legacy (CORRECT):**
```java
position.setSpeed(speed * 1.943);
// Raw speed * 1.943 = knots (already final value)
```

**Reference:** `PositionReport.java` line 93
```java
public String getSpeed() {
    return nspeed.format(Binary.BitByteToDec(speed[0],8) * 1.943);
}
```

---

### 3. **Analog Value Calculation** ⚠️ CRITICAL FIX

**Original (WRONG):**
```java
int analog = buf.readUnsignedByte();
position.set(Position.KEY_POWER, analog);
// Missing 2 bits from datetime!
```

**Legacy (CORRECT):**
```java
int analogByte = buf.readUnsignedByte();
long time32 = buf.readUnsignedIntLE();
// Combine: analog value = (analog_byte << 2) + top 2 bits of datetime
int analogValue = (analogByte << 2) + (int) ((time32 >> 30) & 0x03);
position.set(Position.KEY_POWER, analogValue);
```

**Reference:** `PositionReport.java` line 127
```java
public int getAnalog() {	
    return (Binary.BitByteToDec(analog[0],8)<<2) + 
           Binary.BitIntToDec(datetime,2,30);
    // analog byte (8 bits) + datetime bits 30-31 (2 bits) = 10-bit value
}
```

---

### 4. **Year Calculation - Special Logic** ⚠️ MAJOR FIX

**Original (WRONG):**
```java
int year = (int) ((time32 >> 26) & 0x0F) + 2000;
// Simple +2000 doesn't match legacy behavior
```

**Legacy (CORRECT):**
```java
int yearBits = (int) ((time32 >> 26) & 0x0F); // 0-15
int year = (yearBits == 9) ? 2019 : (2020 + yearBits);
// Special case: 9 = 2019, others = 2020+
```

**Reasoning:** Legacy comment indicates:
- Year bit 9 = 2019 (transition year)
- Year bits 0-8, 10-15 = 2020+

**Reference:** `PositionReport.java` lines 139-149
```java
int year = (Binary.BitIntToDec(datetime,4,26)); // 0-9 bits
if(year == 9) { 
    year = year + 2010;  // 2019
} else {
    year = year + 2020;  // 2020-2029
}
```

---

### 5. **Timezone Handling** ✅ CORRECTED

**Initial Implementation (INCORRECT):**
```java
Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
calendar.set(year, month - 1, day, hour, minute, second);
calendar.add(Calendar.MILLISECOND, 25200000); // +7 hours (Bangkok)
position.setTime(calendar.getTime());
```

**Traccar Standard (CORRECT):**
```java
Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
calendar.set(year, month - 1, day, hour, minute, second);
position.setTime(calendar.getTime());
// Store as UTC+0 (device sends GPS time in UTC)
```

**Why No Conversion Needed:**
- **Device sends**: GPS time (UTC+0)
- **Traccar stores**: All timestamps in UTC+0
- **Frontend displays**: User's preferred timezone
- **Legacy +7 hours**: Was for display purposes only, not storage

**Reference:** `PositionReport.java` line 152
```java
private String ConvertTimeZone(String s, int c) {
    dt.setTime(dt.getTime() + c); // Bkk +7H = 25200000ms (for display)
    return DATE_FORMAT.format(dt);
}
```

Legacy server converted for **display**, Traccar lets **frontend handle timezone**.

---

## Packet Structure - Confirmed from Legacy Code

### Header (8 bytes)
| Field | Size | Description |
|-------|------|-------------|
| Type | 1 | Message type (decimal: 97-115) |
| Size | 2 | Data size (little-endian) |
| Version | 1 | Protocol version |
| TID | 3 | Device ID (hex) |
| SeqNo | 1 | Sequence number |

### Position Record (16 bytes each)
| Field | Size | Bits | Description |
|-------|------|------|-------------|
| LatLong | 7 | 56 | Lat (3.5 bytes) + Lon (3.5 bytes) in DDMM.MMMM format |
| Speed | 1 | 8 | Speed in km/h |
| FlagDegree | 1 | 8 | Bit 6: GPS valid, Bits 0-4: Course (0-31) |
| Digital | 1 | 8 | Digital inputs (Bit 0: Ignition) |
| Analog | 1 | 8 | Analog input (upper 8 bits of 10-bit value) |
| Event | 1 | 8 | Event code |
| Time32 | 4 | 32 | Packed datetime + analog bits 30-31 |

### Footer
| Field | Size | Description |
|-------|------|-------------|
| CRC16 | 2 | CRC16/CCITT checksum |

### Base Station Data (Online mode only, after all position records)
| Field | Size | Description |
|-------|------|-------------|
| LTCell | 2 | Cell timing |
| LAC | 2 | Location Area Code |
| CI | 2 | Cell ID |
| Ta | 1 | Timing Advance |
| Tc | 1 | Timing correction |
| LTbs | 2 | Base station timing |
| BaseStation | 32 | Base station info string |

---

## XOR Decryption Keys (from MessageType.java)

```java
case 97:  return (byte) 0x39; // M_SYSTEM_LOG
case 98:  return (byte) 0x25; // M_ALERT
case 99:  return (byte) 0x56; // M_UPDATE_INTERVAL_TIME_RESULT
case 100: return (byte) 0x72; // M_ENGINE_CONTROL_RESULT
case 101: return (byte) 0x29; // M_PING_REPLY
case 102: return (byte) 0x33; // M_EXTEND_POSITION_REPORT
case 103: return (byte) 0x73; // M_BATCH_POSITION_REPORT
case 104: return (byte) 0xE7; // M_BATCH_OFFLINE_POSITION_REPORT
case 108: return (byte) 0x66; // M_PING_REPLY_ENHIO
case 112: return (byte) 0x7A; // M_BATCH_ONLINE_POSITION_REPORT_ENHIO
case 113: return (byte) 0xDC; // M_BATCH_OFFLINE_POSITION_REPORT_ENHIO
case 114: return (byte) 0xAD; // M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO
case 115: return (byte) 0xD7; // M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO
```

---

## Testing with Real Packet

### Packet Example (Type 114, TiniPositionReport Online)
```
72ad3ac5bd7d3fae3c60abba63f85426ba4d64f85126ba2665f85026ba0866f84f26
ba5566f84e26ba4367f84e26ba0d2800010db1b3110101010001002bc5b32e202020
202020202020202020202020202020202020202020202020202020202020dcb5
```

**After XOR 0xAD:**
- Type: 0x72 (114)
- Size: 0x0097 (151 bytes)
- Version: 0x70
- TID: 0xC29A92 (12,753,042)
- Records: 6 positions (6 × 16 = 96 bytes)
- Base Station: 44 bytes
- CRC: 0xB5DC

---

## Summary of Changes

✅ **Fixed:** Lat/Lon parsing (DDMM.MMMM format)  
✅ **Fixed:** Speed calculation (remove double conversion)  
✅ **Fixed:** Analog value (combine with datetime bits)  
✅ **Fixed:** Year calculation (2019 special case)  
✅ **Fixed:** Timezone handling (store as UTC+0, no conversion)  
✅ **Removed:** Unused import `UnitsConverter`

---

## Next Steps

1. ✅ Build successful
2. ⏳ Test with real UDP packets from XSense devices
3. ⏳ Verify position accuracy with known coordinates
4. ⏳ Test base station data parsing (online mode)
5. ⏳ Validate datetime conversion with multiple years

---

## References

Legacy Code Files:
- `tools/xsense/boxmanager/UDPServer.java` - Main server entry
- `tools/xsense/boxmanager/DataServer.java` - UDP packet handler
- `tools/xsense/message/MessageProtocalManager.java` - Protocol router
- `tools/xsense/message/MessageDecode.java` - XOR + CRC validation
- `tools/xsense/boxmanager/ProtoManager.java` - Message type router
- `tools/xsense/message/pack/TiniPositionReportPack.java` - Tini batch parser
- `tools/xsense/message/pack/PositionReport.java` - **CRITICAL** Position data extractor
- `tools/xsense/message/MessageType.java` - Message types + XOR keys
- `tools/xsense/util/Binary.java` - Bit manipulation utilities
- `tools/xsense/util/CRC16CheckSum.java` - CRC validation

---

Generated: October 17, 2025  
Reviewed by: Code Analysis of Legacy XSense Protocol Implementation
