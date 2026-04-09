use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct PolteDecoder;

impl PolteDecoder {
    pub const PROTOCOL_NAME: &str = "polte";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
