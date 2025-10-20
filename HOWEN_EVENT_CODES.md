# Howen Protocol Event Codes

This document lists all supported Howen device event codes implemented in `HowenProtocolDecoder.java`.

## Event Code Referen- **10** - Humidity Alarm
- **11** - Parking Overtime
- **14** - Electronic Route
- **15** - Door Abnormal
- **16** - Falling
- **18** - Low Speed Alarmotal: **62 unique event codes** (0-61, 768-770)

### Event Code List (Sorted by Code Number)

| Code | Event Name | Category | Traccar Alarm Type | Description |
|------|------------|----------|-------------------|-------------|
| 0 | Normal Status | System Status | - | Normal operation, no alarm |
| 1 | Video Loss | Video & Sensors | videoLoss | Video signal lost on camera channel |
| 2 | Motion Detection | Video & Sensors | motionDetection | Motion detected by camera |
| 3 | Video Cover | Video & Sensors | videoCover | Camera lens covered or blocked |
| 4 | Input Trigger | Video & Sensors | input | Digital input triggered (ch0-ch8) |
| 5 | Emergency/SOS | Critical Safety | ALARM_SOS | Emergency button pressed |
| 6 | Low Speed Alarm | System Status | ALARM_LOW_SPEED | Speed below minimum threshold |
| 7 | Overspeed | Critical Safety | ALARM_OVERSPEED | Vehicle speed exceeds limit |
| 8 | Low Temperature | System Status | temperatureLow | Temperature below minimum threshold |
| 9 | High Temperature | System Status | temperatureHigh | Temperature above maximum threshold |
| 10 | Humidity Alarm | System Status | humidity | Humidity out of range |
| 11 | Parking Overtime | System Status | parkingOvertime | Parked too long |
| 12 | G-Sensor Alarm | Video & Sensors | Various (sub-types) | Acceleration/collision detection |
| 13 | Geofence Alarm | Geofence | ALARM_GEOFENCE_ENTER/EXIT | Geofence boundary events |
| 14 | Electronic Route | System Status | electronicRoute | Route planning/navigation event |
| 15 | Door Abnormal | System Status | ALARM_DOOR | Door abnormal open/close |
| 16 | Storage Abnormal | System Status | ALARM_FAULT | Disk/storage issues (15 sub-types) |
| 17 | Fatigue Driving | Critical Safety | ALARM_FATIGUE_DRIVING | Driver overtime/fatigue |
| 18 | Fuel Consumption Abnormal | System Status | fuelAbnormal | Fuel refuel/theft detection (2 sub-types) |
| 19 | ACC Off | Trip Management | - | Accessory power turned off |
| 20 | GPS Module Abnormal | System Status | ALARM_FAULT | GPS module malfunction |
| 21 | Front Panel Open | System Status | ALARM_TAMPERING | Front panel opened/tampered |
| 22 | Driver Card/ID | Critical Safety | - | Driver identification card swipe |
| 23 | IBUTTON | Critical Safety | - | iButton driver identification |
| 24 | Harsh Acceleration | Critical Safety | ALARM_ACCELERATION | Sudden acceleration detected |
| 25 | Harsh Braking | Critical Safety | ALARM_BRAKING | Sudden braking detected |
| 26 | Low Speed Warning | System Status | ALARM_LOW_SPEED | Speed below warning threshold |
| 27 | High Speed Warning | Critical Safety | ALARM_OVERSPEED | Speed above warning threshold |
| 28 | Voltage Alarm | System Status | Various | Power/voltage issues (7 sub-types) |
| 29 | People Counting | Video & Sensors | peopleCounting | Passenger counting system |
| 30 | ADAS/DMS/BSD | ADAS/DMS/BSD | Various (39 sub-types) | Advanced driver assistance alarms |
| 31 | ACC On | Trip Management | - | Accessory power on (reported once at boot) |
| 32 | Idle | System Status | idle | Vehicle idle alarm |
| 33 | GPS Antenna Break | System Status | ALARM_GPS_ANTENNA_CUT | GPS antenna disconnected |
| 34 | GPS Antenna Short | System Status | ALARM_FAULT | GPS antenna short circuit |
| 35 | IO Output | System Status | ioOutput | Digital output triggered (ch1-ch2) |
| 36 | CANBUS Connection Abnormal | System Status | ALARM_FAULT | CAN bus connection error |
| 37 | Towing | Critical Safety | ALARM_TOW | Vehicle being towed |
| 38 | Free Wheeling | System Status | freeWheeling | Coasting/neutral gear detected |
| 39 | RPM Exceeds | System Status | ALARM_HIGH_RPM | Engine RPM over limit |
| 40 | Vehicle Move | Trip Management | ALARM_MOVEMENT | Vehicle movement detected |
| 41 | Trip Start | Trip Management | - | Trip started (st/et/dtu time same) |
| 42 | In Trip | Trip Management | - | Trip in progress |
| 43 | Trip Ends | Trip Management | - | Trip ended (periodic report after ACC off) |
| 44 | GPS Location Recover | System Status | gpsLocationRecover | GPS signal recovered |
| 45 | Video Abnormal | Video & Sensors | ALARM_FAULT | Video system malfunction |
| 46 | None Trip Position | Trip Management | - | Periodic report after trip ends |
| 47 | Main Unit Anomaly | System Status | ALARM_FAULT | Device not connected for long time |
| 48 | Excessive Overspeed | Critical Safety | ALARM_OVERSPEED | Severe overspeed violation |
| 49 | Load Alarm | System Status | loadAlarm | Load sensor alarm |
| 50 | SIM Card Lost | System Status | simCardLost | SIM card missing |
| 51 | Tracker Seat Belt Alarm | System Status | seatBeltAlarm | Seat belt not fastened |
| 52 | Tracker Harsh Acceleration | System Status | ALARM_ACCELERATION | Tracker harsh acceleration |
| 53 | Tracker Harsh Braking | System Status | ALARM_BRAKING | Tracker harsh braking |
| 54 | Tracker Overspeed | System Status | ALARM_OVERSPEED | Tracker overspeed alarm |
| 55 | Tracker Excessive Overspeed | System Status | ALARM_OVERSPEED | Tracker excessive overspeed |
| 56 | Tracker Panel Open | System Status | ALARM_TAMPERING | Tracker panel opened |
| 57 | Roaming Mode Start | System Status | roamingModeStart | Roaming mode activated |
| 58 | Roaming Mode End | System Status | roamingModeEnd | Roaming mode deactivated |
| 59 | Wake Up Event | System Status | wakeUpEvent | Device wake up |
| 60 | Satellite Modem Status | System Status | satelliteModemStatus | Satellite modem status change |
| 61 | Alcohol Detection Alarm | Critical Safety | ALARM_GENERAL | Alcohol detected |
| 768 | Trip Notification | Trip Management | - | Trip information update |
| 769 | Tire Pressure | System Status | tirePressure | Tire pressure abnormal |
| 770 | Disk Detection | System Status | diskDetection | Disk status change |

---

## Event Categories

### 1. Critical Safety (10 codes)
High-priority safety alarms requiring immediate attention.

- **5** - Emergency/SOS
- **7** - Overspeed
- **17** - Fatigue Driving
- **22** - Driver Card/ID
- **23** - IBUTTON
- **24** - Harsh Acceleration
- **25** - Harsh Braking
- **27** - High Speed Warning
- **37** - Towing
- **48** - Excessive Overspeed
- **61** - Alcohol Detection

### 2. Trip Management (7 codes)
Vehicle operation and trip tracking.

- **19** - ACC Off
- **31** - ACC On
- **40** - Vehicle Move
- **41** - Trip Start
- **42** - In Trip
- **43** - Trip Ends
- **46** - None Trip Position

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

### 4. Video & Sensors (7 codes + 24 sub-types)
Camera and sensor monitoring.

- **1** - Video Loss
- **2** - Motion Detection
- **3** - Video Cover
- **4** - Input Trigger (ch0-ch8)
- **29** - People Counting
- **45** - Video Abnormal
- **12** - G-Sensor Alarm (with 6 sub-types)
  - Sub-type 1: Collision
  - Sub-type 2: Roll over
  - Sub-type 3: Harsh acceleration
  - Sub-type 4: Harsh braking
  - Sub-type 5: Harsh cornering left
  - Sub-type 6: Harsh cornering right
- **16** - Storage Abnormal (with 15 sub-types)
  - Sub-type 0: Missing
  - Sub-type 1: Broken (disk partition fatal error)
  - Sub-type 2: Log cannot be overwritten
  - Sub-type 3: Failed to write Block (EIO write error)
  - Sub-type 4: Disk failure (cannot be partitioned)
  - Sub-type 5: Disk cannot be mounted
  - Sub-type 6: Too many bad blocks (>20%)
  - Sub-type 7: Disk invalid block
  - Sub-type 8: Video sampling verification failed
  - Sub-type 9: Disk pauses to write video
  - Sub-type 10: Recording overwrite exception
  - Sub-type 11: No recording for over 2 minutes
  - Sub-type 12: Slow write, cached data overwritten
  - Sub-type 13: Video partition abnormality
  - Sub-type 14: Disk temperature alarm (high 70°C / low 0°C)
- **18** - Fuel Consumption Abnormal (with 2 sub-types)
  - Sub-type 1: Refuel
  - Sub-type 2: Fuel theft
- **28** - Voltage Alarm (with 7 sub-types)
  - Sub-type 1: Low voltage
  - Sub-type 2: High voltage
  - Sub-type 3: Power off
  - Sub-type 4: Power on
  - Sub-type 5: Suspicious disconnection
  - Sub-type 6: Abnormal shutdown
  - Sub-type 7: Start up

### 5. ADAS/DMS/BSD (1 code + 39 sub-types)
Advanced Driver Assistance Systems.

- **30** - ADAS/DMS/BSD Alarms
  - **ADAS (Sub-types 1-15)**: Forward collision, lane departure, pedestrian collision, headway warning, road sign recognition, lane change, vehicle distance, pedestrian detection, ADAS failure, obstacle warning, blind spot warning, rear collision, emergency braking, traffic sign violation, lane keeping assist
  - **DMS (Sub-types 16-30)**: Driver fatigue, distraction, phone call, smoking, driver absent, yawning, eyes closed, head down, abnormal driving, DMS failure, face not detected, camera blocked, infrared failure, seatbelt not fastened, driver change
  - **BSD (Sub-types 31-39)**: Left/right blind spot warning, lane change warnings, rear crossing warnings, BSD failure, door open warning, parking assist

### 6. System Status (47 codes)
Device and vehicle system monitoring.

- **0** - Normal Status
- **6** - Low Speed Alarm
- **8** - Low Temperature
- **9** - High Temperature
- **10** - Humidity Alarm
- **11** - Parking Overtime
- **14** - Electronic Route
- **15** - Door Abnormal
- **20** - GPS Module Abnormal
- **21** - Front Panel Open
- **26** - Low Speed Warning
- **32** - Idle
- **33** - GPS Antenna Break
- **34** - GPS Antenna Short
- **35** - IO Output
- **36** - CANBUS Connection Abnormal
- **38** - Free Wheeling
- **39** - RPM Exceeds
- **44** - GPS Location Recover
- **47** - Main Unit Anomaly
- **49** - Load Alarm
- **50** - SIM Card Lost
- **51** - Tracker Seat Belt Alarm
- **52** - Tracker Harsh Acceleration
- **53** - Tracker Harsh Braking
- **54** - Tracker Overspeed
- **55** - Tracker Excessive Overspeed
- **56** - Tracker Panel Open
- **57** - Roaming Mode Start
- **58** - Roaming Mode End
- **59** - Wake Up Event
- **60** - Satellite Modem Status
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
| `spd` | Number | Current speed (km/h) | 6, 7, 13, 18, 48 |
| `lmt` | Number | Speed/RPM/Temperature/Humidity limit | 6, 7, 8, 9, 10, 39, 48 |
| `dur` | Integer | Duration (seconds) | 11, 17, 32, 38, 42, 46, 50 |
| `odo` | Number | Odometer (km) | 41, 43 |
| `rpm` | Integer | Engine RPM | 39 |
| `ch` | Integer | Channel number | 1, 2, 3, 4, 8, 9, 15, 16, 29, 35, 45 |
| `tp` | Integer | Sub-type | 12, 13, 14, 16, 22, 30, 769 |
| `gid` | String | Geofence ID | 13 |
| `gnm` | String | Geofence name | 13 |
| `rid` | String | Route ID | 14 |
| `rnm` | String | Route name | 14 |
| `tid` | String | Trip ID | 40, 41, 42, 768 |
| `dst` | Number | Distance (km) | 42 |
| `val` | Number | Value (generic) | 8, 9, 10, 18, 45, 61, 769 |
| `cn` | String | Card number | 22 |
| `st` | Integer | Status | 15, 16, 35, 60, 770 |
| `dt` | Integer | Detail type / Driving time / Sub-type | 17, 18, 28 |
| `lvl` | Integer | Alarm level | 30 |
| `cnt` | Integer | Count (people count) | 29 |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-10-20 | Initial implementation with all 62 event codes |
| 1.0.1 | 2025-10-20 | Fixed Event Code 39 from "Idling" to "RPM Exceeds" |
| 1.0.2 | 2025-10-20 | Fixed Event Code 2 to "Motion Detection", Code 3 to "Video Cover", and Code 4 to "Input Trigger" |
| 1.0.3 | 2025-10-20 | Fixed Event Codes 6-11: Low Speed, Overspeed, Low/High Temperature, Humidity, Parking Overtime |
| 1.0.4 | 2025-10-20 | Fixed Event Code 14 to "Electronic Route" and Code 15 to "Door Abnormal" |
| 1.0.5 | 2025-10-20 | Fixed Event Code 16 to "Storage Abnormal" with 15 sub-types (st=0 to st=14) |
| 1.0.6 | 2025-10-20 | Fixed Event Code 18 to "Fuel Consumption Abnormal" with 2 sub-types (dt=1: Refuel, dt=2: Fuel theft) |
| 1.0.7 | 2025-10-20 | Fixed Event Codes 19 (ACC Off), 20 (GPS Module Abnormal), 21 (Front Panel Open); swapped 19 and 31 |
| 1.0.8 | 2025-10-20 | Fixed Event Codes 23 (IBUTTON), 26 (Low Speed Warning), 27 (High Speed Warning) |
| 1.0.9 | 2025-10-20 | Fixed Event Code 28 to "Voltage Alarm" with 7 sub-types (dt=1-7: voltage/power events) |
| 1.1.0 | 2025-10-20 | Fixed Event Code 29 to "People Counting" (passenger counting system) |
| 1.1.1 | 2025-10-20 | Fixed Event Codes 31 (ACC On at boot), 32 (Idle), 33 (GPS Antenna Break), 34 (GPS Antenna Short) |
| 1.1.2 | 2025-10-20 | Fixed Event Code 35 to "IO Output" (digital output with channel support ch1-ch2) |
| 2.0.0 | 2025-10-20 | Major update: Fixed Event Codes 36-61 per Excel specification. Changed trip codes (40-43, 46), system codes (36, 38, 44, 45, 47, 49-60) |

---

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
| 1.0.0 | 2025-10-20 | Initial implementation with all 62 event codes |
| 1.0.1 | 2025-10-20 | Fixed Event Code 39 from "Idling" to "RPM Exceeds" |
| 1.0.2 | 2025-10-20 | Fixed Event Code 2 to "Motion Detection", Code 3 to "Video Cover", and Code 4 to "Input Trigger" |
| 1.0.3 | 2025-10-20 | Fixed Event Codes 6-11: Low Speed, Overspeed, Low/High Temperature, Humidity, Parking Overtime |
| 1.0.4 | 2025-10-20 | Fixed Event Code 14 to "Electronic Route" and Code 15 to "Door Abnormal" |
| 1.0.5 | 2025-10-20 | Fixed Event Code 16 to "Storage Abnormal" with 15 sub-types (st=0 to st=14) |
| 1.0.6 | 2025-10-20 | Fixed Event Code 18 to "Fuel Consumption Abnormal" with 2 sub-types (dt=1: Refuel, dt=2: Fuel theft) |
| 1.0.7 | 2025-10-20 | Fixed Event Codes 19 (ACC Off), 20 (GPS Module Abnormal), 21 (Front Panel Open); swapped 19 and 31 |
| 1.0.8 | 2025-10-20 | Fixed Event Codes 23 (IBUTTON), 26 (Low Speed Warning), 27 (High Speed Warning) |
| 1.0.9 | 2025-10-20 | Fixed Event Code 28 to "Voltage Alarm" with 7 sub-types (dt=1-7: voltage/power events) |
| 1.1.0 | 2025-10-20 | Fixed Event Code 29 to "People Counting" (passenger counting system) |
| 1.1.1 | 2025-10-20 | Fixed Event Codes 31 (ACC On at boot), 32 (Idle), 33 (GPS Antenna Break), 34 (GPS Antenna Short) |
| 1.1.2 | 2025-10-20 | Fixed Event Code 35 to "IO Output" (digital output with channel support ch1-ch2) |

---

## References

- Source: Howen Device Event Type Code.xlsx
- Protocol: Howen Binary Protocol (0x48)
- Message Types: 0x1051 (Alarm), 0x1041 (Status)

---

*Last Updated: October 20, 2025*
