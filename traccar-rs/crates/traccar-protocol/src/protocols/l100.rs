use bytes::BytesMut;
use traccar_model::Position;
use crate::ProtocolError;

pub struct L100Decoder;

impl L100Decoder {
    pub const PROTOCOL_NAME: &str = "l100";

    pub fn new() -> Self {
        Self
    }

    pub fn decode(&self, _buf: &mut BytesMut) -> Result<Vec<Position>, ProtocolError> {
        tracing::debug!("Decoding {} protocol data", Self::PROTOCOL_NAME);
        Ok(vec![])
    }
}
