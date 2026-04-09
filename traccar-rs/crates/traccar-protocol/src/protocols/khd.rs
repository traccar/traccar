use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct KhdDecoder;

impl KhdDecoder {
    pub const PROTOCOL_NAME: &str = "khd";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
