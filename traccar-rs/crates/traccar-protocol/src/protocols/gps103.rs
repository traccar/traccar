//! GPS103 Protocol (TK103 clone)
//!
//! Text-based, delimiter-framed protocol used by many cheap Chinese GPS trackers.
//!
//! Message format: `imei:XXXXXXXXX,alarm,YYMMDD HH:MM:SS,rfid,F/L,HHMMSS,validity,lat,N/S,lon,E/W,speed,course,...`
//!
//! Java reference: `Gps103ProtocolDecoder.java`

use async_trait::async_trait;
use bytes::{Bytes, BytesMut};
use chrono::Utc;
use traccar_helper::date::{correct_year, make_datetime};
use traccar_helper::parser::Parser;
use traccar_model::position_keys;
use traccar_model::{Command, Position};

use crate::codec::DelimiterFrameDecoder;
use crate::session::DeviceSession;
use crate::{ProtocolDecoder, ProtocolDefinition, ProtocolEncoder, ProtocolError, ProtocolRegistry, ProtocolResult, Transport};

/// Protocol name constant.
pub const PROTOCOL_NAME: &str = "gps103";

/// Default port for GPS103.
pub const DEFAULT_PORT: u16 = 5001;

// ─── Main regex pattern ─────────────────────────────────────────────
// Matches the standard GPS103 location message format.
const PATTERN: &str = concat!(
    r"imei:(\d+),",                       // imei
    r"([^,]*),",                          // alarm
    r"(?:(\d{2})/?\d{2}/?\d{2}\s?",      // date (yy/mm/dd)  -- simplified group
    r"\d{2}:?\d{2}(?:\d{2})?,|",         // time (hh:mm:ss)
    r"\d*,)",                             // or just digits
    r"([^,]*)?,",                         // rfid
    r"(?:L,|",                            // LBS data
    r"F,",                                // GPS data
    r"(\d{2})(\d{2})(\d{2})(?:\.\d+)?,", // UTC time (hhmmss)
    r"([AV]),",                           // validity
    r"([NS],)?",                          // hemisphere (optional)
    r"(\d+)(\d{2}\.\d+),",               // latitude (ddmm.mmmm)
    r"([NS]),?",                          // hemisphere
    r"([EW],)?",                          // hemisphere (optional)
    r"(\d+)(\d{2}\.\d+),",               // longitude (dddmm.mmmm)
    r"([EW])?,?",                         // hemisphere
    r"(\d+\.?\d*)?,?",                    // speed
    r"(\d+\.?\d*)?,?",                    // course
    r"(-?\d+\.?\d*)?",                    // altitude
    r")",
);

// Simplified regex that captures core fields
const PATTERN_SIMPLE: &str = concat!(
    r"imei:(\d+),",                 // 1: imei
    r"([^,]*),",                    // 2: alarm/type
    r"(\d{2})/(\d{2})/(\d{2}) ",   // 3,4,5: date yy/mm/dd
    r"(\d{2}):(\d{2})(?::(\d{2}))?,", // 6,7,8: time hh:mm:ss
    r"[^,]*,",                      // rfid
    r"F,",                          // GPS flag
    r"(\d{2})(\d{2})(\d{2})",       // 9,10,11: UTC time hhmmss
    r"(?:\.\d+)?,",                 // optional decimals
    r"([AV]),",                     // 12: validity
    r"(?:[NS],)?",                  // hemisphere prefix (optional)
    r"(\d+)(\d{2}\.\d+),",         // 13,14: lat degrees + minutes
    r"([NS]),?",                    // 15: hemisphere
    r"(?:[EW],)?",                  // hemisphere prefix (optional)
    r"(\d+)(\d{2}\.\d+),",         // 16,17: lon degrees + minutes
    r"([EW])?,?",                   // 18: hemisphere
    r"(\d+\.?\d*)?,?",              // 19: speed (knots)
    r"(\d+\.?\d*)?,?",              // 20: course
    r"(-?\d+\.?\d*)?",              // 21: altitude
);

// ─── Alarm decoder ──────────────────────────────────────────────────

fn decode_alarm(value: &str) -> Option<&str> {
    if value.starts_with("T:") {
        return Some(traccar_model::alarm::TEMPERATURE);
    }
    if value.starts_with("oil") {
        return Some(traccar_model::alarm::FUEL_LEAK);
    }
    match value {
        "help me" => Some(traccar_model::alarm::SOS),
        "low battery" => Some(traccar_model::alarm::LOW_BATTERY),
        "stockade" => Some(traccar_model::alarm::GEOFENCE),
        "move" => Some(traccar_model::alarm::MOVEMENT),
        "speed" => Some(traccar_model::alarm::OVERSPEED),
        "door alarm" => Some(traccar_model::alarm::DOOR),
        "ac alarm" => Some(traccar_model::alarm::POWER_CUT),
        "accident alarm" => Some(traccar_model::alarm::ACCIDENT),
        "sensor alarm" => Some(traccar_model::alarm::VIBRATION),
        "bonnet alarm" => Some(traccar_model::alarm::BONNET),
        "footbrake alarm" => Some(traccar_model::alarm::FOOT_BRAKE),
        "DTC" => Some(traccar_model::alarm::FAULT),
        _ => None,
    }
}

// ─── Protocol decoder ───────────────────────────────────────────────

pub struct Gps103Decoder;

#[async_trait]
impl ProtocolDecoder for Gps103Decoder {
    async fn decode(
        &self,
        buf: &mut BytesMut,
        session: &mut DeviceSession,
    ) -> ProtocolResult<Option<Vec<Position>>> {
        let sentence = String::from_utf8_lossy(buf).to_string();
        let sentence = sentence.trim();

        if sentence.is_empty() {
            return Ok(None);
        }

        // Handle login/heartbeat messages: short messages with just imei
        if sentence.contains("imei:") && sentence.len() <= 30 {
            // Extract IMEI from login message
            if let Some(parser) = Parser::new(r"imei:(\d+),", sentence) {
                let mut parser = parser;
                if let Some(imei) = parser.next_str() {
                    session.unique_id = imei;
                    // In a real system, we would look up device_id from storage
                    // For now, use a hash-based device_id
                    session.device_id = simple_device_id(&session.unique_id);
                }
            }
            return Ok(None);
        }

        // Handle numeric prefix (some devices send "012345imei:...")
        let sentence = if !sentence.is_empty() && sentence.as_bytes()[0].is_ascii_digit() {
            if let Some(start) = sentence.find("imei:") {
                &sentence[start..]
            } else {
                return Ok(None);
            }
        } else {
            sentence
        };

        // Try to parse the standard location message
        if let Some(position) = self.decode_regular(sentence, session)? {
            return Ok(Some(vec![position]));
        }

        Ok(None)
    }
}

impl Gps103Decoder {
    fn decode_regular(
        &self,
        sentence: &str,
        session: &mut DeviceSession,
    ) -> ProtocolResult<Option<Position>> {
        let mut parser = match Parser::new(PATTERN_SIMPLE, sentence) {
            Some(p) => p,
            None => return Ok(None),
        };

        // 1: IMEI
        let imei = parser.next_str().unwrap_or_default();
        if !imei.is_empty() {
            session.unique_id = imei;
            session.device_id = simple_device_id(&session.unique_id);
        }

        if session.device_id == 0 {
            return Err(ProtocolError::UnknownDevice(session.unique_id.clone()));
        }

        let mut position = Position::new(PROTOCOL_NAME);
        position.device_id = session.device_id;

        // 2: Alarm
        let alarm_str = parser.next_str().unwrap_or_default();
        if let Some(alarm) = decode_alarm(&alarm_str) {
            position.add_alarm(alarm);
        }

        // Handle special alarm types
        if alarm_str == "acc on" {
            position.set(position_keys::KEY_IGNITION, true);
        } else if alarm_str == "acc off" {
            position.set(position_keys::KEY_IGNITION, false);
        } else if alarm_str.starts_with("T:") {
            if let Ok(temp) = alarm_str[2..].parse::<f64>() {
                position.set(&format!("{}1", position_keys::PREFIX_TEMP), temp);
            }
        } else if alarm_str.starts_with("oil ") {
            if let Ok(fuel) = alarm_str[4..].parse::<f64>() {
                position.set(position_keys::KEY_FUEL, fuel);
            }
        } else if !position.has_attribute(position_keys::KEY_ALARM) && alarm_str != "tracker" {
            position.set(position_keys::KEY_EVENT, alarm_str.clone());
        }

        // 3,4,5: Date YY/MM/DD
        let year = correct_year(parser.next_int(0) as i32);
        let month = parser.next_int(0) as u32;
        let day = parser.next_int(0) as u32;

        // 6,7,8: Local time HH:MM:SS
        let _local_hour = parser.next_int(0) as u32;
        let _local_min = parser.next_int(0) as u32;
        let _local_sec = parser.next_int(0) as u32;

        // 9,10,11: UTC time HHMMSS
        let utc_hour = parser.next_int(0) as u32;
        let utc_min = parser.next_int(0) as u32;
        let utc_sec = parser.next_int(0) as u32;

        if let Some(dt) = make_datetime(year, month, day, utc_hour, utc_min, utc_sec) {
            position.set_time(dt);
        }

        // 12: Validity
        let valid = parser.next_str().map_or(false, |v| v == "A");
        position.valid = valid;

        // 13,14: Latitude degrees + minutes
        let lat_deg = parser.next_double(0.0);
        let lat_min = parser.next_double(0.0);
        let mut latitude = lat_deg + lat_min / 60.0;

        // 15: N/S hemisphere
        if let Some(h) = parser.next_str() {
            if h == "S" {
                latitude = -latitude;
            }
        }

        // 16,17: Longitude degrees + minutes
        let lon_deg = parser.next_double(0.0);
        let lon_min = parser.next_double(0.0);
        let mut longitude = lon_deg + lon_min / 60.0;

        // 18: E/W hemisphere
        if let Some(h) = parser.next_str() {
            if h == "W" {
                longitude = -longitude;
            }
        }

        position.latitude = latitude;
        position.longitude = longitude;

        // 19: Speed (knots)
        position.speed = parser.next_double(0.0);

        // 20: Course
        position.course = parser.next_double(0.0);

        // 21: Altitude
        position.altitude = parser.next_double(0.0);

        Ok(Some(position))
    }
}

// ─── Protocol encoder ───────────────────────────────────────────────

pub struct Gps103Encoder;

impl ProtocolEncoder for Gps103Encoder {
    fn encode(&self, command: &Command, unique_id: &str) -> ProtocolResult<Bytes> {
        let msg = match command.command_type.as_str() {
            Command::TYPE_CUSTOM => {
                let data = command
                    .get_string(Command::KEY_DATA)
                    .unwrap_or_default();
                format!("**,imei:{},{}", unique_id, data)
            }
            Command::TYPE_POSITION_SINGLE => {
                format!("**,imei:{},B", unique_id)
            }
            Command::TYPE_POSITION_STOP => {
                format!("**,imei:{},D", unique_id)
            }
            Command::TYPE_POSITION_PERIODIC => {
                let freq = command
                    .get_string(Command::KEY_FREQUENCY)
                    .and_then(|s| s.parse::<i64>().ok())
                    .unwrap_or(60);
                let freq_str = format_frequency(freq);
                format!("**,imei:{},C,{}", unique_id, freq_str)
            }
            Command::TYPE_ENGINE_STOP => {
                format!("**,imei:{},J", unique_id)
            }
            Command::TYPE_ENGINE_RESUME => {
                format!("**,imei:{},K", unique_id)
            }
            Command::TYPE_ALARM_ARM => {
                format!("**,imei:{},L", unique_id)
            }
            Command::TYPE_ALARM_DISARM => {
                format!("**,imei:{},M", unique_id)
            }
            Command::TYPE_REQUEST_PHOTO => {
                format!("**,imei:{},160", unique_id)
            }
            other => {
                return Err(ProtocolError::UnsupportedCommand(other.to_string()));
            }
        };

        Ok(Bytes::from(msg))
    }

    fn supported_commands(&self) -> &[&str] {
        &[
            Command::TYPE_CUSTOM,
            Command::TYPE_POSITION_SINGLE,
            Command::TYPE_POSITION_PERIODIC,
            Command::TYPE_POSITION_STOP,
            Command::TYPE_ENGINE_STOP,
            Command::TYPE_ENGINE_RESUME,
            Command::TYPE_ALARM_ARM,
            Command::TYPE_ALARM_DISARM,
            Command::TYPE_REQUEST_PHOTO,
        ]
    }
}

fn format_frequency(seconds: i64) -> String {
    if seconds / 3600 > 0 {
        format!("{:02}h", seconds / 3600)
    } else if seconds / 60 > 0 {
        format!("{:02}m", seconds / 60)
    } else {
        format!("{:02}s", seconds)
    }
}

// ─── Registration ───────────────────────────────────────────────────

pub fn register(registry: &mut ProtocolRegistry) {
    let encoder = Gps103Encoder;
    let supported = encoder
        .supported_commands()
        .iter()
        .map(|s| s.to_string())
        .collect();

    registry.register(ProtocolDefinition {
        name: PROTOCOL_NAME.to_string(),
        transport: Transport::Both,
        default_port: DEFAULT_PORT,
        decoder_factory: Box::new(|| Box::new(Gps103Decoder)),
        encoder_factory: Box::new(|| Box::new(Gps103Encoder)),
        frame_decoder_factory: Box::new(|| {
            Box::new(DelimiterFrameDecoder::from_strings(
                2048,
                false,
                &["\r\n", "\n", ";", "*"],
            ))
        }),
        supported_commands: supported,
    });
}

/// Simple hash-based device ID for development/testing.
/// In production this would do a database lookup.
fn simple_device_id(unique_id: &str) -> i64 {
    use std::hash::{Hash, Hasher};
    let mut hasher = std::collections::hash_map::DefaultHasher::new();
    unique_id.hash(&mut hasher);
    (hasher.finish() & 0x7FFFFFFFFFFFFFFF) as i64
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_decode_regular_message() {
        let decoder = Gps103Decoder;
        let mut session = DeviceSession::unauthenticated(PROTOCOL_NAME);

        let msg = "imei:359586015829802,tracker,24/05/15 10:30:45,,F,103044,A,22.5430,N,114.0580,E,10.5,270.0,100.0";
        let mut buf = BytesMut::from(msg);

        let result = decoder.decode(&mut buf, &mut session).await;
        assert!(result.is_ok());
        let positions = result.unwrap();
        assert!(positions.is_some());
        let positions = positions.unwrap();
        assert_eq!(positions.len(), 1);

        let pos = &positions[0];
        assert_eq!(pos.protocol, PROTOCOL_NAME);
        assert!(pos.valid);
        assert!((pos.latitude - 22.5430).abs() < 0.1);
        assert!((pos.longitude - 114.0580).abs() < 0.1);
    }

    #[tokio::test]
    async fn test_decode_login_message() {
        let decoder = Gps103Decoder;
        let mut session = DeviceSession::unauthenticated(PROTOCOL_NAME);

        let msg = "imei:359586015829802,";
        let mut buf = BytesMut::from(msg);

        let result = decoder.decode(&mut buf, &mut session).await;
        assert!(result.is_ok());
        assert!(result.unwrap().is_none());
        assert_eq!(session.unique_id, "359586015829802");
    }

    #[test]
    fn test_encode_engine_stop() {
        let encoder = Gps103Encoder;
        let command = Command {
            id: 0,
            device_id: 1,
            command_type: Command::TYPE_ENGINE_STOP.to_string(),
            description: None,
            attributes: serde_json::Value::Object(serde_json::Map::new()),
        };

        let result = encoder.encode(&command, "359586015829802").unwrap();
        assert_eq!(&result[..], b"**,imei:359586015829802,J");
    }

    #[test]
    fn test_format_frequency() {
        assert_eq!(format_frequency(7200), "02h");
        assert_eq!(format_frequency(300), "05m");
        assert_eq!(format_frequency(30), "30s");
    }

    #[test]
    fn test_decode_alarm() {
        assert_eq!(decode_alarm("help me"), Some(traccar_model::alarm::SOS));
        assert_eq!(decode_alarm("low battery"), Some(traccar_model::alarm::LOW_BATTERY));
        assert_eq!(decode_alarm("tracker"), None);
    }
}
