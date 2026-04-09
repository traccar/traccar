use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct M2mDecoder;

impl M2mDecoder {
    pub const PROTOCOL_NAME: &str = "m2m";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
