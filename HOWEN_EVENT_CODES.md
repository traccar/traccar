# Howen Protocol Event Codes

This document lists all supported Howen device event codes implemented in `HowenProtocolDecoder.java`.

## Event Code Reference

Total: **62 unique event codes** (0-61, 768-770)

### Event Code List (Sorted by Code Number)

| Code | Event Name | Category | Traccar Alarm Type | Description |
|------|------------|----------|-------------------|-------------|
| 0 | Normal Status | System Status | - | Normal operation, no alarm |
| 1 | Video Loss | Video & Sensors | videoLoss | Video signal lost on camera channel |
| 2 | Video Occlusion | Video & Sensors | videoOcclusion | Camera lens blocked or covered |
| 3 | Disk Failure | Video & Sensors | ALARM_FAULT | Storage/disk malfunction |
| 4 | Motion Detection | Video & Sensors | motionDetection | Motion detected by camera |
| 5 | Emergency/SOS | Critical Safety | ALARM_SOS | Emergency button pressed |
| 6 | Illegal Ignition | System Status | ALARM_TAMPERING | Unauthorized ignition attempt |
| 7 | Overspeed | Critical Safety | ALARM_OVERSPEED | Vehicle speed exceeds limit |
| 8 | Illegal Displacement | System Status | ALARM_TAMPERING | Vehicle moved without authorization |
| 9 | Entry Alarm | System Status | ALARM_DOOR | Unauthorized entry detected |
| 10 | Low Battery | System Status | ALARM_LOW_BATTERY | Battery level below threshold |
| 11 | Power Cut | System Status | ALARM_POWER_CUT | Main power disconnected |
| 12 | G-Sensor Alarm | Video & Sensors | Various (sub-types) | Acceleration/collision detection |
| 13 | Geofence Alarm | Geofence | ALARM_GEOFENCE_ENTER/EXIT | Geofence boundary events |
| 14 | Geofence Related | Geofence | ALARM_GEOFENCE | General geofence event |
| 15 | Vibration | System Status | ALARM_VIBRATION | Abnormal vibration detected |
| 16 | Falling | System Status | ALARM_FALL_DOWN | Device falling detected |
| 17 | Fatigue Driving | Critical Safety | ALARM_FATIGUE_DRIVING | Driver overtime/fatigue |
| 18 | Low Speed | System Status | ALARM_LOW_SPEED | Speed below minimum threshold |
| 19 | ACC On | Trip Management | - | Accessory power turned on |
| 20 | GPS Antenna Cut | System Status | ALARM_GPS_ANTENNA_CUT | GPS antenna disconnected |
| 21 | GPS Antenna Short | System Status | ALARM_FAULT | GPS antenna short circuit |
| 22 | Driver Card/ID | Critical Safety | - | Driver identification card swipe |
| 23 | Device Power On | System Status | ALARM_POWER_ON | Device powered on |
| 24 | Harsh Acceleration | Critical Safety | ALARM_ACCELERATION | Sudden acceleration detected |
| 25 | Harsh Braking | Critical Safety | ALARM_BRAKING | Sudden braking detected |
| 26 | Illegal Door Open | System Status | ALARM_DOOR | Door opened without authorization |
| 27 | Door Open | System Status | ALARM_DOOR | Door opened |
| 28 | Door Close | System Status | doorClose | Door closed |
| 29 | Input Alarm | System Status | input | Digital input triggered |
| 30 | ADAS/DMS/BSD | ADAS/DMS/BSD | Various (39 sub-types) | Advanced driver assistance alarms |
| 31 | ACC Off | Trip Management | - | Accessory power turned off |
| 32 | Cornering | System Status | ALARM_CORNERING | Sharp turn detected |
| 33 | Fuel Leak | System Status | fuelLeak | Fuel leak detected |
| 34 | Fuel Theft | System Status | fuelTheft | Fuel theft detected |
| 35 | Engine Start | System Status | - | Engine started |
| 36 | Engine Stop | System Status | - | Engine stopped |
| 37 | Towing | Critical Safety | ALARM_TOW | Vehicle being towed |
| 38 | External Power | System Status | externalPower | External power status change |
| 39 | RPM Exceeds | System Status | ALARM_HIGH_RPM | Engine RPM over limit |
| 40 | Trip Start | Trip Management | - | Trip started |
| 41 | Trip In Progress | Trip Management | - | Trip ongoing |
| 42 | Trip End | Trip Management | - | Trip ended |
| 43 | Vehicle Movement | Trip Management | ALARM_MOVEMENT | Vehicle moving alarm |
| 44 | Mileage Alarm | System Status | mileage | Mileage threshold reached |
| 45 | Temperature Alarm | Video & Sensors | temperature | Temperature out of range |
| 46 | Vehicle Stationary | Trip Management | - | Vehicle stationary for duration |
| 47 | Jamming | System Status | jamming | Signal jamming detected |
| 48 | Excessive Overspeed | Critical Safety | ALARM_OVERSPEED | Severe overspeed violation |
| 49 | Light Sensor | System Status | lightSensor | Light sensor triggered |
| 50 | Parking Overtime | System Status | parkingOvertime | Parked too long |
| 51 | Bluetooth Disconnect | System Status | bluetoothDisconnect | Bluetooth connection lost |
| 52 | CANbus Failure | System Status | ALARM_FAULT | CANbus communication error |
| 53 | SIM Not Inserted | System Status | simNotInserted | SIM card missing |
| 54 | Network Failure | System Status | networkFailure | Network connection error |
| 55 | GPS Failure | System Status | gpsFailure | GPS positioning failure |
| 56 | Device Malfunction | System Status | ALARM_FAULT | Device hardware fault |
| 57 | Camera Failure | System Status | ALARM_FAULT | Camera malfunction |
| 58 | Abnormal Driving Time | System Status | abnormalDrivingTime | Driving time violation |
| 59 | Harsh Turn | System Status | ALARM_CORNERING | Sharp turn detected |
| 60 | U-Turn | System Status | uTurn | U-turn detected |
| 61 | Alcohol Detection | Critical Safety | ALARM_GENERAL | Alcohol detected |
| 768 | Trip Notification | Trip Management | - | Trip information update |
| 769 | Tire Pressure | System Status | tirePressure | Tire pressure abnormal |
| 770 | Disk Detection | System Status | diskDetection | Disk status change |

---

## Event Categories

### 1. Critical Safety (9 codes)
High-priority safety alarms requiring immediate attention.

- **5** - Emergency/SOS
- **7** - Overspeed
- **17** - Fatigue Driving
- **22** - Driver Card/ID
- **24** - Harsh Acceleration
- **25** - Harsh Braking
- **37** - Towing
- **48** - Excessive Overspeed
- **61** - Alcohol Detection

### 2. Trip Management (7 codes)
Vehicle operation and trip tracking.

- **19** - ACC On
- **31** - ACC Off
- **40** - Trip Start
- **41** - Trip In Progress
- **42** - Trip End
- **43** - Vehicle Movement
- **46** - Vehicle Stationary

### 3. Geofence (2 codes + 11 sub-types)
Geographical boundary monitoring.

- **13** - Geofence Alarm (with 11 sub-types)
  - Sub-type 0: Enter geofence
  - Sub-type 1: Exit geofence
  - Sub-type 2: Enter line geofence
  - Sub-type 3: Exit line geofence
  - Sub-type 4: Enter polygon geofence
  - Sub-type 5: Exit polygon geofence
  - Sub-type 6: Enter circular geofence
  - Sub-type 7: Exit circular geofence
  - Sub-type 8: Overspeed in geofence
  - Sub-type 9: Dwell in geofence
  - Sub-type 10: Route deviation
- **14** - Geofence Related Event

### 4. Video & Sensors (6 codes + 17 sub-types)
Camera and sensor monitoring.

- **1** - Video Loss
- **2** - Video Occlusion
- **3** - Disk Failure
- **4** - Motion Detection
- **12** - G-Sensor Alarm (with 6 sub-types)
  - Sub-type 1: Collision
  - Sub-type 2: Roll over
  - Sub-type 3: Harsh acceleration
  - Sub-type 4: Harsh braking
  - Sub-type 5: Harsh cornering left
  - Sub-type 6: Harsh cornering right
- **45** - Temperature Alarm (with 2 sub-types)
  - Sub-type 1: High temperature
  - Sub-type 2: Low temperature

### 5. ADAS/DMS/BSD (1 code + 39 sub-types)
Advanced Driver Assistance Systems.

- **30** - ADAS/DMS/BSD Alarms
  - **ADAS (Sub-types 1-15)**: Forward collision, lane departure, pedestrian collision, headway warning, road sign recognition, lane change, vehicle distance, pedestrian detection, ADAS failure, obstacle warning, blind spot warning, rear collision, emergency braking, traffic sign violation, lane keeping assist
  - **DMS (Sub-types 16-30)**: Driver fatigue, distraction, phone call, smoking, driver absent, yawning, eyes closed, head down, abnormal driving, DMS failure, face not detected, camera blocked, infrared failure, seatbelt not fastened, driver change
  - **BSD (Sub-types 31-39)**: Left/right blind spot warning, lane change warnings, rear crossing warnings, BSD failure, door open warning, parking assist

### 6. System Status (37 codes)
Device and vehicle system monitoring.

- **0** - Normal Status
- **6** - Illegal Ignition
- **8** - Illegal Displacement
- **9** - Entry Alarm
- **10** - Low Battery
- **11** - Power Cut
- **15** - Vibration
- **16** - Falling
- **18** - Low Speed
- **20** - GPS Antenna Cut
- **21** - GPS Antenna Short
- **23** - Device Power On
- **26** - Illegal Door Open
- **27** - Door Open
- **28** - Door Close
- **29** - Input Alarm
- **32** - Cornering
- **33** - Fuel Leak
- **34** - Fuel Theft
- **35** - Engine Start
- **36** - Engine Stop
- **38** - External Power
- **39** - RPM Exceeds
- **44** - Mileage Alarm
- **47** - Jamming
- **49** - Light Sensor
- **50** - Parking Overtime
- **51** - Bluetooth Disconnect
- **52** - CANbus Failure
- **53** - SIM Not Inserted
- **54** - Network Failure
- **55** - GPS Failure
- **56** - Device Malfunction
- **57** - Camera Failure
- **58** - Abnormal Driving Time
- **59** - Harsh Turn
- **60** - U-Turn
- **768** - Trip Notification
- **769** - Tire Pressure
- **770** - Disk Detection

---

## JSON Data Structure

Event alarms are received in JSON format:

```json
{
  "ec": "7",           // Event code
  "dtu": "2024-10-20 10:30:45",  // Device time
  "det": {             // Detail object (varies by event type)
    "spd": 120.5,      // Current speed (for overspeed)
    "lmt": 100.0       // Speed limit
  }
}
```

### Common Detail Fields

| Field | Type | Description | Used In Events |
|-------|------|-------------|----------------|
| `spd` | Number | Current speed (km/h) | 7, 13, 18, 48 |
| `lmt` | Number | Speed/RPM limit | 7, 39, 48 |
| `dur` | Integer | Duration (seconds) | 17, 39, 42, 46, 50 |
| `odo` | Number | Odometer (km) | 40, 42, 44 |
| `rpm` | Integer | Engine RPM | 39 |
| `ch` | Integer | Channel number | 1, 2, 4, 29, 45, 57 |
| `tp` | Integer | Sub-type | 12, 13, 22, 30, 45, 769 |
| `gid` | String | Geofence ID | 13, 14 |
| `gnm` | String | Geofence name | 13, 14 |
| `tid` | String | Trip ID | 40, 41, 42, 768 |
| `dst` | Number | Distance (km) | 42 |
| `val` | Number | Value (generic) | 10, 45, 61, 769 |
| `cn` | String | Card number | 22 |
| `st` | Integer | Status | 38, 770 |
| `dt` | Integer | Driving time | 17 |
| `lvl` | Integer | Alarm level | 30 |

---

## Implementation Details

### File Location
```
src/main/java/org/traccar/protocol/HowenProtocolDecoder.java
```

### Key Methods
- `decodeEventAlarm()` - Main event code decoder with switch statement
- `decodeAlarm()` - Processes alarm messages from device
- `decodeStatusData()` - Extracts position and status data

### Standard Traccar Alarm Types Used
- `ALARM_SOS` - Emergency/panic button
- `ALARM_OVERSPEED` - Speed limit exceeded
- `ALARM_FATIGUE_DRIVING` - Driver fatigue detected
- `ALARM_ACCELERATION` - Harsh acceleration
- `ALARM_BRAKING` - Harsh braking
- `ALARM_TOW` - Vehicle towing
- `ALARM_GEOFENCE_ENTER` - Entered geofence
- `ALARM_GEOFENCE_EXIT` - Exited geofence
- `ALARM_GEOFENCE` - General geofence event
- `ALARM_ACCIDENT` - Collision/accident
- `ALARM_CORNERING` - Sharp cornering
- `ALARM_MOVEMENT` - Vehicle movement
- `ALARM_DOOR` - Door event
- `ALARM_LOW_BATTERY` - Low battery
- `ALARM_POWER_CUT` - Power disconnected
- `ALARM_VIBRATION` - Vibration detected
- `ALARM_FALL_DOWN` - Falling detected
- `ALARM_LOW_SPEED` - Speed too low
- `ALARM_GPS_ANTENNA_CUT` - GPS antenna disconnected
- `ALARM_POWER_ON` - Device powered on
- `ALARM_TAMPERING` - Tampering detected
- `ALARM_FAULT` - System fault
- `ALARM_HIGH_RPM` - Engine RPM too high
- `ALARM_GENERAL` - Generic alarm

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2024-10-20 | Initial implementation with all 62 event codes |
| 1.0.1 | 2024-10-20 | Fixed Event Code 39 from "Idling" to "RPM Exceeds" |

---

## References

- Source: Howen Device Event Type Code.xlsx
- Protocol: Howen Binary Protocol (0x48)
- Message Types: 0x1051 (Alarm), 0x1041 (Status)

---

*Last Updated: October 20, 2025*
