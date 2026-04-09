use bytes::{Bytes, BytesMut};
use chrono::Utc;
use traccar_model::{Command, Position};
use crate::{ProtocolDecoder, ProtocolEncoder, ProtocolError, ProtocolRegistry, ProtocolDefinition, Transport};
use crate::codec::DelimiterFrameDecoder;
use crate::session::DeviceSession;
use async_trait::async_trait;
use traccar_helper::date;

pub struct H02Decoder;

#[async_trait]
impl ProtocolDecoder for H02Decoder {
    async fn decode(
        &self,
        buf: &mut BytesMut,
        session: &mut DeviceSession,
    ) -> Result<Option<Vec<Position>>, ProtocolError> {
        let data = String::from_utf8_lossy(buf).to_string();
        tracing::debug!("H02 received: {}", data);

        // Format: *HQ,IMEI,V1,HHMMSS,A/V,lat,N/S,lon,E/W,speed,course,DDMMYY,...#
        if !data.starts_with('*') {
            return Err(ProtocolError::InvalidData("Invalid H02 header".into()));
        }

        let parts: Vec<&str> = data.trim_matches(|c| c == '*' || c == '#' || c == '\r' || c == '\n')
            .split(',')
            .collect();

        if parts.len() < 12 {
            return Ok(None);
        }

        // parts[0] = "HQ", parts[1] = IMEI
        let imei = parts[1];
        session.unique_id = imei.to_string();

        let msg_type = parts[2];
        if msg_type != "V1" && msg_type != "V4" && msg_type != "NBR" {
            return Ok(None); // Heartbeat or other
        }

        let mut position = Position::new("h02");
        position.device_id = session.device_id;
        position.server_time = Utc::now();

        // Time: HHMMSS
        let time_str = parts[3];
        let date_str = parts.get(11).unwrap_or(&"010100");

        if time_str.len() >= 6 && date_str.len() >= 6 {
            let hour = time_str[0..2].parse::<u32>().unwrap_or(0);
            let min = time_str[2..4].parse::<u32>().unwrap_or(0);
            let sec = time_str[4..6].parse::<u32>().unwrap_or(0);
            let day = date_str[0..2].parse::<u32>().unwrap_or(1);
            let month = date_str[2..4].parse::<u32>().unwrap_or(1);
            let year = date_str[4..6].parse::<i32>().unwrap_or(0);

            if let Some(dt) = date::make_datetime(date::correct_year(year), month, day, hour, min, sec) {
                position.set_time(dt);
            }
        }

        // Valid
        position.valid = parts[4] == "A";

        // Latitude (DDmm.mmmm)
        if let Ok(raw_lat) = parts[5].parse::<f64>() {
            let degrees = (raw_lat / 100.0).floor();
            let minutes = raw_lat - degrees * 100.0;
            position.latitude = degrees + minutes / 60.0;
            if parts.get(6) == Some(&"S") {
                position.latitude = -position.latitude;
            }
        }

        // Longitude (DDDmm.mmmm)
        if let Ok(raw_lon) = parts[7].parse::<f64>() {
            let degrees = (raw_lon / 100.0).floor();
            let minutes = raw_lon - degrees * 100.0;
            position.longitude = degrees + minutes / 60.0;
            if parts.get(8) == Some(&"W") {
                position.longitude = -position.longitude;
            }
        }

        // Speed
        position.speed = parts.get(9).and_then(|s| s.parse().ok()).unwrap_or(0.0);

        // Course
        position.course = parts.get(10).and_then(|s| s.parse().ok()).unwrap_or(0.0);

        Ok(Some(vec![position]))
    }
}

pub struct H02Encoder;

impl ProtocolEncoder for H02Encoder {
    fn encode(&self, _command: &Command, _unique_id: &str) -> Result<Bytes, ProtocolError> {
        Err(ProtocolError::UnsupportedCommand("H02 commands not supported".into()))
    }
}

pub fn register(registry: &mut ProtocolRegistry) {
    registry.register(ProtocolDefinition {
        name: "h02".to_string(),
        transport: Transport::Both,
        default_port: 5013,
        decoder_factory: Box::new(|| Box::new(H02Decoder)),
        encoder_factory: Box::new(|| Box::new(H02Encoder)),
        frame_decoder_factory: Box::new(|| {
            Box::new(DelimiterFrameDecoder::from_strings(2048, false, &["#", "\r\n"]))
        }),
        supported_commands: vec![],
    });
}
