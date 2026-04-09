use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct S168Decoder;

impl S168Decoder {
    pub const PROTOCOL_NAME: &str = "s168";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
