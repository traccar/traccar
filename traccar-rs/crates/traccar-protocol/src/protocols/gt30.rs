use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct Gt30Decoder;

impl Gt30Decoder {
    pub const PROTOCOL_NAME: &str = "gt30";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
