use bytes::{Bytes, BytesMut};
use chrono::Utc;
use traccar_model::{position_keys, Command, Position};
use crate::{ProtocolDecoder, ProtocolEncoder, ProtocolError, ProtocolRegistry, ProtocolDefinition, Transport};
use crate::codec::LineBasedFrameDecoder;
use crate::session::DeviceSession;
use async_trait::async_trait;
use traccar_helper::date;
use std::collections::HashMap;

/// OsmAnd protocol: HTTP-based.
/// Devices send GET/POST requests with URL parameters.
/// Example: /?id=DEVICE_ID&timestamp=1234567890&lat=12.345&lon=67.890&speed=10&bearing=90&altitude=100
pub struct OsmAndDecoder;

#[async_trait]
impl ProtocolDecoder for OsmAndDecoder {
    async fn decode(
        &self,
        buf: &mut BytesMut,
        session: &mut DeviceSession,
    ) -> Result<Option<Vec<Position>>, ProtocolError> {
        let data = String::from_utf8_lossy(buf).to_string();
        tracing::debug!("OsmAnd received: {}", data);

        // Parse as HTTP request line or URL params
        let query_string = if data.starts_with("GET ") || data.starts_with("POST ") {
            // Extract path and query
            data.split_whitespace()
                .nth(1)
                .and_then(|path| path.split('?').nth(1))
                .unwrap_or("")
                .to_string()
        } else {
            data.clone()
        };

        let params: HashMap<String, String> = query_string
            .split('&')
            .filter_map(|pair| {
                let mut parts = pair.splitn(2, '=');
                match (parts.next(), parts.next()) {
                    (Some(k), Some(v)) => Some((k.to_string(), v.to_string())),
                    _ => None,
                }
            })
            .collect();

        let device_id = params.get("id").cloned().unwrap_or_default();
        if device_id.is_empty() {
            return Ok(None);
        }

        session.unique_id = device_id;

        let mut position = Position::new("osmand");
        position.device_id = session.device_id;
        position.server_time = Utc::now();
        position.valid = true;

        // Timestamp
        if let Some(ts) = params.get("timestamp") {
            if let Ok(secs) = ts.parse::<i64>() {
                position.set_time(date::from_unix_secs(secs));
            }
        }

        // Coordinates
        position.latitude = params.get("lat").and_then(|s| s.parse().ok()).unwrap_or(0.0);
        position.longitude = params.get("lon").and_then(|s| s.parse().ok()).unwrap_or(0.0);

        // Speed (m/s -> knots)
        if let Some(speed) = params.get("speed").and_then(|s| s.parse::<f64>().ok()) {
            position.speed = traccar_helper::knots_from_mps(speed);
        }

        // Bearing
        position.course = params.get("bearing").and_then(|s| s.parse().ok()).unwrap_or(0.0);

        // Altitude
        position.altitude = params.get("altitude").and_then(|s| s.parse().ok()).unwrap_or(0.0);

        // Accuracy
        position.accuracy = params.get("accuracy").and_then(|s| s.parse().ok()).unwrap_or(0.0);

        // Battery
        if let Some(batt) = params.get("batt").and_then(|s| s.parse::<f64>().ok()) {
            position.set(position_keys::KEY_BATTERY_LEVEL, batt);
        }

        // HDOP
        if let Some(hdop) = params.get("hdop").and_then(|s| s.parse::<f64>().ok()) {
            position.set(position_keys::KEY_HDOP, hdop);
        }

        Ok(Some(vec![position]))
    }
}

pub struct OsmAndEncoder;

impl ProtocolEncoder for OsmAndEncoder {
    fn encode(&self, _command: &Command, _unique_id: &str) -> Result<Bytes, ProtocolError> {
        Err(ProtocolError::UnsupportedCommand("OsmAnd is HTTP-only, no commands".into()))
    }
}

pub fn register(registry: &mut ProtocolRegistry) {
    registry.register(ProtocolDefinition {
        name: "osmand".to_string(),
        transport: Transport::Tcp,
        default_port: 5055,
        decoder_factory: Box::new(|| Box::new(OsmAndDecoder)),
        encoder_factory: Box::new(|| Box::new(OsmAndEncoder)),
        frame_decoder_factory: Box::new(|| Box::new(LineBasedFrameDecoder::new(4096))),
        supported_commands: vec![],
    });
}
