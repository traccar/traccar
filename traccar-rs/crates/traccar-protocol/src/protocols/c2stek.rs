use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct C2stekDecoder;

impl C2stekDecoder {
    pub const PROTOCOL_NAME: &str = "c2stek";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
