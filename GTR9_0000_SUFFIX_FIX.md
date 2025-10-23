# GTR-9 Frame Decoder - Support 0000 Suffix

## Problem
Some GTR-9 devices send packets with `0000` suffix instead of standard `7E7E` suffix:
```
7E7E7E7E00 [data] 0000  ← Invalid (was rejected)
```

This caused CRC validation failures with error:
```
WARN: CRC validation failed: type=126, received=0000, calculated=139B
```

## Root Cause
- XsenseFrameDecoder only recognized `7E7E` as valid suffix
- Packets with `0000` suffix were not properly framed
- Frame decoder waited indefinitely for `7E7E`, causing incomplete packet processing

## Solution
Modified `XsenseFrameDecoder.java` to accept both `7E7E` and `0000` as valid suffixes:

```java
// Find suffix: 7E 7E or 00 00 (some devices send 0000 instead of 7E7E)
int endIndex = -1;
for (int i = buf.readerIndex(); i < buf.writerIndex() - 1; i++) {
    int byte1 = buf.getUnsignedByte(i);
    int byte2 = buf.getUnsignedByte(i + 1);
    
    if ((byte1 == 0x7E && byte2 == 0x7E) || (byte1 == 0x00 && byte2 == 0x00)) {
        endIndex = i;
        break;
    }
}
```

## Test Results
✓ Packets with `7E7E` suffix decode correctly (existing behavior)
✓ Packets with `0000` suffix now decode correctly (new behavior)
✓ All GTR-9 protocol tests pass

## Example Packet (0000 suffix)
```
Full: 7E7E7E7E00 73092410AB...746B 0000
After framing: 73092410AB...746B
Type: 115 (OFFLINE position report)
Device: 1304267
Status: ✓ Decoded successfully
```
