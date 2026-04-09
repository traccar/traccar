use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct R12wDecoder;

impl R12wDecoder {
    pub const PROTOCOL_NAME: &str = "r12w";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
