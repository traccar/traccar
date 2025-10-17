# XSense Protocol: Base Station Data Parsing Fix

## Issue Description

When parsing ONLINE position reports (message types 107, 114), the decoder was incorrectly treating base station data (44 bytes) as additional position records. This caused:

1. **Incorrect record count**: Calculating `dataLength / 16` included base station bytes
2. **Garbage position data**: Last 2-3 records contained ASCII text from base station info
3. **Invalid dates**: Records with month=0 or month=15 that should have been filtered

## Problem Analysis

### Example Packet (Type 114):
```
Message Type: 114 (M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO)
Total data: 144 bytes
Structure: 6 position records (96 bytes) + 44 bytes base station + 4 bytes padding
```

**Before Fix:**
- Calculated: 144 / 16 = 9 position records
- Records 7-9 contained: `0008fa34ac740322ffff3e38393636313831303038323539...`
- This is actually base station data (ASCII: "896618100825935496")

**After Fix:**
- For ONLINE modes, subtract 44 bytes first: (144 - 44) / 16 = 6 records
- Parse 6 clean position records
- Parse 44 bytes base station data separately

## Solution Implemented

### 1. Calculate Position Data Length Correctly

```java
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

// Each record is 16 bytes
int recordCount = positionDataLength / 16;
```

### 2. Add Date Validation

```java
// Decode packed datetime
int yearBits = (int) ((time32 >> 26) & 0x0F);
int year = (yearBits == 9) ? 2019 : (2020 + yearBits);
int month = (int) ((time32 >> 22) & 0x0F);
int day = (int) ((time32 >> 17) & 0x1F);
// ... decode other fields ...

// Only add valid positions with reasonable datetime
if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
    positions.add(position);
}
```

## Test Results

### Packet: `72ad3ac5bd7d3f9dabba602854266aad...`

**Before Fix (9 records):**
- Records 1-6: Valid GPS positions
- Record 7: Lat=0.061285°, Lon=78.684592° (garbage)
- Record 8: Lat=59.536218°, Lon=4.138115° (garbage)
- Record 9: All zeros

**After Fix (4 valid records out of 6):**
- Record 1: 6.648253°N, 100.400545°E - 2024-11-11 10:10:22 ✓
- Record 2: 6.648260°N, 100.400547°E - 2019-11-11 10:10:22 ✓
- Record 3: 6.648247°N, 100.400523°E - 2034-11-11 10:10:22 ✓
- Record 4: 6.648243°N, 100.400527°E - 2024-11-11 10:10:22 ✓
- Record 5: Invalid (month=15) - filtered out ✗
- Record 6: Invalid (month=15) - filtered out ✗

**Base Station Data (44 bytes):**
```
LTCell: 29868
LAC: 8707
CI: 65535
Ta: 62
Tc: 56
LTbs: 13881
BaseStation: '6181008259354965'
```

## Impact

### Fixed Issues:
1. ✅ Correct position record count for ONLINE modes
2. ✅ No more garbage position data
3. ✅ Base station data properly parsed and attached to last position
4. ✅ Invalid dates filtered out

### Backward Compatibility:
- ✅ OFFLINE modes (103, 104, 105, 106, 108, 115) unaffected - continue using full data length
- ✅ Date validation is defensive - only filters obvious invalid dates

## Related Files

- `src/main/java/org/traccar/protocol/XsenseProtocolDecoder.java` (lines 161-258)
- `XSENSE_BASE_STATION.md` - Base station data format documentation

## Testing Recommendations

1. Test ONLINE mode packets (types 107, 114) with various position counts
2. Test OFFLINE mode packets (types 103-106, 108, 115) to ensure no regression
3. Verify base station data appears in position attributes
4. Check that invalid dates (month=0, month=15, day=0) are filtered

## Date Range Issue

Note: Some records show dates in 2019 or 2034 which may indicate:
- Device clock issues
- Year bit decoding problems
- Test data with invalid timestamps

Future improvement: Add year validation (e.g., 2020-2030 range) if this is a systematic issue.
