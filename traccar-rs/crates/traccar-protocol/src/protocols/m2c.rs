use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct M2cDecoder;

impl M2cDecoder {
    pub const PROTOCOL_NAME: &str = "m2c";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
