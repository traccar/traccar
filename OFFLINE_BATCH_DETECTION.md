# Offline Batch Detection

## Overview

This document describes the offline batch detection feature implemented in Traccar. The feature automatically identifies and marks position data that was recorded offline and sent later, distinguishing it from real-time position updates.

## Purpose

GPS tracking devices often lose connectivity (no network signal) and store position data locally. When connectivity is restored, they send this buffered data in batches. This feature helps:

- **Data Analysis**: Distinguish between real-time tracking and historical data playback
- **Alert Management**: Avoid triggering false real-time alerts for old data
- **Reporting**: Separate live tracking from offline batch uploads
- **Quality Control**: Identify data delivery delays

## Implementation

### Base Implementation

The core detection logic is implemented in `BaseProtocolDecoder.java` as a reusable helper method:

```java
/**
 * Detect and mark offline batch data based on time gap.
 * If position time differs from server time by more than the threshold, mark as offline batch.
 *
 * @param position Position to check
 * @param thresholdMinutes Time gap threshold in minutes (default: 10 minutes)
 */
protected void detectOfflineBatch(Position position, int thresholdMinutes)

/**
 * Detect and mark offline batch data with default threshold of 10 minutes.
 *
 * @param position Position to check
 */
protected void detectOfflineBatch(Position position)
```

### Detection Methods

There are two approaches to detect offline batch data:

#### 1. Message Type Detection (Protocol-Specific)

Some protocols have explicit message types for offline data:

| Protocol | Message Type | Description |
|----------|--------------|-------------|
| **XsenseGtr9** | Type 115 | `M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO` |
| **XsenseProtocol** | Types 108, 115 | `M_BATCH_OFFLINE_POSITION_REPORT_ENHIO` and `M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO` |

**Example Implementation:**
```java
// In XsenseGtr9ProtocolDecoder.java
if (messageType == M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO) {
    position.set("offlineBatch", true);
    position.set("batchType", "offline");
} else {
    position.set("offlineBatch", false);
    position.set("batchType", "online");
}
```

#### 2. Time Gap Detection (Universal)

For protocols without explicit offline indicators, detection is based on the time difference between the position timestamp and server receipt time.

**Default Threshold:** 10 minutes

**Logic:**
```
if (|serverTime - positionTime| > 10 minutes) {
    → offlineBatch = true
} else {
    → offlineBatch = false
}
```

**Protocols Using Time Gap Detection:**
- Howen
- Ruptela
- Meitrack
- Fifotrack

## Usage

### For Protocol Developers

#### Adding Offline Batch Detection to Any Protocol

Simply call the helper method after decoding position data:

```java
// In your protocol decoder
Position position = new Position(getProtocolName());
// ... decode position data ...

// Add offline batch detection (uses 10-minute default)
detectOfflineBatch(position);

return position;
```

#### Custom Threshold

If your protocol needs a different time threshold:

```java
// Use 15-minute threshold instead of default 10
detectOfflineBatch(position, 15);
```

#### Example: Adding to a New Protocol

```java
public class MyProtocolDecoder extends BaseProtocolDecoder {
    
    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) {
        // ... protocol-specific decoding ...
        
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        
        // Decode GPS data
        position.setTime(/* parse time */);
        position.setLatitude(/* parse latitude */);
        position.setLongitude(/* parse longitude */);
        
        // Add offline batch detection
        detectOfflineBatch(position);  // ← Just one line!
        
        return position;
    }
}
```

## Output Format

### Position Attributes

When offline batch detection is applied, the following attributes are added to the position:

| Attribute | Type | Values | Description |
|-----------|------|--------|-------------|
| `offlineBatch` | Boolean | `true` / `false` | Indicates if data is from offline batch |
| `batchType` | String | `"offline"` / `"online"` | Human-readable batch type |

### Example Position Data

**Real-time Position:**
```json
{
  "deviceId": 1,
  "latitude": 13.7563,
  "longitude": 100.5018,
  "deviceTime": "2025-10-24T10:00:00Z",
  "serverTime": "2025-10-24T10:01:00Z",
  "attributes": {
    "offlineBatch": false,
    "batchType": "online"
  }
}
```

**Offline Batch Position:**
```json
{
  "deviceId": 1,
  "latitude": 13.7563,
  "longitude": 100.5018,
  "deviceTime": "2025-10-24T08:00:00Z",
  "serverTime": "2025-10-24T10:00:00Z",
  "attributes": {
    "offlineBatch": true,
    "batchType": "offline"
  }
}
```

## Database Queries

### SQL Examples

**Query all offline batch positions:**
```sql
SELECT * FROM tc_positions 
WHERE attributes LIKE '%"offlineBatch":true%';
```

**Query online (real-time) positions:**
```sql
SELECT * FROM tc_positions 
WHERE attributes LIKE '%"offlineBatch":false%';
```

**Count offline vs online positions by device:**
```sql
SELECT 
    deviceid,
    SUM(CASE WHEN attributes LIKE '%"offlineBatch":true%' THEN 1 ELSE 0 END) as offline_count,
    SUM(CASE WHEN attributes LIKE '%"offlineBatch":false%' THEN 1 ELSE 0 END) as online_count
FROM tc_positions
GROUP BY deviceid;
```

**Find positions with large time gaps:**
```sql
SELECT 
    deviceid,
    devicetime,
    servertime,
    TIMESTAMPDIFF(MINUTE, devicetime, servertime) as gap_minutes,
    attributes
FROM tc_positions
WHERE TIMESTAMPDIFF(MINUTE, devicetime, servertime) > 10
ORDER BY gap_minutes DESC;
```

## Supported Protocols

### Current Status

| Protocol | Detection Method | Threshold | Status |
|----------|------------------|-----------|--------|
| XsenseGtr9 | Message Type (115) | N/A | ✅ Implemented |
| XsenseProtocol | Message Type (108, 115) | N/A | ✅ Implemented |
| Howen | Time Gap | 10 min | ✅ Implemented |
| Ruptela | Time Gap | 10 min | ✅ Implemented |
| Meitrack | Time Gap | 10 min | ✅ Implemented |
| Fifotrack | Time Gap | 10 min | ✅ Implemented |
| Others | Manual Implementation | Configurable | 🔧 Available via helper method |

## Configuration

### Adjusting Time Threshold

The default 10-minute threshold is suitable for most use cases. However, you can adjust it based on your needs:

**Conservative (5 minutes):**
```java
detectOfflineBatch(position, 5);  // Stricter detection
```

**Relaxed (30 minutes):**
```java
detectOfflineBatch(position, 30);  // More tolerant of delays
```

**Considerations for threshold selection:**

| Threshold | Pros | Cons | Best For |
|-----------|------|------|----------|
| 5 min | Catches even small delays | May flag network delays as offline | High-reliability networks |
| 10 min (default) | Good balance | May miss some edge cases | General use |
| 15-30 min | Tolerant of network issues | May miss moderate delays | Poor network conditions |

## Troubleshooting

### Common Issues

#### 1. False Positives (Online data marked as offline)

**Symptoms:** Real-time positions marked as `offlineBatch: true`

**Causes:**
- Device clock is incorrect
- Server clock is incorrect
- Large network delays

**Solutions:**
- Verify device time synchronization
- Check server NTP configuration
- Increase threshold: `detectOfflineBatch(position, 15)`

#### 2. False Negatives (Offline data marked as online)

**Symptoms:** Batch uploads marked as `offlineBatch: false`

**Causes:**
- Threshold too high
- Device buffering for less than threshold time

**Solutions:**
- Decrease threshold: `detectOfflineBatch(position, 5)`
- Use protocol-specific message types if available

#### 3. Missing Detection

**Symptoms:** No `offlineBatch` attribute

**Causes:**
- Protocol decoder doesn't call `detectOfflineBatch()`
- Position time is null

**Solutions:**
```java
// Ensure position has valid time before detection
if (position.getDeviceTime() != null || position.getFixTime() != null) {
    detectOfflineBatch(position);
}
```

## Best Practices

### 1. Always Set Position Time

```java
// ✅ Good
position.setDeviceTime(parsedTime);
detectOfflineBatch(position);

// ❌ Bad - detection won't work
detectOfflineBatch(position);  // No time set yet!
```

### 2. Choose Appropriate Threshold

```java
// For cellular networks with good coverage
detectOfflineBatch(position, 5);

// For satellite or remote areas
detectOfflineBatch(position, 30);
```

### 3. Log Detection Results (Optional)

```java
detectOfflineBatch(position);
if (position.getBoolean("offlineBatch")) {
    LOGGER.debug("Offline batch detected: device {}, time gap {} minutes",
        deviceId, calculateTimeGap(position));
}
```

## API Integration

### REST API

Access offline batch information through Traccar API:

```bash
# Get positions with offline batch flag
curl -H "Accept: application/json" \
     "http://localhost:8082/api/positions?deviceId=1&from=2025-10-24T00:00:00Z&to=2025-10-24T23:59:59Z"
```

**Response:**
```json
[
  {
    "id": 12345,
    "deviceId": 1,
    "deviceTime": "2025-10-24T10:00:00.000+00:00",
    "serverTime": "2025-10-24T10:01:00.000+00:00",
    "attributes": {
      "offlineBatch": false,
      "batchType": "online"
    }
  }
]
```

### WebSocket

Offline batch information is included in real-time position updates via WebSocket.

## Performance Considerations

### Overhead

The `detectOfflineBatch()` method has minimal performance impact:

- **CPU:** Single timestamp comparison (~microseconds)
- **Memory:** Two additional attributes per position (~50 bytes)
- **Database:** Stored in attributes JSON field (no schema changes)

### Optimization

For high-volume systems:

```java
// Only detect for batch protocols
if (isBatchProtocol()) {
    detectOfflineBatch(position);
}
```

## Future Enhancements

### Planned Features

1. **Configurable thresholds** via device attributes
2. **Statistics dashboard** for offline batch analysis
3. **Automatic notifications** for devices with high offline rates
4. **Historical analysis** tools

### Contribution

To add offline batch detection to a new protocol:

1. Implement in your protocol decoder
2. Add unit tests
3. Update this documentation
4. Submit pull request

## References

### Source Files

- `src/main/java/org/traccar/BaseProtocolDecoder.java` - Core implementation
- `src/main/java/org/traccar/protocol/XsenseGtr9ProtocolDecoder.java` - Message type example
- `src/main/java/org/traccar/protocol/HowenProtocolDecoder.java` - Time gap example

### Related Documentation

- [Traccar Protocol Development Guide](https://www.traccar.org/documentation/)
- [Position Attributes Reference](https://www.traccar.org/documentation/attributes/)

## Support

For issues or questions:

- GitHub Issues: https://github.com/traccar/traccar/issues
- Forum: https://www.traccar.org/forums/

---

**Last Updated:** October 24, 2025  
**Version:** 1.0  
**Authors:** Traccar Development Team
