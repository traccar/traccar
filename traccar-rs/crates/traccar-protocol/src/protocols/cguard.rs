use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct CguardDecoder;

impl CguardDecoder {
    pub const PROTOCOL_NAME: &str = "cguard";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
