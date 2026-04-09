use bytes::{Buf, BufMut, Bytes, BytesMut};
use chrono::Utc;
use traccar_model::{Command, Position};
use crate::{ProtocolDecoder, ProtocolEncoder, ProtocolError, ProtocolRegistry, ProtocolDefinition, Transport};
use crate::codec::LengthFieldFrameDecoder;
use crate::session::DeviceSession;
use async_trait::async_trait;
use traccar_helper::date;

const MSG_LOGIN: u8 = 0x01;
const MSG_GPS: u8 = 0x12;
const MSG_GPS_LBS: u8 = 0x22;
const MSG_STATUS: u8 = 0x13;
const MSG_HEARTBEAT: u8 = 0x23;
const MSG_ALARM: u8 = 0x26;

pub struct Gt06Decoder;

#[async_trait]
impl ProtocolDecoder for Gt06Decoder {
    async fn decode(
        &self,
        buf: &mut BytesMut,
        session: &mut DeviceSession,
    ) -> Result<Option<Vec<Position>>, ProtocolError> {
        if buf.len() < 5 {
            return Ok(None);
        }

        // Check header: 0x78 0x78
        if buf[0] != 0x78 || buf[1] != 0x78 {
            // Extended header: 0x79 0x79
            if buf[0] != 0x79 || buf[1] != 0x79 {
                return Err(ProtocolError::InvalidData("Invalid GT06 header".into()));
            }
        }

        let mut reader = buf.clone();
        reader.advance(2); // skip header

        let length = if buf[0] == 0x78 {
            reader.get_u8() as usize
        } else {
            reader.get_u16() as usize
        };

        if reader.remaining() < length {
            return Ok(None);
        }

        let protocol_number = reader.get_u8();

        match protocol_number {
            MSG_LOGIN => {
                // IMEI is next 8 bytes as BCD
                if reader.remaining() >= 8 {
                    let mut imei_bytes = [0u8; 8];
                    reader.copy_to_slice(&mut imei_bytes);
                    let imei = traccar_helper::parser::bcd_to_string(&imei_bytes);
                    let imei = imei.trim_start_matches('0').to_string();
                    session.unique_id = imei;
                    tracing::info!("GT06 login: {}", session.unique_id);
                }
                Ok(None)
            }
            MSG_GPS | MSG_GPS_LBS | MSG_ALARM => {
                let mut position = Position::new("gt06");
                position.device_id = session.device_id;
                position.server_time = Utc::now();

                // Date/Time: 6 bytes (YY MM DD HH MM SS)
                if reader.remaining() >= 6 {
                    let year = reader.get_u8() as i32;
                    let month = reader.get_u8() as u32;
                    let day = reader.get_u8() as u32;
                    let hour = reader.get_u8() as u32;
                    let min = reader.get_u8() as u32;
                    let sec = reader.get_u8() as u32;

                    if let Some(dt) = date::make_datetime(date::correct_year(year), month, day, hour, min, sec) {
                        position.set_time(dt);
                    }
                }

                // GPS info length and satellites
                if reader.remaining() >= 1 {
                    let gps_info = reader.get_u8();
                    let _gps_length = (gps_info >> 4) & 0x0F;
                    let satellites = gps_info & 0x0F;
                    position.set("sat", satellites as i64);
                }

                // Latitude (4 bytes, raw / 30000 / 60)
                if reader.remaining() >= 4 {
                    let raw_lat = reader.get_u32();
                    position.latitude = raw_lat as f64 / 30000.0 / 60.0;
                }

                // Longitude (4 bytes)
                if reader.remaining() >= 4 {
                    let raw_lon = reader.get_u32();
                    position.longitude = raw_lon as f64 / 30000.0 / 60.0;
                }

                // Speed
                if reader.remaining() >= 1 {
                    position.speed = reader.get_u8() as f64;
                }

                // Course and flags (2 bytes)
                if reader.remaining() >= 2 {
                    let course_status = reader.get_u16();
                    position.course = (course_status & 0x03FF) as f64;
                    position.valid = (course_status & 0x1000) != 0;

                    if (course_status & 0x0400) == 0 {
                        position.latitude = -position.latitude;
                    }
                    if (course_status & 0x0800) != 0 {
                        position.longitude = -position.longitude;
                    }
                }

                Ok(Some(vec![position]))
            }
            MSG_HEARTBEAT | MSG_STATUS => {
                tracing::debug!("GT06 heartbeat/status received");
                Ok(None)
            }
            _ => {
                tracing::debug!("GT06 unknown message type: 0x{:02X}", protocol_number);
                Ok(None)
            }
        }
    }
}

pub struct Gt06Encoder;

impl ProtocolEncoder for Gt06Encoder {
    fn encode(&self, command: &Command, _unique_id: &str) -> Result<Bytes, ProtocolError> {
        let mut buf = BytesMut::new();
        buf.put_u16(0x7878); // header
        buf.put_u8(0); // length placeholder
        buf.put_u8(0x80); // protocol number for server commands
        let data = command.get_string("data").unwrap_or_default();
        buf.extend_from_slice(data.as_bytes());
        let len = buf.len() - 3; // length excludes header and length byte
        buf[2] = len as u8;
        buf.put_u16(0x0D0A); // tail
        Ok(buf.freeze())
    }
}

pub fn register(registry: &mut ProtocolRegistry) {
    registry.register(ProtocolDefinition {
        name: "gt06".to_string(),
        transport: Transport::Tcp,
        default_port: 5023,
        decoder_factory: Box::new(|| Box::new(Gt06Decoder)),
        encoder_factory: Box::new(|| Box::new(Gt06Encoder)),
        frame_decoder_factory: Box::new(|| {
            Box::new(LengthFieldFrameDecoder::new(1024, 2, 1, 2, 0))
        }),
        supported_commands: vec!["custom".into(), "engineStop".into(), "engineResume".into()],
    });
}
