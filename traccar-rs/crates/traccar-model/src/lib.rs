use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;

// ─── Attribute helpers ───────────────────────────────────────────────

pub fn attr_string(attrs: &Value, key: &str) -> Option<String> {
    attrs.get(key).and_then(|v| match v {
        Value::String(s) => Some(s.clone()),
        other => Some(other.to_string()),
    })
}

pub fn attr_f64(attrs: &Value, key: &str) -> Option<f64> {
    attrs.get(key).and_then(|v| match v {
        Value::Number(n) => n.as_f64(),
        Value::String(s) => s.parse().ok(),
        _ => None,
    })
}

pub fn attr_i64(attrs: &Value, key: &str) -> Option<i64> {
    attrs.get(key).and_then(|v| match v {
        Value::Number(n) => n.as_i64(),
        Value::String(s) => s.parse().ok(),
        _ => None,
    })
}

pub fn attr_bool(attrs: &Value, key: &str) -> Option<bool> {
    attrs.get(key).and_then(|v| match v {
        Value::Bool(b) => Some(*b),
        Value::String(s) => s.parse().ok(),
        _ => None,
    })
}

// ─── Position key constants ──────────────────────────────────────────

pub mod position_keys {
    pub const KEY_ORIGINAL: &str = "raw";
    pub const KEY_INDEX: &str = "index";
    pub const KEY_HDOP: &str = "hdop";
    pub const KEY_VDOP: &str = "vdop";
    pub const KEY_PDOP: &str = "pdop";
    pub const KEY_SATELLITES: &str = "sat";
    pub const KEY_SATELLITES_VISIBLE: &str = "satVisible";
    pub const KEY_RSSI: &str = "rssi";
    pub const KEY_GPS: &str = "gps";
    pub const KEY_ROAMING: &str = "roaming";
    pub const KEY_EVENT: &str = "event";
    pub const KEY_ALARM: &str = "alarm";
    pub const KEY_STATUS: &str = "status";
    pub const KEY_ODOMETER: &str = "odometer";
    pub const KEY_ODOMETER_SERVICE: &str = "serviceOdometer";
    pub const KEY_ODOMETER_TRIP: &str = "tripOdometer";
    pub const KEY_HOURS: &str = "hours";
    pub const KEY_STEPS: &str = "steps";
    pub const KEY_HEART_RATE: &str = "heartRate";
    pub const KEY_INPUT: &str = "input";
    pub const KEY_OUTPUT: &str = "output";
    pub const KEY_IMAGE: &str = "image";
    pub const KEY_VIDEO: &str = "video";
    pub const KEY_AUDIO: &str = "audio";
    pub const KEY_POWER: &str = "power";
    pub const KEY_BATTERY: &str = "battery";
    pub const KEY_BATTERY_LEVEL: &str = "batteryLevel";
    pub const KEY_FUEL: &str = "fuel";
    pub const KEY_FUEL_USED: &str = "fuelUsed";
    pub const KEY_FUEL_CONSUMPTION: &str = "fuelConsumption";
    pub const KEY_FUEL_LEVEL: &str = "fuelLevel";
    pub const KEY_VERSION_FW: &str = "versionFw";
    pub const KEY_VERSION_HW: &str = "versionHw";
    pub const KEY_TYPE: &str = "type";
    pub const KEY_IGNITION: &str = "ignition";
    pub const KEY_FLAGS: &str = "flags";
    pub const KEY_ANTENNA: &str = "antenna";
    pub const KEY_CHARGE: &str = "charge";
    pub const KEY_IP: &str = "ip";
    pub const KEY_ARCHIVE: &str = "archive";
    pub const KEY_DISTANCE: &str = "distance";
    pub const KEY_TOTAL_DISTANCE: &str = "totalDistance";
    pub const KEY_RPM: &str = "rpm";
    pub const KEY_VIN: &str = "vin";
    pub const KEY_APPROXIMATE: &str = "approximate";
    pub const KEY_THROTTLE: &str = "throttle";
    pub const KEY_MOTION: &str = "motion";
    pub const KEY_ARMED: &str = "armed";
    pub const KEY_GEOFENCE: &str = "geofence";
    pub const KEY_ACCELERATION: &str = "acceleration";
    pub const KEY_HUMIDITY: &str = "humidity";
    pub const KEY_DEVICE_TEMP: &str = "deviceTemp";
    pub const KEY_COOLANT_TEMP: &str = "coolantTemp";
    pub const KEY_ENGINE_LOAD: &str = "engineLoad";
    pub const KEY_ENGINE_TEMP: &str = "engineTemp";
    pub const KEY_OPERATOR: &str = "operator";
    pub const KEY_COMMAND: &str = "command";
    pub const KEY_BLOCKED: &str = "blocked";
    pub const KEY_LOCK: &str = "lock";
    pub const KEY_DOOR: &str = "door";
    pub const KEY_AXLE_WEIGHT: &str = "axleWeight";
    pub const KEY_G_SENSOR: &str = "gSensor";
    pub const KEY_ICCID: &str = "iccid";
    pub const KEY_PHONE: &str = "phone";
    pub const KEY_SPEED_LIMIT: &str = "speedLimit";
    pub const KEY_DRIVING_TIME: &str = "drivingTime";
    pub const KEY_DTCS: &str = "dtcs";
    pub const KEY_OBD_SPEED: &str = "obdSpeed";
    pub const KEY_OBD_ODOMETER: &str = "obdOdometer";
    pub const KEY_RESULT: &str = "result";
    pub const KEY_DRIVER_UNIQUE_ID: &str = "driverUniqueId";
    pub const KEY_CARD: &str = "card";

    pub const PREFIX_TEMP: &str = "temp";
    pub const PREFIX_ADC: &str = "adc";
    pub const PREFIX_IO: &str = "io";
    pub const PREFIX_COUNT: &str = "count";
    pub const PREFIX_IN: &str = "in";
    pub const PREFIX_OUT: &str = "out";
}

pub mod alarm {
    pub const GENERAL: &str = "general";
    pub const SOS: &str = "sos";
    pub const VIBRATION: &str = "vibration";
    pub const MOVEMENT: &str = "movement";
    pub const LOW_SPEED: &str = "lowspeed";
    pub const OVERSPEED: &str = "overspeed";
    pub const FALL_DOWN: &str = "fallDown";
    pub const LOW_POWER: &str = "lowPower";
    pub const LOW_BATTERY: &str = "lowBattery";
    pub const FAULT: &str = "fault";
    pub const POWER_OFF: &str = "powerOff";
    pub const POWER_ON: &str = "powerOn";
    pub const DOOR: &str = "door";
    pub const LOCK: &str = "lock";
    pub const UNLOCK: &str = "unlock";
    pub const GEOFENCE: &str = "geofence";
    pub const GEOFENCE_ENTER: &str = "geofenceEnter";
    pub const GEOFENCE_EXIT: &str = "geofenceExit";
    pub const GPS_ANTENNA_CUT: &str = "gpsAntennaCut";
    pub const ACCIDENT: &str = "accident";
    pub const TOW: &str = "tow";
    pub const IDLE: &str = "idle";
    pub const HIGH_RPM: &str = "highRpm";
    pub const ACCELERATION: &str = "hardAcceleration";
    pub const BRAKING: &str = "hardBraking";
    pub const CORNERING: &str = "hardCornering";
    pub const LANE_CHANGE: &str = "laneChange";
    pub const FATIGUE_DRIVING: &str = "fatigueDriving";
    pub const POWER_CUT: &str = "powerCut";
    pub const POWER_RESTORED: &str = "powerRestored";
    pub const JAMMING: &str = "jamming";
    pub const TEMPERATURE: &str = "temperature";
    pub const PARKING: &str = "parking";
    pub const BONNET: &str = "bonnet";
    pub const FOOT_BRAKE: &str = "footBrake";
    pub const FUEL_LEAK: &str = "fuelLeak";
    pub const TAMPERING: &str = "tampering";
    pub const REMOVING: &str = "removing";
}

// ─── Position ────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Position {
    #[serde(default)]
    pub id: i64,
    pub protocol: String,
    pub device_id: i64,
    pub server_time: DateTime<Utc>,
    pub device_time: DateTime<Utc>,
    pub fix_time: DateTime<Utc>,
    pub valid: bool,
    pub latitude: f64,
    pub longitude: f64,
    #[serde(default)]
    pub altitude: f64,
    #[serde(default)]
    pub speed: f64,
    #[serde(default)]
    pub course: f64,
    pub address: Option<String>,
    #[serde(default)]
    pub accuracy: f64,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
    pub network: Option<Value>,
    pub geofence_ids: Option<Vec<i64>>,
    #[serde(skip)]
    pub outdated: bool,
}

fn default_attributes() -> Value {
    Value::Object(serde_json::Map::new())
}

impl Position {
    pub fn new(protocol: &str) -> Self {
        let now = Utc::now();
        Self {
            id: 0,
            protocol: protocol.to_string(),
            device_id: 0,
            server_time: now,
            device_time: now,
            fix_time: now,
            valid: false,
            latitude: 0.0,
            longitude: 0.0,
            altitude: 0.0,
            speed: 0.0,
            course: 0.0,
            address: None,
            accuracy: 0.0,
            attributes: default_attributes(),
            network: None,
            geofence_ids: None,
            outdated: false,
        }
    }

    pub fn set_time(&mut self, time: DateTime<Utc>) {
        self.device_time = time;
        self.fix_time = time;
    }

    pub fn set<V: Into<Value>>(&mut self, key: &str, value: V) {
        if let Value::Object(ref mut map) = self.attributes {
            map.insert(key.to_string(), value.into());
        }
    }

    pub fn has_attribute(&self, key: &str) -> bool {
        self.attributes.get(key).is_some()
    }

    pub fn get_string(&self, key: &str) -> Option<String> {
        attr_string(&self.attributes, key)
    }

    pub fn get_double(&self, key: &str) -> Option<f64> {
        attr_f64(&self.attributes, key)
    }

    pub fn get_integer(&self, key: &str) -> Option<i64> {
        attr_i64(&self.attributes, key)
    }

    pub fn get_boolean(&self, key: &str) -> Option<bool> {
        attr_bool(&self.attributes, key)
    }

    pub fn add_alarm(&mut self, alarm: &str) {
        if let Some(existing) = self.get_string(position_keys::KEY_ALARM) {
            self.set(position_keys::KEY_ALARM, format!("{},{}", existing, alarm));
        } else {
            self.set(position_keys::KEY_ALARM, alarm.to_string());
        }
    }
}

// ─── Device ──────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Device {
    #[serde(default)]
    pub id: i64,
    pub name: String,
    pub unique_id: String,
    #[serde(default = "default_status")]
    pub status: String,
    pub last_update: Option<DateTime<Utc>>,
    #[serde(default)]
    pub position_id: i64,
    pub group_id: Option<i64>,
    pub phone: Option<String>,
    pub model: Option<String>,
    pub contact: Option<String>,
    pub category: Option<String>,
    #[serde(default)]
    pub disabled: bool,
    pub expiration_time: Option<DateTime<Utc>>,
    pub calendar_id: Option<i64>,
    #[serde(default = "default_attributes")]
    pub attributes: Value,

    // Motion tracking (internal state, not serialized to API)
    #[serde(skip)]
    pub motion_streak: bool,
    #[serde(skip)]
    pub motion_state: bool,
    #[serde(skip)]
    pub motion_position_id: i64,
    #[serde(skip)]
    pub motion_time: Option<DateTime<Utc>>,
    #[serde(skip)]
    pub motion_distance: f64,
    #[serde(skip)]
    pub motion_latitude: f64,
    #[serde(skip)]
    pub motion_longitude: f64,
    #[serde(skip)]
    pub overspeed_state: bool,
    #[serde(skip)]
    pub overspeed_time: Option<DateTime<Utc>>,
    #[serde(skip)]
    pub overspeed_geofence_id: i64,
}

fn default_status() -> String {
    "offline".to_string()
}

impl Device {
    pub const STATUS_UNKNOWN: &str = "unknown";
    pub const STATUS_ONLINE: &str = "online";
    pub const STATUS_OFFLINE: &str = "offline";
}

impl Default for Device {
    fn default() -> Self {
        Self {
            id: 0,
            name: String::new(),
            unique_id: String::new(),
            status: default_status(),
            last_update: None,
            position_id: 0,
            group_id: None,
            phone: None,
            model: None,
            contact: None,
            category: None,
            disabled: false,
            expiration_time: None,
            calendar_id: None,
            attributes: default_attributes(),
            motion_streak: false,
            motion_state: false,
            motion_position_id: 0,
            motion_time: None,
            motion_distance: 0.0,
            motion_latitude: 0.0,
            motion_longitude: 0.0,
            overspeed_state: false,
            overspeed_time: None,
            overspeed_geofence_id: 0,
        }
    }
}

// ─── User ────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct User {
    #[serde(default)]
    pub id: i64,
    pub name: String,
    pub login: Option<String>,
    pub email: String,
    pub phone: Option<String>,
    #[serde(default)]
    pub readonly: bool,
    #[serde(default)]
    pub administrator: bool,
    pub map: Option<String>,
    #[serde(default)]
    pub latitude: f64,
    #[serde(default)]
    pub longitude: f64,
    #[serde(default)]
    pub zoom: i32,
    pub coordinate_format: Option<String>,
    #[serde(default)]
    pub disabled: bool,
    pub expiration_time: Option<DateTime<Utc>>,
    #[serde(default)]
    pub device_limit: i32,
    #[serde(default)]
    pub user_limit: i32,
    #[serde(default)]
    pub device_readonly: bool,
    #[serde(default)]
    pub limit_commands: bool,
    #[serde(default)]
    pub disable_reports: bool,
    #[serde(default)]
    pub fixed_email: bool,
    pub poi_layer: Option<String>,
    pub totp_key: Option<String>,
    #[serde(default)]
    pub temporary: bool,
    #[serde(skip_serializing)]
    pub hashed_password: Option<String>,
    #[serde(skip_serializing)]
    pub salt: Option<String>,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

impl Default for User {
    fn default() -> Self {
        Self {
            id: 0,
            name: String::new(),
            login: None,
            email: String::new(),
            phone: None,
            readonly: false,
            administrator: false,
            map: None,
            latitude: 0.0,
            longitude: 0.0,
            zoom: 0,
            coordinate_format: None,
            disabled: false,
            expiration_time: None,
            device_limit: 0,
            user_limit: 0,
            device_readonly: false,
            limit_commands: false,
            disable_reports: false,
            fixed_email: false,
            poi_layer: None,
            totp_key: None,
            temporary: false,
            hashed_password: None,
            salt: None,
            attributes: default_attributes(),
        }
    }
}

impl User {
    pub fn is_manager(&self) -> bool {
        self.user_limit != 0
    }
}

// ─── Event ───────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Event {
    #[serde(default)]
    pub id: i64,
    pub device_id: i64,
    #[serde(rename = "type")]
    pub event_type: String,
    pub event_time: DateTime<Utc>,
    #[serde(default)]
    pub position_id: i64,
    #[serde(default)]
    pub geofence_id: i64,
    #[serde(default)]
    pub maintenance_id: i64,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

impl Event {
    pub const ALL_EVENTS: &str = "allEvents";
    pub const TYPE_COMMAND_RESULT: &str = "commandResult";
    pub const TYPE_DEVICE_ONLINE: &str = "deviceOnline";
    pub const TYPE_DEVICE_UNKNOWN: &str = "deviceUnknown";
    pub const TYPE_DEVICE_OFFLINE: &str = "deviceOffline";
    pub const TYPE_DEVICE_INACTIVE: &str = "deviceInactive";
    pub const TYPE_QUEUED_COMMAND_SENT: &str = "queuedCommandSent";
    pub const TYPE_DEVICE_MOVING: &str = "deviceMoving";
    pub const TYPE_DEVICE_STOPPED: &str = "deviceStopped";
    pub const TYPE_DEVICE_OVERSPEED: &str = "deviceOverspeed";
    pub const TYPE_DEVICE_FUEL_DROP: &str = "deviceFuelDrop";
    pub const TYPE_DEVICE_FUEL_INCREASE: &str = "deviceFuelIncrease";
    pub const TYPE_GEOFENCE_ENTER: &str = "geofenceEnter";
    pub const TYPE_GEOFENCE_EXIT: &str = "geofenceExit";
    pub const TYPE_ALARM: &str = "alarm";
    pub const TYPE_IGNITION_ON: &str = "ignitionOn";
    pub const TYPE_IGNITION_OFF: &str = "ignitionOff";
    pub const TYPE_MAINTENANCE: &str = "maintenance";
    pub const TYPE_DRIVER_CHANGED: &str = "driverChanged";
    pub const TYPE_MEDIA: &str = "media";

    pub fn new(event_type: &str, position: &Position) -> Self {
        Self {
            id: 0,
            device_id: position.device_id,
            event_type: event_type.to_string(),
            event_time: position.device_time,
            position_id: position.id,
            geofence_id: 0,
            maintenance_id: 0,
            attributes: default_attributes(),
        }
    }

    pub fn new_for_device(event_type: &str, device_id: i64) -> Self {
        Self {
            id: 0,
            device_id,
            event_type: event_type.to_string(),
            event_time: Utc::now(),
            position_id: 0,
            geofence_id: 0,
            maintenance_id: 0,
            attributes: default_attributes(),
        }
    }
}

// ─── Command ─────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Command {
    #[serde(default)]
    pub id: i64,
    pub device_id: i64,
    #[serde(rename = "type")]
    pub command_type: String,
    pub description: Option<String>,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

impl Command {
    pub const TYPE_CUSTOM: &str = "custom";
    pub const TYPE_IDENTIFICATION: &str = "deviceIdentification";
    pub const TYPE_POSITION_SINGLE: &str = "positionSingle";
    pub const TYPE_POSITION_PERIODIC: &str = "positionPeriodic";
    pub const TYPE_POSITION_STOP: &str = "positionStop";
    pub const TYPE_ENGINE_STOP: &str = "engineStop";
    pub const TYPE_ENGINE_RESUME: &str = "engineResume";
    pub const TYPE_ALARM_ARM: &str = "alarmArm";
    pub const TYPE_ALARM_DISARM: &str = "alarmDisarm";
    pub const TYPE_ALARM_DISMISS: &str = "alarmDismiss";
    pub const TYPE_SET_TIMEZONE: &str = "setTimezone";
    pub const TYPE_REQUEST_PHOTO: &str = "requestPhoto";
    pub const TYPE_POWER_OFF: &str = "powerOff";
    pub const TYPE_REBOOT_DEVICE: &str = "rebootDevice";
    pub const TYPE_FACTORY_RESET: &str = "factoryReset";
    pub const TYPE_SEND_SMS: &str = "sendSms";
    pub const TYPE_SEND_USSD: &str = "sendUssd";
    pub const TYPE_SOS_NUMBER: &str = "sosNumber";
    pub const TYPE_SILENCE_TIME: &str = "silenceTime";
    pub const TYPE_SET_PHONEBOOK: &str = "setPhonebook";
    pub const TYPE_MESSAGE: &str = "message";
    pub const TYPE_VOICE_MESSAGE: &str = "voiceMessage";
    pub const TYPE_OUTPUT_CONTROL: &str = "outputControl";
    pub const TYPE_VOICE_MONITORING: &str = "voiceMonitoring";
    pub const TYPE_SET_AGPS: &str = "setAgps";
    pub const TYPE_SET_INDICATOR: &str = "setIndicator";
    pub const TYPE_CONFIGURATION: &str = "configuration";
    pub const TYPE_GET_VERSION: &str = "getVersion";
    pub const TYPE_FIRMWARE_UPDATE: &str = "firmwareUpdate";
    pub const TYPE_SET_CONNECTION: &str = "setConnection";
    pub const TYPE_SET_ODOMETER: &str = "setOdometer";
    pub const TYPE_GET_MODEM_STATUS: &str = "getModemStatus";
    pub const TYPE_GET_DEVICE_STATUS: &str = "getDeviceStatus";
    pub const TYPE_SET_SPEED_LIMIT: &str = "setSpeedLimit";
    pub const TYPE_MODE_POWER_SAVING: &str = "modePowerSaving";
    pub const TYPE_MODE_DEEP_SLEEP: &str = "modeDeepSleep";
    pub const TYPE_ALARM_GEOFENCE: &str = "alarmGeofence";
    pub const TYPE_ALARM_BATTERY: &str = "alarmBattery";
    pub const TYPE_ALARM_SOS: &str = "alarmSos";
    pub const TYPE_ALARM_REMOVE: &str = "alarmRemove";
    pub const TYPE_ALARM_CLOCK: &str = "alarmClock";
    pub const TYPE_ALARM_SPEED: &str = "alarmSpeed";
    pub const TYPE_ALARM_FALL: &str = "alarmFall";
    pub const TYPE_ALARM_VIBRATION: &str = "alarmVibration";

    pub const KEY_UNIQUE_ID: &str = "uniqueId";
    pub const KEY_FREQUENCY: &str = "frequency";
    pub const KEY_LANGUAGE: &str = "language";
    pub const KEY_TIMEZONE: &str = "timezone";
    pub const KEY_DEVICE_PASSWORD: &str = "devicePassword";
    pub const KEY_RADIUS: &str = "radius";
    pub const KEY_MESSAGE: &str = "message";
    pub const KEY_ENABLE: &str = "enable";
    pub const KEY_DATA: &str = "data";
    pub const KEY_INDEX: &str = "index";
    pub const KEY_PHONE: &str = "phone";
    pub const KEY_SERVER: &str = "server";
    pub const KEY_PORT: &str = "port";
    pub const KEY_NO_QUEUE: &str = "noQueue";

    pub fn get_string(&self, key: &str) -> Option<String> {
        attr_string(&self.attributes, key)
    }
}

// ─── Group ───────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Group {
    #[serde(default)]
    pub id: i64,
    pub name: String,
    pub group_id: Option<i64>,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

// ─── Geofence ────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Geofence {
    #[serde(default)]
    pub id: i64,
    pub name: String,
    pub description: Option<String>,
    pub area: String,
    pub calendar_id: Option<i64>,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

// ─── Notification ────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Notification {
    #[serde(default)]
    pub id: i64,
    #[serde(rename = "type")]
    pub notification_type: String,
    #[serde(default)]
    pub always: bool,
    pub notificators: Option<String>,
    pub calendar_id: Option<i64>,
    pub command_id: Option<i64>,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

// ─── Driver ──────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Driver {
    #[serde(default)]
    pub id: i64,
    pub name: String,
    pub unique_id: String,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

// ─── Maintenance ─────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Maintenance {
    #[serde(default)]
    pub id: i64,
    pub name: String,
    #[serde(rename = "type")]
    pub maintenance_type: String,
    pub start: f64,
    pub period: f64,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

// ─── Calendar ────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Calendar {
    #[serde(default)]
    pub id: i64,
    pub name: String,
    pub data: Option<String>,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

// ─── Server ──────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Server {
    #[serde(default)]
    pub id: i64,
    pub registration: bool,
    pub readonly: bool,
    pub device_readonly: bool,
    pub limit_commands: bool,
    pub disable_reports: bool,
    pub fixed_email: bool,
    pub map: Option<String>,
    pub bingkey: Option<String>,
    pub map_url: Option<String>,
    pub overlay_url: Option<String>,
    #[serde(default)]
    pub latitude: f64,
    #[serde(default)]
    pub longitude: f64,
    #[serde(default)]
    pub zoom: i32,
    pub force_settings: bool,
    pub coordinate_format: Option<String>,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

// ─── Statistics ──────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Statistics {
    #[serde(default)]
    pub id: i64,
    pub capture_time: DateTime<Utc>,
    #[serde(default)]
    pub active_users: i32,
    #[serde(default)]
    pub active_devices: i32,
    #[serde(default)]
    pub requests: i32,
    #[serde(default)]
    pub messages_received: i32,
    #[serde(default)]
    pub messages_stored: i32,
    #[serde(default)]
    pub geocoder_requests: i32,
    #[serde(default)]
    pub geolocation_requests: i32,
    #[serde(default)]
    pub mail_sent: i32,
    #[serde(default)]
    pub sms_sent: i32,
    pub protocol: Option<String>,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

// ─── Permission ──────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Permission {
    pub owner_class: String,
    pub owner_id: i64,
    pub property_class: String,
    pub property_id: i64,
}

// ─── Network / CellTower / WifiAccessPoint ───────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CellTower {
    pub radio_type: Option<String>,
    #[serde(default)]
    pub mobile_country_code: i32,
    #[serde(default)]
    pub mobile_network_code: i32,
    #[serde(default)]
    pub location_area_code: i32,
    #[serde(default)]
    pub cell_id: i64,
    #[serde(default)]
    pub signal_strength: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WifiAccessPoint {
    pub mac_address: String,
    #[serde(default)]
    pub signal_strength: i32,
    pub channel: Option<i32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Network {
    pub cell_towers: Option<Vec<CellTower>>,
    pub wifi_access_points: Option<Vec<WifiAccessPoint>>,
}

// ─── QueuedCommand ───────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueuedCommand {
    #[serde(default)]
    pub id: i64,
    pub device_id: i64,
    pub command_type: String,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

// ─── DeviceAccumulators ──────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DeviceAccumulators {
    pub device_id: i64,
    pub total_distance: Option<f64>,
    pub hours: Option<i64>,
}

// ─── Attribute (computed) ────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Attribute {
    #[serde(default)]
    pub id: i64,
    pub description: String,
    pub attribute: String,
    pub expression: String,
    #[serde(rename = "type")]
    pub attribute_type: String,
}

// ─── Report ──────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Report {
    #[serde(default)]
    pub id: i64,
    #[serde(rename = "type")]
    pub report_type: String,
    pub description: Option<String>,
    pub calendar_id: Option<i64>,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

// ─── Order ───────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Order {
    #[serde(default)]
    pub id: i64,
    pub description: Option<String>,
    pub from_address: Option<String>,
    pub to_address: Option<String>,
    #[serde(default = "default_attributes")]
    pub attributes: Value,
}

// ─── LogRecord ───────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LogRecord {
    pub address: String,
    pub protocol: Option<String>,
    pub device_id: i64,
    pub data: String,
}

// ─── Typed wrapper ───────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Typed {
    #[serde(rename = "type")]
    pub typed_type: String,
}

// ─── Table name mapping ─────────────────────────────────────────────

pub fn table_name_for<T: ?Sized>() -> &'static str {
    let type_name = std::any::type_name::<T>();
    let short = type_name.rsplit("::").next().unwrap_or(type_name);
    match short {
        "Position" => "tc_positions",
        "Device" => "tc_devices",
        "User" => "tc_users",
        "Event" => "tc_events",
        "Command" => "tc_commands",
        "Group" => "tc_groups",
        "Geofence" => "tc_geofences",
        "Notification" => "tc_notifications",
        "Driver" => "tc_drivers",
        "Maintenance" => "tc_maintenances",
        "Calendar" => "tc_calendars",
        "Server" => "tc_servers",
        "Statistics" => "tc_statistics",
        "Attribute" => "tc_attributes",
        "Report" => "tc_reports",
        "Order" => "tc_orders",
        "QueuedCommand" => "tc_commands_queue",
        _ => "tc_unknown",
    }
}
