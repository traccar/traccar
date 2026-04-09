use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct Minifinder2Decoder;

impl Minifinder2Decoder {
    pub const PROTOCOL_NAME: &str = "minifinder2";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
