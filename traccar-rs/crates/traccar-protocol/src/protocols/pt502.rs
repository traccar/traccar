use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct Pt502Decoder;

impl Pt502Decoder {
    pub const PROTOCOL_NAME: &str = "pt502";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
