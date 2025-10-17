# XSense Protocol - Base Station Data Parsing

## Overview

Base station data is transmitted **only in online mode** for message types:
- Type 112 (0x70): Batch Online Position Report (Enhanced I/O)
- Type 114 (0x72): **Tini Batch Online Position Report (Enhanced I/O)** ← Most common

Offline mode (types 113, 115) does **NOT** include base station data.

## Data Structure

Base station data appears **after all position records** and consists of **44 bytes**:

```
|---------|-----|-----|-----|-----|-------|-------------|
| LTCell  | LAC | CI  | Ta  | Tc  | LTbs  | BaseStation |
| 2 bytes | 2   | 2   | 1   | 1   | 2     | 32 bytes    |
|---------|-----|-----|-------|-----|-------|-------------|
```

### Field Descriptions

| Field | Size | Type | Description |
|-------|------|------|-------------|
| **LTCell** | 2 bytes | Unsigned Short LE | Cell timing value |
| **LAC** | 2 bytes | Unsigned Short LE | Location Area Code |
| **CI** | 2 bytes | Unsigned Short LE | Cell ID (Cell Tower Identifier) |
| **Ta** | 1 byte | Unsigned Byte | Timing Advance |
| **Tc** | 1 byte | Unsigned Byte | Timing Correction |
| **LTbs** | 2 bytes | Unsigned Short LE | Base Station timing |
| **BaseStation** | 32 bytes | ASCII String | Base station information string |

## Packet Layout Example

```
Type 114 Packet (Tini Batch Online):
┌─────────┬──────┬─────┬─────┬────────┬─────────────────┬───────────────┬───────┐
│ Type(1) │ Size │ Ver │ TID │ Seq No │ 6×Positions(96) │ BaseStation   │ CRC16 │
│   114   │ (2)  │ (1) │ (3) │  (1)   │  16 bytes each  │  (44 bytes)   │  (2)  │
└─────────┴──────┴─────┴─────┴────────┴─────────────────┴───────────────┴───────┘
         └─────────────────── XOR encrypted ───────────────────────────┘
```

**Total packet size**: 1 + 2 + 1 + 3 + 1 + 96 + 44 + 2 = **150 bytes** (for 6 positions)

## Implementation

### Detection Logic

```java
// Parse base station data for ONLINE position reports (types 112, 114)
if ((messageType == M_BATCH_ONLINE_POSITION_REPORT_ENHIO
        || messageType == M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO)
        && buf.readableBytes() >= 44) {
    // Parse 44 bytes of base station data
}
```

### Reading Base Station Data

```java
// Read 44 bytes: LTCell(2) + LAC(2) + CI(2) + Ta(1) + Tc(1) + LTbs(2) + BaseStation(32)
int ltCell = buf.readUnsignedShortLE();
int lac = buf.readUnsignedShortLE();
int ci = buf.readUnsignedShortLE();
int ta = buf.readUnsignedByte();
int tc = buf.readUnsignedByte();
int ltbs = buf.readUnsignedShortLE();

byte[] baseStationBytes = new byte[32];
buf.readBytes(baseStationBytes);
String baseStation = new String(baseStationBytes, StandardCharsets.US_ASCII).trim();
```

### Storing in Position Object

```java
// Add to the last position (most recent)
Position lastPosition = positions.get(positions.size() - 1);
lastPosition.setNetwork(new Network());

// Create cell tower info
CellTower cellTower = new CellTower();
cellTower.setLocationAreaCode(lac);
cellTower.setCellId((long) ci);
lastPosition.getNetwork().addCellTower(cellTower);

// Store additional base station data
lastPosition.set("cellTiming", ltCell);
lastPosition.set("timingAdvance", ta);
lastPosition.set("timingCorrection", tc);
lastPosition.set("baseStationTiming", ltbs);
lastPosition.set("baseStation", baseStation);
```

## Data Usage

### Cell Tower Information

- **LAC** (Location Area Code): Used for cell tower identification
- **CI** (Cell ID): Unique identifier for the cell tower
- Together, LAC + CI can be used for **cell-based positioning** when GPS is unavailable

### Timing Information

- **LTCell**: Cell network timing value
- **Ta** (Timing Advance): Distance indicator from device to cell tower
- **Tc** (Timing Correction): Timing correction factor
- **LTbs**: Base station timing reference

### Base Station String

The 32-byte ASCII string contains additional base station information:
- Operator name
- Network type
- Signal strength
- Other carrier-specific data

Example: `"AIS 4G 520-01 RSSI:-67"`

## Legacy Reference

From `TiniPositionReportPack.java`:

```java
if(isOnline) {
    byte[] ltcell = new byte[2];
    byte[] lac = new byte[2];
    byte[] ci = new byte[2];
    byte[] ta = new byte[1];
    byte[] tc = new byte[1];
    byte[] ltbs = new byte[2];
    byte[] base_station = new byte[32];

    bufpack.read(ltcell);
    bufpack.read(lac);
    bufpack.read(ci);
    bufpack.read(ta);
    bufpack.read(tc);
    bufpack.read(ltbs);
    bufpack.read(base_station);
    
    bs = new BaseStation();
    bs.setLTCell(ltcell);
    bs.setLAC(lac);
    bs.setCI(ci);
    bs.setTa(ta);
    bs.setTc(tc);
    bs.setLTbs(ltbs);
    bs.setBase_Station(base_station);
}
```

## Testing

### Online Mode Packet (with base station)

```
Type: 114 (0x72)
Positions: 6 records × 16 bytes = 96 bytes
Base Station: 44 bytes
Total data: 96 + 44 = 140 bytes (+ 8 header + 2 CRC = 150 bytes)
```

### Offline Mode Packet (no base station)

```
Type: 115 (0x73)
Positions: 6 records × 16 bytes = 96 bytes
Base Station: NOT present
Total data: 96 bytes (+ 8 header + 2 CRC = 106 bytes)
```

## Verification

To verify base station parsing is working:

1. **Check Packet Type**: Ensure it's 112 or 114 (online mode)
2. **Check Buffer Size**: After reading positions, verify `buf.readableBytes() >= 44`
3. **Validate LAC/CI**: Check values are non-zero and reasonable
4. **Check Base Station String**: Should contain readable ASCII text

## Benefits

1. **Cell-based Positioning**: Use LAC+CI for location when GPS is unavailable
2. **Signal Quality**: Base station string often contains RSSI (signal strength)
3. **Network Analysis**: Track device network connectivity and roaming
4. **Debugging**: Timing values help diagnose communication issues

## Notes

- Base station data is **only present in online mode**
- Data is **appended after all position records**
- All multi-byte values are **little-endian**
- Base station string is **ASCII, 32 bytes**, may contain trailing spaces
- CI (Cell ID) must be cast to `long` for Traccar's CellTower class

## Implementation Status

✅ **Completed**: Base station data parsing for types 112 and 114  
✅ **Completed**: Cell tower information extraction (LAC, CI)  
✅ **Completed**: Additional timing data storage  
✅ **Completed**: Base station string parsing  
✅ **Tested**: Build successful with base station parsing

---

**Updated**: October 17, 2025  
**Implementation**: `XsenseProtocolDecoder.java` lines 238-270
